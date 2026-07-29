/*
 * CoolReader for Android
 * Copyright (C) 2011,2012,2014 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2014 klush
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.coolreader.R;

public class SwitchProfileDialog extends BaseDialog {
	private final ProfileSwitchHandler profileSwitchHandler;
	private final ListView listView;
	private final int currentProfile;

	SwitchProfileDialog(
			BaseActivity activity,
			ProfileSwitchHandler profileSwitchHandler) {
		super(
				activity,
				activity.getString(
						R.string.action_switch_settings_profile),
				false,
				false);
		if (profileSwitchHandler == null
				|| !profileSwitchHandler.isActive())
			throw new IllegalArgumentException(
					"active profile switch handler is required");
		setCancelable(true);
		this.profileSwitchHandler = profileSwitchHandler;
		listView = new BaseListView(getContext(), false);
		currentProfile =
				profileSwitchHandler.getCurrentProfile();
		listView.setOnItemClickListener(
				(parent, view, position, id) -> {
			profileSwitchHandler.selectProfile(position + 1);
			SwitchProfileDialog.this.dismiss();
		});
		listView.setOnItemLongClickListener(
				(parent, view, position, id) -> {
			// TODO: rename?
			SwitchProfileDialog.this.dismiss();
			return true;
		});
		listView.setLongClickable(true);
		listView.setClickable(true);
		listView.setFocusable(true);
		listView.setFocusableInTouchMode(true);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		setView(listView);
		setFlingHandlers(
				listView,
				SwitchProfileDialog.this::dismiss,
				SwitchProfileDialog.this::dismiss);
		listView.setAdapter(new ProfileListAdapter());
	}

	private static String profileName(int position) {
		return "Profile " + (position + 1);
	}

	private class ProfileListAdapter extends BaseListAdapter {
		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int arg0) {
			return true;
		}

		@Override
		public int getCount() {
			return Settings.MAX_PROFILES;
		}

		@Override
		public Object getItem(int position) {
			return profileName(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view;
			boolean isCurrentItem = position == currentProfile - 1;
			if (convertView == null) {
				LayoutInflater inflater =
						LayoutInflater.from(getContext());
				view = inflater.inflate(
						R.layout.profile_item, parent, false);
			} else {
				view = convertView;
			}
			RadioButton cb = view.findViewById(R.id.option_value_check);
			TextView title = view.findViewById(R.id.option_value_text);
			cb.setChecked(isCurrentItem);
			cb.setFocusable(false);
			cb.setFocusableInTouchMode(false);
			title.setText(profileName(position));
			cb.setOnClickListener(v -> {
				profileSwitchHandler.selectProfile(position + 1);
				SwitchProfileDialog.this.dismiss();
			});
			return view;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}
	}
}
