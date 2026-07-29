/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.coolreader.R;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BackgroundTextureCatalogTest {
	@Rule
	public final TemporaryFolder temporaryFolder =
			new TemporaryFolder();

	@Test
	public void legacyCatalogPreservesOrderLookupAndResources() {
		BackgroundTextureCatalog catalog =
				BackgroundTextureCatalog.legacy();
		List<String> ids = new ArrayList<>();
		List<Integer> resources = new ArrayList<>();
		for (BackgroundTextureInfo entry : catalog.entries()) {
			ids.add(entry.getId());
			resources.add(entry.getResourceId());
		}

		assertEquals(
				Arrays.asList(
						"(NONE)",
						"bg_paper1",
						"bg_paper1_dark",
						"bg_paper2",
						"bg_paper2_dark",
						"tx_wood",
						"tx_wood_dark",
						"tx_fabric",
						"tx_fabric_dark",
						"tx_fabric_indigo_fibre",
						"tx_fabric_indigo_fibre_dark",
						"tx_gray_sand",
						"tx_gray_sand_dark",
						"tx_green_wall",
						"tx_green_wall_dark",
						"tx_metal_red_light",
						"tx_metal_red_dark",
						"tx_metall_copper",
						"tx_metall_copper_dark",
						"tx_metall_old_blue",
						"tx_metall_old_blue_dark",
						"tx_old_book",
						"tx_old_book_dark",
						"tx_old_paper",
						"tx_old_paper_dark",
						"tx_paper",
						"tx_paper_dark",
						"tx_rust",
						"tx_rust_dark",
						"tx_sand",
						"tx_sand_dark",
						"tx_stones",
						"tx_stones_dark"),
				ids);
		assertEquals(
				Arrays.asList(
						0,
						R.drawable.bg_paper1,
						R.drawable.bg_paper1_dark,
						R.drawable.bg_paper2,
						R.drawable.bg_paper2_dark,
						R.drawable.tx_wood,
						R.drawable.tx_wood_dark,
						R.drawable.tx_fabric,
						R.drawable.tx_fabric_dark,
						R.drawable.tx_fabric_indigo_fibre,
						R.drawable.tx_fabric_indigo_fibre_dark,
						R.drawable.tx_gray_sand,
						R.drawable.tx_gray_sand_dark,
						R.drawable.tx_green_wall,
						R.drawable.tx_green_wall_dark,
						R.drawable.tx_metal_red_light,
						R.drawable.tx_metal_red_dark,
						R.drawable.tx_metall_copper,
						R.drawable.tx_metall_copper_dark,
						R.drawable.tx_metall_old_blue,
						R.drawable.tx_metall_old_blue_dark,
						R.drawable.tx_old_book,
						R.drawable.tx_old_book_dark,
						R.drawable.tx_old_paper,
						R.drawable.tx_old_paper_dark,
						R.drawable.tx_paper,
						R.drawable.tx_paper_dark,
						R.drawable.tx_rust,
						R.drawable.tx_rust_dark,
						R.drawable.tx_sand,
						R.drawable.tx_sand_dark,
						R.drawable.tx_stones,
						R.drawable.tx_stones_dark),
				resources);
		assertEquals("(SOLID COLOR)", catalog.none().getName());
		assertEquals(
				"Stones (dark)",
				catalog.entries().get(32).getName());
		assertSame(
				catalog.entries().get(2),
				catalog.findById("bg_paper1_dark"));
		assertNull(catalog.findById("missing"));
		assertNull(catalog.findById(null));
	}

	@Test
	public void catalogAndMetadataHaveImmutablePrivateStorage() {
		assertTrue(Modifier.isFinal(
				BackgroundTextureInfo.class.getModifiers()));
		for (Field field :
				BackgroundTextureInfo.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertTrue(Modifier.isFinal(field.getModifiers()));
				assertFalse(field.getType().isArray());
			} else {
				assertTrue(Modifier.isPrivate(field.getModifiers()));
				assertTrue(Modifier.isFinal(field.getModifiers()));
			}
		}
		for (Field field :
				BackgroundTextureCatalog.class.getDeclaredFields()) {
			assertFalse(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isPrivate(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}

		List<BackgroundTextureInfo> source = new ArrayList<>(
				Arrays.asList(
						none(),
						new BackgroundTextureInfo("one", "One", 1)));
		BackgroundTextureCatalog catalog =
				new BackgroundTextureCatalog(source);
		source.clear();
		assertEquals(2, catalog.entries().size());
		try {
			catalog.entries().clear();
			fail("texture catalog was mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void externalTexturesRemainBetweenNoneAndBuiltIns() {
		BackgroundTextureCatalog catalog =
				BackgroundTextureCatalog.legacy();
		BackgroundTextureInfo external =
				new BackgroundTextureInfo(
						"/books/backgrounds/custom.png",
						"Custom",
						0);
		List<BackgroundTextureInfo> source =
				new ArrayList<>(Arrays.asList(external));

		List<BackgroundTextureInfo> combined =
				catalog.withExternal(source);
		source.clear();

		assertSame(catalog.none(), combined.get(0));
		assertSame(external, combined.get(1));
		assertSame(catalog.entries().get(1), combined.get(2));
		assertEquals(34, combined.size());
		try {
			combined.clear();
			fail("combined texture snapshot was mutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void invalidCatalogsAndTextureIdsAreRejected() {
		assertRejected(null);
		assertRejected(new ArrayList<BackgroundTextureInfo>());
		assertRejected(Arrays.asList(
				new BackgroundTextureInfo("one", "One", 1)));
		assertRejected(Arrays.asList(none(), null));
		assertRejected(Arrays.asList(
				none(),
				new BackgroundTextureInfo(
						BackgroundTextureInfo.NO_TEXTURE_ID,
						"Duplicate",
						0)));
		try {
			new BackgroundTextureInfo(null, "Missing", 0);
			fail("missing texture id was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
		try {
			BackgroundTextureCatalog.legacy().withExternal(null);
			fail("missing external texture list was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
		try {
			BackgroundTextureCatalog.legacy().withExternal(
					Arrays.asList((BackgroundTextureInfo) null));
			fail("missing external texture was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}

	@Test
	public void externalFileRecognitionPreservesLegacyRules()
			throws Exception {
		File textures = temporaryFolder.newFolder("textures");
		File image = new File(textures, "Cover.JPG");
		assertTrue(image.createNewFile());

		BackgroundTextureInfo texture =
				BackgroundTextureInfo.fromFile(image.getAbsolutePath());

		assertEquals(image.getAbsolutePath(), texture.getId());
		assertEquals("Cover", texture.getName());
		assertEquals(0, texture.getResourceId());
		assertTrue(texture.isTiled());

		File text = temporaryFolder.newFile("cover.txt");
		assertNull(BackgroundTextureInfo.fromFile(text.getAbsolutePath()));
		assertNull(BackgroundTextureInfo.fromFile(null));
	}

	private static BackgroundTextureInfo none() {
		return new BackgroundTextureInfo(
				BackgroundTextureInfo.NO_TEXTURE_ID,
				"(SOLID COLOR)",
				0);
	}

	private static void assertRejected(
			List<BackgroundTextureInfo> entries) {
		try {
			new BackgroundTextureCatalog(entries);
			fail("invalid texture catalog was accepted");
		} catch (IllegalArgumentException expected) {
			// Expected.
		}
	}
}
