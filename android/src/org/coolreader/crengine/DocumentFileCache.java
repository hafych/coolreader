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
import java.util.Arrays;

public final class DocumentFileCache {
	public static final Logger log = L.create("dfc");

	static final long MAX_CACHE_SIZE_BYTES = 512L * 1024L * 1024L;
	static final int MAX_CACHE_FILE_COUNT = 32;

	String mBasePath = null;

	public DocumentFileCache(Activity activity) {
		this(new File(activity.getCacheDir(), "bookCache"));
	}

	DocumentFileCache(File dir) {
		if (dir.isDirectory() || dir.mkdirs()) {
			mBasePath = dir.getAbsolutePath();
			pruneCacheDirectory(dir, MAX_CACHE_SIZE_BYTES, MAX_CACHE_FILE_COUNT, null);
		} else {
			log.e("Failed to obtain private app cache directory!");
		}
	}

	public final String getBasePath() {
		return mBasePath;
	}

	public BookInfo saveStream(FileInfo fileInfo, InputStream inputStream) {
		return saveStream(fileInfo, inputStream, ParseBudget.MAX_DOCUMENT_INPUT_BYTES);
	}

	public BookInfo saveStream(FileInfo fileInfo, InputStream inputStream, long maxBytes) {
		if (null == mBasePath) {
			log.e("Attempt to save stream while private app cache directory uninitialized!");
			return null;
		}
		long effectiveMaxBytes = Math.min(maxBytes, MAX_CACHE_SIZE_BYTES);
		if (effectiveMaxBytes < 0) {
			log.e("Attempt to save stream with a negative byte limit!");
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
			long size = copyStreamToFile(inputStream, file, effectiveMaxBytes);
			if (size > 0) {
				pruneCacheDirectory(new File(mBasePath), MAX_CACHE_SIZE_BYTES,
						MAX_CACHE_FILE_COUNT, file);
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
			} else if (file.exists() && !file.delete()) {
				log.w("Cannot delete empty cache file " + file);
			}
		} catch (Exception e) {
			log.e("Exception while saving stream: " + e.getMessage());
			if (file != null && file.exists() && !file.delete())
				log.w("Cannot delete incomplete cache file " + file);
		}
		return bookInfo;
	}

	static long copyStreamToFile(InputStream inputStream, File file, long maxBytes)
			throws IOException {
		if (maxBytes < 0)
			throw new IOException("Negative cache byte limit");
		long size = 0;
		boolean complete = false;
		byte[] buffer = new byte[64 * 1024];
		try (FileOutputStream outputStream = new FileOutputStream(file)) {
			int count;
			while ((count = inputStream.read(buffer)) != -1) {
				if (count > maxBytes - size)
					throw new ParseBudget.LimitExceededException(
							ParseBudget.Error.INPUT_BYTES);
				outputStream.write(buffer, 0, count);
				size += count;
			}
			outputStream.getFD().sync();
			complete = true;
			return size;
		} finally {
			if (!complete && file.exists() && !file.delete())
				log.w("Cannot delete incomplete cache file " + file);
		}
	}

	static void pruneCacheDirectory(File directory, long maxBytes, int maxFiles,
									File protectedFile) {
		if (directory == null || maxBytes < 0 || maxFiles < 0)
			return;
		File[] files = directory.listFiles(File::isFile);
		if (files == null || files.length == 0)
			return;

		Arrays.sort(files, (left, right) -> {
			int byAge = Long.compare(left.lastModified(), right.lastModified());
			return byAge != 0 ? byAge : left.getName().compareTo(right.getName());
		});

		long totalBytes = 0;
		int totalFiles = 0;
		for (File file : files) {
			totalBytes += file.length();
			totalFiles++;
		}

		for (File file : files) {
			if (totalBytes <= maxBytes && totalFiles <= maxFiles)
				break;
			if (protectedFile != null && protectedFile.equals(file))
				continue;
			long fileSize = file.length();
			if (file.delete()) {
				totalBytes -= fileSize;
				totalFiles--;
			} else {
				log.w("Cannot delete expired cache file " + file);
			}
		}
	}
}
