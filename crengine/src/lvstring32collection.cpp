/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2009,2010,2012 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2019 poire-z <poire-z@users.noreply.github.com>         *
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

#include "../include/lvstring32collection.h"

#include <algorithm>

void lString32Collection::reserve(int space)
{
    if (space <= 0)
        return;
    const std::size_t requested =
            _items.size() + static_cast<std::size_t>(space);
    if (requested > _items.capacity())
        _items.reserve(requested + 64);
}

void lString32Collection::sort(int(comparator)(lString32 & s1, lString32 & s2))
{
    if (!comparator)
        return;
    std::sort(
            _items.begin(), _items.end(),
            [comparator](const lString32 &left, const lString32 &right) {
                lString32 leftCopy(left);
                lString32 rightCopy(right);
                return comparator(leftCopy, rightCopy) < 0;
            });
}

void lString32Collection::sort()
{
    std::sort(
            _items.begin(), _items.end(),
            [](const lString32 &left, const lString32 &right) {
                return left.compare(right) < 0;
            });
}

int lString32Collection::add( const lString32 & str )
{
    const int index = length();
    _items.push_back(str);
    return index;
}
int lString32Collection::insert( int pos, const lString32 & str )
{
    const int previousLength = length();
    if (pos < 0 || pos >= previousLength)
        return add(str);
    _items.insert(_items.begin() + pos, str);
    return previousLength;
}
void lString32Collection::clear()
{
    std::vector<lString32>().swap(_items);
}

void lString32Collection::erase(int offset, int cnt)
{
    const int itemCount = length();
    if (itemCount <= 0)
        return;
    if (offset < 0 || cnt <= 0
            || offset > itemCount || cnt > itemCount - offset)
        return;
    _items.erase(_items.begin() + offset, _items.begin() + offset + cnt);
}

void lString32Collection::split( const lString32 & str, const lString32 & delimiter )
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

void lString32Collection::parse( lString32 string, lChar32 delimiter, bool flgTrim )
{
    int wstart=0;
    for ( int i=0; i<=string.length(); i++ ) {
        if ( i==string.length() || string[i]==delimiter ) {
            lString32 s( string.substr( wstart, i-wstart) );
            if ( flgTrim )
                s.trimDoubleSpaces(false, false, false);
            if ( !flgTrim || !s.empty() )
                add( s );
            wstart = i+1;
        }
    }
}

void lString32Collection::parse( lString32 string, lString32 delimiter, bool flgTrim )
{
    if ( delimiter.empty() || string.pos(delimiter)<0 ) {
        lString32 s( string );
        if ( flgTrim )
            s.trimDoubleSpaces(false, false, false);
        add(s);
        return;
    }
    int wstart=0;
    for ( int i=0; i<=string.length(); i++ ) {
        bool matched = true;
        for ( int j=0; j<delimiter.length() && i+j<string.length(); j++ ) {
            if ( string[i+j]!=delimiter[j] ) {
                matched = false;
                break;
            }
        }
        if ( matched ) {
            lString32 s( string.substr( wstart, i-wstart) );
            if ( flgTrim )
                s.trimDoubleSpaces(false, false, false);
            if ( !flgTrim || !s.empty() )
                add( s );
            wstart = i+delimiter.length();
            i+= delimiter.length()-1;
        }
    }
}
