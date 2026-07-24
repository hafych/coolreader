/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2011,2014 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2018 poire-z <poire-z@users.noreply.github.com>         *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or         *
 *   modify it under the terms of the GNU General Public License           *
 *   as published by the Free Software Foundation; either version 2        *
 *   of the License, or (at your option) any later version.                *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the Free Software           *
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,            *
 *   MA 02110-1301, USA.                                                   *
 ***************************************************************************/

#include "lvfntman.h"
#include "lvstyles.h"
#include "crlog.h"
#include "lvfreetypefontman.h"
#include "lvwin32fontman.h"
#include "lvbitmapfontman.h"

#include <atomic>
#include <memory>
#include <mutex>

#define GAMMA_TABLES_IMPL
#include "gammatbl.h"

LVFontManager *fontMan = NULL;

namespace {
std::unique_ptr<LVFontManager> g_font_manager_owner;
std::mutex g_font_manager_lifecycle_mutex;
std::atomic<int> g_font_gamma_index(GAMMA_NO_CORRECTION_INDEX);
std::mutex g_font_gamma_mutex;
}

/// returns first found face from passed list, or return face for font found by family only
lString8 LVFontManager::findFontFace(lString8 commaSeparatedFaceList, css_font_family_t fallbackByFamily) {
    // faces we want
    lString8Collection list;
    splitPropertyValueList(commaSeparatedFaceList.c_str(), list);
    // faces we have
    lString32Collection faces;
    getFaceList(faces);
    // find first matched
    for (int i = 0; i < list.length(); i++) {
        lString8 wantFace = list[i];
        for (int j = 0; j < faces.length(); j++) {
            lString32 haveFace = faces[j];
            if (wantFace == haveFace)
                return wantFace;
        }
    }
    // not matched - get by family name
    LVFontRef fnt = GetFont(10, 400, false, fallbackByFamily, lString8("Arial"));
    if (fnt.isNull())
        return lString8::empty_str; // not found
    // get face from found font
    return fnt->getTypeFace();
}

/// fills array with list of available gamma levels
void LVFontManager::GetGammaLevels(LVArray<double> dst) {
    dst.clear();
    for (int i = 0; i < GAMMA_LEVELS; i++)
        dst.add(cr_gamma_levels[i]);
}

/// returns current gamma level index
int LVFontManager::GetGammaIndex() {
    return g_font_gamma_index.load(std::memory_order_relaxed);
}

/// sets current gamma level index
void LVFontManager::SetGammaIndex(int index) {
    if (index < 0)
        index = 0;
    if (index >= GAMMA_LEVELS)
        index = GAMMA_LEVELS - 1;
    std::lock_guard<std::mutex> guard(g_font_gamma_mutex);
    int old_index =
            g_font_gamma_index.exchange(index, std::memory_order_relaxed);
    if (old_index != index) {
        CRLog::trace("FontManager gamma index changed from %d to %d",
                old_index, index);
        gc();
        clearGlyphCache();
    }
}

/// returns current gamma level
double LVFontManager::GetGamma() {
    return cr_gamma_levels[GetGammaIndex()];
}

/// sets current gamma level
void LVFontManager::SetGamma( double gamma ) {
    int gamma_index = GetGammaIndex();
    double gamma_level = cr_gamma_levels[gamma_index];
    for (int i = 0; i < GAMMA_LEVELS; i++) {
        double diff1 = cr_gamma_levels[i] - gamma;
        if (diff1 < 0) diff1 = -diff1;
        double diff2 = gamma_level - gamma;
        if (diff2 < 0) diff2 = -diff2;
        if (diff1 < diff2) {
            gamma_level = cr_gamma_levels[i];
            gamma_index = i;
        }
    }
    SetGammaIndex(gamma_index);
}

bool InitFontManager(lString8 path) {
    std::lock_guard<std::mutex> guard(g_font_manager_lifecycle_mutex);
    if (fontMan) {
        return true;
    }
#if (USE_WIN32_FONTS == 1)
    std::unique_ptr<LVFontManager> candidate(new LVWin32FontManager);
#elif (USE_FREETYPE == 1)
    std::unique_ptr<LVFontManager> candidate(new LVFreeTypeFontManager);
#else
    std::unique_ptr<LVFontManager> candidate(new LVBitmapFontManager);
#endif
    // Platform discovery still reaches the compatibility pointer from Init().
    // Initialization and shutdown are quiescent lifecycle operations.
    fontMan = candidate.get();
    if (!fontMan->Init(path)) {
        fontMan = NULL;
        return false;
    }
    g_font_manager_owner = std::move(candidate);
    return true;
}

bool ShutdownFontManager() {
    std::lock_guard<std::mutex> guard(g_font_manager_lifecycle_mutex);
    if (!fontMan)
        return false;
    fontMan = NULL;
    g_font_manager_owner.reset();
    return true;
}
