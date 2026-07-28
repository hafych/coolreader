/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2009-2011 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2011 Konstantin Potapov <pkbo@users.sourceforge.net>    *
 *   Copyright (C) 2018 EXL <exlmotodev@gmail.com>                         *
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

#include "lvfilemappedstream.h"
#include "lvstreamutils.h"
#include "crlog.h"

#include <algorithm>
#include <cstring>
#include <limits>
#include <memory>

#if !defined(__SYMBIAN32__) && defined(_WIN32)
extern "C" {
#include <windows.h>
}
#include <io.h>
#else
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <errno.h>
#endif

// To support "large files" on 32-bit platforms
// Since we have defined own types 'lvoffset_t', 'lvpos_t' and do not use the system type 'off_t'
// it is logical to define our own wrapper function 'lseek'.
static inline lvpos_t cr3_lseek(int fd, lvoffset_t offset, int whence) {
#if LVLONG_FILE_SUPPORT == 1 && !defined(__APPLE__) && !defined(__FreeBSD__) && !defined(__OpenBSD__) && !defined(__NetBSD__)
    return (lvpos_t)::lseek64(fd, (off64_t)offset, whence);
#else
    return (lvpos_t)::lseek(fd, (off_t)offset, whence);
#endif
}

namespace {

bool checkedMappedSize(lvsize_t size, std::size_t &result)
{
    if (size > static_cast<lvsize_t>(
            std::numeric_limits<std::size_t>::max()))
        return false;
    result = static_cast<std::size_t>(size);
    return true;
}

bool checkedMappedOffset(
        lvpos_t base, lvoffset_t offset, lvpos_t &result)
{
    if (offset < 0) {
        const lvpos_t magnitude =
                static_cast<lvpos_t>(-(offset + 1)) + 1;
        if (magnitude > base)
            return false;
        result = base - magnitude;
        return true;
    }
    const lvpos_t magnitude = static_cast<lvpos_t>(offset);
    if (magnitude > std::numeric_limits<lvpos_t>::max() - base)
        return false;
    result = base + magnitude;
    return true;
}

} // namespace

LVFileMappedStream::MappedRegion::MappedRegion()
    : _data(NULL), _size(0)
{
}

LVFileMappedStream::MappedRegion::~MappedRegion()
{
    reset();
}

void LVFileMappedStream::MappedRegion::adopt(
        lUInt8 *data, lvsize_t size)
{
    reset();
    _data = data;
    _size = size;
}

bool LVFileMappedStream::MappedRegion::reset()
{
    if (!_data)
        return true;
    lUInt8 *data = _data;
    const lvsize_t size = _size;
    _data = NULL;
    _size = 0;
#if defined(_WIN32)
    return UnmapViewOfFile(data) != 0;
#else
    return munmap(data, size) != -1;
#endif
}

#if defined(_WIN32)
LVFileMappedStream::ScopedHandle::ScopedHandle()
    : _handle(NULL)
{
}

LVFileMappedStream::ScopedHandle::~ScopedHandle()
{
    reset();
}

bool LVFileMappedStream::ScopedHandle::reset(HANDLE handle)
{
    if (_handle == handle)
        return true;
    HANDLE previous = _handle;
    _handle = handle;
    return previous == NULL
            || previous == INVALID_HANDLE_VALUE
            || CloseHandle(previous) != 0;
}
#else
LVFileMappedStream::ScopedDescriptor::ScopedDescriptor()
    : _fd(-1)
{
}

LVFileMappedStream::ScopedDescriptor::~ScopedDescriptor()
{
    reset();
}

bool LVFileMappedStream::ScopedDescriptor::reset(int fd)
{
    if (_fd == fd)
        return true;
    const int previous = _fd;
    _fd = fd;
    return previous < 0 || ::close(previous) == 0;
}
#endif

LVStreamBufferRef LVFileMappedStream::GetReadBuffer(lvpos_t pos, lvpos_t size)
{
    LVStreamBufferRef res;
    if ( m_map.empty() )
        return res;
    if ( (m_mode!=LVOM_APPEND && m_mode!=LVOM_READ)
            || pos > m_size || size > m_size - pos || size==0 )
        return res;
    std::unique_ptr<LVBuffer> buffer = std::make_unique<LVBuffer>(
            LVStreamRef(this), m_map.data() + pos, size, true);
    return LVStreamBufferRef(buffer.release());
}

LVStreamBufferRef LVFileMappedStream::GetWriteBuffer(lvpos_t pos, lvpos_t size)
{
    LVStreamBufferRef res;
    if ( m_map.empty() )
        return res;
    if ( m_mode!=LVOM_APPEND
            || pos > m_size || size > m_size - pos || size==0 )
        return res;
    std::unique_ptr<LVBuffer> buffer = std::make_unique<LVBuffer>(
            LVStreamRef(this), m_map.data() + pos, size, false);
    return LVStreamBufferRef(buffer.release());
}

lverror_t LVFileMappedStream::Seek(lvoffset_t offset, lvseek_origin_t origin, lvpos_t* pNewPos)
{
    lvpos_t base = 0;
    switch ( origin )
    {
        case LVSEEK_SET:
            base = 0;
            break;
        case LVSEEK_CUR:
            base = m_pos;
            break;
        case LVSEEK_END:
            base = m_size;
            break;
        default:
            return LVERR_FAIL;
    }
    lvpos_t newpos = 0;
    if (!checkedMappedOffset(base, offset, newpos))
        return LVERR_FAIL;
    if ( newpos>m_size )
        return LVERR_FAIL;
    if ( pNewPos!=NULL )
        *pNewPos = newpos;
    m_pos = newpos;
    return LVERR_OK;
}

lverror_t LVFileMappedStream::Tell(lvpos_t* pPos)
{
    if (!pPos)
        return LVERR_FAIL;
    *pPos = m_pos;
    return LVERR_OK;
}

lvpos_t LVFileMappedStream::SetPos(lvpos_t p)
{
    if ( p<=m_size ) {
        m_pos = p;
        return m_pos;
    }
    return (lvpos_t)(~0);
}

lverror_t LVFileMappedStream::error()
{
#if defined(_WIN32)
    UnMap();
    if (m_hFile.valid()) {
        if (!m_hFile.reset())
            CRLog::error("Error while closing file handle");
    }
#else
    UnMap();
    if (m_fd.valid()) {
        CRLog::trace("Closing mapped file %s", UnicodeToUtf8(GetName()).c_str() );
        if (!m_fd.reset())
            CRLog::error("Error while closing mapped file descriptor");
    }
#endif
    m_size = 0;
    m_pos = 0;
    m_mode = LVOM_ERROR;
    return LVERR_FAIL;
}

lverror_t LVFileMappedStream::Read(void* buf, lvsize_t count, lvsize_t* nBytesRead)
{
    if (nBytesRead)
        *nBytesRead = 0;
    if (m_map.empty() || !buf || m_pos > m_size)
        return LVERR_FAIL;
    const lvsize_t amount = std::min(count, m_size - m_pos);
    if (amount == 0)
        return LVERR_FAIL;
    std::size_t offset = 0;
    std::size_t bytes = 0;
    if (!checkedMappedSize(m_pos, offset)
            || !checkedMappedSize(amount, bytes))
        return LVERR_FAIL;
    std::memcpy(buf, m_map.data() + offset, bytes);
    m_pos += amount;
    if (nBytesRead)
        *nBytesRead = amount;
    return LVERR_OK;
}

bool LVFileMappedStream::Read(lUInt8* buf)
{
    if (buf && !m_map.empty() && m_pos < m_size) {
        *buf = m_map.data()[m_pos++];
        return true;
    }
    return false;
}

bool LVFileMappedStream::Read(lUInt16* buf)
{
    if (buf && !m_map.empty()
            && m_pos <= m_size && m_size - m_pos >= 2) {
        *buf = m_map.data()[m_pos]
                | (((lUInt16)m_map.data()[m_pos+1])<<8);
        m_pos += 2;
        return true;
    }
    return false;
}

bool LVFileMappedStream::Read(lUInt32* buf)
{
    if (buf && !m_map.empty()
            && m_pos <= m_size && m_size - m_pos >= 4) {
        *buf = m_map.data()[m_pos]
                | (((lUInt32)m_map.data()[m_pos+1])<<8)
                | (((lUInt32)m_map.data()[m_pos+2])<<16)
                | (((lUInt32)m_map.data()[m_pos+3])<<24)
                ;
        m_pos += 4;
        return true;
    }
    return false;
}

int LVFileMappedStream::ReadByte()
{
    if (!m_map.empty() && m_pos < m_size) {
        return m_map.data()[m_pos++];
    }
    return -1;
}

lverror_t LVFileMappedStream::Write(const void* buf, lvsize_t count, lvsize_t* nBytesWritten)
{
    if (nBytesWritten)
        *nBytesWritten = 0;
    if (m_mode!=LVOM_APPEND || m_map.empty()
            || !buf || m_pos > m_size)
        return LVERR_FAIL;
    const lvsize_t maxSize = m_size - m_pos;
    if ( maxSize==0 )
        return LVERR_FAIL; // end of file reached: resize is not supported yet
    const lvsize_t amount = std::min(count, maxSize);
    std::size_t offset = 0;
    std::size_t bytes = 0;
    if (amount == 0
            || !checkedMappedSize(m_pos, offset)
            || !checkedMappedSize(amount, bytes))
        return LVERR_FAIL;
    std::memcpy(m_map.data() + offset, buf, bytes);
    m_pos += amount;
    if ( nBytesWritten )
        *nBytesWritten = amount;
    return LVERR_OK;
}

std::unique_ptr<LVFileMappedStream>
LVFileMappedStream::CreateFileStream(
        lString32 fname, lvopen_mode_t mode, lvsize_t minSize)
{
    std::unique_ptr<LVFileMappedStream> stream =
            std::make_unique<LVFileMappedStream>();
    if (stream->OpenFile(fname, mode, minSize) != LVERR_OK)
        return std::unique_ptr<LVFileMappedStream>();
    return stream;
}

lverror_t LVFileMappedStream::Map()
{
    std::size_t mappedSize = 0;
    if (m_size == 0 || !checkedMappedSize(m_size, mappedSize))
        return error();
#if defined(_WIN32)
    m_hMap.reset(CreateFileMapping(
                 m_hFile.get(),
                 NULL,
                 (m_mode==LVOM_READ)?PAGE_READONLY:PAGE_READWRITE, //flProtect,
                 0,
                 0,
                 NULL
                 ));
    if (!m_hMap.valid()) {
        DWORD err = GetLastError();
        CRLog::error( "LVFileMappedStream::Map() -- Cannot map file to memory, err=%08x, hFile=%p", err, m_hFile.get() );
        return error();
    }
    lUInt8 *mapped = static_cast<lUInt8 *>(MapViewOfFile(
                m_hMap.get(),
                m_mode==LVOM_READ ? FILE_MAP_READ : FILE_MAP_READ|FILE_MAP_WRITE,
                0,
                0,
                mappedSize
                ));
    if (!mapped) {
        CRLog::error( "LVFileMappedStream::Map() -- Cannot map file to memory" );
        return error();
    }
    m_map.adopt(mapped, m_size);
    return LVERR_OK;
#else
    int mapFlags = (m_mode==LVOM_READ) ? PROT_READ : PROT_READ | PROT_WRITE;
    void *mapped = mmap(
            0, mappedSize, mapFlags, MAP_SHARED, m_fd.get(), 0);
    if (mapped == MAP_FAILED) {
        CRLog::error( "LVFileMappedStream::Map() -- Cannot map file to memory" );
        return error();
    }
    m_map.adopt(static_cast<lUInt8 *>(mapped), m_size);
    return LVERR_OK;
#endif
}

lverror_t LVFileMappedStream::UnMap()
{
#if defined(_WIN32)
    const bool viewReleased = m_map.reset();
    const bool handleReleased = m_hMap.reset();
    if (!viewReleased || !handleReleased) {
        CRLog::error(
                "LVFileMappedStream::UnMap() -- Error while unmapping file");
        return LVERR_FAIL;
    }
    return LVERR_OK;
#else
    if (!m_map.reset()) {
        CRLog::error("LVFileMappedStream::UnMap() -- Error while unmapping file");
        return LVERR_FAIL;
    }
    return LVERR_OK;
#endif
}

lverror_t LVFileMappedStream::SetSize(lvsize_t size)
{
    // support only size grow
    if ( m_mode!=LVOM_APPEND )
        return LVERR_FAIL;
    if ( size == m_size )
        return LVERR_OK;
    if (size == 0 || size < m_size)
        return LVERR_FAIL;

    const bool wasMapped = !m_map.empty();
    if (wasMapped) {
        if ( UnMap()!=LVERR_OK )
            return LVERR_FAIL;
    }
    m_size = size;

#if defined(_WIN32)
    // WIN32
    __int64 offset = size - 1;
    lUInt32 pos_low = (lUInt32)((__int64)offset & 0xFFFFFFFF);
    LONG pos_high = (long)(((__int64)offset >> 32) & 0xFFFFFFFF);
    pos_low = SetFilePointer(
            m_hFile.get(), pos_low, &pos_high, FILE_BEGIN );
    if (pos_low == 0xFFFFFFFF) {
        lUInt32 err = GetLastError();
        if (err == ERROR_NOACCESS)
            pos_low = (lUInt32)offset;
        else if ( err != ERROR_SUCCESS)
            return error();
    }
    DWORD bytesWritten = 0;
    if ( !WriteFile(
            m_hFile.get(), "", 1, &bytesWritten, NULL )
            || bytesWritten!=1 )
        return error();
#else
    // LINUX
    if ( cr3_lseek( m_fd.get(), size-1, SEEK_SET ) == (lvpos_t)-1 ) {
        CRLog::error("LVFileMappedStream::SetSize() -- Seek error");
        return error();
    }
    if ( write(m_fd.get(), "", 1) != 1 ) {
        CRLog::error("LVFileMappedStream::SetSize() -- File resize error");
        return error();
    }
#endif
    if ( wasMapped ) {
        if ( Map() != LVERR_OK ) {
            return error();
        }
    }
    return LVERR_OK;
}

lverror_t LVFileMappedStream::OpenFile(lString32 fname, lvopen_mode_t mode, lvsize_t minSize)
{
    error();
    if ( mode!=LVOM_READ && mode!=LVOM_APPEND )
        return LVERR_FAIL; // not supported
    if ( minSize==(lvsize_t)-1 ) {
        if ( !LVFileExists(fname) )
            return LVERR_FAIL;
        minSize = 0;
    }
    m_mode = mode;
    //if ( mode==LVOM_APPEND && minSize<=0 )
    //    return LVERR_FAIL;
    SetName(fname.c_str());
    lString8 fn8 = UnicodeToUtf8( fname );
#if defined(_WIN32)
    //========================================================
    // WIN32 IMPLEMENTATION
    lUInt32 m = 0;
    lUInt32 s = 0;
    lUInt32 c = 0;
    lString16 fn16 = UnicodeToUtf16( fname );
    switch (mode) {
        case LVOM_READWRITE:
            m |= GENERIC_WRITE|GENERIC_READ;
            s |= FILE_SHARE_WRITE|FILE_SHARE_READ;
            c |= OPEN_ALWAYS;
            break;
        case LVOM_READ:
            m |= GENERIC_READ;
            s |= FILE_SHARE_READ;
            c |= OPEN_EXISTING;
            break;
        case LVOM_WRITE:
            m |= GENERIC_WRITE;
            s |= FILE_SHARE_WRITE;
            c |= CREATE_ALWAYS;
            break;
        case LVOM_APPEND:
            m |= GENERIC_WRITE|GENERIC_READ;
            s |= FILE_SHARE_WRITE;
            c |= OPEN_ALWAYS;
            break;
        case LVOM_CLOSED:
        case LVOM_ERROR:
            crFatalError();
            break;
    }
    HANDLE fileHandle = CreateFileW(
            fn16.c_str(), m, s, NULL, c, FILE_ATTRIBUTE_NORMAL, NULL);
    if (fileHandle == INVALID_HANDLE_VALUE || !fileHandle) {
        // unicode not implemented?
        lUInt32 err = GetLastError();
        if (err==ERROR_CALL_NOT_IMPLEMENTED)
            fileHandle = CreateFileA(
                    UnicodeToLocal(fname).c_str(), m, s,
                    NULL, c, FILE_ATTRIBUTE_NORMAL, NULL);
        if (fileHandle == INVALID_HANDLE_VALUE || !fileHandle) {
            CRLog::error("Error opening file %s", fn8.c_str() );
            // error
            return error();
        }
    }
    m_hFile.reset(fileHandle);
    // check size
    lUInt32 hw=0;
    m_size = GetFileSize( m_hFile.get(), (LPDWORD)&hw );
#if LVLONG_FILE_SUPPORT
    if (hw)
        m_size |= (((lvsize_t)hw)<<32);
#endif

    if ( mode == LVOM_APPEND && m_size < minSize ) {
        if ( SetSize( minSize ) != LVERR_OK ) {
            CRLog::error( "Cannot set file size for %s", fn8.c_str() );
            return error();
        }
    }

    if ( Map()!=LVERR_OK )
        return error();

    return LVERR_OK;


#else
    //========================================================
    // LINUX IMPLEMENTATION
    int flags = (mode==LVOM_READ) ? O_RDONLY : O_RDWR | O_CREAT; // | O_SYNC
    const int fd = open( fn8.c_str(), flags, (mode_t)0666);
    if (fd == -1) {
        CRLog::error( "Error opening file %s for %s, errno=%d, msg=%s", fn8.c_str(), (mode==LVOM_READ) ? "reading" : "read/write",  (int)errno, strerror(errno) );
        return error();
    }
    m_fd.reset(fd);
    struct stat stat;
    if ( fstat( m_fd.get(), &stat ) ) {
        CRLog::error( "Cannot get file size for %s", fn8.c_str() );
        return error();
    }
    m_size = (lvsize_t) stat.st_size;
    if ( mode == LVOM_APPEND && m_size < minSize ) {
        if ( SetSize( minSize ) != LVERR_OK ) {
            CRLog::error( "Cannot set file size for %s", fn8.c_str() );
            return error();
        }
    }

    if (Map() != LVERR_OK) {
        CRLog::error("Cannot map file %s to memory", fn8.c_str());
        return error();
    }
    return LVERR_OK;
#endif
}

LVFileMappedStream::LVFileMappedStream()
    : m_size(0), m_pos(0)
{
    m_mode=LVOM_ERROR;
}
