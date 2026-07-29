/*
 * CoolReader for Android
 * Copyright (C) 2010-2013 Vadim Lopatin <coolreader.org@gmail.com>
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

import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;

import org.coolreader.R;

public class FindNextDlg {
	public interface SearchNavigationHandler {
		boolean isActive();
		void findNext(boolean reverse);
		void clearSelection();
	}

	private final PopupWindow mWindow;
	private final View mAnchor;
	private final View mPanel;
	private final SearchNavigationHandler searchHandler;

	static public void showDialog(
			BaseActivity coolReader, View anchor,
			SearchNavigationHandler searchHandler)
	{
		FindNextDlg dlg = new FindNextDlg(
				coolReader, anchor, searchHandler);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
	}
	public FindNextDlg(
			BaseActivity coolReader, View anchor,
			SearchNavigationHandler searchHandler)
	{
		this.searchHandler = searchHandler;
		mAnchor = anchor;

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.search_popup, null));
		panel.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		//mReaderView.getS
		
		mWindow = new PopupWindow( mAnchor.getContext() );
		mWindow.setTouchInterceptor((v, event) -> {
			if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
				clearSelection();
				mWindow.dismiss();
				return true;
			}
			return false;
		});
		//super(panel);
		mPanel = panel;
		mPanel.findViewById(R.id.search_btn_prev).setOnClickListener(
				v -> findNext(true));
		mPanel.findViewById(R.id.search_btn_next).setOnClickListener(
				v -> findNext(false));
		mPanel.findViewById(R.id.search_btn_close).setOnClickListener(v -> {
			clearSelection();
			mWindow.dismiss();
		});
		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_BACK:
					clearSelection();
					mWindow.dismiss();
					return true;
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_UP:
					findNext(true);
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
				case KeyEvent.KEYCODE_DPAD_DOWN:
					findNext(false);
					return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
					switch ( keyCode ) {
					case KeyEvent.KEYCODE_BACK:
					case KeyEvent.KEYCODE_DPAD_LEFT:
					case KeyEvent.KEYCODE_DPAD_UP:
					case KeyEvent.KEYCODE_DPAD_RIGHT:
					case KeyEvent.KEYCODE_DPAD_DOWN:
						return true;
					}
				}
			return keyCode == KeyEvent.KEYCODE_BACK;
		});

		mWindow.setOnDismissListener(
				this::clearSelection);
		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		//mWindow.setAnimationStyle(android.R.style.Animation_Toast);
		mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
//		setWidth(panel.getWidth());
//		setHeight(panel.getHeight());
		
		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);
		
		
		int [] location = new int[2];
		mAnchor.getLocationOnScreen(location);
		//mWindow.update(location[0], location[1], mPanel.getWidth(), mPanel.getHeight() );
		//mWindow.setWidth(mPanel.getWidth());
		//mWindow.setHeight(mPanel.getHeight());

		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], location[1] + mAnchor.getHeight() - mPanel.getHeight());
//		if ( mWindow.isShowing() )
//			mWindow.update(mAnchor, 50, 50);
		//dlg.mWindow.showAsDropDown(dlg.mAnchor);
	
	}

	private void findNext(boolean reverse) {
		if (!searchHandler.isActive()) {
			mWindow.dismiss();
			return;
		}
		searchHandler.findNext(reverse);
	}

	private void clearSelection() {
		searchHandler.clearSelection();
	}
	
}
