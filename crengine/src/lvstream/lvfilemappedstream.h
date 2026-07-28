/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2009,2010 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#ifndef __LVFILEMAPPEDSTREAM_H_INCLUDED__
#define __LVFILEMAPPEDSTREAM_H_INCLUDED__

#include "lvnamedstream.h"
#include "lvstreambuffer.h"

#include <memory>

#if defined(_WIN32) && !defined(__SYMBIAN32__)
extern "C" {
#include <windows.h>
}
#endif

//#if USE__FILES==1
#if defined(_LINUX) || defined(_WIN32)

class LVFileMappedStream : public LVNamedStream
{
private:
    class MappedRegion
    {
        lUInt8 *_data;
        lvsize_t _size;
    public:
        MappedRegion();
        ~MappedRegion();
        MappedRegion(const MappedRegion &) = delete;
        MappedRegion &operator=(const MappedRegion &) = delete;

        lUInt8 *data() const { return _data; }
        bool empty() const { return _data == NULL; }
        void adopt(lUInt8 *data, lvsize_t size);
        bool reset();
    };

#if defined(_WIN32)
    class ScopedHandle
    {
        HANDLE _handle;
    public:
        ScopedHandle();
        ~ScopedHandle();
        ScopedHandle(const ScopedHandle &) = delete;
        ScopedHandle &operator=(const ScopedHandle &) = delete;

        HANDLE get() const { return _handle; }
        bool valid() const {
            return _handle != NULL && _handle != INVALID_HANDLE_VALUE;
        }
        bool reset(HANDLE handle = NULL);
    };

    ScopedHandle m_hFile;
    ScopedHandle m_hMap;
#else
    class ScopedDescriptor
    {
        int _fd;
    public:
        ScopedDescriptor();
        ~ScopedDescriptor();
        ScopedDescriptor(const ScopedDescriptor &) = delete;
        ScopedDescriptor &operator=(const ScopedDescriptor &) = delete;

        int get() const { return _fd; }
        bool valid() const { return _fd >= 0; }
        bool reset(int fd = -1);
    };

    ScopedDescriptor m_fd;
#endif
    MappedRegion m_map;
    lvsize_t m_size;
    lvpos_t m_pos;

    /// Read or write buffer for stream region
    class LVBuffer : public LVStreamBuffer
    {
    protected:
        LVStreamRef m_stream;
        lUInt8 * m_buf;
        lvsize_t m_size;
        bool m_readonly;
    public:
        LVBuffer( LVStreamRef stream, lUInt8 * buf, lvsize_t size, bool readonly )
        : m_stream( stream ), m_buf( buf ), m_size( size ), m_readonly( readonly )
        {
        }
        LVBuffer(const LVBuffer &) = delete;
        LVBuffer &operator=(const LVBuffer &) = delete;
        /// get pointer to read-only buffer, returns NULL if unavailable
        const lUInt8 * getReadOnly() override
        {
            return m_buf;
        }
        /// get pointer to read-write buffer, returns NULL if unavailable
        lUInt8 * getReadWrite() override
        {
            return m_readonly ? NULL : m_buf;
        }
        /// get buffer size
        lvsize_t getSize() override
        {
            return m_size;
        }
        /// flush on destroy
        ~LVBuffer() override = default;
    };
public:
    /// Get read buffer (optimal for )
    LVStreamBufferRef GetReadBuffer(
            lvpos_t pos, lvpos_t size ) override;
    /// Get read/write buffer (optimal for )
    LVStreamBufferRef GetWriteBuffer(
            lvpos_t pos, lvpos_t size ) override;
    lverror_t Seek(
            lvoffset_t offset, lvseek_origin_t origin,
            lvpos_t * pNewPos ) override;
    /// Tell current file position
    /**
        \param pNewPos points to place to store file position
        \return lverror_t status: LVERR_OK if success
    */
    lverror_t Tell( lvpos_t * pPos ) override;
    lvpos_t SetPos(lvpos_t p) override;
    /// Get file position
    /**
        \return lvpos_t file position
    */
    lvpos_t GetPos() override
    {
        return m_pos;
    }
    /// Get file size
    /**
        \return lvsize_t file size
    */
    lvsize_t GetSize() override
    {
        return m_size;
    }
    lverror_t GetSize( lvsize_t * pSize ) override
    {
        if (!pSize)
            return LVERR_FAIL;
        *pSize = m_size;
        return LVERR_OK;
    }
    lverror_t error();
    lverror_t Read(
            void * buf, lvsize_t count,
            lvsize_t * nBytesRead ) override;
    bool Read( lUInt8 * buf ) override;
    bool Read( lUInt16 * buf ) override;
    bool Read( lUInt32 * buf ) override;
    int ReadByte() override;
    lverror_t Write(
            const void * buf, lvsize_t count,
            lvsize_t * nBytesWritten ) override;
    bool Eof() override
    {
        return (m_pos >= m_size);
    }
    static std::unique_ptr<LVFileMappedStream> CreateFileStream(
            lString32 fname, lvopen_mode_t mode, lvsize_t minSize );
    lverror_t Map();
    lverror_t UnMap();
    lverror_t SetSize( lvsize_t size ) override;
    lverror_t OpenFile( lString32 fname, lvopen_mode_t mode, lvsize_t minSize = (lvsize_t)-1 );
    LVFileMappedStream();
    LVFileMappedStream(const LVFileMappedStream &) = delete;
    LVFileMappedStream &operator=(const LVFileMappedStream &) = delete;
    ~LVFileMappedStream() override = default;
};
#endif  // #if defined(_LINUX) || defined(_WIN32)

#endif  // __LVFILEMAPPEDSTREAM_H_INCLUDED__
