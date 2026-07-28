/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2010,2011 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#ifndef __LVBLOCKWRITESTREAM_H_INCLUDED__
#define __LVBLOCKWRITESTREAM_H_INCLUDED__

#include "lvnamedstream.h"

#include <memory>
#include <vector>

class LVBlockWriteStream : public LVNamedStream
{
    LVStreamRef _baseStream;
    int _blockSize;
    int _blockCount;
    lvpos_t _pos;
    lvpos_t _size;

    struct Block
    {
        lvpos_t block_start;
        lvpos_t block_end;
        lvpos_t modified_start;
        lvpos_t modified_end;
        std::vector<lUInt8> buf;
        int size;
        std::unique_ptr<Block> next;

        Block( lvpos_t start, lvpos_t end, int block_size );
        bool save( const lUInt8 * ptr, lvpos_t pos, lvsize_t len );
        bool containsPos( lvpos_t pos )
        {
            return pos >= block_start
                    && pos - block_start < static_cast<lvpos_t>(size);
        }
    };

    // list of blocks
    std::unique_ptr<Block> _firstBlock;
    int _count;

    /// set write bytes limit to call flush(true) automatically after writing of each sz bytes
    void setAutoSyncSize(lvsize_t sz) override;

    /// fills block with data existing in file
    lverror_t readBlock( Block * block );

    lverror_t writeBlock( Block * block );

    std::unique_ptr<Block> newBlock( lvpos_t start, int len );

    lverror_t evictLastBlock();

    /// find block, move to top if found
    Block * findBlock( lvpos_t pos );

    // try read block-aligned fragment from cache
    bool readFromCache( void * buf, lvpos_t pos, lvsize_t count );

    // write block-aligned fragment to cache
    lverror_t writeToCache( const void * buf, lvpos_t pos, lvsize_t count );
public:
    lverror_t Flush( bool sync ) override;
    /// flushes unsaved data from buffers to file, with optional flush of OS buffers
    lverror_t Flush( bool sync, CRTimerUtil & timeout ) override;

    ~LVBlockWriteStream() override;

    const lChar32 * GetName() override
            { return _baseStream->GetName(); }
    lvopen_mode_t GetMode() override
            { return _baseStream->GetMode(); }

    LVBlockWriteStream( LVStreamRef baseStream, int blockSize, int blockCount );
    LVBlockWriteStream(const LVBlockWriteStream &) = delete;
    LVBlockWriteStream &operator=(const LVBlockWriteStream &) = delete;

    lvsize_t GetSize() override
    {
        return _size;
    }

    lverror_t Seek(
            lvoffset_t offset, lvseek_origin_t origin,
            lvpos_t * pNewPos ) override;

    lverror_t Tell( lvpos_t * pPos ) override;
    lvpos_t SetPos(lvpos_t p) override;
    lvpos_t GetPos() override
    {
        return _pos;
    }
    lverror_t SetSize( lvsize_t size ) override;

    void dumpBlocks( const char * context);

    lverror_t Read(
            void * buf, lvsize_t count, lvsize_t * nBytesRead ) override;

    lverror_t Write(
            const void * buf, lvsize_t count,
            lvsize_t * nBytesWritten ) override;
    bool Eof() override
    {
        return _pos >= _size;
    }
};

#endif  // __LVBLOCKWRITESTREAM_H_INCLUDED__
