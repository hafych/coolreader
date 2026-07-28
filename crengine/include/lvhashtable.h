/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2009-2013 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2020,2021 Aleksey Chernov <valexlin@gmail.com>          *
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
 * \file lvhashtable.h
 * \brief hash table template
 */

#ifndef __LVHASHTABLE_H_INCLUDED__
#define __LVHASHTABLE_H_INCLUDED__

#include "lvtypes.h"

#include <climits>
#include <memory>
#include <stdexcept>
#include <utility>
#include <vector>

inline lUInt32 getHash( lUInt16 n )
{
    return (lUInt32)n * 1975317 + 164521;
}

inline lUInt32 getHash( lUInt32 n )
{
    return n * 1975317 + 164521;
}

inline lUInt32 getHash( lUInt64 n )
{
    return (lUInt32)(n * 1975317 + (n >> 32) * 31 + 164521);
}

class LVFont;
inline lUInt32 getHash(LVFont * n )
{
    return getHash((lUInt64)n);
}

inline lUInt32 getHash(void * n )
{
    return getHash((lUInt64)n);
}

/// Hash table
/**
    Implements hash table map
*/
template <typename keyT, typename valueT> class LVHashTable
{
    friend class iterator;
public:
    class pair {
        friend class LVHashTable;
    public:
        std::unique_ptr<pair> next;
        keyT key;
        valueT value;

        pair(const keyT &nkey, const valueT &nvalue)
            : next(), key(nkey), value(nvalue)
        {
        }
        pair(const pair &) = delete;
        pair &operator=(const pair &) = delete;
    };

    class iterator {
        friend class LVHashTable;
        const LVHashTable &_tbl;
        std::size_t index;
        pair *ptr;
        iterator &operator=(const iterator &) = delete;
    public:
        iterator(const LVHashTable &table)
            : _tbl(table), index(0), ptr(NULL)
        {
        }
        iterator(const iterator &v)
            : _tbl(v._tbl), index(v.index), ptr(v.ptr)
        {
        }
        pair *next()
        {
            if (ptr) {
                ptr = ptr->next.get();
                if (ptr)
                    return ptr;
            }
            for (; index < _tbl._table.size();) {
                ptr = _tbl._table[index++].get();
                if (ptr)
                    return ptr;
            }
            return NULL;
        }
    };

    iterator forwardIterator() const
    {
        return iterator(*this);
    }

    LVHashTable(int size)
        : _table(static_cast<std::size_t>(size < 16 ? 16 : size))
        , _count(0)
    {
    }

    LVHashTable(const LVHashTable &table)
        : _table(table._table.size())
        , _count(0)
    {
        for (std::size_t i = 0; i < table._table.size(); i++) {
            std::unique_ptr<pair> *link = &_table[i];
            for (pair *item = table._table[i].get();
                    item; item = item->next.get()) {
                link->reset(new pair(item->key, item->value));
                link = &(*link)->next;
                _count++;
            }
        }
    }

    LVHashTable &operator=(const LVHashTable &table)
    {
        if (this != &table) {
            LVHashTable copy(table);
            swap(copy);
        }
        return *this;
    }

    ~LVHashTable()
    {
        clear();
    }

    void swap(LVHashTable &table) noexcept
    {
        _table.swap(table._table);
        std::swap(_count, table._count);
    }

    void clear()
    {
        for (std::unique_ptr<pair> &bucket : _table) {
            while (bucket) {
                std::unique_ptr<pair> item = std::move(bucket);
                bucket = std::move(item->next);
            }
        }
        _count = 0;
    }

    int length() const { return _count; }
    int size() const { return static_cast<int>(_table.size()); }

    void resize(int nsize)
    {
        const std::size_t bucketCount =
                static_cast<std::size_t>(nsize > 0 ? nsize : 1);
        std::vector<std::unique_ptr<pair>> replacement(bucketCount);
        for (std::unique_ptr<pair> &bucket : _table) {
            while (bucket) {
                std::unique_ptr<pair> item = std::move(bucket);
                bucket = std::move(item->next);
                const std::size_t index =
                        getHash(item->key) % bucketCount;
                item->next = std::move(replacement[index]);
                replacement[index] = std::move(item);
            }
        }
        _table.swap(replacement);
    }

    void set(const keyT &key, valueT value)
    {
        std::size_t index = getHash(key) % _table.size();
        std::unique_ptr<pair> *link = &_table[index];
        for (; *link; link = &(*link)->next) {
            if ((*link)->key == key) {
                (*link)->value = value;
                return;
            }
        }
        if (static_cast<std::size_t>(_count) >= _table.size()) {
            if (_table.size()
                    > static_cast<std::size_t>(INT_MAX) / 2)
                throw std::length_error("LVHashTable capacity overflow");
            const std::size_t doubled = _table.size() * 2;
            const std::size_t required =
                    static_cast<std::size_t>(_count) + 1;
            resize(static_cast<int>(
                    doubled > required ? doubled : required));
            index = getHash(key) % _table.size();
            link = &_table[index];
            for (; *link; link = &(*link)->next) {
            }
        }
        link->reset(new pair(key, value));
        _count++;
    }

    void remove(const keyT &key)
    {
        const std::size_t index = getHash(key) % _table.size();
        std::unique_ptr<pair> *link = &_table[index];
        for (; *link; link = &(*link)->next) {
            if ((*link)->key == key) {
                std::unique_ptr<pair> removed = std::move(*link);
                *link = std::move(removed->next);
                _count--;
                return;
            }
        }
    }

    valueT get(const keyT &key) const
    {
        const std::size_t index = getHash(key) % _table.size();
        for (pair *item = _table[index].get();
                item; item = item->next.get()) {
            if (item->key == key)
                return item->value;
        }
        return valueT();
    }

    bool get(const keyT &key, valueT &res) const
    {
        const std::size_t index = getHash(key) % _table.size();
        for (pair *item = _table[index].get();
                item; item = item->next.get()) {
            if (item->key == key) {
                res = item->value;
                return true;
            }
        }
        return false;
    }

    void compact()
    {
        if (_count > 0
                && static_cast<std::size_t>(_count) < _table.size())
            resize(_count);
    }

private:
    std::vector<std::unique_ptr<pair>> _table;
    int _count;
};


#endif
