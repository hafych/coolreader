/*
 * CoolReader for Android
 * Copyright (C) 2012-2014 Vadim Lopatin <coolreader.org@gmail.com>
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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.plugins.AsyncOperationControl;
import org.coolreader.plugins.BookInfoCallback;
import org.coolreader.plugins.DownloadBookCallback;
import org.coolreader.plugins.OnlineStoreBook;
import org.coolreader.plugins.OnlineStoreBookInfo;
import org.coolreader.plugins.OnlineStorePluginManager;
import org.coolreader.plugins.OnlineStoreWrapper;

import java.io.File;

public class OnlineStoreBookInfoDialog extends BaseDialog {
	private CoolReader mActivity;
	private final CoverpageManager mCoverpageManager;
	private final ServiceLifecycle mServiceLifecycle;
	private final OnlineStoreDialogSession session =
			new OnlineStoreDialogSession();
	private OnlineStoreBookInfo mBookInfo;
	private FileInfo mFileInfo;
	private LayoutInflater mInflater;
	private int mWindowSize;
	private OnlineStoreWrapper mPlugin;
	private File downloadDir;
	private File downloadTrialDir;
	private File downloadFilename;
	private File downloadTrialFilename;
	
	private ViewGroup mContentView;
	
	public OnlineStoreBookInfoDialog(
			CoolReader activity,
			Scanner scanner,
			CoverpageManager coverpageManager,
			OnlineStoreBookInfo book,
			FileInfo fileInfo)
	{
		super(activity, null, false, false);
		this.mCoverpageManager = coverpageManager;
		this.mServiceLifecycle =
				activity.getServiceDependencies().getLifecycle();
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = Math.min(outMetrics.widthPixels, outMetrics.heightPixels);
		this.mActivity = activity;
		this.mBookInfo = book;
		this.mFileInfo = fileInfo;
		this.mPlugin = OnlineStorePluginManager.getPlugin(mActivity, fileInfo.getOnlineCatalogPluginPackage());
		File baseDir = new File(scanner.getDownloadDirectory().pathname);
		this.downloadDir = new File(baseDir, mPlugin.getDescription());
		this.downloadFilename = new File(downloadDir, book.book.downloadFileName);
		this.downloadTrialDir = new File(baseDir, mPlugin.getDescription() + "-trials");
		this.downloadTrialFilename = book.book.trialFileName != null ? new File(downloadTrialDir, book.book.trialFileName) : null;
	}

	@Override
	protected void onCreate() {
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        super.onCreate();
	}
	
	RatingBar rbBookRating;
    Button btnBuyOrDownload;
    Button btnPreview;
    TextView lblTitle;
    TextView lblSeries;
    TextView lblAuthors;
    TextView lblFileInfo;
    TextView lblLogin;
    TextView lblStatus;
    TextView lblBalance;
    TextView lblPrice;
    TextView lblNormalPrice;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mInflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.online_store_book_info_dialog, null);
        mContentView = view;
        
        ImageButton btnBack = view.findViewById(R.id.base_dlg_btn_back);
        btnBack.setOnClickListener(v -> onNegativeButtonClick());
        btnBuyOrDownload = view.findViewById(R.id.btn_buy);
        btnBuyOrDownload.setOnClickListener(v -> onBuyButtonClick());
        btnPreview = view.findViewById(R.id.btn_preview);
        btnPreview.setOnClickListener(v -> onPreviewButtonClick());
        lblTitle = view.findViewById(R.id.lbl_book_title);
        lblSeries = view.findViewById(R.id.lbl_book_series);
        lblAuthors = view.findViewById(R.id.lbl_book_author);
        lblFileInfo = view.findViewById(R.id.lbl_book_file_info);
        lblLogin = view.findViewById(R.id.lbl_login);
        lblStatus = view.findViewById(R.id.lbl_status);
        lblBalance = view.findViewById(R.id.lbl_balance);
        lblPrice = view.findViewById(R.id.lbl_price);
        lblNormalPrice = view.findViewById(R.id.lbl_normal_price);
        rbBookRating = view.findViewById(R.id.book_rating);

        final ImageView image = view.findViewById(R.id.book_cover);
        int w = mWindowSize * 4 / 10;
        int h = w * 4 / 3;
        image.setMinimumHeight(h);
        image.setMaxHeight(h);
        image.setMinimumWidth(w);
        image.setMaxWidth(w);
        Bitmap bmp = Bitmap.createBitmap(w, h, Config.RGB_565);
		OnlineStoreDialogSession.Request coverRequest =
				session.replace(
						OnlineStoreDialogSession.Channel.COVER);
        mCoverpageManager.drawCoverpageFor(
				mActivity.getDB(),
				mFileInfo,
				bmp,
				false,
				(file, bitmap) -> {
					if (mServiceLifecycle.isActive()
							&& session.complete(coverRequest)) {
						BitmapDrawable drawable =
								new BitmapDrawable(bitmap);
						image.setImageDrawable(drawable);
					}
				});

        if (mBookInfo.book.rating > 0)
        	rbBookRating.setRating(mBookInfo.book.rating / 2.0f);
        else
        	rbBookRating.setVisibility(View.GONE);
		progress = new ProgressPopup(mActivity, mContentView);
        updateInfo();
        setView(view);
	}
	
	private String getString(int resourceId) {
		return getContext().getString(resourceId);
	}
	
	private void updateInfo() {
		lblTitle.setText(mBookInfo.book.bookTitle);
		lblAuthors.setText(Utils.formatAuthorsNormalNames(mBookInfo.book.getAuthors()));
		lblSeries.setText(mBookInfo.book.getSeries());
        lblLogin.setText(mBookInfo.isLoggedIn ? mBookInfo.login : getString(R.string.online_store_please_login));
        lblBalance.setText("");
        lblStatus.setText(mBookInfo.isPurchased ? getString(R.string.online_store_status_purchased) : "");
        lblPrice.setText(mBookInfo.book.price > 0 ? getString(R.string.online_store_price) + " " + mBookInfo.book.price : getString(R.string.online_store_status_free));
        lblNormalPrice.setText(mBookInfo.book.price != mBookInfo.book.basePrice ? String.valueOf(mBookInfo.book.basePrice) : "");
        lblNormalPrice.setPaintFlags(lblNormalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        lblFileInfo.setText(Utils.formatSize(mBookInfo.book.zipSize));
        if (mBookInfo.book.trialUrl == null)
        	btnPreview.setVisibility(View.GONE);
        else {
        	if (bookFileExists(true))
        		btnPreview.setText(R.string.online_store_open_trial);
        	else
        		btnPreview.setText(R.string.online_store_download_trial);
        }
        btnBuyOrDownload.setVisibility(View.VISIBLE);
        if (bookFileExists(false)) {
			btnBuyOrDownload.setText(R.string.online_store_open);
        } else if (mBookInfo.isLoggedIn && mBookInfo.isPurchased) {
			btnBuyOrDownload.setText(R.string.online_store_download);
		} else if (mBookInfo.isLoggedIn) {
			btnBuyOrDownload.setVisibility(View.GONE);
		} else {
			btnBuyOrDownload.setText(R.string.online_store_login);
		}
	}
	
	@Override
	protected void onPositiveButtonClick() {
		super.onPositiveButtonClick();
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	
	protected void onBuyButtonClick() {
		if (bookFileExists(false)) {
			openBook(false);
		} else if (mBookInfo.isLoggedIn && mBookInfo.isPurchased) {
			download(false);
		} else if (!mBookInfo.isLoggedIn) {
			// LOGIN
			OnlineStoreDialogSession.Request request =
					session.replace(
							OnlineStoreDialogSession.Channel.BOOK_INFO);
			if (request == null)
				return;
			OnlineStoreLoginDialog dlg =
					new OnlineStoreLoginDialog(
							mActivity,
							mPlugin,
							() -> {
								if (mServiceLifecycle.isActive()
										&& session.complete(request))
									reloadBookInfo();
							});
			dlg.show();
		}
	}
	
	private ProgressPopup progress;
	
	private void reloadBookInfo() {
		if (!mServiceLifecycle.isActive())
			return;
		String bookId = mFileInfo.getOnlineCatalogPluginId();
		OnlineStoreDialogSession.Request request =
				session.replace(
						OnlineStoreDialogSession.Channel.BOOK_INFO);
		if (request == null)
			return;
		setActionsEnabled(false);
		progress.show();
		try {
			AsyncOperationControl control =
					mPlugin.loadBookInfo(
							bookId,
							new BookInfoCallback() {
								@Override
								public void onError(
										int errorCode,
										String errorMessage) {
									postBookInfoError(request);
								}

								@Override
								public void onBookInfoReady(
										OnlineStoreBookInfo bookInfo) {
									postBookInfoReady(
											request, bookInfo);
								}
							});
			session.attachCancellation(request, control::cancel);
		} catch (RuntimeException e) {
			L.e("Cannot reload online-store book info", e);
			postBookInfoError(request);
		}
	}

	private void postBookInfoError(
			OnlineStoreDialogSession.Request request) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!mServiceLifecycle.isActive()
					|| !session.complete(request))
				return;
			progress.hide();
			setActionsEnabled(true);
			mActivity.showToast("Error while loading book info");
		});
	}

	private void postBookInfoReady(
			OnlineStoreDialogSession.Request request,
			OnlineStoreBookInfo bookInfo) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!mServiceLifecycle.isActive()
					|| !session.complete(request))
				return;
			progress.hide();
			setActionsEnabled(true);
			if (bookInfo != null) {
				mBookInfo = bookInfo;
				updateInfo();
			}
		});
	}
	
	protected void onPreviewButtonClick() {
		if (bookFileExists(true))
			openBook(true);
		else
			download(true);
	}
	
	private boolean ensureDownloadDirectoryExists(boolean trial) {
		File dir = (trial ? downloadTrialDir : downloadDir);
		if (dir.isDirectory())
			return true;
		return dir.mkdirs();
	}
	
	private File getBookFile(boolean trial) {
		return (trial ? downloadTrialFilename : downloadFilename);
	}
	
	private boolean bookFileExists(boolean trial) {
		return getBookFile(trial).exists();
	}
	
	private void download(final boolean trial) {
		File f = getBookFile(trial);
		if (!ensureDownloadDirectoryExists(trial)) {
			mActivity.showToast("Cannot create download directory " + f.getAbsolutePath());
			return;
		}
		OnlineStoreDialogSession.Request request =
				session.replace(
						OnlineStoreDialogSession.Channel.DOWNLOAD);
		if (request == null)
			return;
		setActionsEnabled(false);
		progress.show();
		try {
			AsyncOperationControl control =
					mPlugin.downloadBook(
							mBookInfo.book,
							trial,
							f,
							new DownloadBookCallback() {
								@Override
								public void onError(
										int errorCode,
										String errorMessage) {
									postDownloadError(
											request,
											errorMessage);
								}

								@Override
								public void onBookDownloaded(
										OnlineStoreBook book,
										boolean downloadedTrial,
										File savedFileName) {
									postDownloadReady(
											request,
											trial);
								}
							});
			session.attachCancellation(request, control::cancel);
		} catch (RuntimeException e) {
			L.e("Cannot start online-store download", e);
			postDownloadError(request, e.getMessage());
		}
	}

	private void postDownloadError(
			OnlineStoreDialogSession.Request request,
			String errorMessage) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!mServiceLifecycle.isActive()
					|| !session.complete(request))
				return;
			progress.hide();
			setActionsEnabled(true);
			mActivity.showToast(
					"Error while downloading book"
							+ (errorMessage != null
									? ": " + errorMessage
									: ""));
		});
	}

	private void postDownloadReady(
			OnlineStoreDialogSession.Request request,
			boolean trial) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!mServiceLifecycle.isActive()
					|| !session.complete(request))
				return;
			progress.hide();
			setActionsEnabled(true);
			openBook(trial);
		});
	}

	private void setActionsEnabled(boolean enabled) {
		btnBuyOrDownload.setEnabled(enabled);
		btnPreview.setEnabled(enabled);
	}
	
	private void openBook(boolean trial) {
		File book = getBookFile(trial);
		dismiss();
		mActivity.loadDocument(
				DocumentSource.file(book.getAbsolutePath()),
				null, null, true);
	}

	@Override
	protected void onClose() {
		session.close();
		if (progress != null)
			progress.hide();
		super.onClose();
	}
}
