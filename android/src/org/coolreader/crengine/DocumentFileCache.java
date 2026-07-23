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

import android.app.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class DocumentFileCache {
	public static final Logger log = L.create("dfc");

	String mBasePath = null;

	public DocumentFileCache(Activity activity) {
		File fd = activity.getCacheDir();
		File dir = new File(fd, "bookCache");
		if (dir.isDirectory() || dir.mkdirs()) {
			mBasePath = dir.getAbsolutePath();
		} else {
			log.e("Failed to obtain private app cache directory!");
		}
	}

	public final String getBasePath() {
		return mBasePath;
	}

	public BookInfo saveStream(FileInfo fileInfo, InputStream inputStream) {
		return saveStream(fileInfo, inputStream, Long.MAX_VALUE);
	}

	public BookInfo saveStream(FileInfo fileInfo, InputStream inputStream, long maxBytes) {
		if (null == mBasePath) {
			log.e("Attempt to save stream while private app cache directory uninitialized!");
			return null;
		}
		BookInfo bookInfo = null;
		File file = null;
		String extension;
		long codebase;
		if (0 != fileInfo.crc32)
			codebase = fileInfo.crc32;
		else
			codebase = android.os.SystemClock.uptimeMillis();
		if (null != fileInfo.format)
			extension = fileInfo.format.getExtensions()[0];
		else
			extension = ".fb2";
		if (fileInfo.isArchive) {
			// No info about archive type
			extension += ".pack";
		}
		String filename = Long.valueOf(codebase).toString() + extension;
		try {
			file = new File(mBasePath, filename);
			long size = 0;
			byte[] buffer = new byte[64 * 1024];
			try (FileOutputStream outputStream = new FileOutputStream(file)) {
				int count;
				while ((count = inputStream.read(buffer)) != -1) {
					if (size + count > maxBytes)
						throw new IOException("Document exceeds cache limit of " + maxBytes + " bytes");
					outputStream.write(buffer, 0, count);
					size += count;
				}
				outputStream.getFD().sync();
			}
			if (size > 0) {
				FileInfo newFileInfo = new FileInfo(fileInfo);
				// Set new path & name
				if (fileInfo.isArchive) {
					newFileInfo.arcname = file.getAbsolutePath();
					newFileInfo.arcsize = size;
				} else {
					newFileInfo.filename = file.getName();
					newFileInfo.path = file.getParent();
					newFileInfo.pathname = file.getAbsolutePath();
					newFileInfo.createTime = file.lastModified();
					newFileInfo.size = size;
				}
				bookInfo = new BookInfo(newFileInfo);
			}
		} catch (Exception e) {
			log.e("Exception while saving stream: " + e.getMessage());
			if (file != null && file.exists() && !file.delete())
				log.w("Cannot delete incomplete cache file " + file);
		}
		return bookInfo;
	}
}
