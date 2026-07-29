/*
 * CoolReader for Android
 * Copyright (C) 2011-2014 Vadim Lopatin <coolreader.org@gmail.com>
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

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.db.CRDBService;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class OPDSCatalogEditDialog extends BaseDialog {

	private final CoolReader mActivity;
	private final LayoutInflater mInflater;
	private final FileInfo mItem;
	private final EditText nameEdit;
	private final EditText urlEdit;
	private final Runnable mOnUpdate;
	private final ServiceLifecycle serviceLifecycle;
	private final CatalogEditSession editSession =
			new CatalogEditSession();

	public OPDSCatalogEditDialog(CoolReader activity, FileInfo item, Runnable onUpdate) {
		super(activity, activity.getString((item.id == null) ? R.string.dlg_catalog_add_title
				: R.string.dlg_catalog_edit_title), true,
				false);
		mActivity = activity;
		mItem = item;
		mOnUpdate = onUpdate;
		serviceLifecycle =
				activity.getServiceDependencies().getLifecycle();
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.catalog_edit_dialog, null);
		nameEdit = (EditText) view.findViewById(R.id.catalog_name);
		urlEdit = (EditText) view.findViewById(R.id.catalog_url);
		nameEdit.setText(mItem.filename);
		urlEdit.setText(mItem.getOPDSUrl());
		setThirdButtonImage(Utils.resolveResourceIdByAttr(activity, R.attr.cr3_button_remove_drawable, R.drawable.cr3_button_remove),
				R.string.mi_catalog_delete);
		setView(view);
	}

	@Override
	protected void onPositiveButtonClick() {
		String url = urlEdit.getText().toString();
		String name = nameEdit.getText().toString();
		boolean blacklisted = checkBlackList(url);
		if (blacklisted
				&& OPDSConst.BLACK_LIST_MODE
						== OPDSConst.BLACK_LIST_MODE_FORCE) {
			mActivity.showToast(R.string.black_list_enforced);
		} else if (blacklisted
				&& OPDSConst.BLACK_LIST_MODE
						== OPDSConst.BLACK_LIST_MODE_WARN) {
			showBlacklistWarning(url, name);
		} else {
			saveAndClose(url, name);
		}
	}

	private void showBlacklistWarning(
			String url,
			String name) {
		if (!editSession.beginConfirmation())
			return;
		AlertDialog warning =
				new AlertDialog.Builder(mActivity)
						.setMessage(R.string.black_list_warning)
						.setPositiveButton(
								R.string.dlg_button_ok,
								(dialog, which) ->
										saveAndClose(url, name))
						.setNegativeButton(
								R.string.dlg_button_cancel,
								(dialog, which) ->
										onNegativeButtonClick())
						.create();
		warning.setOnCancelListener(
				dialog -> editSession.cancelConfirmation());
		warning.show();
	}
	
	private boolean checkBlackList(String url) {
		for (String s : OPDSConst.BLACK_LIST) {
			if (s.equals(url))
				return true;
		}
		return false;
	}
	
	private void saveAndClose(String url, String name) {
		if (!editSession.claim(
				CatalogEditSession.TerminalAction.SAVE))
			return;
		persistCatalog(
				mActivity,
				serviceLifecycle,
				mItem.id,
				url,
				name,
				mOnUpdate);
		super.onPositiveButtonClick();
	}

	private static void persistCatalog(
			CoolReader activity,
			ServiceLifecycle lifecycle,
			Long id,
			String url,
			String name,
			Runnable onUpdate) {
		if (!lifecycle.isActive())
			return;
		CRDBService.LocalBinder db = activity.getDB();
		if (db != null) {
			saveCatalog(db, id, url, name);
			if (onUpdate != null && lifecycle.isActive())
				onUpdate.run();
			return;
		}
		activity.waitForCRDBService(() -> {
			if (!lifecycle.isActive())
				return;
			CRDBService.LocalBinder connectedDb =
					activity.getDB();
			if (connectedDb == null)
				return;
			saveCatalog(connectedDb, id, url, name);
			if (onUpdate != null && lifecycle.isActive())
				onUpdate.run();
		});
	}

	private static void saveCatalog(
			CRDBService.LocalBinder db,
			Long id,
			String url,
			String name) {
		db.saveOPDSCatalog(id, url, name);
	}

	@Override
	protected void onNegativeButtonClick() {
		if (!editSession.claim(
				CatalogEditSession.TerminalAction.CANCEL))
			return;
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		if (!editSession.claim(
				CatalogEditSession.TerminalAction.DELETE))
			return;
		mActivity.askDeleteCatalog(mItem);
		super.onThirdButtonClick();
	}

	
}
