/*
 * CoolReader for Android
 * Copyright (C) 2010-2013 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2018 Yuri Plotnikov <plotnikovya@gmail.com>
 * Copyright (C) 2020,2021 Aleksey Chernov <valexlin@gmail.com>
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

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.coolreader.CoolReader;
import org.coolreader.R;

public class SelectionToolbarDlg {
	private final PopupWindow mWindow;
	private final View mAnchor;
	private final CoolReader mCoolReader;
	private final SelectionToolbarHandler selectionToolbarHandler;
	private final View mPanel;
	private Selection selection;

	static void showDialog(
			CoolReader coolReader,
			View anchor,
			final Selection selection,
			SelectionToolbarHandler selectionToolbarHandler)
	{
		SelectionToolbarDlg dlg = new SelectionToolbarDlg(
				coolReader, anchor, selection,
				selectionToolbarHandler);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.update();
	}

	private boolean pageModeSet = false;
	private boolean changedPageMode;

	private boolean isActive() {
		return selectionToolbarHandler.isActive();
	}

	private boolean requireActive() {
		if (isActive())
			return true;
		mWindow.dismiss();
		return false;
	}

	private void setReaderMode()
	{
		if (pageModeSet || !isActive())
			return;
		changedPageMode =
				selectionToolbarHandler.enterAdjustmentMode();
		pageModeSet = true;
	}
	
	private void restoreReaderMode()
	{
		selectionToolbarHandler.restoreAdjustmentMode(
				changedPageMode);
	}
	
	private void changeSelectionBound(boolean start, int delta) {
		if (!requireActive())
			return;
		L.d("changeSelectionBound(" + (start?"start":"end") + ", " + delta + ")");
		setReaderMode();
		selectionToolbarHandler.moveSelectionBound(
				start,
				delta,
				new SelectionToolbarHandler.SelectionUpdateHandler() {
			
			@Override
			public void onNewSelection(Selection selection) {
				Log.d("cr3", "onNewSelection: " + selection.text);
				SelectionToolbarDlg.this.selection = selection;
			}
			
			@Override
			public void onFail() {
				Log.d("cr3", "fail()");
				//currentSelection = null;
			}
		});
	}
	
	private final static int SELECTION_CONTROL_STEP = 10; 
	private class BoundControlListener implements OnSeekBarChangeListener {

		public BoundControlListener(SeekBar sb, boolean start) {
			this.start = start;
			this.sb = sb;
			sb.setOnSeekBarChangeListener(this);
		}
		final boolean start;
		final SeekBar sb;
		int lastProgress = 50;
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			sb.setProgress(50);
			lastProgress = 50;
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			sb.setProgress(50);
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (!fromUser)
				return;
			int diff = (progress - lastProgress) / SELECTION_CONTROL_STEP * SELECTION_CONTROL_STEP;
			if (diff!=0) {
				lastProgress += diff;
				changeSelectionBound(start, diff/SELECTION_CONTROL_STEP);
			}
		}
	};
	
	private void closeDialog(boolean clearSelection) {
		if (clearSelection)
			selectionToolbarHandler.clearSelection();
		restoreReaderMode();
		mWindow.dismiss();
	}
	
	SelectionToolbarDlg(
			CoolReader coolReader,
			View anchor,
			Selection sel,
			SelectionToolbarHandler selectionToolbarHandler)
	{
		if (selectionToolbarHandler == null
				|| !selectionToolbarHandler.isActive())
			throw new IllegalArgumentException(
					"active selection toolbar handler is required");
		this.selection = sel;
		mCoolReader = coolReader;
		mAnchor = anchor;
		this.selectionToolbarHandler =
				selectionToolbarHandler;

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.selection_toolbar, null));
		panel.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		mWindow = new PopupWindow( mAnchor.getContext() );
		mWindow.setTouchInterceptor((v, event) -> {
			if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
				closeDialog(true);
				return true;
			}
			return false;
		});
		//super(panel);
		mPanel = panel;
		mPanel.findViewById(R.id.selection_copy).setOnClickListener(v -> {
			if (!requireActive())
				return;
			selectionToolbarHandler.copyToClipboard(
					selection.text);
			closeDialog(true);
		});

		mPanel.findViewById(R.id.selection_dict).setOnClickListener(v -> {
			if (!requireActive())
				return;
			mCoolReader.findInDictionary( selection.text );
			closeDialog(
					!selectionToolbarHandler
							.shouldPersistSelection());
		});

		mPanel.findViewById(R.id.selection_dict).setOnLongClickListener(v -> {
			if (!requireActive())
				return true;
			//mCoolReader.showToast("long tap on dic");
			DictsDlg dlg = new DictsDlg(
					mCoolReader, selection.text,
					new DictsDlg.SelectionHandler() {
						@Override
						public boolean isActive() {
							return SelectionToolbarDlg.this.isActive();
						}

						@Override
						public boolean shouldPersistSelection() {
							return selectionToolbarHandler
									.shouldPersistSelection();
						}

						@Override
						public void clearSelection() {
							selectionToolbarHandler
									.clearSelection();
						}
					});
			dlg.show();
			closeDialog(
					!selectionToolbarHandler
							.shouldPersistSelection());
			return true;
		});

		mPanel.findViewById(R.id.selection_bookmark).setOnClickListener(v -> {
			if (!requireActive())
				return;
			selectionToolbarHandler.showNewBookmark(
					selection);
			closeDialog(true);
		});

		mPanel.findViewById(R.id.selection_bookmark).setOnLongClickListener(v -> {
			if (!requireActive())
				return true;
			selectionToolbarHandler.showBookmarks();
			closeDialog(true);
			return true;
		});
		mPanel.findViewById(R.id.selection_email).setOnClickListener(v -> {
			if (!requireActive())
				return;
			selectionToolbarHandler.sendQuotation(
					selection);
			closeDialog(true);
		});
		mPanel.findViewById(R.id.selection_find).setOnClickListener(v -> {
			if (!requireActive())
				return;
			selectionToolbarHandler.showSearch(
					selection.text.trim());
			closeDialog(true);
		});
		mPanel.findViewById(R.id.selection_find).setOnLongClickListener(v -> {
			if (!requireActive())
				return true;
			final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
			emailIntent.putExtra(SearchManager.QUERY, selection.text.trim());
			mCoolReader.startActivity(emailIntent);
			closeDialog(true);
			return true;
		});
		mPanel.findViewById(R.id.selection_cancel).setOnClickListener(v -> closeDialog(true));
		new BoundControlListener(mPanel.findViewById(R.id.selection_left_bound_control), true);
		new BoundControlListener(mPanel.findViewById(R.id.selection_right_bound_control), false);
		mPanel.setFocusable(true);
		mPanel.setOnKeyListener((v, keyCode, event) -> {
			if ( event.getAction()==KeyEvent.ACTION_UP ) {
				switch ( keyCode ) {
				case KeyEvent.KEYCODE_BACK:
					closeDialog(true);
					return true;
				}
			} else if ( event.getAction()==KeyEvent.ACTION_DOWN ) {
					switch ( keyCode ) {
					}
				}
			return keyCode == KeyEvent.KEYCODE_BACK;
		});

		mWindow.setOnDismissListener(() -> {
			restoreReaderMode();
			selectionToolbarHandler.clearSelection();
		});

		if (!DeviceInfo.EINK_SCREEN) {
			// transparent
			mWindow.setBackgroundDrawable(new BitmapDrawable());
		}
		else {
			// white background with rectangle
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				mWindow.setBackgroundDrawable(mCoolReader.getDrawable(R.drawable.btn_default_normal_hc_light));
			else
				mWindow.setBackgroundDrawable(mCoolReader.getResources().getDrawable(R.drawable.btn_default_normal_hc_light));
		}
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

		int popupY = location[1] + mAnchor.getHeight() - mPanel.getHeight();
		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.CENTER_HORIZONTAL, location[0], popupY);
//		if ( mWindow.isShowing() )
//			mWindow.update(mAnchor, 50, 50);
		//dlg.mWindow.showAsDropDown(dlg.mAnchor);
		int y = sel.startY;
		if (y > sel.endY)
			y = sel.endY;
		int maxy = mAnchor.getHeight() * 4 / 5;
		if (y > maxy) {
			setReaderMode(); // selection is overlapped by toolbar: set scroll mode and move
			BackgroundThread.instance().postGUI(() -> {
				BackgroundThread.instance().postBackground(() -> BackgroundThread.instance().postGUI(() -> {
					if (isActive())
						selectionToolbarHandler.scrollBy(
								mAnchor.getHeight() / 3);
				}));
			});
		}
	}
}
