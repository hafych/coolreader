/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import android.content.Intent;

import org.coolreader.Dictionaries.DictInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable owner of external dictionary integration definitions.
 */
final class DictionaryCatalog {
	private final List<DictInfo> entries;

	DictionaryCatalog(List<DictInfo> entries) {
		if (entries == null || entries.isEmpty())
			throw new IllegalArgumentException(
					"dictionary catalog must not be empty");
		List<DictInfo> copy = new ArrayList<>(entries.size());
		Set<String> ids = new HashSet<>();
		for (DictInfo entry : entries) {
			if (entry == null
					|| entry.id == null
					|| entry.id.length() == 0
					|| !ids.add(entry.id)) {
				throw new IllegalArgumentException(
						"dictionary IDs must be non-empty and unique");
			}
			copy.add(entry);
		}
		this.entries = Collections.unmodifiableList(copy);
	}

	static DictionaryCatalog legacy() {
		return new DictionaryCatalog(Arrays.asList(
				new DictInfo(
						"Fora",
						"Fora Dictionary",
						"com.ngc.fora",
						"com.ngc.fora.ForaDictionary",
						Intent.ACTION_SEARCH,
						0),
				new DictInfo(
						"ColorDict",
						"ColorDict",
						"com.socialnmobile.colordict",
						"com.socialnmobile.colordict.activity.Main",
						Intent.ACTION_SEARCH,
						0),
				new DictInfo(
						"ColorDictApi",
						"ColorDict new / GoldenDict",
						"com.socialnmobile.colordict",
						"com.socialnmobile.colordict.activity.Main",
						Intent.ACTION_SEARCH,
						1),
				new DictInfo(
						"AardDict",
						"Aard Dictionary",
						"aarddict.android",
						"aarddict.android.Article",
						Intent.ACTION_SEARCH,
						0),
				new DictInfo(
						"AardDictLookup",
						"Aard Dictionary Lookup",
						"aarddict.android",
						"aarddict.android.Lookup",
						Intent.ACTION_SEARCH,
						0),
				new DictInfo(
						"Aard2",
						"Aard 2 Dictionary",
						"itkach.aard2",
						"aard2.lookup",
						Intent.ACTION_SEARCH,
						3),
				new DictInfo(
						"OnyxDictOld",
						"ONYX Dictionary (Old)",
						"com.onyx.dict",
						"com.onyx.dict.activity.DictMainActivity",
						Intent.ACTION_VIEW,
						0,
						"android.intent.action.SEARCH"),
				new DictInfo(
						"OnyxDict",
						"ONYX Dictionary",
						"com.onyx.dict",
						"com.onyx.dict.main.ui.DictMainActivity",
						Intent.ACTION_VIEW,
						0,
						"android.intent.action.SEARCH"),
				new DictInfo(
						"OnyxDictWindowed",
						"ONYX Dictionary (Windowed)",
						"com.onyx.dict",
						"com.onyx.dict.translation.ui.ProcessTextActivity",
						Intent.ACTION_VIEW,
						0,
						"android.intent.extra.PROCESS_TEXT"),
				new DictInfo(
						"Dictan",
						"Dictan Dictionary",
						"info.softex.dictan",
						null,
						Intent.ACTION_VIEW,
						2),
				new DictInfo(
						"FreeDictionary.org",
						"Free Dictionary . org",
						"org.freedictionary",
						"org.freedictionary.MainActivity",
						Intent.ACTION_VIEW,
						0),
				new DictInfo(
						"ABBYYLingvo",
						"ABBYY Lingvo",
						"com.abbyy.mobile.lingvo.market",
						null,
						"com.abbyy.mobile.lingvo.intent.action.TRANSLATE",
						0,
						"com.abbyy.mobile.lingvo.intent.extra.TEXT"),
				new DictInfo(
						"LingoQuizLite",
						"Lingo Quiz Lite",
						"mnm.lite.lingoquiz",
						"mnm.lite.lingoquiz.ExchangeActivity",
						"lingoquiz.intent.action.ADD_WORD",
						0,
						"EXTRA_WORD"),
				new DictInfo(
						"LingoQuiz",
						"Lingo Quiz",
						"mnm.lingoquiz",
						"mnm.lingoquiz.ExchangeActivity",
						"lingoquiz.intent.action.ADD_WORD",
						0,
						"EXTRA_WORD"),
				new DictInfo(
						"LEODictionary",
						"LEO Dictionary",
						"org.leo.android.dict",
						"org.leo.android.dict.LeoDict",
						Intent.ACTION_SEARCH,
						0,
						"query"),
				new DictInfo(
						"PopupDictionary",
						"Popup Dictionary",
						"com.barisatamer.popupdictionary",
						"com.barisatamer.popupdictionary.MainActivity",
						Intent.ACTION_VIEW,
						0),
				new DictInfo(
						"GoogleTranslate",
						"Google Translate",
						"com.google.android.apps.translate",
						"com.google.android.apps.translate.TranslateActivity",
						Intent.ACTION_SEND,
						4),
				new DictInfo(
						"YandexTranslate",
						"Yandex Translate",
						"ru.yandex.translate",
						"ru.yandex.translate.ui.activities.MainActivity",
						Intent.ACTION_SEND,
						4),
				new DictInfo(
						"Wikipedia",
						"Wikipedia",
						"org.wikipedia",
						"org.wikipedia.search.SearchActivity",
						Intent.ACTION_SEND,
						0)));
	}

	List<DictInfo> entries() {
		return entries;
	}

	DictInfo findById(String id) {
		for (DictInfo entry : entries) {
			if (entry.id.equals(id))
				return entry;
		}
		return null;
	}

	DictInfo[] snapshot() {
		return entries.toArray(new DictInfo[0]);
	}
}
