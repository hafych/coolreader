/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2010-2012 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2018,2019 Aleksey Chernov <valexlin@gmail.com>          *
 *   Copyright (C) 2020 poire-z <poire-z@users.noreply.github.com>         *
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

#include "lvblockwritestream.h"
#include "crtimerutil.h"
#include "crlog.h"

#include <cstring>
#include <limits>
#include <memory>
#include <new>
#include <stdio.h>
#include <utility>


#define TRACE_BLOCK_WRITE_STREAM 0


LVBlockWriteStream::Block::Block(lvpos_t start, lvpos_t end, int block_size)
    : block_start(block_size > 0 ? start / block_size * block_size : 0),
      block_end(end)
    , modified_start((lvpos_t)-1), modified_end((lvpos_t)-1)
    , buf(block_size > 0 ? static_cast<std::size_t>(block_size) : 0),
      size(block_size)
{
}

bool LVBlockWriteStream::Block::save(
        const lUInt8 *ptr, lvpos_t pos, lvsize_t len)
{
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("block %x save %x, %x", (int)block_start, (int)pos, (int)len);
#endif
    if (!ptr || pos < block_start
            || pos - block_start > static_cast<lvpos_t>(size)
            || len > static_cast<lvsize_t>(size)
            || len > static_cast<lvsize_t>(
                    size - static_cast<int>(pos - block_start))) {
        CRLog::error("Unaligned access to block %x", (int)block_start);
        return false;
    }
    const std::size_t offset =
            static_cast<std::size_t>(pos - block_start);
    for (lvsize_t i = 0; i < len; i++ ) {
        lUInt8 ch1 = buf[offset+i];
        if ( pos+i >= block_end || ch1!=ptr[i] ) {
            buf[offset+i] = ptr[i];
            if ( modified_start==(lvpos_t)-1 ) {
                modified_start = pos + i;
                modified_end = modified_start + 1;
            } else {
                if ( modified_start>pos+i )
                    modified_start = pos+i;
                if ( modified_end<pos+i+1)
                    modified_end = pos+i+1;
            }
            if ( block_end<pos+i+1)
                block_end = pos+i+1;
        }
    }
    return true;
}



void LVBlockWriteStream::setAutoSyncSize(lvsize_t sz) {
    _baseStream->setAutoSyncSize(sz);
    handleAutoSync(0);
}

lverror_t LVBlockWriteStream::readBlock(LVBlockWriteStream::Block *block)
{
    if ( !block->size ) {
        CRLog::error("Invalid block size");
    }
    lvpos_t start = block->block_start;
    lvpos_t end = start + _blockSize;
    lvpos_t ssize = 0;
    lverror_t res = LVERR_OK;
    res = _baseStream->GetSize( &ssize);
    if ( res!=LVERR_OK )
        return res;
    if ( end>ssize )
        end = ssize;
    if ( end<=start )
        return LVERR_OK;
    if (_baseStream->SetPos(start) != start)
        return LVERR_FAIL;
    lvsize_t bytesRead = 0;
    block->block_end = end;
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("block %x filling from stream %x, %x", (int)block->block_start, (int)block->block_start, (int)(block->block_end-block->block_start));
#endif
    res = _baseStream->Read(
            block->buf.data(), end-start, &bytesRead );
    if ( res!=LVERR_OK || bytesRead != end-start ) {
        CRLog::error("Error while reading block %x from file of size %x", block->block_start, ssize);
        return LVERR_FAIL;
    }
    return LVERR_OK;
}

lverror_t LVBlockWriteStream::writeBlock(LVBlockWriteStream::Block *block)
{
    if ( block->modified_start < block->modified_end ) {
#if TRACE_BLOCK_WRITE_STREAM
        CRLog::trace("WRITE BLOCK %x (%x, %x)", (int)block->block_start, (int)block->modified_start, (int)(block->modified_end-block->modified_start));
#endif
        if (_baseStream->SetPos(block->modified_start)
                != block->modified_start) {
            lvsize_t baseSize = 0;
            if (_baseStream->GetSize(&baseSize) != LVERR_OK
                    || block->modified_start <= baseSize
                    || _baseStream->SetSize(block->modified_start)
                            != LVERR_OK
                    || _baseStream->SetPos(block->modified_start)
                            != block->modified_start)
                return LVERR_FAIL;
        }
        if (block->modified_end > _size) {
            block->modified_end = block->block_end;
        }
        const lvsize_t writeSize =
                block->modified_end - block->modified_start;
        lvsize_t bytesWritten = 0;
        lverror_t res = _baseStream->Write(
                block->buf.data()
                        + (block->modified_start-block->block_start),
                writeSize, &bytesWritten );
        if ( res!=LVERR_OK || bytesWritten != writeSize )
            return LVERR_FAIL;
        if (_size < block->modified_end)
            _size = block->modified_end;
        block->modified_end = block->modified_start = (lvpos_t)-1;
        return LVERR_OK;
    } else
        return LVERR_OK;
}

std::unique_ptr<LVBlockWriteStream::Block>
LVBlockWriteStream::newBlock(lvpos_t start, int len)
{
    try {
        return std::make_unique<Block>(
                start, start + len, _blockSize);
    } catch (const std::bad_alloc &) {
        CRLog::error("block write-cache allocation failed");
        return std::unique_ptr<Block>();
    }
}

LVBlockWriteStream::Block *LVBlockWriteStream::findBlock(lvpos_t pos)
{
    for (std::unique_ptr<Block> *link = &_firstBlock;
            link->get(); link = &(*link)->next) {
        Block * item = link->get();
        if ( item->containsPos(pos) ) {
            if ( item!=_firstBlock.get() ) {
#if TRACE_BLOCK_WRITE_STREAM
                dumpBlocks("before reorder");
#endif
                std::unique_ptr<Block> found = std::move(*link);
                *link = std::move(found->next);
                found->next = std::move(_firstBlock);
                _firstBlock = std::move(found);
#if TRACE_BLOCK_WRITE_STREAM
                dumpBlocks("after reorder");
                CRLog::trace("found block %x (%x, %x)", (int)item->block_start, (int)item->modified_start, (int)(item->modified_end-item->modified_start));
#endif
            }
            return item;
        }
    }
    return NULL;
}

bool LVBlockWriteStream::readFromCache(void *buf, lvpos_t pos, lvsize_t count)
{
    Block * p = findBlock( pos );
    if ( p && buf
            && count <= static_cast<lvsize_t>(
                    p->size - static_cast<int>(pos - p->block_start))) {
#if TRACE_BLOCK_WRITE_STREAM
        CRLog::trace("read from cache block %x (%x, %x)", (int)p->block_start, (int)pos, (int)(count));
#endif
        std::memcpy(
                buf, p->buf.data() + (pos-p->block_start), count );
        return true;
    }
    return false;
}

lverror_t LVBlockWriteStream::evictLastBlock()
{
    if (!_firstBlock)
        return LVERR_OK;
    std::unique_ptr<Block> *last = &_firstBlock;
    while ((*last)->next)
        last = &(*last)->next;
    if (writeBlock(last->get()) != LVERR_OK)
        return LVERR_FAIL;
    last->reset();
    _count--;
    return LVERR_OK;
}

lverror_t LVBlockWriteStream::writeToCache(const void *buf, lvpos_t pos, lvsize_t count)
{
    if (!buf || count == 0
            || count > std::numeric_limits<lvpos_t>::max() - pos)
        return LVERR_FAIL;
    const lvpos_t endPos = pos + count;
    Block * p = findBlock( pos );
    if ( p ) {
#if TRACE_BLOCK_WRITE_STREAM
        CRLog::trace("saving data to existing block %x (%x, %x)", (int)p->block_start, (int)pos, (int)count);
#endif
        if (!p->save(static_cast<const lUInt8 *>(buf), pos, count))
            return LVERR_FAIL;
        if ( endPos > _size )
            _size = endPos;
        return LVERR_OK;
    }
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("Block %x not found in cache", pos);
#endif
    if (_count >= _blockCount) {
#if TRACE_BLOCK_WRITE_STREAM
        dumpBlocks("before remove last");
#endif
        if (evictLastBlock() != LVERR_OK)
            return LVERR_FAIL;
#if TRACE_BLOCK_WRITE_STREAM
        dumpBlocks("after remove last");
#endif
    }
    std::unique_ptr<Block> candidate =
            newBlock(pos, static_cast<int>(count));
    if (!candidate)
        return LVERR_FAIL;
    p = candidate.get();
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("creating block %x", (int)p->block_start);
#endif
    if ( readBlock( p )!=LVERR_OK ) {
        return LVERR_FAIL;
    }
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("saving data to new block %x (%x, %x)", (int)p->block_start, (int)pos, (int)count);
#endif
    if (!p->save(static_cast<const lUInt8 *>(buf), pos, count))
        return LVERR_FAIL;
    candidate->next = std::move(_firstBlock);
    _firstBlock = std::move(candidate);
    _count++;
    if ( endPos > _size ) {
        _size = endPos;
        p->modified_start = p->block_start;
        p->modified_end = p->block_end;
    }
    return LVERR_OK;
}

lverror_t LVBlockWriteStream::Flush(bool sync) {
    CRTimerUtil infinite;
    return Flush(sync, infinite); // NOLINT: Call to virtual function during destruction
}

lverror_t LVBlockWriteStream::Flush(bool sync, CRTimerUtil &timeout)
{
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("flushing unsaved blocks");
#endif
    while (_firstBlock) {
        if (writeBlock(_firstBlock.get()) != LVERR_OK)
            return LVERR_FAIL;
        std::unique_ptr<Block> flushed = std::move(_firstBlock);
        _firstBlock = std::move(flushed->next);
        _count--;
        if (!sync && timeout.expired()) {
            //CRLog::trace("LVBlockWriteStream::flush - timeout expired");
            return LVERR_OK;
        }
    }
    return _baseStream->Flush(sync);
}

LVBlockWriteStream::~LVBlockWriteStream()
{
    Flush( true ); // NOLINT: Call to virtual function during destruction
}

LVBlockWriteStream::LVBlockWriteStream(LVStreamRef baseStream, int blockSize, int blockCount)
    : _baseStream( baseStream ), _blockSize( blockSize ),
      _blockCount( blockCount ), _count(0)
{
    _pos = _baseStream->GetPos();
    _size = _baseStream->GetSize();
}

lverror_t LVBlockWriteStream::Seek(lvoffset_t offset, lvseek_origin_t origin, lvpos_t *pNewPos)
{
    if ( origin==LVSEEK_CUR ) {
        origin = LVSEEK_SET;
        offset = _pos + offset;
    } else if ( origin==LVSEEK_END ) {
        origin = LVSEEK_SET;
        offset = _size + offset;
    }
    
    lvpos_t newpos = 0;
    lverror_t res = _baseStream->Seek(offset, origin, &newpos);
    if ( res==LVERR_OK ) {
        if ( pNewPos )
            *pNewPos = newpos;
        _pos = newpos;
    } else {
        CRLog::error("baseStream->Seek(%d,%x) failed: %d", (int)origin, (int)offset, (int)res);
    }
    return res;
}

lverror_t LVBlockWriteStream::Tell(lvpos_t *pPos)
{
    *pPos = _pos;
    return LVERR_OK;
}

lvpos_t LVBlockWriteStream::SetPos(lvpos_t p)
{
    lvpos_t res = _baseStream->SetPos(p);
    _pos = _baseStream->GetPos();
    //                if ( _size<_pos )
    //                    _size = _pos;
    return res;
}

lverror_t LVBlockWriteStream::SetSize(lvsize_t size)
{
    // TODO:
    lverror_t res = _baseStream->SetSize(size);
    if ( res==LVERR_OK )
        _size = size;
    return res;
}

void LVBlockWriteStream::dumpBlocks(const char *context)
{
    lString8 buf;
    for ( Block * p = _firstBlock.get(); p; p = p->next.get() ) {
        char s[1000];
        snprintf(s, 999, "%x ", (int)p->block_start);
        s[999] = 0;
        buf << s;
    }
    CRLog::trace("BLOCKS (%s): %s   count=%d", context, buf.c_str(), _count);
}

lverror_t LVBlockWriteStream::Read(void *buf, lvsize_t count, lvsize_t *nBytesRead)
{
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("stream::Read(%x, %x)", (int)_pos, (int)count);
    dumpBlocks("before read");
#endif
    // slice by block bounds
    lvsize_t bytesRead = 0;
    lverror_t res = LVERR_OK;
    if (nBytesRead)
        *nBytesRead = 0;
    if ((!buf && count > 0) || _blockSize <= 0 || _pos > _size)
        return LVERR_FAIL;
    if (count > _size - _pos)
        count = _size - _pos;
    while (count>0 && res==LVERR_OK) {
        lvpos_t blockSpaceLeft = _blockSize - (_pos % _blockSize);
        if ( blockSpaceLeft > count )
            blockSpaceLeft = count;
        lvsize_t blockBytesRead = 0;
        
        // read from Write buffers if possible, otherwise - from base stream
        if ( readFromCache( buf, _pos, blockSpaceLeft ) ) {
            blockBytesRead = blockSpaceLeft;
            res = LVERR_OK;
        } else {
            lvpos_t fsize = _baseStream->GetSize();
            if ( _pos + blockSpaceLeft > fsize && fsize < _size) {
#if TRACE_BLOCK_WRITE_STREAM
                CRLog::trace("stream::Read: inconsistent cache state detected: fsize=%d, _size=%d, force flush...", (int)fsize, (int)_size);
#endif
                // Workaround to exclude fatal error in ldomTextStorageChunk::ensureUnpacked()
                // Write cached data to a file stream if the required read block is larger than the rest of the file.
                // This is a very rare case.
                Flush(true);
            }
#if TRACE_BLOCK_WRITE_STREAM
            CRLog::trace("direct reading from stream (%x, %x)", (int)_pos, (int)blockSpaceLeft);
#endif
            if (_baseStream->SetPos(_pos) != _pos)
                res = LVERR_FAIL;
            else
                res = _baseStream->Read(
                        buf, blockSpaceLeft, &blockBytesRead);
        }
        if ( res!=LVERR_OK )
            break;
        
        count -= blockBytesRead;
        buf = ((char*)buf) + blockBytesRead;
        _pos += blockBytesRead;
        bytesRead += blockBytesRead;
        if ( !blockBytesRead )
            break;
    }
    if ( nBytesRead )
        *nBytesRead = bytesRead;
    return res;
}

lverror_t LVBlockWriteStream::Write(const void *buf, lvsize_t count, lvsize_t *nBytesWritten)
{
#if TRACE_BLOCK_WRITE_STREAM
    CRLog::trace("stream::Write(%x, %x)", (int)_pos, (int)count);
    dumpBlocks("before write");
#endif
    // slice by block bounds
    lvsize_t bytesWritten = 0;
    lverror_t res = LVERR_OK;
    if (nBytesWritten)
        *nBytesWritten = 0;
    if ((!buf && count > 0)
            || _blockSize <= 0 || _blockCount <= 0
            || count > std::numeric_limits<lvpos_t>::max() - _pos)
        return LVERR_FAIL;
    while ( count>0 && res==LVERR_OK ) {
        lvpos_t blockSpaceLeft = _blockSize - (_pos % _blockSize);
        if ( blockSpaceLeft > count )
            blockSpaceLeft = count;
        lvsize_t blockBytesWritten = 0;
        
        // write to Write buffers
        res = writeToCache(buf, _pos, blockSpaceLeft);
        if ( res!=LVERR_OK )
            break;
        
        blockBytesWritten = blockSpaceLeft;
        
        count -= blockBytesWritten;
        buf = ((char*)buf) + blockBytesWritten;
        _pos += blockBytesWritten;
        bytesWritten += blockBytesWritten;
        if ( _pos>_size )
            _size = _pos;
        if ( !blockBytesWritten )
            break;
    }
    if ( nBytesWritten )
        *nBytesWritten = bytesWritten;
#if TRACE_BLOCK_WRITE_STREAM
    dumpBlocks("after write");
#endif
    return res;
}
