/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.coolreader.Dictionaries.DictInfo;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class DictionaryCatalogTest {
	@Test
	public void legacyCatalogPreservesEveryIntegrationDefinition() {
		StringBuilder actual = new StringBuilder();
		for (DictInfo entry : DictionaryCatalog.legacy().entries()) {
			if (actual.length() > 0)
				actual.append('\n');
			actual.append(definition(entry));
		}

		assertEquals(
				"Fora|Fora Dictionary|com.ngc.fora|"
						+ "com.ngc.fora.ForaDictionary|"
						+ "android.intent.action.SEARCH|0|query\n"
						+ "ColorDict|ColorDict|com.socialnmobile.colordict|"
						+ "com.socialnmobile.colordict.activity.Main|"
						+ "android.intent.action.SEARCH|0|query\n"
						+ "ColorDictApi|ColorDict new / GoldenDict|"
						+ "com.socialnmobile.colordict|"
						+ "com.socialnmobile.colordict.activity.Main|"
						+ "android.intent.action.SEARCH|1|query\n"
						+ "AardDict|Aard Dictionary|aarddict.android|"
						+ "aarddict.android.Article|"
						+ "android.intent.action.SEARCH|0|query\n"
						+ "AardDictLookup|Aard Dictionary Lookup|"
						+ "aarddict.android|aarddict.android.Lookup|"
						+ "android.intent.action.SEARCH|0|query\n"
						+ "Aard2|Aard 2 Dictionary|itkach.aard2|"
						+ "aard2.lookup|android.intent.action.SEARCH|3|query\n"
						+ "OnyxDictOld|ONYX Dictionary (Old)|com.onyx.dict|"
						+ "com.onyx.dict.activity.DictMainActivity|"
						+ "android.intent.action.VIEW|0|"
						+ "android.intent.action.SEARCH\n"
						+ "OnyxDict|ONYX Dictionary|com.onyx.dict|"
						+ "com.onyx.dict.main.ui.DictMainActivity|"
						+ "android.intent.action.VIEW|0|"
						+ "android.intent.action.SEARCH\n"
						+ "OnyxDictWindowed|ONYX Dictionary (Windowed)|"
						+ "com.onyx.dict|"
						+ "com.onyx.dict.translation.ui.ProcessTextActivity|"
						+ "android.intent.action.VIEW|0|"
						+ "android.intent.extra.PROCESS_TEXT\n"
						+ "Dictan|Dictan Dictionary|info.softex.dictan|"
						+ "<null>|android.intent.action.VIEW|2|query\n"
						+ "FreeDictionary.org|Free Dictionary . org|"
						+ "org.freedictionary|"
						+ "org.freedictionary.MainActivity|"
						+ "android.intent.action.VIEW|0|query\n"
						+ "ABBYYLingvo|ABBYY Lingvo|"
						+ "com.abbyy.mobile.lingvo.market|<null>|"
						+ "com.abbyy.mobile.lingvo.intent.action.TRANSLATE|0|"
						+ "com.abbyy.mobile.lingvo.intent.extra.TEXT\n"
						+ "LingoQuizLite|Lingo Quiz Lite|mnm.lite.lingoquiz|"
						+ "mnm.lite.lingoquiz.ExchangeActivity|"
						+ "lingoquiz.intent.action.ADD_WORD|0|EXTRA_WORD\n"
						+ "LingoQuiz|Lingo Quiz|mnm.lingoquiz|"
						+ "mnm.lingoquiz.ExchangeActivity|"
						+ "lingoquiz.intent.action.ADD_WORD|0|EXTRA_WORD\n"
						+ "LEODictionary|LEO Dictionary|org.leo.android.dict|"
						+ "org.leo.android.dict.LeoDict|"
						+ "android.intent.action.SEARCH|0|query\n"
						+ "PopupDictionary|Popup Dictionary|"
						+ "com.barisatamer.popupdictionary|"
						+ "com.barisatamer.popupdictionary.MainActivity|"
						+ "android.intent.action.VIEW|0|query\n"
						+ "GoogleTranslate|Google Translate|"
						+ "com.google.android.apps.translate|"
						+ "com.google.android.apps.translate.TranslateActivity|"
						+ "android.intent.action.SEND|4|query\n"
						+ "YandexTranslate|Yandex Translate|ru.yandex.translate|"
						+ "ru.yandex.translate.ui.activities.MainActivity|"
						+ "android.intent.action.SEND|4|query\n"
						+ "Wikipedia|Wikipedia|org.wikipedia|"
						+ "org.wikipedia.search.SearchActivity|"
						+ "android.intent.action.SEND|0|query",
				actual.toString());
	}

	@Test
	public void publicArrayApiReturnsIndependentSnapshots() {
		DictInfo[] first = Dictionaries.getDictList();
		DictInfo[] second = Dictionaries.getDictList();

		assertNotSame(first, second);
		assertSame(first[0], second[0]);
		first[0] = first[1];
		assertEquals("Fora", Dictionaries.getDictList()[0].id);
	}

	@Test
	public void catalogLookupAndEntryListAreStable() {
		DictionaryCatalog catalog = DictionaryCatalog.legacy();

		assertEquals(19, catalog.entries().size());
		assertEquals("OnyxDictWindowed",
				catalog.findById("OnyxDictWindowed").id);
		assertNull(catalog.findById("missing"));
		try {
			catalog.entries().clear();
			fail("dictionary entries were mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void definitionsAreImmutableAndDuplicateIdsAreRejected() {
		for (Field field : DictInfo.class.getDeclaredFields()) {
			assertTrue(
					"Dictionary definition field is mutable: "
							+ field.getName(),
					Modifier.isFinal(field.getModifiers()));
		}
		DictInfo duplicate = new DictInfo(
				"same", "First", "one", null, "action", 0);
		try {
			new DictionaryCatalog(Arrays.asList(
					duplicate,
					new DictInfo(
							"same", "Second", "two", null, "action", 0)));
			fail("duplicate dictionary ID was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	private static String definition(DictInfo entry) {
		return entry.id
				+ "|" + entry.name
				+ "|" + entry.packageName
				+ "|" + (entry.className == null ? "<null>" : entry.className)
				+ "|" + entry.action
				+ "|" + entry.internal
				+ "|" + entry.dataKey;
	}
}
