/*
 * CoolReader for Android
 * Copyright (C) 2011,2012 Vadim Lopatin <coolreader.org@gmail.com>
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

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.coolreader.R;

public class BookSearchDialog extends BaseDialog {

	private static final int MAX_RESULTS = 50;
	private static final long PREVIEW_DELAY_MS = 3000L;

	private final EditText authorEdit;
	private final EditText titleEdit;
	private final EditText seriesEdit;
	private final EditText filenameEdit;
	private final TextView statusText;
	private final BookSearchBackend backend;
	private final SearchCallback callback;
	private final BookSearchSession session = new BookSearchSession();
	private final DelayedExecutor previewScheduler =
			DelayedExecutor.createGUI("book-search-preview");

	public BookSearchDialog(
			BaseActivity activity,
			BookSearchBackend backend,
			SearchCallback callback)
	{
		super(activity, activity.getString( R.string.dlg_book_search), true, false);
		this.backend = backend;
		this.callback = callback;
		setTitle(activity.getString(R.string.dlg_book_search));
		LayoutInflater inflater = LayoutInflater.from(getContext());
		View view = inflater.inflate(R.layout.book_search_dialog, null);
		authorEdit = view.findViewById(R.id.search_text_author);
		titleEdit = view.findViewById(R.id.search_text_title);
		seriesEdit = view.findViewById(R.id.search_text_series);
		filenameEdit = view.findViewById(R.id.search_text_filename);
		statusText = view.findViewById(R.id.search_status);
		TextWatcher watcher = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				postSearchTask();
			}
			
		}; 
		authorEdit.addTextChangedListener(watcher);
		seriesEdit.addTextChangedListener(watcher);
		titleEdit.addTextChangedListener(watcher);
		filenameEdit.addTextChangedListener(watcher);
		setView( view );
	}

	private void postSearchTask() {
		final BookSearchSession.Preview preview =
				session.replacePreview();
		if (preview == null)
			return;
		previewScheduler.postDelayed(
				() -> startPreview(preview),
				PREVIEW_DELAY_MS);
	}

	private void startPreview(BookSearchSession.Preview preview) {
		if (!session.isPreviewActive(preview))
			return;
		backend.find(query(), results -> {
			if (!session.completePreview(preview))
				return;
			statusText.setText(
					getContext().getString(
							R.string.dlg_book_search_found)
							+ " " + results.length);
		});
	}
	
	public interface SearchCallback {
		void done(FileInfo[] results);
	}

	private BookSearchBackend.Query query() {
		return new BookSearchBackend.Query(
				MAX_RESULTS,
				authorEdit.getText().toString().trim(),
				titleEdit.getText().toString().trim(),
				seriesEdit.getText().toString().trim(),
				filenameEdit.getText().toString().trim());
	}
	
	@Override
	protected void onPositiveButtonClick() {
		if (!session.submit())
			return;
		BookSearchBackend.Query query = query();
		previewScheduler.cancel();
		super.onPositiveButtonClick();
		backend.find(query, callback::done);
	}

	@Override
	protected void onNegativeButtonClick() {
		if (!session.cancel())
			return;
		previewScheduler.cancel();
		super.onNegativeButtonClick();
		callback.done(null);
	}
}
