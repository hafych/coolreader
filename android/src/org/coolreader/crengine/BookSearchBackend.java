/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

/**
 * Narrow asynchronous backend used by the library book-search dialog.
 */
interface BookSearchBackend {
	void find(Query query, ResultsCallback callback);

	interface ResultsCallback {
		void onResults(FileInfo[] results);
	}

	final class Query {
		final int maxResults;
		final String authors;
		final String title;
		final String series;
		final String filename;

		Query(
				int maxResults,
				String authors,
				String title,
				String series,
				String filename) {
			this.maxResults = maxResults;
			this.authors = authors;
			this.title = title;
			this.series = series;
			this.filename = filename;
		}
	}
}
