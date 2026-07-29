/*
 * CoolReader for Android
 * Copyright (C) 2021 Aleksey Chernov <valexlin@gmail.com>
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

// Based on https://stackoverflow.com/a/12795551 for details

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
 * click is fired immediately, next one after the initialInterval, and subsequent
 * ones after the normalInterval.
 *
 * <p>Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks. Can be rewritten to
 * achieve this.
 */
public class RepeatOnTouchListener
		implements OnTouchListener, View.OnAttachStateChangeListener {

	private final Handler handler =
			new Handler(Looper.getMainLooper());
	private final ReplaceableTaskSlot repeatTasks =
			new ReplaceableTaskSlot();
	private final int initialInterval;
	private final int normalInterval;
	private final OnClickListener clickListener;
	private View pressedView;

	/**
	 * @param initialInterval The interval after first click event
	 * @param normalInterval  The interval after second and subsequent click
	 *                        events
	 * @param clickListener   The OnClickListener, that will be called
	 *                        periodically
	 */
	public RepeatOnTouchListener(int initialInterval, int normalInterval,
								 OnClickListener clickListener) {
		if (clickListener == null)
			throw new IllegalArgumentException("null runnable");
		if (initialInterval < 0 || normalInterval < 0)
			throw new IllegalArgumentException("negative interval");

		this.initialInterval = initialInterval;
		this.normalInterval = normalInterval;
		this.clickListener = clickListener;
	}

	public boolean onTouch(View view, MotionEvent motionEvent) {
		switch (motionEvent.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				stopRepeating();
				pressedView = view;
				view.addOnAttachStateChangeListener(this);
				view.setPressed(true);
				clickListener.onClick(view);
				if (pressedView == view && view.isEnabled())
					scheduleRepeat(view, initialInterval);
				else
					stopRepeating();
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				stopRepeating();
				return true;
		}
		return false;
	}

	private void scheduleRepeat(View view, int delayMillis) {
		ReplaceableTaskSlot.Replacement replacement =
				repeatTasks.replace(() -> repeat(view));
		if (replacement.previous() != null)
			handler.removeCallbacks(replacement.previous());
		handler.postDelayed(replacement.current(), delayMillis);
	}

	private void repeat(View view) {
		if (pressedView != view || !view.isEnabled()) {
			stopRepeating();
			return;
		}
		clickListener.onClick(view);
		if (pressedView == view && view.isEnabled())
			scheduleRepeat(view, normalInterval);
		else
			stopRepeating();
	}

	private void stopRepeating() {
		Runnable pending = repeatTasks.cancel();
		if (pending != null)
			handler.removeCallbacks(pending);
		View view = pressedView;
		pressedView = null;
		if (view != null) {
			view.removeOnAttachStateChangeListener(this);
			view.setPressed(false);
		}
	}

	@Override
	public void onViewAttachedToWindow(View view) {
	}

	@Override
	public void onViewDetachedFromWindow(View view) {
		if (pressedView == view)
			stopRepeating();
	}
}
