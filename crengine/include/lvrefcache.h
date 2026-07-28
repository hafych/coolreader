/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2009-2011 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2013 Konstantin Potapov <pkbo@users.sourceforge.net>    *
 *   Copyright (C) 2018 Aleksey Chernov <valexlin@gmail.com>               *
 *   Copyright (C) 2018,2020 poire-z <poire-z@users.noreply.github.com>    *
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
 * \file lvrefcache.h
 * \brief Referenced objects cache.
 *
 * Allows to reuse objects with the same contents.
 */

#if !defined(__LV_REF_CACHE_H_INCLUDED__)
#define __LV_REF_CACHE_H_INCLUDED__

#include "lvmemman.h"
#include "lvref.h"
#include "lvarray.h"
#include "lvcache.h"

#include <climits>
#include <memory>
#include <stdexcept>
#include <utility>
#include <vector>

/*
    Object cache

    Requirements: 
       bool operator == (LVRef<T> & r1, LVRef<T> & r2 ) should be defined
       lUInt32 calcHash( LVRef<T> & r1 ) should be defined
*/

template <class ref_t> 
class LVRefCache {

    class LVRefCacheRec {
        ref_t style;
        lUInt32 hash;
        std::unique_ptr<LVRefCacheRec> next;
        LVRefCacheRec(const ref_t &s, lUInt32 h)
            : style(s), hash(h), next() { }
        friend class LVRefCache< ref_t >;
    };

private:
    std::vector<std::unique_ptr<LVRefCacheRec> > table;

    static int normalizedBucketCount(int requested)
    {
        if (requested <= 1)
            return 1;
        int count = 1;
        while (count < requested) {
            if (count > INT_MAX / 2)
                throw std::length_error("LVRefCache bucket count overflow");
            count <<= 1;
        }
        return count;
    }

    size_t bucketIndex(lUInt32 hash) const
    {
        return static_cast<size_t>(hash)
                & (static_cast<size_t>(table.size()) - 1);
    }

    void clearBuckets()
    {
        for (size_t index = 0; index < table.size(); ++index) {
            while (table[index]) {
                std::unique_ptr<LVRefCacheRec> removed =
                        std::move(table[index]);
                table[index] = std::move(removed->next);
            }
        }
    }

public:
    explicit LVRefCache(int sz)
        : table(static_cast<size_t>(normalizedBucketCount(sz))) {
    }

    LVRefCache(const LVRefCache &) = delete;
    LVRefCache &operator=(const LVRefCache &) = delete;

    // check whether equal object already exists if cache
    // if found, replace reference with cached value
    void cacheIt(ref_t & style)
    {
        if (style.isNull())
            return;
        lUInt32 hash = calcHash( style );
        std::unique_ptr<LVRefCacheRec> *link = &table[bucketIndex(hash)];
        while (*link) {
            LVRefCacheRec *record = link->get();
            if (record->hash == hash
                    && *record->style.get() == *style.get()) {
                style = record->style;
                return;
            }
            link = &record->next;
        }
        link->reset(new LVRefCacheRec(style, hash));
    }

    // garbage collector: remove unused entries
    void gc()
    {
        for (size_t index = 0; index < table.size(); ++index) {
            std::unique_ptr<LVRefCacheRec> *link = &table[index];
            while (*link) {
                if ((*link)->style.getRefCount() == 1) {
                    std::unique_ptr<LVRefCacheRec> removed =
                            std::move(*link);
                    *link = std::move(removed->next);
                } else {
                    link = &(*link)->next;
                }
            }
        }
    }

    ~LVRefCache()
    {
        clearBuckets();
    }
};

template <class ref_t>
class LVIndexedRefCache {

    // hash table item
    struct LVRefCacheRec {
        int index;
        ref_t style;
        lUInt32 hash;
        std::unique_ptr<LVRefCacheRec> next;
        LVRefCacheRec(const ref_t &s, lUInt32 h)
            : index(0), style(s), hash(h), next() { }
    };

    // index item
    struct LVRefCacheIndexRec {
        LVRefCacheRec * item;
        int refcount; // refcount, or next free index if item==NULL
        LVRefCacheIndexRec()
            : item(NULL), refcount(0) { }
    };

private:
    int size;
    std::vector<std::unique_ptr<LVRefCacheRec> > table;
    std::vector<LVRefCacheIndexRec> index;
    int nextindex;
    int freeindex;
    int numitems;

    static int normalizedBucketCount(int requested)
    {
        if (requested <= 1)
            return 1;
        int count = 1;
        while (count < requested) {
            if (count > INT_MAX / 2)
                throw std::length_error(
                        "LVIndexedRefCache bucket count overflow");
            count <<= 1;
        }
        return count;
    }

    size_t bucketIndex(lUInt32 hash) const
    {
        return static_cast<size_t>(hash)
                & (static_cast<size_t>(table.size()) - 1);
    }

    void clearBuckets()
    {
        for (size_t tableIndex = 0;
                tableIndex < table.size(); ++tableIndex) {
            while (table[tableIndex]) {
                std::unique_ptr<LVRefCacheRec> removed =
                        std::move(table[tableIndex]);
                table[tableIndex] = std::move(removed->next);
            }
        }
    }

    void swap(LVIndexedRefCache &cache) noexcept
    {
        std::swap(size, cache.size);
        table.swap(cache.table);
        index.swap(cache.index);
        std::swap(nextindex, cache.nextindex);
        std::swap(freeindex, cache.freeindex);
        std::swap(numitems, cache.numitems);
    }

    int allocateIndex()
    {
        int n = 0;
        if (freeindex) {
            n = freeindex;
        } else {
            if (nextindex == INT_MAX)
                throw std::length_error(
                        "LVIndexedRefCache index overflow");
            n = nextindex + 1;
        }

        if (static_cast<size_t>(n) >= index.size()) {
            size_t newSize = index.empty()
                    ? static_cast<size_t>(size / 2)
                    : index.size() * 2;
            if (newSize < 2)
                newSize = 2;
            if (newSize <= static_cast<size_t>(n))
                newSize = static_cast<size_t>(n) + 1;
            if (newSize > static_cast<size_t>(INT_MAX))
                throw std::length_error(
                        "LVIndexedRefCache index storage overflow");
            index.resize(newSize);
        }

        if (freeindex)
            freeindex = index[n].refcount;
        else
            nextindex = n;
        return n;
    }

    // remove item from hash table
    bool removeItem(LVRefCacheRec *item)
    {
        std::unique_ptr<LVRefCacheRec> *link =
                &table[bucketIndex(item->hash)];
        while (*link) {
            if (link->get() == item) {
                std::unique_ptr<LVRefCacheRec> removed =
                        std::move(*link);
                *link = std::move(removed->next);
                --numitems;
                return true;
            }
            link = &(*link)->next;
        }
        return false;
    }

public:

    std::unique_ptr<LVArray<ref_t> > getIndex() const
    {
        std::unique_ptr<LVArray<ref_t> > list(
                new LVArray<ref_t>(static_cast<int>(index.size()), ref_t()));
        for (size_t i = 1; i < index.size(); ++i) {
            if (index[i].item)
                list->set(static_cast<int>(i), index[i].item->style);
        }
        return list;
    }

    int length() const
    {
        return numitems;
    }

    void release( ref_t r )
    {
        int i = find(r);
        if (  i>0 )
            release(i);
    }

    void release( int n )
    {
        if (n < 1 || n > nextindex
                || static_cast<size_t>(n) >= index.size())
            return;
        if (index[n].item) {
            if (index[n].refcount > 1) {
                --index[n].refcount;
            } else if (removeItem(index[n].item)) {
                // next free
                index[n].refcount = freeindex;
                index[n].item = NULL;
                freeindex = n;
            }
        }
    }

    // get by index
    ref_t get( int n ) const
    {
        if (n > 0 && n <= nextindex
                && static_cast<size_t>(n) < index.size()
                && index[n].item)
            return index[n].item->style;
        return ref_t();
    }

    // check whether equal object already exists if cache
    // if found, replace reference with cached value
    // returns index of item - use it to release reference
    bool cache( lUInt16 &indexholder, ref_t & style)
    {
        int newindex = cache( style );
        if (newindex > USHRT_MAX) {
            release(newindex);
            throw std::length_error(
                    "LVIndexedRefCache index does not fit lUInt16");
        }
        // printf("newindex: %d  /  provided indexholder: %d\n", newindex, (int)indexholder);
        if ( indexholder != newindex ) {
            release( indexholder );
            indexholder = (lUInt16)newindex;
            return true;
        } else { // indexholder == newindex
            // Here, we want to decrement the refcount that has been incremented
            // by the above call "newindex = cache( style )", as it returned the
            // same index as the already referenced one.
            // In normal cases, when the item is already present in the cache
            // and referenced once, we are here with refcount=2, and we want to
            // put it back to refcount=1, as it should be.
            // In rare cases for some yet undertermined reason or bug, we may
            // have been called with a non-0 indexholder, but not previously
            // present in the cache, and we get the same value for newindex:
            // the cache item refcount is then 1. And we want it to stay 1 (as
            // it's really referenced once) so the value/pointer are not freed,
            // and some segmentation fault is avoided later.
            // So, we just don't release it if the refcount is 1 (we were
            // asked to cache something: so don't drop it!)
            // printf("released: refcount for %d = %d\n", indexholder, this->index[indexholder].refcount);
            if (static_cast<size_t>(indexholder) < index.size()
                    && this->index[indexholder].refcount > 1)
                release( indexholder );
            return false;
            // This returned boolean seems not used anywhere, so it's not
            // clear whether we should return true or false when the
            // item was not in the cache, and thus created - but the
            // indexholder (although not existing) stayed unchanged.
        }
    }

    bool addIndexRef( lUInt16 n )
    {
        if (n > 0 && n <= nextindex
                && static_cast<size_t>(n) < index.size()
                && index[n].item) {
            if (index[n].refcount == INT_MAX)
                throw std::length_error(
                        "LVIndexedRefCache refcount overflow");
            index[n].refcount++;
            return true;
        } else
            return false;
    }

    // check whether equal object already exists if cache
    // if found, replace reference with cached value
    // returns index of item - use it to release reference
    int cache(ref_t & style)
    {
        if (style.isNull())
            return 0;
        lUInt32 hash = calcHash( style );
        const size_t tableIndex = bucketIndex(hash);
        std::unique_ptr<LVRefCacheRec> *link = &table[tableIndex];
        while (*link) {
            LVRefCacheRec *record = link->get();
            if (record->hash == hash
                    && *record->style.get() == *style.get()) {
                int n = record->index;
                if (index[n].refcount == INT_MAX)
                    throw std::length_error(
                            "LVIndexedRefCache refcount overflow");
                style = record->style;
                this->index[n].refcount++;
                return n;
            }
            link = &record->next;
        }

        if (numitems == INT_MAX)
            throw std::length_error(
                    "LVIndexedRefCache item count overflow");
        std::unique_ptr<LVRefCacheRec> record(
                new LVRefCacheRec(style, hash));
        int n = allocateIndex();
        record->index = n;
        LVRefCacheRec *recordView = record.get();
        record->next = std::move(table[tableIndex]);
        table[tableIndex] = std::move(record);
        index[n].item = recordView;
        index[n].refcount = 1;
        ++numitems;
        return n;
    }

    // check whether equal object already exists if cache
    // if found, replace reference with cached value
    // returns index of item - use it to release reference
    int find(ref_t & style)
    {
        if (style.isNull())
            return 0;
        lUInt32 hash = calcHash( style );
        LVRefCacheRec *record = table[bucketIndex(hash)].get();
        while (record) {
            if (record->hash == hash
                    && *record->style.get() == *style.get())
                return record->index;
            record = record->next.get();
        }
        return 0;
    }

    /// from index array
    LVIndexedRefCache( LVArray<ref_t> &list )
    : size(1)
    , table(1)
    , index()
    , nextindex(0)
    , freeindex(0)
    , numitems(0)
    {
        setIndex(list);
    }

    /// init from index array
    void setIndex( LVArray<ref_t> &list )
    {
        LVIndexedRefCache candidate(
                list.length() > 0 ? list.length() : 32);
        candidate.index.resize(static_cast<size_t>(list.length()));
        candidate.nextindex = list.length() > 0 ? list.length() - 1 : 0;
        for (int i = 1; i < list.length(); ++i) {
            if (list[i].isNull()) {
                // add free node
                candidate.index[i].refcount = candidate.freeindex;
                candidate.freeindex = i;
            } else {
                // add item
                lUInt32 hash = calcHash(list[i]);
                const size_t tableIndex = candidate.bucketIndex(hash);
                std::unique_ptr<LVRefCacheRec> record(
                        new LVRefCacheRec(list[i], hash));
                record->index = i;
                LVRefCacheRec *recordView = record.get();
                record->next = std::move(candidate.table[tableIndex]);
                candidate.table[tableIndex] = std::move(record);
                candidate.index[i].item = recordView;
                candidate.index[i].refcount = 1;
                ++candidate.numitems;
            }
        }
        swap(candidate);
    }

    explicit LVIndexedRefCache(int sz)
    : size(normalizedBucketCount(sz))
    , table(static_cast<size_t>(size))
    , index()
    , nextindex(0)
    , freeindex(0)
    , numitems(0)
    {
    }

    LVIndexedRefCache(const LVIndexedRefCache &) = delete;
    LVIndexedRefCache &operator=(const LVIndexedRefCache &) = delete;

    void clear( int sz = 0 )
    {
        if (sz == -1)
            sz = size;
        if (sz) {
            const int newSize = normalizedBucketCount(sz);
            if (newSize != size) {
                std::vector<std::unique_ptr<LVRefCacheRec> > replacement(
                        static_cast<size_t>(newSize));
                clearBuckets();
                table = std::move(replacement);
                size = newSize;
            } else {
                clearBuckets();
            }
        } else {
            clearBuckets();
        }
        std::vector<LVRefCacheIndexRec>().swap(index);
        nextindex = 0;
        freeindex = 0;
        numitems = 0;
    }

    ~LVIndexedRefCache()
    {
        clearBuckets();
    }
};

template <typename keyT, class dataT> class LVCacheMap
{
private:
    class Pair {
    public: 
        keyT key;
        dataT data;
        int lastAccess;
    };
    std::vector<Pair> buf;
    int size;
    int orig_size;
    int numitems;
    int lastAccess;
    lUInt64 hits;
    lUInt64 misses;
    lUInt64 evictions;
    void checkOverflow( int oldestAccessTime )
    {
        int i;
        if ( oldestAccessTime==-1 ) {
            for ( i=0; i<size; i++ )
                if ( oldestAccessTime==-1 || buf[i].lastAccess>oldestAccessTime )
                    oldestAccessTime = buf[i].lastAccess;
        }
        if ( oldestAccessTime>1000000000 ) {
            int maxLastAccess = 0;
            for ( i=0; i<size; i++ ) {
                buf[i].lastAccess -= 1000000000;
                if ( maxLastAccess==0 || buf[i].lastAccess>maxLastAccess )
                    maxLastAccess = buf[i].lastAccess;
            }
            lastAccess = maxLastAccess+1;
        }
    }
public:
    int length()
    {
        return numitems;
    }
    LVCacheMap( int maxSize )
    : buf(static_cast<size_t>(maxSize > 0 ? maxSize : 0)),
      size(maxSize > 0 ? maxSize : 0),
      orig_size(maxSize > 0 ? maxSize : 0),
      numitems(0), lastAccess(1),
      hits(0), misses(0), evictions(0)
    {
        clear();
    }
    void reduceSize(int newSize)
    {
        const int boundedSize = newSize > 0 ? newSize : 0;
        if (boundedSize < orig_size) {
            clear();
            size = boundedSize;
        }
    }
    void restoreSize()
    {
        size = orig_size;
        clear();
    }
    void clear()
    {
        for ( int i=0; i<size; i++ )
        {
            buf[i].key = keyT();
            buf[i].data = dataT();
            buf[i].lastAccess = 0;
        }
        numitems = 0;
    }
    bool get( keyT key, dataT & data )
    {
        for ( int i=0; i<size; i++ ) {
            if ( buf[i].key == key ) {
                data = buf[i].data;
                buf[i].lastAccess = ++lastAccess;
                hits++;
                if ( lastAccess>1000000000 )
                    checkOverflow(-1);
                return true;
            }
        }
        misses++;
        return false;
    }
    bool remove( keyT key )
    {
        for ( int i=0; i<size; i++ ) {
            if ( buf[i].key == key ) {
                buf[i].key = keyT();
                buf[i].data = dataT();
                buf[i].lastAccess = 0;
                numitems--;
                return true;
            }
        }
        return false;
    }
    void set( keyT key, dataT data )
    {
        if ( size <= 0 )
            return;
        int oldestAccessTime = -1;
        int oldestIndex = 0;
        for ( int i=0; i<size; i++ ) {
            if ( buf[i].key == key ) {
                buf[i].data = data;
                buf[i].lastAccess = ++lastAccess;
                return;
            }
            int at = buf[i].lastAccess;
            if ( at < oldestAccessTime || oldestAccessTime==-1 ) {
                oldestAccessTime = at;
                oldestIndex = i;
            }
        }
        checkOverflow(oldestAccessTime);
        if ( buf[oldestIndex].key==keyT() )
            numitems++;
        else
            evictions++;
        buf[oldestIndex].key = key;
        buf[oldestIndex].data = data;
        buf[oldestIndex].lastAccess = ++lastAccess;
        return;
    }
    LVCacheStats getStats()
    {
        return LVCacheStats(0, 0, hits, misses, evictions,
                size, numitems);
    }
    void resetStats()
    {
        hits = 0;
        misses = 0;
        evictions = 0;
    }
    ~LVCacheMap() = default;
};

#endif // __LV_REF_CACHE_H_INCLUDED__
