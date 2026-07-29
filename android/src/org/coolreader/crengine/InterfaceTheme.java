/*
 * CoolReader for Android
 * Copyright (C) 2011,2012,2014 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2018 Yuri Plotnikov <plotnikovya@gmail.com>
 * Copyright (C) 2018,2021 Aleksey Chernov <valexlin@gmail.com>
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

public final class InterfaceTheme {

	public String getCode() {
		return code;
	}

	public int getThemeId() {
		return themeId;
	}

	public int getDialogThemeId() {
		return dialogThemeId;
	}

	public int getFullscreenDialogThemeId() {
		return fsDialogThemeId;
	}

	public int getDisplayNameResourceId() {
		return displayNameResourceId;
	}
	
	public int getActionBarBackgroundColorReading() {
		return actionBarBackgroundColorReading;
	}
	
//	public Drawable getActionBarBackgroundDrawableReading() {
//		return Utils.solidColorDrawable(actionBarBackgroundColorReading);
//	}
//	
//	public Drawable getActionBarBackgroundDrawableBrowser() {
//		return Utils.solidColorDrawable(0);
//	}
	
	public int getRootDelimiterResourceId() {
		return visuals.rootDelimiterResourceId;
	}
	
	public int getRootDelimiterHeight() {
		return visuals.rootDelimiterHeight;
	}
	
	public int getBrowserStatusBackground() {
		return visuals.browserStatusBackground;
	}
	
	public int getBrowserToolbarBackground(boolean vertical) {
		return !vertical
				? visuals.browserToolbarBackground
				: visuals.browserToolbarBackgroundVertical;
	}
	
//	public int getReaderStatusBackground() {
//		return readerStatusBackground;
//	}
//	
//	public int getReaderToolbarBackground(boolean vertical) {
//		return !vertical ? readerToolbarBackground : readerToolbarBackgroundVertical;
//	}
	
//	public int getStatusTextColor() {
//		return statusTextColor;
//	}
	
	public int getToolbarButtonAlpha() {
		return visuals.toolbarButtonAlpha;
	}

	public int getPopupToolbarBackground() {
		return visuals.popupToolbarBackground;
	}

	public int getPopupToolbarBackgroundColor() {
		return visuals.popupToolbarBackgroundColor;
	}

	private final String code;
	private final int themeId;
	private final int dialogThemeId;
	private final int fsDialogThemeId;
	private final int displayNameResourceId;
	private final int actionBarBackgroundColorReading;
	private final Visuals visuals;

	static InterfaceTheme create(
			String code,
			int themeId,
			int dialogThemeId,
			int fsDialogThemeId,
			int displayNameResourceId,
			int actionBarBackgroundColorReading,
			int rootDelimiterResourceId,
			int rootDelimiterHeight,
			int browserStatusBackground,
			int browserToolbarBackground,
			int browserToolbarBackgroundVertical,
			int popupToolbarBackground,
			int popupToolbarBackgroundColor,
			int toolbarButtonAlpha) {
		return new InterfaceTheme(
				code,
				themeId,
				dialogThemeId,
				fsDialogThemeId,
				displayNameResourceId,
				actionBarBackgroundColorReading,
				new Visuals(
						rootDelimiterResourceId,
						rootDelimiterHeight,
						browserStatusBackground,
						browserToolbarBackground,
						browserToolbarBackgroundVertical,
						popupToolbarBackground,
						popupToolbarBackgroundColor,
						toolbarButtonAlpha));
	}

	private InterfaceTheme(
			String code,
			int themeId,
			int dialogThemeId,
			int fsDialogThemeId,
			int displayNameResourceId,
			int actionBarBackgroundColorReading,
			Visuals visuals) {
		this.code = code;
		this.themeId = themeId;
		this.dialogThemeId = dialogThemeId;
		this.fsDialogThemeId = fsDialogThemeId;
		this.displayNameResourceId = displayNameResourceId;
		this.actionBarBackgroundColorReading = actionBarBackgroundColorReading;
		this.visuals = visuals;
	}
	
	@Override
	public String toString() {
		return "Theme[code=" + code + ", themeId=" + themeId + "]";
	}

	public final void applyActionIcons() {

	}

	private static final class Visuals {
		private final int rootDelimiterResourceId;
		private final int rootDelimiterHeight;
		private final int browserStatusBackground;
		private final int browserToolbarBackground;
		private final int browserToolbarBackgroundVertical;
		private final int popupToolbarBackground;
		private final int popupToolbarBackgroundColor;
		private final int toolbarButtonAlpha;

		private Visuals(
				int rootDelimiterResourceId,
				int rootDelimiterHeight,
				int browserStatusBackground,
				int browserToolbarBackground,
				int browserToolbarBackgroundVertical,
				int popupToolbarBackground,
				int popupToolbarBackgroundColor,
				int toolbarButtonAlpha) {
			this.rootDelimiterResourceId = rootDelimiterResourceId;
			this.rootDelimiterHeight = rootDelimiterHeight;
			this.browserStatusBackground = browserStatusBackground;
			this.browserToolbarBackground = browserToolbarBackground;
			this.browserToolbarBackgroundVertical =
					browserToolbarBackgroundVertical;
			this.popupToolbarBackground = popupToolbarBackground;
			this.popupToolbarBackgroundColor = popupToolbarBackgroundColor;
			this.toolbarButtonAlpha = toolbarButtonAlpha;
		}
	}
}
