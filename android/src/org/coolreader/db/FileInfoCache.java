/*
 * CoolReader for Android
 * Copyright (C) 2012 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2020 Aleksey Chernov <valexlin@gmail.com>
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

package org.coolreader.db;

import org.coolreader.crengine.FileInfo;

public class FileInfoCache {

	private final FileInfoCacheState cacheState;
	
	public FileInfoCache(int maxSize) {
		this.cacheState = new FileInfoCacheState(maxSize);
	}
	
	public FileInfo remove(FileInfo entry) {
		int index = cacheState.findByBookKey(entry.bookKey);
		if (index == -1)
			index = cacheState.findByPath(entry.getPathName());
		if (index == -1)
			index = cacheState.findById(entry.id);
		if (index == -1)
			return null;
		return cacheState.removeAt(index);
	}
	
	public void put(FileInfo entry) {
		int index = cacheState.findByBookKey(entry.bookKey);
		if (index == -1)
			index = cacheState.findByPath(entry.getPathName());
		if (index == -1)
			index = cacheState.findById(entry.id);
		if (index == -1) {
			cacheState.add(entry);
			cacheState.checkSize();
			return;
		}
		cacheState.setAt(index, entry);
		cacheState.moveOnTop(index);
	}
	
	public FileInfo get(String path) {
		int index = cacheState.findByPath(path);
		if (index == -1)
			return null;
		FileInfo item = cacheState.getAt(index);
		cacheState.moveOnTop(index);
		return item;
	}
	
	public FileInfo get(Long id) {
		if (id == null)
			return null;
		int index = cacheState.findById(id);
		if (index == -1)
			return null;
		FileInfo item = cacheState.getAt(index);
		cacheState.moveOnTop(index);
		return item;
	}

	public FileInfo getByBookKey(String bookKey) {
		int index = cacheState.findByBookKey(bookKey);
		if (index == -1)
			return null;
		FileInfo item = cacheState.getAt(index);
		cacheState.moveOnTop(index);
		return item;
	}
	
	public void clear() {
		cacheState.clear();
	}
}
