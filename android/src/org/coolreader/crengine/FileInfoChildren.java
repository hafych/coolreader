/*
 * CoolReader for Android
 * Copyright (C) 2026 CoolReader Next contributors
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the files/dirs child lists of one {@link FileInfo} node.
 *
 * Mutations stay exclusive to the node so concurrent listing/UI paths cannot
 * escape a half-built child array. Snapshot accessors return copies for
 * parcel/hash paths.
 */
final class FileInfoChildren {
	private ArrayList<FileInfo> files;
	private ArrayList<FileInfo> dirs;

	int dirCount() {
		return dirs != null ? dirs.size() : 0;
	}

	int fileCount() {
		return files != null ? files.size() : 0;
	}

	boolean isEmpty() {
		return dirCount() == 0 && fileCount() == 0;
	}

	void addDir(FileInfo dir) {
		if (dirs == null)
			dirs = new ArrayList<>();
		dirs.add(dir);
	}

	void addFile(FileInfo file) {
		if (files == null)
			files = new ArrayList<>();
		files.add(file);
	}

	FileInfo getDir(int index) {
		return dirs.get(index);
	}

	FileInfo getFile(int index) {
		return files.get(index);
	}

	void setFile(int index, FileInfo file) {
		files.set(index, file);
	}

	void clear() {
		files = null;
		dirs = null;
	}

	void removeDirAt(int index) {
		if (dirs != null)
			dirs.remove(index);
	}

	void removeFileAt(int index) {
		if (files != null)
			files.remove(index);
	}

	int indexOfFile(FileInfo item) {
		return files != null ? files.indexOf(item) : -1;
	}

	int indexOfDir(FileInfo item) {
		return dirs != null ? dirs.indexOf(item) : -1;
	}

	boolean hasDirs() {
		return dirs != null;
	}

	boolean hasFiles() {
		return files != null;
	}

	List<FileInfo> dirsView() {
		return dirs != null
				? Collections.unmodifiableList(dirs)
				: Collections.emptyList();
	}

	List<FileInfo> filesView() {
		return files != null
				? Collections.unmodifiableList(files)
				: Collections.emptyList();
	}

	/**
	 * Parcel write: may be null lists (legacy).
	 */
	ArrayList<FileInfo> dirsOrNull() {
		return dirs;
	}

	ArrayList<FileInfo> filesOrNull() {
		return files;
	}

	void setFromParcel(
			ArrayList<FileInfo> filesFromParcel,
			ArrayList<FileInfo> dirsFromParcel) {
		files = filesFromParcel;
		dirs = dirsFromParcel;
	}

	void sortDirs(java.util.Comparator<FileInfo> cmp) {
		if (dirs != null)
			Collections.sort(dirs, cmp);
	}

	void sortFiles(java.util.Comparator<FileInfo> cmp) {
		if (files != null)
			Collections.sort(files, cmp);
	}

	ArrayList<FileInfo> copyDirs() {
		return dirs != null ? new ArrayList<>(dirs) : null;
	}

	ArrayList<FileInfo> copyFiles() {
		return files != null ? new ArrayList<>(files) : null;
	}

	void replaceDirs(ArrayList<FileInfo> newDirs) {
		dirs = newDirs;
	}

	void replaceFiles(ArrayList<FileInfo> newFiles) {
		files = newFiles;
	}

	int dirsHash() {
		return dirs == null ? 0 : dirs.hashCode();
	}

	int filesHash() {
		return files == null ? 0 : files.hashCode();
	}

	boolean dirsEqual(FileInfoChildren other) {
		if (dirs == null)
			return other.dirs == null;
		return dirs.equals(other.dirs);
	}

	boolean filesEqual(FileInfoChildren other) {
		if (files == null)
			return other.files == null;
		return files.equals(other.files);
	}
}
