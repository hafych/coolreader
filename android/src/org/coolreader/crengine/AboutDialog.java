/*
 * CoolReader for Android
 * Copyright (C) 2011,2012 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2018 Yuri Plotnikov <plotnikovya@gmail.com>
 * Copyright (C) 2018,2021 Aleksey Chernov <valexlin@gmail.com>
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
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;
import java.util.Iterator;

public class AboutDialog extends BaseDialog implements TabContentFactory {
	final CoolReader mCoolReader;
	
	private View mAppTab;
	private View mDirsTab;
	private View mLicenseTab;

	public AboutDialog(CoolReader activity, Engine engine)
	{
		super(activity);
		mCoolReader = activity;

		setTitle(R.string.dlg_about);
		LayoutInflater inflater = LayoutInflater.from(getContext());
		TabHost tabs = (TabHost)inflater.inflate(R.layout.about_dialog, null);
		mAppTab = inflater.inflate(R.layout.about_dialog_app, null);
		((TextView)mAppTab.findViewById(R.id.version)).setText("Cool Reader " + mCoolReader.getVersion());

		mDirsTab = inflater.inflate(R.layout.about_dialog_dirs, null);
		TextView fonts_dir = mDirsTab.findViewById(R.id.fonts_dirs);

		ArrayList<String> fontsDirs = Engine.getFontsDirs();
		StringBuilder sbuf = new StringBuilder();
		Iterator<String> it = fontsDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		fonts_dir.setText(sbuf.toString());

		ArrayList<String> testuresDirs = Engine.getDataDirs(Engine.DataDirType.TexturesDirs);
		sbuf = new StringBuilder();
		it = testuresDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		TextView textures_dir = mDirsTab.findViewById(R.id.textures_dirs);
		textures_dir.setText(sbuf.toString());

		ArrayList<String> backgroundsDirs = Engine.getDataDirs(Engine.DataDirType.BackgroundsDirs);
		sbuf = new StringBuilder();
		it = backgroundsDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		TextView backgrounds_dir = mDirsTab.findViewById(R.id.backgrounds_dirs);
		backgrounds_dir.setText(sbuf.toString());

		ArrayList<String> hyphDirs = Engine.getDataDirs(Engine.DataDirType.HyphsDirs);
		sbuf = new StringBuilder();
		it = hyphDirs.iterator();
		while (it.hasNext()) {
			String s = it.next();
			sbuf.append(s);
			if (it.hasNext()) {
				sbuf.append("\n");
			}
		}
		TextView hyph_dir = mDirsTab.findViewById(R.id.hyph_dirs);
		hyph_dir.setText(sbuf.toString());

		mLicenseTab = inflater.inflate(R.layout.about_dialog_license, null);
		String license = engine.loadResourceUtf8(R.raw.license);
		((TextView)mLicenseTab.findViewById(R.id.license)).setText(license);
		
		tabs.setup();
		TabHost.TabSpec tsApp = tabs.newTabSpec("App");
		tsApp.setIndicator("", 
				getContext().getResources().getDrawable(R.drawable.cr3_menu_link));
		tsApp.setContent(this);
		tabs.addTab(tsApp);

		TabHost.TabSpec tsDirectories = tabs.newTabSpec("Directories");
		tsDirectories.setIndicator("",
				getContext().getResources().getDrawable(R.drawable.ic_menu_archive));
		tsDirectories.setContent(this);
		tabs.addTab(tsDirectories);

		TabHost.TabSpec tsLicense = tabs.newTabSpec("License");
		tsLicense.setIndicator("", 
				getContext().getResources().getDrawable(R.drawable.ic_menu_star));
		tsLicense.setContent(this);
		tabs.addTab(tsLicense);
		
		setView( tabs );
		
	}

	
	@Override
	public View createTabContent(String tag) {

		if ( "App".equals(tag) )
			return mAppTab;
		else if ( "Directories".equals(tag) )
			return mDirsTab;
		else if ( "License".equals(tag) )
			return mLicenseTab;
		return null;
	}
	
}
