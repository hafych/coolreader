/*
 * CoolReader for Android
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 */

package org.coolreader.crengine;

import org.coolreader.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable built-in background texture metadata.
 */
final class BackgroundTextureCatalog {
	private final List<BackgroundTextureInfo> entries;
	private final Map<String, BackgroundTextureInfo> entriesById;

	BackgroundTextureCatalog(List<BackgroundTextureInfo> entries) {
		if (entries == null || entries.isEmpty())
			throw new IllegalArgumentException(
					"texture catalog must not be empty");
		List<BackgroundTextureInfo> entryCopy =
				new ArrayList<>(entries.size());
		Map<String, BackgroundTextureInfo> index = new HashMap<>();
		for (BackgroundTextureInfo entry : entries) {
			if (entry == null)
				throw new IllegalArgumentException(
						"texture catalog entry is required");
			if (index.put(entry.getId(), entry) != null)
				throw new IllegalArgumentException(
						"duplicate texture id: " + entry.getId());
			entryCopy.add(entry);
		}
		if (!entryCopy.get(0).isNone())
			throw new IllegalArgumentException(
					"texture catalog must start with no-texture");
		this.entries = Collections.unmodifiableList(entryCopy);
		this.entriesById = Collections.unmodifiableMap(index);
	}

	static BackgroundTextureCatalog legacy() {
		return new BackgroundTextureCatalog(Arrays.asList(
				new BackgroundTextureInfo(
						BackgroundTextureInfo.NO_TEXTURE_ID,
						"(SOLID COLOR)",
						0),
				new BackgroundTextureInfo(
						"bg_paper1",
						"Paper 1",
						R.drawable.bg_paper1),
				new BackgroundTextureInfo(
						"bg_paper1_dark",
						"Paper 1 (dark)",
						R.drawable.bg_paper1_dark),
				new BackgroundTextureInfo(
						"bg_paper2",
						"Paper 2",
						R.drawable.bg_paper2),
				new BackgroundTextureInfo(
						"bg_paper2_dark",
						"Paper 2 (dark)",
						R.drawable.bg_paper2_dark),
				new BackgroundTextureInfo(
						"tx_wood",
						"Wood",
						R.drawable.tx_wood),
				new BackgroundTextureInfo(
						"tx_wood_dark",
						"Wood (dark)",
						R.drawable.tx_wood_dark),
				new BackgroundTextureInfo(
						"tx_fabric",
						"Fabric",
						R.drawable.tx_fabric),
				new BackgroundTextureInfo(
						"tx_fabric_dark",
						"Fabric (dark)",
						R.drawable.tx_fabric_dark),
				new BackgroundTextureInfo(
						"tx_fabric_indigo_fibre",
						"Fabric fibre",
						R.drawable.tx_fabric_indigo_fibre),
				new BackgroundTextureInfo(
						"tx_fabric_indigo_fibre_dark",
						"Fabric fibre (dark)",
						R.drawable.tx_fabric_indigo_fibre_dark),
				new BackgroundTextureInfo(
						"tx_gray_sand",
						"Gray sand",
						R.drawable.tx_gray_sand),
				new BackgroundTextureInfo(
						"tx_gray_sand_dark",
						"Gray sand (dark)",
						R.drawable.tx_gray_sand_dark),
				new BackgroundTextureInfo(
						"tx_green_wall",
						"Green wall",
						R.drawable.tx_green_wall),
				new BackgroundTextureInfo(
						"tx_green_wall_dark",
						"Green wall (dark)",
						R.drawable.tx_green_wall_dark),
				new BackgroundTextureInfo(
						"tx_metal_red_light",
						"Metall red",
						R.drawable.tx_metal_red_light),
				new BackgroundTextureInfo(
						"tx_metal_red_dark",
						"Metall red (dark)",
						R.drawable.tx_metal_red_dark),
				new BackgroundTextureInfo(
						"tx_metall_copper",
						"Metall copper",
						R.drawable.tx_metall_copper),
				new BackgroundTextureInfo(
						"tx_metall_copper_dark",
						"Metall copper (dark)",
						R.drawable.tx_metall_copper_dark),
				new BackgroundTextureInfo(
						"tx_metall_old_blue",
						"Metall blue",
						R.drawable.tx_metall_old_blue),
				new BackgroundTextureInfo(
						"tx_metall_old_blue_dark",
						"Metall blue (dark)",
						R.drawable.tx_metall_old_blue_dark),
				new BackgroundTextureInfo(
						"tx_old_book",
						"Old book",
						R.drawable.tx_old_book),
				new BackgroundTextureInfo(
						"tx_old_book_dark",
						"Old book (dark)",
						R.drawable.tx_old_book_dark),
				new BackgroundTextureInfo(
						"tx_old_paper",
						"Old paper",
						R.drawable.tx_old_paper),
				new BackgroundTextureInfo(
						"tx_old_paper_dark",
						"Old paper (dark)",
						R.drawable.tx_old_paper_dark),
				new BackgroundTextureInfo(
						"tx_paper",
						"Paper",
						R.drawable.tx_paper),
				new BackgroundTextureInfo(
						"tx_paper_dark",
						"Paper (dark)",
						R.drawable.tx_paper_dark),
				new BackgroundTextureInfo(
						"tx_rust",
						"Rust",
						R.drawable.tx_rust),
				new BackgroundTextureInfo(
						"tx_rust_dark",
						"Rust (dark)",
						R.drawable.tx_rust_dark),
				new BackgroundTextureInfo(
						"tx_sand",
						"Sand",
						R.drawable.tx_sand),
				new BackgroundTextureInfo(
						"tx_sand_dark",
						"Sand (dark)",
						R.drawable.tx_sand_dark),
				new BackgroundTextureInfo(
						"tx_stones",
						"Stones",
						R.drawable.tx_stones),
				new BackgroundTextureInfo(
						"tx_stones_dark",
						"Stones (dark)",
						R.drawable.tx_stones_dark)));
	}

	List<BackgroundTextureInfo> entries() {
		return entries;
	}

	BackgroundTextureInfo none() {
		return entries.get(0);
	}

	BackgroundTextureInfo findById(String id) {
		return id != null ? entriesById.get(id) : null;
	}

	List<BackgroundTextureInfo> withExternal(
			Collection<BackgroundTextureInfo> external) {
		if (external == null)
			throw new IllegalArgumentException(
					"external textures are required");
		List<BackgroundTextureInfo> result =
				new ArrayList<>(entries.size() + external.size());
		result.add(none());
		for (BackgroundTextureInfo entry : external) {
			if (entry == null)
				throw new IllegalArgumentException(
						"external texture is required");
			result.add(entry);
		}
		result.addAll(entries.subList(1, entries.size()));
		return Collections.unmodifiableList(result);
	}
}
