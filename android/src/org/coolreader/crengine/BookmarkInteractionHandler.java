/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

interface BookmarkInteractionHandler {
	boolean isActive();

	boolean addBookmark(Bookmark bookmark);

	boolean addBookmark(int shortcut);

	boolean removeBookmark(Bookmark bookmark);

	boolean updateBookmark(Bookmark bookmark);

	boolean goToBookmark(Bookmark bookmark);

	boolean goToBookmark(int shortcut);
}
