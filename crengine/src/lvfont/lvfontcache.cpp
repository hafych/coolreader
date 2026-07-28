/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2008,2010-2012 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2015 Yifei(Frank) ZHU <fredyifei@gmail.com>             *
 *   Copyright (C) 2017,2021 poire-z <poire-z@users.noreply.github.com>    *
 *   Copyright (C) 2019-2021 Aleksey Chernov <valexlin@gmail.com>          *
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

#include "lvfontcache.h"
#include "lvstyles.h"
#include "crlog.h"

#include <algorithm>
#include <memory>
#include <utility>

LVFontCacheItem *LVFontCache::findDuplicate(const LVFontDef *def) {
    for (const auto &item : _registered_list) {
        if (item->_def.CalcDuplicateMatch(*def))
            return item.get();
    }
    return NULL;
}

LVFontCacheItem *LVFontCache::findDocumentFontDuplicate(int documentId, lString8 name) {
    for (const auto &item : _registered_list) {
        if (item->_def.getDocumentId() == documentId
                && item->_def.getName() == name)
            return item.get();
    }
    return NULL;
}

LVFontCacheItem *LVFontCache::findFallback(lString8 face, int size) {
    LVFontCacheItem *best = NULL;
    int best_match = -1;
    LVFontCacheItem *best_instance = NULL;
    int best_instance_match = -1;
    for (const auto &item : _instance_list) {
        int match = item->_def.CalcFallbackMatch(face, size);
        if (match > best_instance_match) {
            best_instance_match = match;
            best_instance = item.get();
        }
    }
    for (const auto &item : _registered_list) {
        int match = item->_def.CalcFallbackMatch(face, size);
        if (match > best_match) {
            best_match = match;
            best = item.get();
        }
    }
    if (!best || best_match <= 0)
        return NULL;
    return best_instance_match >= best_match ? best_instance : best;
}

LVFontCacheItem *LVFontCache::find(const LVFontDef *fntdef, bool useBias) {
    LVFontCacheItem *best = NULL;
    int best_match = -1;
    LVFontCacheItem *best_instance = NULL;
    int best_instance_match = -1;
    LVFontDef def(*fntdef);
    lString8Collection list;
    splitPropertyValueList(fntdef->getTypeFace().c_str(), list);
    int nlen = list.length();
    for (int nindex=0; nindex==0 || nindex<nlen; nindex++) {
        // Give more weight to first fonts, so we don't risk (with the test at end)
        // picking an already instantiated second font over a not yet instantiated
        // first font with the same match.
        int ordering_weight = nlen - nindex;
        if ( nindex < nlen )
            def.setTypeFace(list[nindex]);
        else
            def.setTypeFace(lString8::empty_str);
        for (const auto &item : _instance_list) {
            int match = item->_def.CalcMatch(def, useBias);
            match = match * 256 + ordering_weight;
            if (match > best_instance_match) {
                best_instance_match = match;
                best_instance = item.get();
            }
        }
        for (const auto &item : _registered_list) {
            int match = item->_def.CalcMatch(def, useBias);
            match = match * 256 + ordering_weight;
            if (match > best_match) {
                best_match = match;
                best = item.get();
            }
        }
    }
    if (!best)
        return NULL;
    return best_instance_match >= best_match ? best_instance : best;
}

bool LVFontCache::setAsPreferredFontWithBias( lString8 face, int bias, bool clearOthersBias )
{
    bool found = false;
    for (const auto &item : _instance_list) {
        if (item->_def.setBiasIfNameMatch(
                face, bias, clearOthersBias))
            found = true;
    }
    for (const auto &item : _registered_list) {
        if (item->_def.setBiasIfNameMatch(
                face, bias, clearOthersBias))
            found = true;
    }
    return found;
}

void LVFontCache::addInstance(const LVFontDef *def, LVFontRef ref) {
    if (ref.isNull())
        CRLog::error("Adding null font instance!");
    std::unique_ptr<LVFontCacheItem> item(
            new LVFontCacheItem(*def));
    item->_fnt = ref;
    _instance_list.push_back(std::move(item));
}

void LVFontCache::removefont(const LVFontDef *def) {
    const lString8 typeface = def->getTypeFace();
    auto matches = [&typeface](
            const std::unique_ptr<LVFontCacheItem> &item) {
        return item->_def.getTypeFace() == typeface;
    };
    _instance_list.erase(
            std::remove_if(_instance_list.begin(),
                    _instance_list.end(), matches),
            _instance_list.end());
    _registered_list.erase(
            std::remove_if(_registered_list.begin(),
                    _registered_list.end(), matches),
            _registered_list.end());
}

void LVFontCache::update(const LVFontDef *def, LVFontRef ref) {
    if (!ref.isNull()) {
        for (const auto &item : _instance_list) {
            if (item->_def == *def) {
                item->_fnt = ref;
                return;
            }
        }
        addInstance(def, ref);
    } else {
        for (const auto &item : _registered_list) {
            if (item->_def == *def)
                return;
        }
        _registered_list.push_back(
                std::unique_ptr<LVFontCacheItem>(
                        new LVFontCacheItem(*def)));
    }
}

void LVFontCache::removeDocumentFonts(int documentId) {
    if (-1 == documentId)
        return;
    auto belongsToDocument = [documentId](
            const std::unique_ptr<LVFontCacheItem> &item) {
        return item->_def.getDocumentId() == documentId;
    };
    _instance_list.erase(
            std::remove_if(_instance_list.begin(),
                    _instance_list.end(), belongsToDocument),
            _instance_list.end());
    _registered_list.erase(
            std::remove_if(_registered_list.begin(),
                    _registered_list.end(), belongsToDocument),
            _registered_list.end());
}

static int s_int_comparator(const void * n1, const void * n2)
{
    int* i1 = (int*)n1;
    int* i2 = (int*)n2;
    return *i1 == *i2 ? 0 : (*i1 < *i2 ? -1 : 1);
}

void LVFontCache::getAvailableFontWeights(LVArray<int>& weights, lString8 faceName) {
    weights.clear();
    for (const auto &owner : _registered_list) {
        const LVFontCacheItem* item = owner.get();
        if (item->_def.getTypeFace() == faceName) {
            if (item->_def.isRealWeight()) {       // ignore fonts with fake weight
                int weight = item->_def.getWeight();
                if (weights.indexOf(weight) < 0) {
                    weights.add(weight);
                }
            }
        }
    }
    int* ptr = weights.get();
    qsort(ptr, (size_t)weights.length(), sizeof(int), s_int_comparator);
}

// garbage collector
void LVFontCache::gc() {
    int droppedCount = 0;
    int usedCount = 0;
    for (auto item = _instance_list.begin();
            item != _instance_list.end();) {
        if ((*item)->_fnt.getRefCount() <= 1) {
            if (CRLog::isTraceEnabled())
                CRLog::trace("dropping font instance %s[%d] by gc()",
                             (*item)->getDef()->getTypeFace().c_str(),
                             (*item)->getDef()->getSize());
            item = _instance_list.erase(item);
            droppedCount++;
        } else {
            ++item;
            usedCount++;
        }
    }
    if (CRLog::isDebugEnabled())
        CRLog::debug("LVFontCache::gc() : %d fonts still used, %d fonts dropped", usedCount,
                     droppedCount);
}
