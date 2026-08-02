/*
 * CoolReader for Android
 * Copyright (C) 2010-2012 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2012 Jeff Doozan <jeff@doozan.com>
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

import java.util.ArrayList;
import java.util.List;

import org.coolreader.db.CRDBService;


public class History extends FileInfoChangeSource {
	private final HistoryBooksState booksState = new HistoryBooksState();

	public History(Scanner scanner)
	{
		this.mScanner = scanner;
	}
	
	public BookInfo getLastBook()
	{
		return booksState.get(0);
	}

	public BookInfo getPreviousBook()
	{
		return booksState.get(1);
	}

	public interface BookInfoLoadedCallback {
		void onBookInfoLoaded(BookInfo bookInfo);
	}
	
	public void getOrCreateBookInfo(final CRDBService.LocalBinder db, final FileInfo file, final BookInfoLoadedCallback callback)
	{
		BookInfo res = getBookInfo(file);
		if (res != null) {
			callback.onBookInfoLoaded(res);
			return;
		}
		db.loadBookInfo(file, bookInfo -> {
			if (bookInfo == null || bookInfo.getFileInfo() == null
					|| bookInfo.getFileInfo().arcsize < 0
					|| bookInfo.getFileInfo().size < 0
					|| bookInfo.getFileInfo().crc32 < 0) {
				bookInfo = new BookInfo(file);
				booksState.addFirst(bookInfo);
			}
			callback.onBookInfoLoaded(bookInfo);
		});
	}
	
	public BookInfo getBookInfo( FileInfo file )
	{
		int index = findBookInfo( file );
		if ( index>=0 )
			return booksState.get(index);
		return null;
	}

	public BookInfo getBookInfo( String pathname )
	{
		int index = findBookInfo( pathname );
		if ( index>=0 )
			return booksState.get(index);
		return null;
	}
	
	public void removeBookInfo(final CRDBService.LocalBinder db, FileInfo fileInfo, boolean removeRecentAccessFromDB, boolean removeBookFromDB)
	{
		int index = findBookInfo(fileInfo);
		if (index >= 0)
			booksState.removeAt(index);
		if ( removeBookFromDB )
			db.deleteBook(fileInfo);
		else if ( removeRecentAccessFromDB )
			db.deleteRecentPosition(fileInfo);
		updateRecentDir();
	}

	public void updateBookInfo(BookInfo bookInfo)
	{
		Log.v("cr3", "History.updateBookInfo() for " + bookInfo.getFileInfo().getPathName());
		bookInfo.updateAccess();
		int index = findBookInfo(bookInfo.getFileInfo());
		if ( index>=0 ) {
			BookInfo info = booksState.moveToFront(index);
			if (info != null)
				info.setBookmarks(bookInfo.getAllBookmarks());
		} else {
			booksState.addFirst(bookInfo);
		}
	}

	public void updateBookAccess(BookInfo bookInfo, long timeElapsed)
	{
		Log.v("cr3", "History.updateBookAccess() for " + bookInfo.getFileInfo().getPathName());
		bookInfo.updateAccess();
		bookInfo.updateTimeElapsed(timeElapsed);
		int index = findBookInfo(bookInfo.getFileInfo());
		if ( index>=0 ) {
			BookInfo info = booksState.moveToFront(index);
			if (info != null)
				info.setBookmarks(bookInfo.getAllBookmarks());
		} else {
			booksState.addFirst(bookInfo);
		}
		updateRecentDir();
	}

	public int findBookInfo( String pathname )
	{
		return booksState.findByPathname(pathname);
	}
	
	public int findBookInfo( FileInfo file )
	{
		return booksState.findByFile(file);
	}
	
	public Bookmark getLastPos( FileInfo file )
	{
		int index = findBookInfo(file);
		if ( index<0 )
			return null;
		BookInfo info = booksState.get(index);
		return info != null ? info.getLastPosition() : null;
	}
	protected void updateRecentDir()
	{
		Log.v("cr3", "History.updateRecentDir()");
		FileInfo recent = booksState.getRecentFolder();
		if ( recent!=null ) { 
			recent.clear();
			List<BookInfo> books = booksState.snapshot();
			for ( BookInfo book : books )
				recent.addFile(book.getFileInfo());
			onChange(recent, false);
		} else {
			Log.v("cr3", "History.updateRecentDir() : mRecentBooksFolder is null");
		}
	}
	Scanner mScanner;

	
	public void getOrLoadRecentBooks(final CRDBService.LocalBinder db, final CRDBService.RecentBooksLoadingCallback callback) {
		if (!booksState.isEmpty()) {
			callback.onRecentBooksListLoaded(booksState.copyAsArrayList());
		} else {
			// not yet loaded. Wait until ready: sync with DB thread.
			db.sync(() -> callback.onRecentBooksListLoaded(
					booksState.copyAsArrayList()));
		}
	}
	
	public boolean loadFromDB(final CRDBService.LocalBinder db, int maxItems )
	{
		Log.v("cr3", "History.loadFromDB()");
		booksState.setRecentFolder(mScanner.getRecentDir());
		db.loadRecentBooks(100, bookList -> {
			if (bookList != null) {
				booksState.replaceAll(bookList);
				updateRecentDir();
			}
		});
		if ( booksState.getRecentFolder()==null )
			Log.v("cr3", "History.loadFromDB() : mRecentBooksFolder is null");
		return true;
	}

}
