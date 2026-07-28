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

/**
 * \file lvembeddedfont.h
 * \brief embedded font definition interface
 */

#ifndef __LV_EMBEDDEDFONT_H_INCLUDED__
#define __LV_EMBEDDEDFONT_H_INCLUDED__

#include "crsetup.h"
#include "lvstring.h"

#include <memory>
#include <vector>

class SerialBuf;

class LVEmbeddedFontDef {
    lString32 _url;
    lString8 _face;
    bool _bold;
    bool _italic;
public:
    LVEmbeddedFontDef(lString32 url, lString8 face, bool bold, bool italic) :
            _url(url), _face(face), _bold(bold), _italic(italic) {
    }

    LVEmbeddedFontDef() : _bold(false), _italic(false) {
    }

    const lString32 &getUrl() const { return _url; }

    const lString8 &getFace() const { return _face; }

    bool getBold() const { return _bold; }

    bool getItalic() const { return _italic; }

    void setFace(const lString8 &face) { _face = face; }

    void setBold(bool bold) { _bold = bold; }

    void setItalic(bool italic) { _italic = italic; }

    bool serialize(SerialBuf &buf) const;

    bool deserialize(SerialBuf &buf);
};

class LVEmbeddedFontList {
    std::vector<std::unique_ptr<LVEmbeddedFontDef> > _items;

    void addOwned(std::unique_ptr<LVEmbeddedFontDef> def);

public:
    LVEmbeddedFontList() = default;
    LVEmbeddedFontList(const LVEmbeddedFontList &list);
    LVEmbeddedFontList &operator=(const LVEmbeddedFontList &) = delete;
    ~LVEmbeddedFontList() = default;

    int length() const {
        return static_cast<int>(_items.size());
    }

    bool empty() const {
        return _items.empty();
    }

    LVEmbeddedFontDef *get(int index) {
        return _items[static_cast<std::size_t>(index)].get();
    }

    const LVEmbeddedFontDef *get(int index) const {
        return _items[static_cast<std::size_t>(index)].get();
    }

    void clear() {
        _items.clear();
    }

    void swap(LVEmbeddedFontList &list) noexcept {
        _items.swap(list._items);
    }

    LVEmbeddedFontDef *findByUrl(const lString32 &url);

    /// Legacy raw-pointer boundary transfers ownership to this list.
    void add(LVEmbeddedFontDef *def);

    bool add(lString32 url, lString8 face, bool bold, bool italic);

    bool add(lString32 url) { return add(url, lString8::empty_str, false, false); }

    bool addAll(const LVEmbeddedFontList &list);

    void set(const LVEmbeddedFontList &list);

    bool serialize(SerialBuf &buf) const;

    bool deserialize(SerialBuf &buf);
};

#endif // __LV_EMBEDDEDFONT_H_INCLUDED__
