package org.coolreader.plugins;

import org.coolreader.crengine.FileInfo;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OnlineStoreWrapperTest {
	@Test
	public void canceledAuthenticationSuppressesLateCallbacks() {
		RecordingPlugin plugin = new RecordingPlugin();
		OnlineStoreWrapper wrapper = new OnlineStoreWrapper(plugin);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger errors = new AtomicInteger();

		AsyncOperationControl control =
				wrapper.authenticate(
						"reader",
						"secret",
						new AuthenticationCallback() {
							@Override
							public void onSuccess() {
								successes.incrementAndGet();
							}

							@Override
							public void onError(
									int errorCode,
									String errorMessage) {
								errors.incrementAndGet();
							}
						});

		control.cancel();
		plugin.authenticationCallback.onSuccess();
		plugin.authenticationCallback.onError(1, "late");

		assertTrue(control.isCancelled());
		assertEquals(0, successes.get());
		assertEquals(0, errors.get());
	}

	@Test
	public void canceledDownloadSuppressesLateCallbacks() {
		RecordingPlugin plugin = new RecordingPlugin();
		OnlineStoreWrapper wrapper = new OnlineStoreWrapper(plugin);
		AtomicInteger downloads = new AtomicInteger();
		AtomicInteger errors = new AtomicInteger();
		OnlineStoreBook book = new OnlineStoreBook();
		File destination = new File("download.fb2");

		AsyncOperationControl control =
				wrapper.downloadBook(
						book,
						false,
						destination,
						new DownloadBookCallback() {
							@Override
							public void onBookDownloaded(
									OnlineStoreBook downloadedBook,
									boolean trial,
									File savedFileName) {
								downloads.incrementAndGet();
							}

							@Override
							public void onError(
									int errorCode,
									String errorMessage) {
								errors.incrementAndGet();
							}
						});

		control.cancel();
		plugin.downloadCallback.onBookDownloaded(
				book, false, destination);
		plugin.downloadCallback.onError(1, "late");

		assertTrue(control.isCancelled());
		assertEquals(0, downloads.get());
		assertEquals(0, errors.get());
	}

	@Test
	public void canceledBookInfoSuppressesLateCallbacks() {
		RecordingPlugin plugin = new RecordingPlugin();
		OnlineStoreWrapper wrapper = new OnlineStoreWrapper(plugin);
		AtomicInteger books = new AtomicInteger();
		AtomicInteger errors = new AtomicInteger();

		AsyncOperationControl control =
				wrapper.loadBookInfo(
						"book",
						new BookInfoCallback() {
							@Override
							public void onBookInfoReady(
									OnlineStoreBookInfo bookInfo) {
								books.incrementAndGet();
							}

							@Override
							public void onError(
									int errorCode,
									String errorMessage) {
								errors.incrementAndGet();
							}
						});

		control.cancel();
		plugin.bookInfoCallback.onBookInfoReady(null);
		plugin.bookInfoCallback.onError(1, "late");

		assertTrue(control.isCancelled());
		assertEquals(0, books.get());
		assertEquals(0, errors.get());
	}

	private static final class RecordingPlugin
			implements OnlineStorePlugin {
		private AuthenticationCallback authenticationCallback;
		private BookInfoCallback bookInfoCallback;
		private DownloadBookCallback downloadCallback;

		@Override
		public String getPackageName() {
			return "test";
		}

		@Override
		public String getName() {
			return "Test";
		}

		@Override
		public String getDescription() {
			return "Test plugin";
		}

		@Override
		public String getLogin() {
			return null;
		}

		@Override
		public String getPassword() {
			return null;
		}

		@Override
		public String getFirstAuthorNameLetters() {
			return "";
		}

		@Override
		public void authenticate(
				AsyncOperationControl control,
				String login,
				String password,
				AuthenticationCallback callback) {
			authenticationCallback = callback;
		}

		@Override
		public void fillGenres(
				AsyncOperationControl control,
				FileInfo dir,
				FileInfoCallback callback) {
		}

		@Override
		public void getBookInfo(
				AsyncOperationControl control,
				String bookId,
				boolean myOnly,
				BookInfoCallback callback) {
			bookInfoCallback = callback;
		}

		@Override
		public void getBooksForGenre(
				AsyncOperationControl control,
				FileInfo dir,
				String genreId,
				FileInfoCallback callback) {
		}

		@Override
		public void getPurchasedBooks(
				AsyncOperationControl control,
				FileInfo dir,
				FileInfoCallback callback) {
		}

		@Override
		public void getPopularBooks(
				AsyncOperationControl control,
				FileInfo dir,
				FileInfoCallback callback) {
		}

		@Override
		public void getNewBooks(
				AsyncOperationControl control,
				FileInfo dir,
				FileInfoCallback callback) {
		}

		@Override
		public void getBooksByAuthor(
				AsyncOperationControl control,
				FileInfo dir,
				String authorId,
				FileInfoCallback callback) {
		}

		@Override
		public void getAuthorsByPrefix(
				AsyncOperationControl control,
				FileInfo dir,
				String prefix,
				FileInfoCallback callback) {
		}

		@Override
		public void downloadBook(
				AsyncOperationControl control,
				OnlineStoreBook book,
				boolean trial,
				File fileToSave,
				DownloadBookCallback callback) {
			downloadCallback = callback;
		}
	}
}
