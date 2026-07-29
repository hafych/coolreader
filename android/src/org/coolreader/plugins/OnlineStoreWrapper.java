/*
 * CoolReader for Android
 * Copyright (C) 2012,2014 Vadim Lopatin <coolreader.org@gmail.com>
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

package org.coolreader.plugins;

import java.io.File;

import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.Scanner;

public class OnlineStoreWrapper {
	private OnlineStorePlugin plugin;
	public OnlineStoreWrapper(OnlineStorePlugin plugin) {
		this.plugin = plugin;
	}
	public FileInfo createRootDirectory() {
		final FileInfo root = Scanner.createOnlineLibraryPluginItem(plugin.getPackageName(), plugin.getName());
		root.addDir(Scanner.createOnlineLibraryPluginItem(plugin.getPackageName() + ":genres", "Books by genres"));
		FileInfo authors = Scanner.createOnlineLibraryPluginItem(plugin.getPackageName() + ":authors", "Books by authors");
		root.addDir(authors);
		String firstLetters = plugin.getFirstAuthorNameLetters();
		for (char ch : firstLetters.toCharArray()) {
			authors.addDir(Scanner.createOnlineLibraryPluginItem(plugin.getPackageName() + ":authors=" + ch, ("" + ch).toUpperCase()));
		}
		root.addDir(Scanner.createOnlineLibraryPluginItem(plugin.getPackageName() + ":my", "My books"));
		root.addDir(Scanner.createOnlineLibraryPluginItem(plugin.getPackageName() + ":popular", "Popular"));
		root.addDir(Scanner.createOnlineLibraryPluginItem(plugin.getPackageName() + ":new", "Hot new"));
		return root;
	}
	public AsyncOperationControl openDirectory(final FileInfo dir, final FileInfoCallback callback) {
		AsyncOperationControl control = new AsyncOperationControl();
		FileInfoCallback guardedCallback = new FileInfoCallback() {
			@Override
			public void onFileInfoReady(FileInfo fileInfo) {
				if (!control.isCancelled())
					callback.onFileInfoReady(fileInfo);
			}

			@Override
			public void onError(
					int errorCode, String errorMessage) {
				if (!control.isCancelled())
					callback.onError(errorCode, errorMessage);
			}
		};
		if (!plugin.getPackageName().equals(dir.getOnlineCatalogPluginPackage())) {
			control.finished();
			guardedCallback.onError(0, "wrong plugin");
			return control;
		}
		String path = dir.getOnlineCatalogPluginPath();
		if (path == null) {
			control.finished();
			guardedCallback.onError(0, "wrong path");
			return control;
		}
		if ("genres".equals(path)) {
			plugin.fillGenres(control, dir, guardedCallback);
			control.finished();
			return control;
		} else if (path.startsWith("genre=")) {
			String genre = dir.getOnlineCatalogPluginId();
			plugin.getBooksForGenre(
					control, dir, genre, guardedCallback);
			control.finished();
			return control;
		} else if (path.startsWith("authors=")) {
			String prefix = dir.getOnlineCatalogPluginId();
			plugin.getAuthorsByPrefix(
					control, dir, prefix, guardedCallback);
			control.finished();
			return control;
		} else if (path.startsWith("author=")) {
			String authorId = dir.getOnlineCatalogPluginId();
			plugin.getBooksByAuthor(
					control, dir, authorId, guardedCallback);
			control.finished();
			return control;
		} else if ("my".equals(path)) {
			plugin.getPurchasedBooks(
					control, dir, guardedCallback);
			return control;
		} else if ("new".equals(path)) {
			plugin.getNewBooks(control, dir, guardedCallback);
			return control;
		} else if ("popular".equals(path)) {
			plugin.getPopularBooks(
					control, dir, guardedCallback);
			return control;
		} else {
			
		}
			
		control.finished();
		guardedCallback.onFileInfoReady(dir);
		return control;
	}
	public AsyncOperationControl authenticate(String login, String password, AuthenticationCallback callback) {
		AsyncOperationControl control = new AsyncOperationControl();
		plugin.authenticate(
				control,
				login,
				password,
				new AuthenticationCallback() {
					@Override
					public void onError(
							int errorCode,
							String errorMessage) {
						if (!control.isCancelled())
							callback.onError(
									errorCode, errorMessage);
					}

					@Override
					public void onSuccess() {
						if (!control.isCancelled())
							callback.onSuccess();
					}
				});
		return control;
	}
	private void loadBookInfoContinue(final AsyncOperationControl control, final String bookId, final boolean isBought, final BookInfoCallback callback) {
		plugin.getBookInfo(control, bookId, false, new BookInfoCallback() {
			@Override
			public void onError(int errorCode, String errorMessage) {
				if (!control.isCancelled())
					callback.onError(errorCode, errorMessage);
			}
			@Override
			public void onBookInfoReady(OnlineStoreBookInfo bookInfo) {
				if (control.isCancelled())
					return;
				bookInfo.isPurchased = isBought;
				callback.onBookInfoReady(bookInfo);
			}
		});
	}
	private void loadBookInfoSkipAuth(final AsyncOperationControl control, final String bookId, final BookInfoCallback callback) {
		if (control.isCancelled())
			return;
		if (plugin.getLogin() == null)
			loadBookInfoContinue(control, bookId, false, callback);
		else
			plugin.getBookInfo(control, bookId, true, new BookInfoCallback() {
				@Override
				public void onError(int errorCode, String errorMessage) {
					if (!control.isCancelled())
						loadBookInfoContinue(
								control,
								bookId,
								false,
								callback);
				}
				
				@Override
				public void onBookInfoReady(OnlineStoreBookInfo bookInfo) {
					if (!control.isCancelled())
						loadBookInfoContinue(
								control,
								bookId,
								true,
								callback);
				}
			});
	}
	public AsyncOperationControl loadBookInfo(final String bookId, final BookInfoCallback callback) {
		final AsyncOperationControl control = new AsyncOperationControl();
		String login = plugin.getLogin();
		String password = plugin.getPassword();
		if (login != null && password != null) {
			plugin.authenticate(control, login, password, new AuthenticationCallback() {
				@Override
				public void onError(int errorCode, String errorMessage) {
					if (!control.isCancelled())
						loadBookInfoSkipAuth(
								control, bookId, callback);
				}
				
				@Override
				public void onSuccess() {
					if (!control.isCancelled())
						loadBookInfoSkipAuth(
								control, bookId, callback);
				}
			});
		} else
			loadBookInfoSkipAuth(control, bookId, callback);
		return control;
	}

	public AsyncOperationControl downloadBook(OnlineStoreBook book, boolean trial, File fileToSave, DownloadBookCallback callback) {
		final AsyncOperationControl control = new AsyncOperationControl();
		plugin.downloadBook(
				control,
				book,
				trial,
				fileToSave,
				new DownloadBookCallback() {
					@Override
					public void onError(
							int errorCode,
							String errorMessage) {
						if (!control.isCancelled())
							callback.onError(
									errorCode, errorMessage);
					}

					@Override
					public void onBookDownloaded(
							OnlineStoreBook downloadedBook,
							boolean downloadedTrial,
							File savedFileName) {
						if (!control.isCancelled())
							callback.onBookDownloaded(
									downloadedBook,
									downloadedTrial,
									savedFileName);
					}
				});
		return control;
	}

	public String getLogin() {
		return plugin.getLogin();
	}

	public String getPassword() {
		return plugin.getPassword();
	}

	public String getDescription() {
		return plugin.getDescription();
	}

	public String getName() {
		return plugin.getName();
	}
	
}
