/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2008,2011-2013 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2018 poire-z <poire-z@users.noreply.github.com>         *
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
 * \file lvmemman.h
 * \brief Fast memory manager implementation
 */

#ifndef __LV_MEM_MAN_H_INCLUDED__
#define __LV_MEM_MAN_H_INCLUDED__


#include "crsetup.h"
#include "lvtypes.h"

#define CR_FATAL_ERROR_UNKNOWN             -1
#define CR_FATAL_ERROR_INDEX_OUT_OF_BOUND   1

/// fatal error function type
typedef void (lv_FatalErrorHandler_t)(int errorCode, const char * errorText );

/// set file to remove of fatal error - for removing of book cache file which caused crash
void crSetFileToRemoveOnFatalError(const char * filename);

/// set signal handler to do some cleanup actions (e.g. delete current cache file since it might be corrupted)
void crSetSignalHandler();

/// fatal error function calls fatal error handler
void crFatalError( int code, const char * errorText );
inline void crFatalError() { crFatalError( -1, "Unknown fatal error" ); }

/// set fatal error handler
void crSetFatalErrorHandler( lv_FatalErrorHandler_t * handler );

/// typed realloc with result check (size is counted in T), fatal error if failed
template <typename T> inline T * cr_realloc( T * ptr, size_t newSize ) {
    T * newptr = reinterpret_cast<T*>(realloc(ptr, sizeof(T)*newSize));
    if ( newptr )
        return newptr;
    free(ptr); // to bypass cppcheck warning
    crFatalError(-2, "realloc failed");
    return NULL;
}


#if (LDOM_USE_OWN_MEM_MAN==1)
#include <array>
#include <cstdint>
#include <cstdlib>
#include <limits>
#include <memory>
#include <new>
#include <utility>

#define THROW_MEM_MAN_EXCEPTION crFatalError(-1, "Memory manager fatal error" );

#define BLOCK_SIZE_GRANULARITY 2
#define LOCAL_STORAGE_COUNT    16
#define FIRST_SLICE_SIZE       16
#define MAX_SLICE_COUNT        24
#define MAX_LOCAL_BLOCK_SIZE   ((1<<BLOCK_SIZE_GRANULARITY)*LOCAL_STORAGE_COUNT)

/// memory block
union ldomMemBlock {
//    struct {
        char buf[4];
//    };
//    struct {
        ldomMemBlock * nextfree;
//    };
};

struct ldomMemBlockArrayDeleter {
    void operator()(ldomMemBlock *blocks) const
    {
        std::free(blocks);
    }
};

/// memory allocation slice
struct ldomMemSlice {
    std::unique_ptr<
            ldomMemBlock,
            ldomMemBlockArrayDeleter> blocks;
    ldomMemBlock * pBlocks; // borrowed first block
    ldomMemBlock * pEnd;    // first free byte after last block
    ldomMemBlock * pFree;   // first free block
    size_t block_size;      // size of block
    size_t block_count;     // count of blocks
    size_t blocks_used;     // number of used blocks

    static ldomMemBlock *allocateBlocks(
            size_t blockSize, size_t blockCount)
    {
        if (blockCount != 0
                && blockSize
                        > std::numeric_limits<size_t>::max() / blockCount)
            throw std::bad_alloc();
        ldomMemBlock *result = static_cast<ldomMemBlock *>(
                std::malloc(blockSize * blockCount));
        if (result == NULL)
            throw std::bad_alloc();
        return result;
    }

    //
    inline ldomMemBlock * blockByIndex( size_t index )
    {
        return (ldomMemBlock *) ( (char*)pBlocks + (block_size*index) );
    }
    inline ldomMemBlock * nextBlock( ldomMemBlock * p )
    {
        return (ldomMemBlock *) ( (char*)p + (block_size) );
    }
    inline ldomMemBlock * prevBlock( ldomMemBlock * p )
    {
        return (ldomMemBlock *) ( (char*)p - (block_size) );
    }
    //
    ldomMemSlice( size_t blockSize, size_t blockCount )
    :   blocks(allocateBlocks(blockSize, blockCount)),
        pBlocks(blocks.get()),
        pEnd(NULL),
        pFree(NULL),
        block_size(blockSize),
        block_count(blockCount),
        blocks_used(0)
    {
        pEnd = blockByIndex(block_count);
        pFree = pBlocks;
        for (ldomMemBlock * p = pBlocks; p<pEnd; )
        {
            p = p->nextfree = nextBlock(p);
        }
        prevBlock(pEnd)->nextfree = NULL;
    }
    ldomMemSlice(const ldomMemSlice &) = delete;
    ldomMemSlice &operator=(const ldomMemSlice &) = delete;
    ~ldomMemSlice() = default;

    inline ldomMemBlock * alloc_block()
    {
        ldomMemBlock * res = pFree;
        pFree = res->nextfree;
        ++blocks_used;
        return res;
    }
    inline bool free_block( ldomMemBlock * pBlock )
    {
        const std::uintptr_t blockAddress =
                reinterpret_cast<std::uintptr_t>(pBlock);
        const std::uintptr_t firstAddress =
                reinterpret_cast<std::uintptr_t>(pBlocks);
        const std::uintptr_t endAddress =
                reinterpret_cast<std::uintptr_t>(pEnd);
        if (blockAddress < firstAddress
                || blockAddress >= endAddress
                || (blockAddress - firstAddress) % block_size != 0)
            return false; // chunk does not belong to this slice
        pBlock->nextfree = pFree;
        pFree = pBlock;
        --blocks_used;
        return true;
    }
};

/// storage for blocks of specified size
struct ldomMemManStorage
{
    size_t block_size;      // size of block
    size_t slice_count;     // count of existing blocks
    std::array<
            std::unique_ptr<ldomMemSlice>,
            MAX_SLICE_COUNT> slices;

    static size_t normalizeBlockSize(size_t blockSize)
    {
        const size_t alignment = alignof(ldomMemBlock);
        if (blockSize < sizeof(ldomMemBlock))
            blockSize = sizeof(ldomMemBlock);
        if (blockSize
                > std::numeric_limits<size_t>::max() - (alignment - 1))
            throw std::bad_alloc();
        return (blockSize + alignment - 1) & ~(alignment - 1);
    }

    void init()
    {
        if (slice_count != 0)
            return;
        slices[0] =
                std::make_unique<ldomMemSlice>(
                        block_size, FIRST_SLICE_SIZE);
        slice_count = 1;
    }

    ldomMemSlice *writableSlice()
    {
        init();
        for (int i=static_cast<int>(slice_count)-1; i>=0; --i)
        {
            if (slices[static_cast<size_t>(i)]->pFree != NULL)
                return slices[static_cast<size_t>(i)].get();
        }
        if (slice_count >= MAX_SLICE_COUNT)
            THROW_MEM_MAN_EXCEPTION;
        std::unique_ptr<ldomMemSlice> candidate =
                std::make_unique<ldomMemSlice>(
                        block_size,
                        static_cast<size_t>(FIRST_SLICE_SIZE)
                                << (slice_count + 1));
        ldomMemSlice *result = candidate.get();
        slices[slice_count++] = std::move(candidate);
        return result;
    }

    //======================================
    ldomMemManStorage( size_t blockSize )
        : block_size(normalizeBlockSize(blockSize)),
          slice_count(0),
          slices()
    {
        init();
    }
    ldomMemManStorage(const ldomMemManStorage &) = delete;
    ldomMemManStorage &operator=(const ldomMemManStorage &) = delete;
    ~ldomMemManStorage() = default;

    void * alloc()
    {
        return writableSlice()->alloc_block();
    }
    void free( ldomMemBlock * pBlock )
    {
        for (int i=static_cast<int>(slice_count)-1; i>=0; --i)
        {
            if (slices[static_cast<size_t>(i)]->free_block(pBlock))
                return;
        }
        //throw; // wrong pointer!!!
    }
    void clear()
    {
        for (size_t i=0; i<slice_count; ++i)
            slices[i].reset();
        slice_count = 0;
    }
};

/// allocate memory
void * ldomAlloc( size_t n );
/// free memory
void   ldomFree( void * p, size_t n );
/// get the first-use-lifetime pool for intrusive reference records
ldomMemManStorage & ldomRefStorage();


//////////////////////////////////////////////////////////////////////

/// declare allocator for class: use in class declarations
#define DECLARE_CLASS_ALLOCATOR(classname) \
    void * operator new( size_t size ); \
    void operator delete( void * p );


/// define allocator for class: use in class definitions
#define DEFINE_CLASS_ALLOCATOR(classname) \
\
static ldomMemManStorage & ms ## classname() \
{ \
    static ldomMemManStorage storage(sizeof(classname)); \
    return storage; \
} \
\
void * classname::operator new( size_t size ) \
{ \
    (void)size; \
    return ms ## classname().alloc(); \
} \
\
void classname::operator delete( void * p ) \
{ \
    ms ## classname().free((ldomMemBlock *)p); \
}

void ldomFreeStorage();
bool LVRunDomBlockStorageOwnershipRegression();

#endif

#endif //__LV_MEM_MAN_H_INCLUDED__
