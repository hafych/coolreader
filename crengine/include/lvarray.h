/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2012 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2021 Aleksey Chernov <valexlin@gmail.com>               *
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
 * \file lvarray.h
 * \brief value array template
 *
 * Implements array of values
 */

#ifndef __LVARRAY_H_INCLUDED__
#define __LVARRAY_H_INCLUDED__

#include "lvref.h"

#include <cassert>
#include <climits>
#include <memory>
#include <stdexcept>
#include <utility>

/** \brief template which implements a contiguous array of values */
template <typename T >
class LVArray
{
    std::unique_ptr<T[]> _array;
    int _size;
    int _count;

    void swap(LVArray &array)
    {
        _array.swap(array._array);
        std::swap(_size, array._size);
        std::swap(_count, array._count);
    }

public:
    /// default constructor
    LVArray() : _array(), _size(0), _count(0) {}

    /// creates array of given size
    LVArray( int len, T value )
        : _array(), _size(0), _count(0)
    {
        if (len <= 0)
            return;
        std::unique_ptr<T[]> storage(new T[len]());
        for (int i = 0; i < len; i++)
            storage[i] = value;
        _array = std::move(storage);
        _size = _count = len;
    }

    LVArray( const LVArray & v )
        : _array(), _size(0), _count(0)
    {
        if (v._count <= 0)
            return;
        std::unique_ptr<T[]> storage(new T[v._count]());
        for (int i = 0; i < v._count; i++)
            storage[i] = v._array[i];
        _array = std::move(storage);
        _size = _count = v._count;
    }

    LVArray( const T * ptr, int len )
        : _array(), _size(0), _count(0)
    {
        if (!ptr || len <= 0)
            return;
        std::unique_ptr<T[]> storage(new T[len]());
        for (int i = 0; i < len; i++)
            storage[i] = ptr[i];
        _array = std::move(storage);
        _size = _count = len;
    }

    LVArray & operator = ( const LVArray & v )
    {
        if (this != &v) {
            LVArray copy(v);
            swap(copy);
        }
        return *this;
    }

    /// retrieves pointer to C array
    T * get() { return _array.get(); }
    /// retrieves item from specified position
    T operator [] ( int pos ) const { return _array[pos]; }
    /// retrieves item from specified position
    T get( int pos ) const { return _array[pos]; }
    /// retrieves item reference from specified position
    T & operator [] ( int pos ) { return _array[pos]; }
    /// ensures that size of vector is not less than specified value
    void reserve( int size )
    {
        if (size <= _size)
            return;
        std::unique_ptr<T[]> storage(new T[size]());
        for (int i = 0; i < _count; i++)
            storage[i] = _array[i];
        _array = std::move(storage);
        _size = size;
    }

    /// sets item by index (extends vector if necessary)
    void set( int index, T item )
    {
        if (index < 0)
            return;
        if (index == INT_MAX)
            throw std::length_error("LVArray index overflow");
        reserve(index + 1);
        _array[index] = item;
        if (index >= _count)
            _count = index + 1;
    }
    /// returns size of buffer
    int size() const { return _size; }
    /// returns number of items in vector
    int length() const { return _count; }
    /// returns true if there are no items in vector
    bool empty() const { return _count==0; }
    /// clears all items, deallocates storage
    void clear()
    {
        _array.reset();
        _size = 0;
        _count = 0;
    }

    /// clears all items, but unlike clear() does not deallocate storage
    void reset()
    {
        for (int i = 0; i < _count; i++)
            _array[i] = T();
        _count = 0;
    }

    /// copies range to beginning of array
    void trim( int pos, int count, int reserved )
    {
        if (pos < 0 || count < 0 || reserved < 0
                || pos > _count || count > _count - pos)
            return;
        int new_sz = count;
        if (new_sz < reserved)
            new_sz = reserved;
        std::unique_ptr<T[]> storage;
        if (new_sz > 0) {
            storage.reset(new T[new_sz]());
            for (int i = 0; i < count; i++)
                storage[i] = _array[pos + i];
        }
        _array = std::move(storage);
        _count = count;
        _size = new_sz;
    }

    /// removes several items from vector
    void erase( int pos, int count )
    {
        if (pos < 0 || count <= 0
                || pos > _count || count > _count - pos)
            return;
        const int oldCount = _count;
        for (int i = pos + count; i < oldCount; i++)
            _array[i - count] = _array[i];
        _count -= count;
        for (int i = _count; i < oldCount; i++)
            _array[i] = T();
    }

    T remove( int pos )
    {
        if (pos < 0 || pos >= _count)
            return T();
        T item = _array[ pos ];
        erase( pos, 1 );
        return item;
    }

    /// adds new item to end of vector
    void add( T item )
    {
        insert( -1, item );
    }

    /// adds new item to end of vector
    void append( const T * items, int count )
    {
        if (!items || count <= 0)
            return;
        if (_count > INT_MAX - count)
            throw std::length_error("LVArray append overflow");
        std::unique_ptr<T[]> snapshot(new T[count]());
        for (int i = 0; i < count; i++)
            snapshot[i] = items[i];
        reserve(_count + count);
        for (int i = 0; i < count; i++)
            _array[_count + i] = snapshot[i];
        _count += count;
    }

    /// adds new items to end of vector
    void add( const LVArray & list )
    {
        append(list._array.get(), list._count);
    }

    /// adds new items to end of vector
    void add( const T * list, int count )
    {
        append(list, count);
    }

    T * addSpace( int count )
    {
        if (count <= 0)
            return _array ? _array.get() + _count : NULL;
        if (_count > INT_MAX - count)
            throw std::length_error("LVArray growth overflow");
        reserve(_count + count);
        T * ptr = _array.get() + _count;
        _count += count;
        return ptr;
    }

    /// inserts new item to specified position
    void insert( int pos, T item )
    {
        if (pos<0 || pos>_count)
            pos = _count;
        if (_count == INT_MAX)
            throw std::length_error("LVArray insertion overflow");
        if (_count >= _size) {
            const long long desiredSize =
                    static_cast<long long>(_count) * 3 / 2 + 8;
            int grownSize = desiredSize > INT_MAX
                    ? INT_MAX
                    : static_cast<int>(desiredSize);
            if (grownSize < _count + 1)
                grownSize = _count + 1;
            reserve(grownSize);
        }
        for (int i=_count; i>pos; --i)
            _array[i] = _array[i-1];
        _array[pos] = item;
        _count++;
    }

    /// returns index of specified value, -1 if not found
    int indexOf(int value) const {
        for ( int i=0; i<_count; i++ ) {
            if ( _array[i] == value )
                return i;
        }
        return -1;
    }

    /// returns array pointer
    T * ptr() const { return _array.get(); }
    /// destructor
    ~LVArray() = default;
};

template <typename T >
class LVArrayQueue
{
private:
    LVArray<T> m_buf;
    int inpos;
public:
    LVArrayQueue()
        : inpos(0)
    {
    }

    /// returns pointer to reserved space of specified size
    T * prepareWrite( int size )
    {
        if ( m_buf.length() + size > m_buf.size() )
        {
            if ( inpos > (m_buf.length() + size) / 2 )
            {
                // trim
                m_buf.erase(0, inpos);
                inpos = 0;
            }
        }
        return m_buf.addSpace( size );
    }

    /// writes data to end of queue
    void write( const T * data, int size )
    {
        T * buf = prepareWrite( size );
        for (int i=0; i<size; i++)
            buf[i] = data[i];
    }

    int length()
    {
        return m_buf.length() - inpos;
    }

    /// returns pointer to data to be read
    T * peek() { return m_buf.ptr() + inpos; }

    /// reads data from start of queue
    void read( T * data, int size )
    {
        if ( size > length() )
            size = length();
        for ( int i=0; i<size; i++ )
            data[i] = m_buf[inpos + i];
        inpos += size;
    }

    /// skips data from start of queue
    void skip( int size )
    {
        if ( size > length() )
            size = length();
        inpos += size;
    }
};

typedef LVArray<lUInt8> LVByteArray;
typedef LVRef<LVByteArray> LVByteArrayRef;

#endif
