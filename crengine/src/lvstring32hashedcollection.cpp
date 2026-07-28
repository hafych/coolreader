/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2009,2010,2012 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2020 Aleksey Chernov <valexlin@gmail.com>               *
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

#include "../include/lvstring32hashedcollection.h"
#include "../include/serialbuf.h"

static const char * str_hash_magic="STRS";

/// serialize to byte array (pointer will be incremented by number of bytes written)
void lString32HashedCollection::serialize( SerialBuf & buf )
{
    if ( buf.error() )
        return;
    int start = buf.pos();
    buf.putMagic( str_hash_magic );
    lUInt32 count = length();
    buf << count;
    for ( int i=0; i<length(); i++ )
    {
        buf << at(i);
    }
    buf.putCRC( buf.pos() - start );
}

/// deserialize from byte array (pointer will be incremented by number of bytes read)
bool lString32HashedCollection::deserialize( SerialBuf & buf )
{
    if ( buf.error() )
        return false;
    clear();
    int start = buf.pos();
    buf.putMagic( str_hash_magic );
    lInt32 count = 0;
    buf >> count;
    for ( int i=0; i<count; i++ ) {
        lString32 s;
        buf >> s;
        if ( buf.error() )
            break;
        add( s.c_str() );
    }
    buf.checkCRC( buf.pos() - start );
    return !buf.error();
}

void lString32HashedCollection::addHashItem(
        std::size_t hashIndex, int storageIndex)
{
    _hashBuckets[hashIndex].push_back(storageIndex);
}

void lString32HashedCollection::clear()
{
    lString32Collection::clear();
    for (std::vector<int> &bucket : _hashBuckets)
        bucket.clear();
}

lString32HashedCollection::lString32HashedCollection( lUInt32 hash_size )
: _hashBuckets(static_cast<std::size_t>(hash_size))
{
}

int lString32HashedCollection::find( const lChar32 * s )
{
    if (_hashBuckets.empty() || !length())
        return -1;
    const lUInt32 hash = calcStringHash(s);
    const std::size_t bucketIndex = hash % _hashBuckets.size();
    for (int storageIndex : _hashBuckets[bucketIndex]) {
        if (at(storageIndex) == s)
            return storageIndex;
    }
    return -1;
}

void lString32HashedCollection::reHash( std::size_t newSize )
{
    if (_hashBuckets.size() == newSize)
        return;
    _hashBuckets.assign(newSize, std::vector<int>());
    if (_hashBuckets.empty())
        return;
    for ( int i=0; i<length(); i++ ) {
        const lUInt32 hash = calcStringHash(at(i).c_str());
        const std::size_t bucketIndex = hash % _hashBuckets.size();
        addHashItem(bucketIndex, i);
    }
}

int lString32HashedCollection::add( const lChar32 * s )
{
    const std::size_t itemCount = static_cast<std::size_t>(length());
    if (_hashBuckets.empty() || _hashBuckets.size() < itemCount * 2) {
        const std::size_t targetSize = itemCount > 0
                ? itemCount * 2
                : 32;
        std::size_t newSize = 32;
        while (newSize < targetSize
                && newSize <= _hashBuckets.max_size() / 2)
            newSize <<= 1;
        if (newSize < targetSize)
            newSize = targetSize;
        reHash(newSize);
    }
    const lUInt32 hash = calcStringHash(s);
    const std::size_t bucketIndex = hash % _hashBuckets.size();
    for (int storageIndex : _hashBuckets[bucketIndex]) {
        if (at(storageIndex) == s)
            return storageIndex;
    }
    const int storageIndex = lString32Collection::add(lString32(s));
    addHashItem(bucketIndex, storageIndex);
    return storageIndex;
}
