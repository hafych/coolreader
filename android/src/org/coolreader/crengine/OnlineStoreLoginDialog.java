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

package org.coolreader.crengine;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.coolreader.R;
import org.coolreader.plugins.AsyncOperationControl;
import org.coolreader.plugins.AuthenticationCallback;
import org.coolreader.plugins.OnlineStoreWrapper;

public class OnlineStoreLoginDialog extends BaseDialog {
	private BaseActivity mActivity;
	private OnlineStoreWrapper mPlugin;
	private LayoutInflater mInflater;
	private final CancelActionState onLoginHandler =
			new CancelActionState();
	private final ServiceLifecycle mServiceLifecycle;
	private final OnlineStoreDialogSession session =
			new OnlineStoreDialogSession();
	public OnlineStoreLoginDialog(BaseActivity activity, OnlineStoreWrapper plugin, Runnable onLoginHandler)
	{
		super(activity, null, false, false);
		mServiceLifecycle =
				activity.getServiceDependencies().getLifecycle();
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mActivity = activity;
		this.mPlugin = plugin;
		this.onLoginHandler.set(onLoginHandler);
	}

	@Override
	protected void onCreate() {
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        super.onCreate();
	}

	
    TextView lblTitle;
    TextView lblDescription;
    Button btnLogin;
    EditText edLogin;
    EditText edPassword;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mInflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.online_store_login_dialog, null);
        
        ImageButton btnBack = view.findViewById(R.id.base_dlg_btn_back);
        btnBack.setOnClickListener(v -> onNegativeButtonClick());
        btnLogin = view.findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> onPositiveButtonClick());
        
        lblTitle = view.findViewById(R.id.dlg_title);
        lblDescription = view.findViewById(R.id.lbl_description);
        

		lblTitle.setText(mPlugin.getName());
		lblDescription.setText(mPlugin.getDescription());
		
        edLogin = view.findViewById(R.id.ed_login);
        edPassword = view.findViewById(R.id.ed_password);
        edLogin.setText(mPlugin.getLogin());
        edPassword.setText(mPlugin.getPassword());
		
        setView(view);
		progress = new ProgressPopup(mActivity, view);
	}
	
	private ProgressPopup progress;
	
	@Override
	protected void onPositiveButtonClick() {
		OnlineStoreDialogSession.Request request =
				session.replace(
						OnlineStoreDialogSession.Channel.AUTHENTICATION);
		if (request == null)
			return;
		String login = edLogin.getText().toString();
		String password = edPassword.getText().toString();
		btnLogin.setEnabled(false);
		progress.show();
		try {
			AsyncOperationControl control =
					mPlugin.authenticate(
							login,
							password,
							new AuthenticationCallback() {
								@Override
								public void onError(
										int errorCode,
										String errorMessage) {
									postAuthenticationError(
											request,
											errorMessage);
								}

								@Override
								public void onSuccess() {
									postAuthenticationSuccess(
											request);
								}
							});
			session.attachCancellation(request, control::cancel);
		} catch (RuntimeException e) {
			L.e("Cannot start online-store authentication", e);
			postAuthenticationError(request, e.getMessage());
		}
	}

	private void postAuthenticationError(
			OnlineStoreDialogSession.Request request,
			String errorMessage) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!mServiceLifecycle.isActive()
					|| !session.complete(request))
				return;
			progress.hide();
			btnLogin.setEnabled(true);
			mActivity.showToast(
					mActivity.getString(
							R.string.online_store_error_cannot_login)
							+ (errorMessage != null
									? " " + errorMessage
									: ""));
		});
	}

	private void postAuthenticationSuccess(
			OnlineStoreDialogSession.Request request) {
		BackgroundThread.instance().executeGUI(() -> {
			if (!mServiceLifecycle.isActive()
					|| !session.complete(request))
				return;
			progress.hide();
			btnLogin.setEnabled(true);
			// Take before dismiss: BaseDialog dismiss → onClose closes
			// the slot, which would drop a post-dismiss take().
			Runnable loginHandler = onLoginHandler.take();
			dismiss();
			mActivity.showToast(
					R.string.online_store_error_successful_login);
			if (loginHandler != null)
				loginHandler.run();
		});
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onClose() {
		session.close();
		onLoginHandler.close();
		if (progress != null)
			progress.hide();
		super.onClose();
	}
}
