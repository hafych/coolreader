/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2013 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2011 Konstantin Potapov <pkbo@users.sourceforge.net>    *
 *   Copyright (C) 2018 Aleksey Chernov <valexlin@gmail.com>               *
 *   Copyright (C) 2018-2020 poire-z <poire-z@users.noreply.github.com>    *
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
 * \file lvptrvec.h
 * \brief pointer vector template
 *
 * Implements vector of pointers.
 */

#ifndef __LVPTRVEC_H_INCLUDED__
#define __LVPTRVEC_H_INCLUDED__

#include <stdlib.h>
#include "lvmemman.h"

#include <algorithm>
#include <climits>
#include <memory>
#include <stdexcept>
#include <utility>
#include <vector>

/** \brief template which implements vector of pointer

    Automatically deletes objects when vector items are destroyed.
*/
template < class T, bool ownItems = true >
class LVPtrVector
{
    std::vector<T *> _list;
    int _size;
    int _count;
    LVPtrVector &operator=(const LVPtrVector &) = delete;

    void discardSlot(int index)
    {
        if constexpr (ownItems) {
            std::unique_ptr<T> item(_list[index]);
            _list[index] = NULL;
        } else {
            _list[index] = NULL;
        }
    }

public:
    /// default constructor
    LVPtrVector() : _list(), _size(0), _count(0) {}
    /// retrieves item from specified position
    T * operator [] ( int pos ) const { return _list[pos]; }
    /// returns pointer array
    T ** get() { return _list.empty() ? NULL : _list.data(); }
    /// retrieves item from specified position
    T * get( int pos ) const { return _list[pos]; }
    /// retrieves item reference from specified position
    T * & operator [] ( int pos ) { return _list[pos]; }
    /// ensures that size of vector is not less than specified value
    void reserve( int size )
    {
        if (size <= _size)
            return;
        _list.resize(static_cast<size_t>(size), NULL);
        _size = size;
    }
    void sort(int (comparator)(const T ** item1, const T ** item2 ) ) {
        if (!comparator || _count < 2)
            return;
        std::sort(_list.begin(), _list.begin() + _count,
                [comparator](T *left, T *right) {
                    const T *leftView = left;
                    const T *rightView = right;
                    return comparator(&leftView, &rightView) < 0;
                });
    }
    /// sets item by index (extends vector if necessary)
    void set( int index, T * item )
    {
        if (index < 0)
            return;
        if (index == INT_MAX)
            throw std::length_error("LVPtrVector index overflow");
        reserve(index + 1);
        if (_list[index] == item) {
            if (_count <= index)
                _count = index + 1;
            return;
        }
        discardSlot(index);
        _list[index] = item;
        if (_count <= index)
            _count = index + 1;
    }
    /// returns size of buffer
    int size() const { return _size; }
    /// returns number of items in vector
    int length() const { return _count; }
    /// returns true if there are no items in vector
    bool empty() const { return _count==0; }
    /// clears all items
    void clear()
    {
        const int count = _count;
        _count = 0;
        for (int i = count - 1; i >= 0; --i)
            discardSlot(i);
        std::vector<T *>().swap(_list);
        _size = 0;
    }
    /// removes several items from vector
    void erase( int pos, int count )
    {
        if ( count<=0 )
            return;
        if (pos < 0 || pos > _count || count > _count - pos)
            crFatalError();
        int i;
        for (i=0; i<count; i++)
        {
            if (_list[pos+i])
                discardSlot(pos + i);
        }
        for (i=pos+count; i<_count; i++)
        {
            _list[i-count] = _list[i];
            _list[i] = NULL;
        }
        _count -= count;
    }
    /// removes item from vector by index
    T * remove( int pos )
    {
        if (pos < 0 || (unsigned)pos >= (unsigned)_count)
            crFatalError();
        int i;
        T * item = _list[pos];
        for ( i=pos; i<_count-1; i++ )
            _list[i] = _list[i+1];
        --_count;
        _list[_count] = NULL;
        return item;
    }
    /// returns vector index of specified pointer, -1 if not found
    int indexOf( T * p )
    {
        for ( int i=0; i<_count; i++ ) {
            if ( _list[i] == p )
                return i;
        }
        return -1;
    }
    T * last()
    {
        if ( _count<=0 )
            return NULL;
        return _list[_count-1];
    }
    T * first()
    {
        if ( _count<=0 )
            return NULL;
        return _list[0];
    }
    /// removes item from vector by index
    T * remove( T * p )
    {
        int i;
        int pos = indexOf( p );
        if ( pos<0 )
            return NULL;
        T * item = _list[pos];
        for ( i=pos; i<_count-1; i++ )
            _list[i] = _list[i+1];
        --_count;
        _list[_count] = NULL;
        return item;
    }
    /// adds new item to end of vector
    void add( T * item ) { insert( -1, item ); }
    /// inserts new item to specified position
    void insert( int pos, T * item )
    {
        if (pos<0 || pos>_count)
            pos = _count;
        if (_count == INT_MAX)
            throw std::length_error("LVPtrVector insertion overflow");
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
            _list[i] = _list[i-1];
        _list[pos] = item;
        _count++;
    }
    /// move item to specified position, other items will be shifted
    void move( int indexTo, int indexFrom )
    {
        if (indexTo < 0 || indexTo >= _count
                || indexFrom < 0 || indexFrom >= _count)
            crFatalError();
        if ( indexTo==indexFrom )
            return;
        T * p = _list[indexFrom];
        if ( indexTo<indexFrom ) {
            for ( int i=indexFrom; i>indexTo; i--)
                _list[i] = _list[i-1];
        } else {
            for ( int i=indexFrom; i<indexTo; i++)
                _list[i] = _list[i+1];
        }
        _list[ indexTo ] = p;
    }
    /// reverse items
    void reverse()
    {
        std::reverse(_list.begin(), _list.begin() + _count);
    }
    /// copy constructor
    LVPtrVector( const LVPtrVector & v )
        : _list(), _size(0), _count(0)
    {
        if (v._count <= 0)
            return;
        std::vector<T *> storage(
                static_cast<size_t>(v._count), NULL);
        if constexpr (ownItems) {
            std::vector<std::unique_ptr<T> > owners(
                    static_cast<size_t>(v._count));
            for (int i = 0; i < v._count; ++i) {
                if (v[i])
                    owners[i].reset(new T(*v[i]));
            }
            for (int i = 0; i < v._count; ++i)
                storage[i] = owners[i].release();
        } else {
            for (int i = 0; i < v._count; ++i)
                storage[i] = v[i];
        }
        _list.swap(storage);
        _size = _count = v._count;
    }
    /// stack-like interface: pop top item from stack
    T * pop()
    {
        if ( empty() )
            return NULL;
        return remove( length() - 1 );
    }
    /// stack-like interface: pop top item from stack
    T * popHead()
    {
        if ( empty() )
            return NULL;
        return remove( (int)0 );
    }
    /// stack-like interface: push item to stack
    void push( T * item )
    {
        add( item );
    }
    /// stack-like interface: push item to stack
    void pushHead( T * item )
    {
        insert( 0, item );
    }
    /// stack-like interface: get top item w/o removing from stack
    T * peek()
    {
        if ( empty() )
            return NULL;
        return get( length() - 1 );
    }
    /// stack-like interface: get top item w/o removing from stack
    T * peekHead()
    {
        if ( empty() )
            return NULL;
        return get( 0 );
    }
    /// destructor
    ~LVPtrVector() { clear(); }
};

template<class _Ty > class LVMatrix {
protected:
    int numcols;
    int numrows;
    std::vector<_Ty> cells;
public:
    LVMatrix() : numcols(0), numrows(0), cells() {}
    LVMatrix(const LVMatrix &) = default;
    LVMatrix &operator=(const LVMatrix &) = default;
    LVMatrix(LVMatrix &&matrix) noexcept
        : numcols(matrix.numcols),
          numrows(matrix.numrows),
          cells(std::move(matrix.cells))
    {
        matrix.cells.clear();
        matrix.numcols = 0;
        matrix.numrows = 0;
    }
    LVMatrix &operator=(LVMatrix &&matrix) noexcept
    {
        if (this == &matrix)
            return *this;
        numcols = matrix.numcols;
        numrows = matrix.numrows;
        cells = std::move(matrix.cells);
        matrix.cells.clear();
        matrix.numcols = 0;
        matrix.numrows = 0;
        return *this;
    }
    void Clear() {
        std::vector<_Ty>().swap(cells);
        numrows = 0;
        numcols = 0;
    }
    ~LVMatrix() = default;

    _Ty * operator [] (int rowindex) {
        return cells.data()
                + static_cast<size_t>(rowindex)
                    * static_cast<size_t>(numcols);
    }
    const _Ty * operator [] (int rowindex) const {
        return cells.data()
                + static_cast<size_t>(rowindex)
                    * static_cast<size_t>(numcols);
    }

    void SetSize( int nrows, int ncols, _Ty fill_elem ) {
        if (nrows <= 0 || ncols <= 0) {
            Clear();
            return;
        }
        const size_t rowCount = static_cast<size_t>(nrows);
        const size_t columnCount = static_cast<size_t>(ncols);
        const size_t maxCellCount = std::min(
                cells.max_size(), static_cast<size_t>(INT_MAX));
        if (rowCount > maxCellCount / columnCount)
            throw std::length_error("LVMatrix dimensions overflow");

        std::vector<_Ty> replacement(
                rowCount * columnCount, fill_elem);
        const int retainedRows = std::min(numrows, nrows);
        const int retainedColumns = std::min(numcols, ncols);
        for (int row = 0; row < retainedRows; ++row) {
            const size_t oldOffset =
                    static_cast<size_t>(row)
                    * static_cast<size_t>(numcols);
            const size_t newOffset =
                    static_cast<size_t>(row) * columnCount;
            std::copy_n(cells.begin() + oldOffset,
                    retainedColumns,
                    replacement.begin() + newOffset);
        }
        cells.swap(replacement);
        numrows = nrows;
        numcols = ncols;
    }
};

template <typename T1, typename T2> class LVPair
{
    T1 _first;
    T2 _second;
public:
    LVPair( const T1 & first, const T2 & second )
    : _first(first), _second(second) {
    }
    LVPair( const LVPair & v ) 
    : _first(v._first), _second(v._second) {
    }
    LVPair & operator = ( const LVPair & v ) 
    {
        _first = v._first;
        _second = v._second;
        return *this;
    }
    T1 & first() { return _first; }
    const T1 & first() const { return _first; }
    T2 & second() { return _second; }
    const T2 & second() const { return _second; }
    ~LVPair() { }
};

#endif
