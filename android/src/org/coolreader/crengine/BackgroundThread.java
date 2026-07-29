/*
 * CoolReader for Android
 * Copyright (C) 2010-2012,2020 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2018,2020 Aleksey Chernov <valexlin@gmail.com>
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.coolreader.crengine;

import java.util.concurrent.Callable;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Allows running tasks either in background thread or in GUI thread.
 */
public class BackgroundThread extends Thread {
	
	private final static Object LOCK = new Object(); 

	private static volatile BackgroundThread instance;
	
	// singleton
	public static BackgroundThread instance()
	{
		BackgroundThread current = instance;
		if (current == null) {
			synchronized( LOCK ) {
				current = instance;
				if (current == null) {
					current = new BackgroundThread();
					instance = current;
					current.start();
				}
			}
		}
		return current;
	}
	
	public static Handler getBackgroundHandler() {
		BackgroundThread current = instance;
		return current == null ? null : current.handler;
	}

	public static Handler getGUIHandler() {
		return instance().guiHandler;
	}

	public final static boolean CHECK_THREAD_CONTEXT = true; 

	/**
	 * Throws exception if not in background thread.
	 */
	public final static void ensureBackground()
	{
		if ( CHECK_THREAD_CONTEXT && !isBackgroundThread() ) {
			L.e("not in background thread", new Exception("ensureInBackgroundThread() is failed"));
			throw new RuntimeException("ensureInBackgroundThread() is failed");
		}
	}
	
	/**
	 * Throws exception if not in GUI thread.
	 */
	public final static void ensureGUI()
	{
		if ( CHECK_THREAD_CONTEXT && isBackgroundThread() ) {
			L.e("not in GUI thread", new Exception("ensureGUI() is failed"));
			throw new RuntimeException("ensureGUI() is failed");
		}
	}
	
	// 
	private volatile Handler handler;
	private final DeferredTaskQueue<Runnable> backgroundTasks =
			new DeferredTaskQueue<>();
	private volatile Handler guiHandler;
	private final DeferredTaskQueue<Runnable> guiTasks =
			new DeferredTaskQueue<>();

	/**
	 * Set view to post GUI tasks to.
	 * @param guiTarget is view to post GUI tasks to.
	 */
	public void setGUIHandler(Handler guiHandler) {
		this.guiHandler = guiHandler;
		int delivered = guiTasks.attach(
				guiHandler == null
						? null
						: (task, delay) -> delay > 0
								? guiHandler.postDelayed(task, delay)
								: guiHandler.post(task));
		if (delivered > 0)
			L.d("Engine.setGUI: " + delivered + " queued tasks delivered");
	}

	/**
	 * Create background thread executor.
	 */
	private BackgroundThread() {
		super();
		setName("BackgroundThread" + Integer.toHexString(hashCode()));
		Log.i("cr3", "Created new background thread instance");
	}

	@Override
	public void run() {
		Log.i("cr3", "Entering background thread");
		Looper.prepare();
		Handler currentHandler = new Handler() {
			public void handleMessage( Message message )
			{
				Log.d("cr3", "message: " + message);
			}
		};
		handler = currentHandler;
		Log.i("cr3", "Background thread handler is created");
		backgroundTasks.attach((task, delay) -> {
			if (delay > 0) {
				Log.i("cr3", "Copying posted bg task to handler : " + task);
				return currentHandler.postDelayed(task, delay);
			}
			return currentHandler.post(task);
		});
		try {
			Looper.loop();
		} finally {
			backgroundTasks.attach(null);
			handler = null;
			synchronized (LOCK) {
				if (instance == this)
					instance = null;
			}
			Log.i("cr3", "Exiting background thread");
		}
	}

	//private final static boolean USE_LOCK = false;
	private Runnable guard( final Runnable r )
	{
		return r;
//		if ( !USE_LOCK )
//			return r;
//		return new Runnable() {
//			public void run() {
//				synchronized (LOCK) {
//					r.run();
//				}
//			}
//		};
	}

	/**
	 * Post runnable to be executed in background thread.
	 * @param task is runnable to execute in background thread.
	 */
	public void postBackground( Runnable task )
	{
		postBackground(task, 0);
	}

	/**
	 * Post runnable to be executed in background thread.
	 * @param task is runnable to execute in background thread.
	 * @param delay is delay before running task, in millis
	 */
	public void postBackground( Runnable task, long delay )
	{
		Engine.suspendLongOperation();
		task = guard(task);
		if (delay > 0) {
			Runnable finalTask = task;
			task = () -> {
				try {
					finalTask.run();
				} catch (Throwable e) {
					Log.e("cr3", "Exception while processing task in Background thread: " + finalTask, e);
				}
			};
		}
		if (!backgroundTasks.post(task, delay))
			L.i("Queued task until background handler is ready: " + task);
	}

	/**
	 * Post runnable to be executed in GUI thread
	 * @param task is runnable to execute in GUI thread
	 */
	public void postGUI( Runnable task )
	{
		postGUI(task, 0);
	}

	/**
	 * Post runnable to be executed in GUI thread
	 * @param task is runnable to execute in GUI thread
	 * @param delay is delay before running task, in millis
	 */
	public void postGUI(final Runnable task, final long delay)
	{
		try {
			Runnable postedTask = task;
			if (delay > 0) {
				postedTask = () -> {
					try {
						task.run();
					} catch (Throwable e) {
						Log.e("cr3", "Exception while processing task in GUI thread: " + task, e);
					}
				};
			}
			guiTasks.post(postedTask, delay);
		} catch (Throwable e) {
			Log.e("cr3", "Exception while posting task to GUI thread: " + task, e);
		}
	}

	/**
	 * Run task instantly if called from the same thread, or post it through message queue otherwise.
	 * @param task is task to execute
	 */
	public void executeBackground( Runnable task )
	{
		Engine.suspendLongOperation();
		task = guard(task);
		if (isBackgroundThread())
			task.run(); // run in this thread
		else 
			postBackground(task); // post
	}

	// assume there are NOT only two threads: main GUI, this background and other thread (Synchronizer, etc)
	public static boolean isGUIThread()
	{
		//return !isBackgroundThread();
		return Thread.currentThread() == Looper.getMainLooper().getThread();
	}

	public static boolean isBackgroundThread()
	{
		return (Thread.currentThread() == instance);
	}

	public void executeGUI( final Runnable task )
	{
		//Handler guiHandler = guiTarget.getHandler();
		//if ( guiHandler!=null && guiHandler.getLooper().getThread()==Thread.currentThread() )
		try {
			if (isGUIThread())
				task.run(); // run in this thread
			else {
				postGUI(() -> {
					try {
						task.run();
					} catch (Throwable e) {
						Log.e("cr3",
								"Exception while executing task in GUI thread: " + task, e);
					}
				});
			}
		} catch (Throwable e) {
			Log.e("cr3", "Exception in executeGUI: " + task, e);
		}
	}

    public <T> Callable<T> guard( final Callable<T> task )
    {
    	return new Callable<T>() {
    		public T call() throws Exception {
    			return task.call();
    		}
    	};
    }
    
    
    /**
     * Waits until all pending background tasks are executed.
     */
    public void syncWithBackground() {
    	callBackground((Callable<Integer>) () -> null);
    }
	
    public <T> T callBackground( final Callable<T> srcTask )
    {
    	final Callable<T> task = srcTask; //guard(srcTask);
    	if ( isBackgroundThread() ) {
    		try {
    			return task.call();
    		} catch ( Exception e ) {
    			return null;
    		}
    	}
    	//L.d("executeSync called");
    	if(DBG) L.d("callBackground : posting Background task " + Thread.currentThread().getName());
		final BlockingResult<T> result = new BlockingResult<>();
    	postBackground(() -> {
			if(DBG) L.d("callBackground : inside background thread " + Thread.currentThread().getName());
			try {
				result.complete(task.call());
			} catch ( Exception e ) {
				result.complete(null);
			}
		});
    	if(DBG) L.d("callBackground : calling get " + Thread.currentThread().getName());
		T res = result.await();
    	if(DBG) L.d("callBackground : returned from get " + Thread.currentThread().getName());
    	//L.d("executeSync done");
    	return res;
    }
	
    private final static boolean DBG = false; 
    
    public <T> T callGUI( final Callable<T> task )
    {
    	if ( isGUIThread() ) {
    		try {
    			return task.call();
    		} catch ( Exception e ) {
    			return null;
    		}
    	}
    	if(DBG) L.d("callGUI : posting GUI task " + Thread.currentThread().getName());
		final BlockingResult<T> result = new BlockingResult<>();
    	postGUI(() -> {
			if(DBG) L.d("callGUI : inside GUI thread " + Thread.currentThread().getName());
			T value = null;
			try {
				L.d("callGUI : calling source callable " + Thread.currentThread().getName());
				value = task.call();
			} catch ( Exception e ) {
				if(DBG) L.e("exception in postGUI", e);
			}
			if(DBG) L.d("callGUI : calling result.complete " + Thread.currentThread().getName());
			result.complete(value);
			if(DBG) L.d("callGUI : returned from result.complete " + Thread.currentThread().getName());
		});
    	if(DBG) L.d("callGUI : calling get " + Thread.currentThread().getName());
		T res = result.await();
    	if(DBG) L.d("callGUI : returned from get " + Thread.currentThread().getName());
    	return res;
    }
	
	public void waitForBackgroundCompletion() {
		Engine.suspendLongOperation();
		callBackground(() -> null);
	}
	
	public void quit() {
		postBackground(() -> {
			if (handler != null) {
				L.i("Calling quit() on background thread looper.");
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
					handler.getLooper().quitSafely();
				else
					handler.getLooper().quit();
			}
		});
	}
}
