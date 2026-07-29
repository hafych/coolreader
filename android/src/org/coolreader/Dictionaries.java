/*
 * CoolReader for Android
 * Copyright (C) 2011,2012,2015 Vadim Lopatin <coolreader.org@gmail.com>
 * Copyright (C) 2011 a_lone
 * Copyright (C) 2012 Jeff Doozan <jeff@doozan.com>
 * Copyright (C) 2018 Yuri Plotnikov <plotnikovya@gmail.com>
 * Copyright (C) 2020 cybersphinx <chr.ohm@gmx.net>
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

package org.coolreader;

import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.DeviceInfo;
import org.coolreader.crengine.L;
import org.coolreader.crengine.Logger;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;

import java.util.ArrayList;
import java.util.List;

public class Dictionaries {

	private Activity mActivity;
	private final ActivityResultLauncher<Intent> mDictanLauncher;

	public Integer isiDic2IsActive() {
		return iDic2IsActive;
	}

	public void setiDic2IsActive(Integer iDic2IsActive) {
		this.iDic2IsActive = iDic2IsActive;
	}

	public void setAdHocDict(DictInfo dict) {
		this.currentDictionary3 = dict;
	}

	private Integer iDic2IsActive = 0;

	public Dictionaries(
			Activity activity,
			ActivityResultLauncher<Intent> dictanLauncher) {
		mActivity = activity;
		mDictanLauncher = dictanLauncher;
		currentDictionary = defaultDictionary();
		currentDictionary2 = defaultDictionary();
	}
	
	DictInfo currentDictionary;
	DictInfo currentDictionary2;
	DictInfo currentDictionary3;
	
	public static final class DictInfo {
		public final String id; 
		public final String name;
		public final String packageName;
		public final String className;
		public final String action;
		public final Integer internal;
		public final String dataKey;

		public DictInfo(
				String id,
				String name,
				String packageName,
				String className,
				String action,
				Integer internal) {
			this(
					id,
					name,
					packageName,
					className,
					action,
					internal,
					SearchManager.QUERY);
		}

		public DictInfo(
				String id,
				String name,
				String packageName,
				String className,
				String action,
				Integer internal,
				String dataKey) {
			this.id = id;
			this.name = name;
			this.packageName = packageName;
			this.className = className;
			this.action = action;
			this.internal = internal;
			this.dataKey = dataKey;
		}
	}

	private static final DictionaryCatalog DICTIONARY_CATALOG =
			DictionaryCatalog.legacy();

	public static final String DEFAULT_DICTIONARY_ID = "Fora";
	public static final String DEFAULT_ONYX_DICTIONARY_ID = "OnyxDictWindowed";

	static DictInfo findById(String id) {
		return DICTIONARY_CATALOG.findById(id);
	}
	
	public static DictInfo defaultDictionary() {
		if (DeviceInfo.EINK_ONYX)
			return findById(DEFAULT_ONYX_DICTIONARY_ID);
		return findById(DEFAULT_DICTIONARY_ID);
	}
		
	
	public static DictInfo[] getDictList() {
		return DICTIONARY_CATALOG.snapshot();
	}


	public static List<DictInfo> getDictListExt(BaseActivity act, boolean bOnlyInstalled) {
		ArrayList<DictInfo> dlist = new ArrayList<DictInfo>();
		for (DictInfo dict : DICTIONARY_CATALOG.entries()) {
			boolean installed = act.isPackageInstalled(dict.packageName);
			if ((dict.internal == 1) && (dict.packageName.equals("com.socialnmobile.colordict")) && (!installed)) {
				installed = act.isPackageInstalled("mobi.goldendict.android");
			}
			if ((installed) || (!bOnlyInstalled)) dlist.add(dict);
		}
		return dlist;
	}

	public void setDict( String id ) {
		DictInfo d = findById(id);
		if (d != null)
			currentDictionary = d;
	}

	public void setDict2( String id ) {
		DictInfo d = findById(id);
		if (d != null)
			currentDictionary2 = d;
	}
	
	public boolean isPackageInstalled(String packageName) {
        PackageManager pm = mActivity.getPackageManager();
        try
        {
            pm.getPackageInfo(packageName, 0); //PackageManager.GET_ACTIVITIES);
            return true;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

	private final static String DICTAN_ARTICLE_WORD = "article.word";
	
	private final static String DICTAN_ERROR_MESSAGE = "error.message";

	private final static int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;
	
	public static final Logger log = L.create("cr3dict");
	
	@SuppressWarnings("serial")
	public static class DictionaryException extends Exception {
		public DictionaryException(String msg) {
			super(msg);
		}
	}
	
	@SuppressLint("NewApi")
	public void findInDictionary(String s) throws DictionaryException {
		log.d("lookup in dictionary: " + s);
		DictInfo curDict = currentDictionary;
		if (iDic2IsActive > 0 && currentDictionary2 != null)
			curDict = currentDictionary2;
		if (iDic2IsActive > 1)
			iDic2IsActive = 0;
		if (currentDictionary3 != null)
			curDict = currentDictionary3;
		currentDictionary3 = null;
		if (null == curDict) {
			throw new DictionaryException("Current dictionary are invalid!");
		}
		switch (curDict.internal) {
		case 0:
			Intent intent0 = new Intent(curDict.action);
			if (curDict.className != null) {
				intent0.setComponent(new ComponentName(
						curDict.packageName, curDict.className));
			} else {
				intent0.setPackage(curDict.packageName);
			}
			intent0.addFlags(DeviceInfo.getSDKLevel() >= 7 ? Intent.FLAG_ACTIVITY_CLEAR_TASK : Intent.FLAG_ACTIVITY_NEW_TASK);
			if (s!=null)
				intent0.putExtra(curDict.dataKey, s);
			try {
				mActivity.startActivity( intent0 );
			} catch ( ActivityNotFoundException e ) {
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
			} catch ( Exception e ) {
				throw new DictionaryException("Can't open dictionary \"" + curDict.name + "\"");
			}
			break;
		case 1:
			final String SEARCH_ACTION  = "colordict.intent.action.SEARCH";
			final String EXTRA_QUERY   = "EXTRA_QUERY";
			final String EXTRA_FULLSCREEN = "EXTRA_FULLSCREEN";
//			final String EXTRA_HEIGHT  = "EXTRA_HEIGHT";
//			final String EXTRA_WIDTH   = "EXTRA_WIDTH";
//			final String EXTRA_GRAVITY  = "EXTRA_GRAVITY";
//			final String EXTRA_MARGIN_LEFT = "EXTRA_MARGIN_LEFT";
//			final String EXTRA_MARGIN_TOP  = "EXTRA_MARGIN_TOP";
//			final String EXTRA_MARGIN_BOTTOM = "EXTRA_MARGIN_BOTTOM";
//			final String EXTRA_MARGIN_RIGHT = "EXTRA_MARGIN_RIGHT";

			Intent intent1 = new Intent(SEARCH_ACTION);
			if (s!=null)
				intent1.putExtra(EXTRA_QUERY, s); //Search Query
			intent1.putExtra(EXTRA_FULLSCREEN, true); //
			try
			{
				mActivity.startActivity(intent1);
			} catch ( ActivityNotFoundException e ) {
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
			}
			break;
		case 2:
			// Dictan support
			Intent intent2 = new Intent("android.intent.action.VIEW");
			// Add custom category to run the Dictan external dispatcher
            intent2.addCategory("info.softex.dictan.EXTERNAL_DISPATCHER");
            
   	        // Don't include the dispatcher in activity  
            // because it doesn't have any content view.	      
            intent2.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		  
			intent2.putExtra(DICTAN_ARTICLE_WORD, s);

			try {
				mDictanLauncher.launch(intent2);
			} catch (ActivityNotFoundException e) {
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
			}
			break;
		case 3:
			Intent intent3 = new Intent("aard2.lookup");
			intent3.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent3.putExtra(SearchManager.QUERY, s);
			try
			{
				mActivity.startActivity(intent3);
			} catch ( ActivityNotFoundException e ) {
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
			}
			break;
		case 4:
			Intent intent4 = new Intent(android.content.Intent.ACTION_SEND);
			intent4.setType("text/plain");
			intent4.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
			intent4.putExtra(android.content.Intent.EXTRA_TEXT, s);
			//List<ResolveInfo> resInfo = mActivity.getPackageManager().queryIntentActivities(intent4, 0);
			//for (resInfo : mActivity.getPackageManager().queryIntentActivities(intent4, 0)) {
			//	if (resInfo.
			//};
			//startActivity(Intent.createChooser(intent4, null));
			//intent4.setAction(Intent.ACTION_VIEW);
			//intent4.putExtra("key_text_input", "What time is it?");
			//intent4.putExtra("key_text_output", "");
			//intent4.putExtra("key_language_from", "en");
			//intent4.putExtra("key_language_to", "es");
			//intent4.putExtra("key_suggest_translation", "");
			//intent4.putExtra("key_from_floating_window", false);
			intent4.setComponent(new ComponentName(curDict.packageName, curDict.className));
			try
			{
				mActivity.startActivity(intent4);
			} catch ( ActivityNotFoundException e ) {
				throw new DictionaryException("Dictionary \"" + curDict.name + "\" is not installed");
			}
			break;
		}

	}

	public void handleDictanResult(int resultCode, Intent intent) throws DictionaryException {
		switch (resultCode) {

			// The article has been shown, the intent is never expected null
			case Activity.RESULT_OK:
				break;

			// Error occurred
			case Activity.RESULT_CANCELED:
				String errMessage = "Unknown Error.";
				if (intent != null) {
					errMessage = "The Requested Word: "
							+ intent.getStringExtra(DICTAN_ARTICLE_WORD)
							+ ". Error: "
							+ intent.getStringExtra(DICTAN_ERROR_MESSAGE);
				}
				throw new DictionaryException(errMessage);

			// Must never occur
			default:
				throw new DictionaryException("Unknown Result Code: " + resultCode);
		}
	}

}
