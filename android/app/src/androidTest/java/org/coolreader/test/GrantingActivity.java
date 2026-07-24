package org.coolreader.test;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class GrantingActivity extends Activity {
	public static final String EXTRA_PERSISTABLE = "persistable";
	public static final String EXTRA_DOCUMENT = "document";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String document = getIntent().getStringExtra(EXTRA_DOCUMENT);
		Uri uri = "temporary".equals(document)
				? TestDocumentProvider.TEMPORARY_BOOK
				: TestDocumentProvider.PERSISTED_BOOK;
		Intent open = new Intent(Intent.ACTION_VIEW);
		open.setClassName("org.coolreader", "org.coolreader.CoolReader");
		open.setDataAndType(uri, "application/octet-stream");
		open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		if (getIntent().getBooleanExtra(EXTRA_PERSISTABLE, false))
			open.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		startActivity(open);
		finish();
	}
}
