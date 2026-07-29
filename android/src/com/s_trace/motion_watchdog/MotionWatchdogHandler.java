/*
 * CoolReader for Android
 * Copyright (C) 2018 S-trace <S-trace@list.ru>
 * Copyright (C) 2018,2022 Aleksey Chernov <valexlin@gmail.com>
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

package com.s_trace.motion_watchdog;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import org.coolreader.CoolReader;
import org.coolreader.crengine.BackgroundThread;
import org.coolreader.crengine.Log;
import org.coolreader.crengine.TTSToolbarDlg;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by s-trace on 13.03.18.
 * Handler for motion events
 */

@TargetApi(Build.VERSION_CODES.ECLAIR)
public final class MotionWatchdogHandler extends Handler
        implements SensorEventListener {
    private static final int MSG_MOTION_DETECTED  = 0;
    private static final int MSG_MOTION_TIMEOUT   = 1;
    private static final int MSG_HANDLE_STOP_STEP = 2;
    private static final String TAG = MotionWatchdogHandler.class.getSimpleName();
    private static final long STEP_TIME = 5 * 1000; // 5 seconds
    private final SensorManager mSensorManager;
    private final CoolReader mCoolReader;
    private final TTSToolbarDlg mTTSToolbarDlg;
    private final HandlerThread mHandlerThread;
    private final AtomicBoolean mClosing = new AtomicBoolean();
    private boolean mIsStopping;
    private AudioManager mAudioService;
    private MotionWatchdogFadeState mFadeState;
    private static final double MOTION_THRESHOLD = 1;
    private final double[] mLastValues = new double[3];
    private final double[] mDelta = new double[3];
    private final int mTimeout;

    public MotionWatchdogHandler(TTSToolbarDlg ttsToolbarDlg, CoolReader coolReader,
                                 HandlerThread handlerThread, int timeout) {
        super(handlerThread.getLooper());
        mHandlerThread = handlerThread;
        mCoolReader = coolReader;
        mTTSToolbarDlg = ttsToolbarDlg;
        mSensorManager = (SensorManager) mCoolReader.getSystemService(Context.SENSOR_SERVICE);
        mTimeout = timeout;

        // Force first sensor event to always fire MSG_MOTION_DETECTED
        mLastValues[0] = MOTION_THRESHOLD + 1000;
        mLastValues[1] = MOTION_THRESHOLD + 1000;
        mLastValues[2] = MOTION_THRESHOLD + 1000;
        try {
            if (mSensorManager != null) {
                Sensor mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                int delay = SensorManager.SENSOR_DELAY_NORMAL;
                mSensorManager.registerListener(this, mAccel, delay, this);
            }
        } catch (Exception e) {
            Log.e(TAG, "run: exception " + e);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (mClosing.get())
            return;
        Log.d(TAG, "handleMessage: msg=" + msg);
        switch (msg.what) {
            case MSG_MOTION_DETECTED:
                mIsStopping = false;
                if (mAudioService != null) {
                    mAudioService.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            mFadeState.originalVolume(),
                            0);
                    mAudioService = null;
                    mFadeState = null;
                }
                this.removeMessages(MSG_MOTION_TIMEOUT);
                this.removeMessages(MSG_HANDLE_STOP_STEP);

                Message message = Message.obtain();
                message.what = MSG_MOTION_TIMEOUT;
                this.sendMessageDelayed(message, mTimeout);
                break;
            case MSG_MOTION_TIMEOUT:
                mIsStopping = true;
                handleStop();
                break;
            case MSG_HANDLE_STOP_STEP:
                if (mIsStopping) {
                    handleStop();
                }
                break;
        }
    }

    private void handleStop() {
        Log.e(TAG, "handleStop: fade=" +
                (mFadeState == null
                        ? "not started"
                        : mFadeState.currentVolume()));
        if (mClosing.get())
            return;
        if (mAudioService == null) {
            mAudioService = (AudioManager) mCoolReader.getSystemService(Context.AUDIO_SERVICE);
            if (mAudioService == null) {
                Log.e(TAG, "handleStop: mAudioService == null! ");
                return;
            }
            mFadeState = new MotionWatchdogFadeState(
                    mAudioService.getStreamVolume(
                            AudioManager.STREAM_MUSIC));
            if (mFadeState.isSilent()) {
                finishTimeout();
                return;
            }
            Message message = Message.obtain();
            message.what = MSG_HANDLE_STOP_STEP;
            this.sendMessageDelayed(message, STEP_TIME);
            return;
        }
        mAudioService.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                mFadeState.step(),
                0);
        if (!mFadeState.isSilent()) {
            Message message = Message.obtain();
            message.what = MSG_HANDLE_STOP_STEP;
            this.sendMessageDelayed(message, STEP_TIME);
            return;
        }

        finishTimeout();
    }

    private void finishTimeout() {
        Log.i(TAG, "Final stop");
        mIsStopping = false;
        BackgroundThread.instance().postGUI(
                mTTSToolbarDlg::stopAndClose);
        close();
    }

    public void close() {
        if (!mClosing.compareAndSet(false, true))
            return;
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        removeCallbacksAndMessages(null);
        if (Looper.myLooper() == getLooper()) {
            finishClose();
        } else if (!postAtFrontOfQueue(this::finishClose)) {
            finishClose();
        }
    }

    private void finishClose() {
        if (mAudioService != null) {
            mAudioService.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    mFadeState.originalVolume(),
                    0);
            mAudioService = null;
            mFadeState = null;
        }
        mIsStopping = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            mHandlerThread.quitSafely();
        else
            mHandlerThread.quit();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mClosing.get())
            return;

        mDelta[0] = Math.abs(Math.abs(event.values[0]) - Math.abs(mLastValues[0]));
        mDelta[1] = Math.abs(Math.abs(event.values[1]) - Math.abs(mLastValues[1]));
        mDelta[2] = Math.abs(Math.abs(event.values[2]) - Math.abs(mLastValues[2]));

        mLastValues[0] = event.values[0];
        mLastValues[1] = event.values[1];
        mLastValues[2] = event.values[2];

//        Log.d(TAG, "onSensorChanged:"
//                + " x=" + mDelta[0]
//                + " y=" + mDelta[1]
//                + " z=" + mDelta[2]
//        );

        if ((mDelta[0] > MOTION_THRESHOLD) || (mDelta[1] > MOTION_THRESHOLD) || (mDelta[2] > MOTION_THRESHOLD)) {
            Log.d(TAG, "Got significant motion");
            Message message = Message.obtain();
            message.what = MotionWatchdogHandler.MSG_MOTION_DETECTED;
            sendMessage(message);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        if (!mClosing.get())
            Log.d(TAG, "onAccuracyChanged: sensor=" + sensor + " i=" + i);
    }

}
