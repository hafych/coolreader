/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2012 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2018,2020 Aleksey Chernov <valexlin@gmail.com>          *
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

#include "../include/lvstring8collection.h"

void lString8Collection::split( const lString8 & str, const lString8 & delimiter )
{
    if (str.empty())
        return;
    for (int startpos = 0; startpos < str.length(); ) {
        int pos = str.pos(delimiter, startpos);
        if (pos < 0)
            pos = str.length();
        add(str.substr(startpos, pos - startpos));
        startpos = pos + delimiter.length();
    }
}

void lString8Collection::erase(int offset, int cnt)
{
    const int itemCount = length();
    if (itemCount <= 0)
        return;
    if (offset < 0 || cnt <= 0
            || offset > itemCount || cnt > itemCount - offset)
        return;
    _items.erase(_items.begin() + offset, _items.begin() + offset + cnt);
}

bool lString8Collection::operator==(const lString8Collection& other) const
{
    bool equal = false;
    // Compare <this> with <other> consider items order
    if (length() == other.length()) {
        equal = true;
        for (int i = 0; i < length(); i++) {
            if (_items[i] != other[i]) {
                equal = false;
                break;
            }
        }
    }
    return equal;
}

bool lString8Collection::operator!=(const lString8Collection &other) const
{
    return !operator ==(other);
}

void lString8Collection::reserve(int space)
{
    if (space <= 0)
        return;
    const std::size_t requested =
            _items.size() + static_cast<std::size_t>(space);
    if (requested > _items.capacity())
        _items.reserve(requested + 64);
}

int lString8Collection::add( const lString8 & str )
{
    const int index = length();
    _items.push_back(str);
    return index;
}

lUInt32 lString8Collection::getHash() const
{
    lUInt32 hash = 0;
    for (const lString8 &item : _items)
        hash = 31 * hash + item.getHash();
    return hash;
}

void lString8Collection::clear()
{
    std::vector<lString8>().swap(_items);
}
