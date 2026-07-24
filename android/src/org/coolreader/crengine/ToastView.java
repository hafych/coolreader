/*
 * CoolReader for Android
 * Copyright (C) 2011 Viktor Soskin <xorzone@gmail.com>
 * Copyright (C) 2011,2012 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2012 Jeff Doozan <jeff@doozan.com>
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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.coolreader.R;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * User: Victor Soskin
 * Date: 11/3/11
 * Time: 2:51 PM
 */
public final class ToastView {
	private static final class ToastMessage {
		private final View anchor;
		private final String message;
		private final int duration;
		private final int textSize;

		private ToastMessage(
				View anchor,
				String message,
				int duration,
				int textSize) {
			this.anchor = anchor;
			this.message = message;
			this.duration = duration;
			this.textSize = textSize;
		}
	}

	private final Queue<ToastMessage> queue = new ArrayDeque<>();
	private final Handler handler = new Handler(Looper.getMainLooper());
	private PopupWindow window;
	private boolean showing;
	private boolean closed;

	private final Runnable handleDismiss = () -> {
		if (window != null) {
			window.dismiss();
			window = null;
		}
		showNext();
	};

	public void showToast(
			View anchor,
			String message,
			int duration,
			int textSize) {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			handler.post(() -> showToast(
					anchor,
					message,
					duration,
					textSize));
			return;
		}
		if (closed)
			return;
		queue.offer(new ToastMessage(
				anchor,
				message,
				duration,
				textSize));
		if (!showing) {
			showing = true;
			showNext();
		}
	}

	private void showNext() {
		if (closed)
			return;
		ToastMessage toast = queue.poll();
		if (toast == null) {
			showing = false;
			return;
		}
		window = new PopupWindow(toast.anchor.getContext());
		window.setWidth(WindowManager.LayoutParams.FILL_PARENT);
		window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setTouchable(false);
		window.setFocusable(false);
		window.setOutsideTouchable(true);
		window.setBackgroundDrawable(null);
		LayoutInflater inflater = (LayoutInflater) toast.anchor
				.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		window.setContentView(inflater.inflate(
				R.layout.custom_toast,
				null,
				true));
		TextView text = window.getContentView().findViewById(R.id.toast);
		text.setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				toast.textSize);
		text.setText(toast.message);
		text.setGravity(Gravity.CENTER);
		window.showAtLocation(
				toast.anchor,
				Gravity.NO_GRAVITY,
				0,
				0);
		handler.postDelayed(
				handleDismiss,
				toast.duration == 0 ? 2000 : 3000);
	}

	public void close() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			handler.post(this::close);
			return;
		}
		closed = true;
		handler.removeCallbacksAndMessages(null);
		queue.clear();
		showing = false;
		if (window != null) {
			window.dismiss();
			window = null;
		}
	}
}
