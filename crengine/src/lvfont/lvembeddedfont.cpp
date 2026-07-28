/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2012 Vadim Lopatin <coolreader.org@gmail.com>           *
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

#include "lvembeddedfont.h"
#include "serialbuf.h"

#include <limits>
#include <memory>
#include <utility>

static const char *EMBEDDED_FONT_LIST_MAGIC = "FNTL";
static const char *EMBEDDED_FONT_DEF_MAGIC = "FNTD";

////////////////////////////////////////////////////////////////////
// LVEmbeddedFontDef
////////////////////////////////////////////////////////////////////
bool LVEmbeddedFontDef::serialize(SerialBuf &buf) const {
    buf.putMagic(EMBEDDED_FONT_DEF_MAGIC);
    buf << _url << _face << _bold << _italic;
    return !buf.error();
}

bool LVEmbeddedFontDef::deserialize(SerialBuf &buf) {
    if (!buf.checkMagic(EMBEDDED_FONT_DEF_MAGIC))
        return false;
    lString32 url;
    lString8 face;
    bool bold = false;
    bool italic = false;
    buf >> url >> face >> bold >> italic;
    if (buf.error())
        return false;
    _url = std::move(url);
    _face = std::move(face);
    _bold = bold;
    _italic = italic;
    return true;
}

////////////////////////////////////////////////////////////////////
// LVEmbeddedFontList
////////////////////////////////////////////////////////////////////
LVEmbeddedFontList::LVEmbeddedFontList(
        const LVEmbeddedFontList &list) {
    for (const auto &item : list._items) {
        std::unique_ptr<LVEmbeddedFontDef> copy;
        if (item)
            copy.reset(new LVEmbeddedFontDef(*item));
        addOwned(std::move(copy));
    }
}

void LVEmbeddedFontList::addOwned(
        std::unique_ptr<LVEmbeddedFontDef> def) {
    _items.push_back(std::move(def));
}

void LVEmbeddedFontList::add(LVEmbeddedFontDef *def) {
    addOwned(std::unique_ptr<LVEmbeddedFontDef>(def));
}

LVEmbeddedFontDef *LVEmbeddedFontList::findByUrl(
        const lString32 &url) {
    for (const auto &item : _items) {
        if (item->getUrl() == url)
            return item.get();
    }
    return NULL;
}

bool LVEmbeddedFontList::addAll(
        const LVEmbeddedFontList &list) {
    bool changed = false;
    for (int i = 0; i < list.length(); i++) {
        const LVEmbeddedFontDef *def = list.get(i);
        changed = add(def->getUrl(), def->getFace(),
                def->getBold(), def->getItalic()) || changed;
    }
    return changed;
}

bool LVEmbeddedFontList::add(lString32 url, lString8 face, bool bold, bool italic) {
    LVEmbeddedFontDef *def = findByUrl(url);
    if (def) {
        bool changed = false;
        if (def->getFace() != face) {
            def->setFace(face);
            changed = true;
        }
        if (def->getBold() != bold) {
            def->setBold(bold);
            changed = true;
        }
        if (def->getItalic() != italic) {
            def->setItalic(italic);
            changed = true;
        }
        return changed;
    }
    std::unique_ptr<LVEmbeddedFontDef> candidate(
            new LVEmbeddedFontDef(url, face, bold, italic));
    addOwned(std::move(candidate));
    return false;
}

void LVEmbeddedFontList::set(
        const LVEmbeddedFontList &list) {
    if (this == &list)
        return;
    LVEmbeddedFontList replacement;
    replacement.addAll(list);
    swap(replacement);
}

bool LVEmbeddedFontList::serialize(SerialBuf &buf) const {
    buf.putMagic(EMBEDDED_FONT_LIST_MAGIC);
    lUInt32 count = static_cast<lUInt32>(length());
    buf << count;
    for (lUInt32 i = 0; i < count; i++) {
        get(static_cast<int>(i))->serialize(buf);
        if (buf.error())
            return false;
    }
    return !buf.error();
}

bool LVEmbeddedFontList::deserialize(SerialBuf &buf) {
    if (!buf.checkMagic(EMBEDDED_FONT_LIST_MAGIC))
        return false;
    lUInt32 count = 0;
    buf >> count;
    static const int minimumSerializedFontDefinitionSize = 10;
    if (buf.error()
            || count > static_cast<lUInt32>(
                    std::numeric_limits<int>::max())
            || count > static_cast<lUInt32>(
                    buf.space()
                            / minimumSerializedFontDefinitionSize)) {
        buf.seterror();
        return false;
    }

    LVEmbeddedFontList parsed;
    for (lUInt32 i = 0; i < count; i++) {
        std::unique_ptr<LVEmbeddedFontDef> item(
                new LVEmbeddedFontDef());
        if (!item->deserialize(buf)) {
            return false;
        }
        parsed.addOwned(std::move(item));
    }
    if (buf.error())
        return false;
    if (parsed._items.size() > _items.max_size() - _items.size()) {
        buf.seterror();
        return false;
    }
    _items.reserve(_items.size() + parsed._items.size());
    for (auto &item : parsed._items)
        _items.push_back(std::move(item));
    return true;
}
