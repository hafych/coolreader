/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2009,2012,2013 Vadim Lopatin <coolreader.org@gmail.com>
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

#ifndef __LV_STRING8COLLECTION_H_INCLUDED__
#define __LV_STRING8COLLECTION_H_INCLUDED__

#include "lvstring.h"

#include <vector>

/// collection of strings
class lString8Collection
{
private:
    std::vector<lString8> _items;
public:
    lString8Collection() = default;
    lString8Collection(const lString8Collection &) = default;
    lString8Collection(const lString8 & str, const lString8 & delimiter)
    {
        split(str, delimiter);
    }
    void reserve(int space);
    int add(const lString8 & str);
    int add(const char * str) { return add(lString8(str)); }
    void addAll(const lString8Collection & src) {
    	for (int i = 0; i < src.length(); i++)
    		add(src[i]);
    }
    /// calculate hash
    lUInt32 getHash() const;
    /// split string by delimiters, and add all substrings to collection
    void split(const lString8 & str, const lString8 & delimiter);
    void erase(int offset, int count);
    const lString8 & at(int index)
    {
        return _items[index];
    }
    const lString8 & operator [] (int index) const
    {
        return _items[index];
    }
    lString8 & operator [] (int index)
    {
        return _items[index];
    }
    lString8Collection &operator=(const lString8Collection &) = default;
    bool operator==(const lString8Collection& other) const;
    bool operator!=(const lString8Collection& other) const;
    int length() const { return static_cast<int>(_items.size()); }
    void clear();
    ~lString8Collection() = default;
    bool empty() const { return _items.empty(); }
};

#endif // __LV_STRING8COLLECTION_H_INCLUDED__
