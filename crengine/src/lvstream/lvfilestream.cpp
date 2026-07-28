/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2008,2010-2013,2015 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2011 Konstantin Potapov <pkbo@users.sourceforge.net>    *
 *   Copyright (C) 2020 poire-z <poire-z@users.noreply.github.com>         *
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

#include "lvfilestream.h"
#include "crlog.h"

#include <utility>

#if (USE_ANSI_FILES==1)

void LVFileStream::FileCloser::operator()(FILE *file) const
{
    if (file)
        fclose(file);
}

lverror_t LVFileStream::Seek(lvoffset_t offset, lvseek_origin_t origin, lvpos_t *pNewPos)
{
    if (!m_file)
        return LVERR_FAIL;
    //
    int res = -1;
    switch ( origin )
    {
        case LVSEEK_SET:
            res = fseek( m_file.get(), offset, SEEK_SET );
            break;
        case LVSEEK_CUR:
            res = fseek( m_file.get(), offset, SEEK_CUR );
            break;
        case LVSEEK_END:
            res = fseek( m_file.get(), offset, SEEK_END );
            break;
    }
    if (res==0)
    {
        if ( pNewPos )
            * pNewPos = ftell(m_file.get());
        return LVERR_OK;
    }
    CRLog::error("error setting file position to %d (%d)", (int)offset, (int)origin );
    return LVERR_FAIL;
}

lverror_t LVFileStream::SetSize(lvsize_t)
{
    /*
        int64 sz = m_file->SetSize( size );
        if (sz==-1)
           return LVERR_FAIL;
        else
           return LVERR_OK;
        */
    return LVERR_FAIL;
}

lverror_t LVFileStream::Read(void *buf, lvsize_t count, lvsize_t *nBytesRead)
{
    if (nBytesRead)
        *nBytesRead = 0;
    if (!m_file)
        return LVERR_FAIL;
    lvsize_t sz = fread( buf, 1, count, m_file.get() );
    if (nBytesRead)
        *nBytesRead = sz;
    if ( sz==0 )
    {
        return LVERR_FAIL;
    }
    return LVERR_OK;
}

lverror_t LVFileStream::Write(const void *buf, lvsize_t count, lvsize_t *nBytesWritten)
{
    if (nBytesWritten)
        *nBytesWritten = 0;
    if (!m_file)
        return LVERR_FAIL;
    lvsize_t sz = fwrite( buf, 1, count, m_file.get() );
    if (nBytesWritten)
        *nBytesWritten = sz;
    handleAutoSync(sz);
    if (sz < count)
    {
        return LVERR_FAIL;
    }
    return LVERR_OK;
}

lverror_t LVFileStream::Flush(bool sync)
{
    CR_UNUSED(sync);
    if ( !m_file )
        return LVERR_FAIL;
    return fflush( m_file.get() ) == 0 ? LVERR_OK : LVERR_FAIL;
}

bool LVFileStream::Eof()
{
    return m_file && feof(m_file.get()) != 0;
}

std::unique_ptr<LVFileStream> LVFileStream::CreateFileStream(
        lString32 fname, lvopen_mode_t mode)
{
    std::unique_ptr<LVFileStream> f(new LVFileStream());
    if (f->OpenFile( fname, mode )==LVERR_OK)
        return f;
    return std::unique_ptr<LVFileStream>();
}

lverror_t LVFileStream::OpenFile(lString32 fname, lvopen_mode_t mode)
{
    m_file.reset();
    m_mode = LVOM_ERROR;
    SetName(NULL);
    const lvopen_mode_t openMode =
            static_cast<lvopen_mode_t>(mode & LVOM_MASK);
    const char * modestr = "r";
    switch (openMode) {
        case LVOM_READ:
            modestr = "rb";
            break;
        case LVOM_WRITE:
            modestr = "wb";
            break;
        case LVOM_READWRITE:
        case LVOM_APPEND:
            modestr = "a+b";
            break;
        case LVOM_CLOSED:
        case LVOM_ERROR:
            return LVERR_FAIL;
    }
    std::unique_ptr<FILE, FileCloser> file(
            fopen(UnicodeToLocal(fname).c_str(), modestr));
    if (!file)
    {
        return LVERR_FAIL;
    }
    m_file = std::move(file);
    m_mode = openMode;
    SetName( fname.c_str() );
    return LVERR_OK;
}

LVFileStream::LVFileStream() : m_file(nullptr)
{
    m_mode=LVOM_ERROR;
}

#else

#if !defined(__SYMBIAN32__) && defined(_WIN32)
extern "C" {
#include <windows.h>
}
#include "io.h"
#else
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>
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

#if defined(_WIN32)
LVFileStream::ScopedHandle::ScopedHandle()
    : _handle(NULL)
{
}

LVFileStream::ScopedHandle::~ScopedHandle()
{
    reset();
}

HANDLE LVFileStream::ScopedHandle::release()
{
    HANDLE handle = _handle;
    _handle = NULL;
    return handle;
}

bool LVFileStream::ScopedHandle::reset(HANDLE handle)
{
    if (_handle == handle)
        return true;
    bool result = true;
    if (valid())
        result = CloseHandle(_handle) != 0;
    _handle = handle;
    return result;
}
#else
LVFileStream::ScopedDescriptor::ScopedDescriptor()
    : _fd(-1)
{
}

LVFileStream::ScopedDescriptor::ScopedDescriptor(int fd)
    : _fd(fd)
{
}

LVFileStream::ScopedDescriptor::~ScopedDescriptor()
{
    reset();
}

int LVFileStream::ScopedDescriptor::release()
{
    int fd = _fd;
    _fd = -1;
    return fd;
}

bool LVFileStream::ScopedDescriptor::reset(int fd)
{
    if (_fd == fd)
        return true;
    bool result = true;
    if (valid())
        result = close(_fd) == 0;
    _fd = fd;
    return result;
}
#endif

lverror_t LVFileStream::Flush(bool sync)
{
    CR_UNUSED(sync);
#ifdef _WIN32
    if ( !m_hFile.valid() || !FlushFileBuffers( m_hFile.get() ) )
        return LVERR_FAIL;
#else
    const int fd = descriptor();
    if ( fd == -1 )
        return LVERR_FAIL;
    if ( sync ) {
        //            CRTimerUtil timer;
        //            CRLog::trace("calling fsync");
        if (fsync(fd) != 0)
            return LVERR_FAIL;
        //            CRLog::trace("fsync took %d ms", (int)timer.elapsed());
    }
#endif
    return LVERR_OK;
}

bool LVFileStream::Eof()
{
    return m_size<=m_pos;
}

lverror_t LVFileStream::Read(void *buf, lvsize_t count, lvsize_t *nBytesRead)
{
    if (nBytesRead)
        *nBytesRead = 0;
#ifdef _WIN32
    //fprintf(stderr, "Read(%08x, %d)\n", buf, count);
    
    if (!m_hFile.valid() || m_mode==LVOM_WRITE ) // || m_mode==LVOM_APPEND
        return LVERR_FAIL;
    //
    if ( m_pos > m_size )
        return LVERR_FAIL; // EOF
    
    lUInt32 dwBytesRead = 0;
    if (ReadFile( m_hFile.get(), buf, (lUInt32)count,
            (LPDWORD)&dwBytesRead, NULL )) {
        if (nBytesRead)
            *nBytesRead = dwBytesRead;
        m_pos += dwBytesRead;
        return LVERR_OK;
    } else {
        //DWORD err = GetLastError();
        return LVERR_FAIL;
    }
    
#else
    const int fd = descriptor();
    if (fd == -1 || m_mode == LVOM_WRITE)
        return LVERR_FAIL;
    ssize_t res = read( fd, buf, count );
    if ( res!=(ssize_t)-1 ) {
        if (nBytesRead)
            *nBytesRead = res;
        m_pos += res;
        return LVERR_OK;
    }
    return LVERR_FAIL;
#endif
}

lverror_t LVFileStream::GetSize(lvsize_t *pSize)
{
#ifdef _WIN32
    if (!m_hFile.valid() || !pSize)
        return LVERR_FAIL;
#else
    if (descriptor() == -1 || !pSize)
        return LVERR_FAIL;
#endif
    if (m_size<m_pos)
        m_size = m_pos;
    *pSize = m_size;
    return LVERR_OK;
}

lvsize_t LVFileStream::GetSize()
{
#ifdef _WIN32
    if (!m_hFile.valid())
        return 0;
    if (m_size<m_pos)
        m_size = m_pos;
    return m_size;
#else
    if (descriptor() == -1)
        return 0;
    if (m_size<m_pos)
        m_size = m_pos;
    return m_size;
#endif
}

lverror_t LVFileStream::SetSize(lvsize_t size)
{
#ifdef _WIN32
    //
    if (!m_hFile.valid() || m_mode==LVOM_READ )
        return LVERR_FAIL;
    lvpos_t oldpos = 0;
    if (Tell(&oldpos) != LVERR_OK)
        return LVERR_FAIL;
    if (Seek(size, LVSEEK_SET, NULL) != LVERR_OK)
        return LVERR_FAIL;
    if (!SetEndOfFile(m_hFile.get())) {
        Seek(oldpos, LVSEEK_SET, NULL);
        return LVERR_FAIL;
    }
    m_size = size;
    const lvpos_t restoredPos = oldpos > size ? size : oldpos;
    if (Seek(restoredPos, LVSEEK_SET, NULL) != LVERR_OK)
        return LVERR_FAIL;
    return LVERR_OK;
#else
    const int fd = descriptor();
    if (fd == -1 || m_mode == LVOM_READ)
        return LVERR_FAIL;
    lvpos_t oldpos = 0;
    if (Tell(&oldpos) != LVERR_OK)
        return LVERR_FAIL;
    const off_t nativeSize = static_cast<off_t>(size);
    if (nativeSize < 0 || static_cast<lvsize_t>(nativeSize) != size)
        return LVERR_FAIL;
    if (ftruncate(fd, nativeSize) != 0)
        return LVERR_FAIL;
    m_size = size;
    const lvpos_t restoredPos = oldpos > size ? size : oldpos;
    if (Seek(restoredPos, LVSEEK_SET, NULL) != LVERR_OK)
        return LVERR_FAIL;
    return LVERR_OK;
#endif
}

lverror_t LVFileStream::Write(const void *buf, lvsize_t count, lvsize_t *nBytesWritten)
{
    if (nBytesWritten)
        *nBytesWritten = 0;
#ifdef _WIN32
    if (!m_hFile.valid() || m_mode==LVOM_READ )
        return LVERR_FAIL;
    //
    lUInt32 dwBytesWritten = 0;
    if (WriteFile( m_hFile.get(), buf, (lUInt32)count,
            (LPDWORD)&dwBytesWritten, NULL )) {
        if (nBytesWritten)
            *nBytesWritten = dwBytesWritten;
        m_pos += dwBytesWritten;
        if ( m_size < m_pos )
            m_size = m_pos;
        handleAutoSync(dwBytesWritten);
        return LVERR_OK;
    }
    return LVERR_FAIL;
    
#else
    const int fd = descriptor();
    if (fd == -1 || m_mode == LVOM_READ)
        return LVERR_FAIL;
    ssize_t res = write( fd, buf, count );
    if ( res!=(ssize_t)-1 ) {
        if (nBytesWritten)
            *nBytesWritten = res;
        m_pos += res;
        if ( m_size < m_pos )
            m_size = m_pos;
        handleAutoSync(res);
        return LVERR_OK;
    }
    return LVERR_FAIL;
#endif
}

lverror_t LVFileStream::Seek(lvoffset_t offset, lvseek_origin_t origin, lvpos_t *pNewPos)
{
#ifdef _WIN32
    //fprintf(stderr, "Seek(%d,%d)\n", offset, origin);
    if (!m_hFile.valid())
        return LVERR_FAIL;
    lUInt32 pos_low = (lUInt32)((__int64)offset & 0xFFFFFFFF);
    LONG pos_high = (LONG)(((__int64)offset >> 32) & 0xFFFFFFFF);
    lUInt32 m=0;
    switch (origin) {
        case LVSEEK_SET:
            m = FILE_BEGIN;
            break;
        case LVSEEK_CUR:
            m = FILE_CURRENT;
            break;
        case LVSEEK_END:
            m = FILE_END;
            break;
        default:
            return LVERR_FAIL;
    }
    
    SetLastError(NO_ERROR);
    pos_low = SetFilePointer(m_hFile.get(), pos_low, &pos_high, m );
    lUInt32 err;
    if (pos_low == INVALID_SET_FILE_POINTER && (err = GetLastError())!=ERROR_SUCCESS ) {
        //if (err == ERROR_NOACCESS)
        //    pos_low = (lUInt32)offset;
        //else if ( err != ERROR_SUCCESS)
        return LVERR_FAIL;
    }
    m_pos = pos_low
        #if LVLONG_FILE_SUPPORT
            | ((lvpos_t)pos_high<<32)
        #endif
            ;
    if (pNewPos)
        *pNewPos = m_pos;
    return LVERR_OK;
#else
    const int fd = descriptor();
    if (fd == -1)
        return LVERR_FAIL;
    //
    lvpos_t res = (lvpos_t)-1;
    switch ( origin )
    {
        case LVSEEK_SET:
            res = cr3_lseek( fd, offset, SEEK_SET );
            break;
        case LVSEEK_CUR:
            res = cr3_lseek( fd, offset, SEEK_CUR );
            break;
        case LVSEEK_END:
            res = cr3_lseek( fd, offset, SEEK_END );
            break;
        default:
            return LVERR_FAIL;
    }
    if (res!=(lvpos_t)-1)
    {
        m_pos = res;
        if ( pNewPos )
            * pNewPos = res;
        return LVERR_OK;
    }
    CRLog::error("error setting file position to %d (%d)", (int)offset, (int)origin );
    return LVERR_FAIL;
#endif
}

lverror_t LVFileStream::Close()
{
    bool closed = true;
#if defined(_WIN32)
    closed = m_hFile.reset();
#else
    closed = m_ownedFd.reset();
    m_borrowedFd = -1;
#endif
    SetName(NULL);
    m_mode = LVOM_CLOSED;
    m_size = 0;
    m_pos = 0;
    return closed ? LVERR_OK : LVERR_FAIL;
}

std::unique_ptr<LVFileStream> LVFileStream::CreateFileStream(
        lString32 fname, lvopen_mode_t mode)
{
    std::unique_ptr<LVFileStream> f(new LVFileStream());
    if (f->OpenFile( fname, mode )==LVERR_OK)
        return f;
    return std::unique_ptr<LVFileStream>();
}

std::unique_ptr<LVFileStream> LVFileStream::CreateFileStream(
        int fd, lvopen_mode_t mode, bool autoClose)
{
    std::unique_ptr<LVFileStream> f(new LVFileStream());
    if (f->OpenFile( fd, (int)mode, autoClose )==LVERR_OK)
        return f;
    return std::unique_ptr<LVFileStream>();
}

lverror_t LVFileStream::OpenFile(int fd, int mode, bool autoClose)
{
    Close();
    m_mode = LVOM_ERROR;
    const lvopen_mode_t openMode =
            static_cast<lvopen_mode_t>(mode & LVOM_MASK);
    if (fd < 0
            || (openMode != LVOM_READ
                    && openMode != LVOM_WRITE
                    && openMode != LVOM_READWRITE
                    && openMode != LVOM_APPEND))
        return LVERR_FAIL;
#if defined(_WIN32)
    CR_UNUSED(autoClose);
    return LVERR_FAIL;
#else
    ScopedDescriptor candidate;
    int candidateFd = fd;
    if (autoClose) {
        candidateFd = dup(fd);
        if (candidateFd < 0) {
            CRLog::error("LVFileStream::OpenFile(fd=%d): dup failed, errno=%d", fd, errno);
            return LVERR_FAIL;
        }
        candidate.reset(candidateFd);
    }
    struct stat st;
    if (fstat(candidateFd, &st)) {
        CRLog::error("LVFileStream::OpenFile(fd=%d): fstat failed", fd);
        return LVERR_FAIL;
    }
    const lvpos_t initialPos = cr3_lseek(
            candidateFd, 0,
            openMode == LVOM_APPEND ? SEEK_END : SEEK_SET);
    if (initialPos == (lvpos_t)-1) {
        CRLog::error("LVFileStream::OpenFile(fd=%d): descriptor is not seekable", fd);
        return LVERR_FAIL;
    }
    if (autoClose)
        m_ownedFd.reset(candidate.release());
    else
        m_borrowedFd = candidateFd;
    m_mode = openMode;
    m_size = static_cast<lvsize_t>(st.st_size);
    m_pos = initialPos;
    char fname[64];
    snprintf(fname, sizeof(fname), "/proc/self/fd/%d", fd);
    SetName(lString32(fname).c_str());
    return LVERR_OK;
#endif
}

lverror_t LVFileStream::OpenFile(lString32 fname, int mode)
{
    Close();
    m_mode = LVOM_ERROR;
    const bool useSync = (mode & LVOM_FLAG_SYNC) != 0;
    const lvopen_mode_t openMode =
            static_cast<lvopen_mode_t>(mode & LVOM_MASK);
    if (openMode != LVOM_READ
            && openMode != LVOM_WRITE
            && openMode != LVOM_READWRITE
            && openMode != LVOM_APPEND)
        return LVERR_FAIL;
#if defined(_WIN32)
    CR_UNUSED(useSync);
    lUInt32 m = 0;
    lUInt32 s = 0;
    lUInt32 c = 0;
    switch (openMode) {
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
            s |= FILE_SHARE_WRITE|FILE_SHARE_READ;
            c |= OPEN_ALWAYS;
            break;
        case LVOM_CLOSED:
        case LVOM_ERROR:
            break;
    }
    lString16 fn16 = UnicodeToUtf16(fname);
    ScopedHandle candidate;
    HANDLE fileHandle =
            CreateFileW(fn16.c_str(), m, s, NULL, c,
                    FILE_ATTRIBUTE_NORMAL, NULL);
    if (fileHandle == INVALID_HANDLE_VALUE || !fileHandle) {
        // unicode not implemented?
        lUInt32 err = GetLastError();
        if (err==ERROR_CALL_NOT_IMPLEMENTED)
            fileHandle = CreateFileA(
                    UnicodeToLocal(fname).c_str(), m, s, NULL, c,
                    FILE_ATTRIBUTE_NORMAL, NULL);
        if (fileHandle == INVALID_HANDLE_VALUE || !fileHandle)
            return LVERR_FAIL;
    }
    candidate.reset(fileHandle);
    
    lUInt32 hw=0;
    SetLastError(NO_ERROR);
    const lUInt32 lowSize = GetFileSize(candidate.get(), (LPDWORD)&hw);
    if (lowSize == INVALID_FILE_SIZE && GetLastError() != NO_ERROR)
        return LVERR_FAIL;
    lvsize_t fileSize = lowSize;
#if LVLONG_FILE_SUPPORT
    if (hw)
        fileSize |= (((lvsize_t)hw)<<32);
#endif
    lvpos_t initialPos = 0;
    if (openMode == LVOM_APPEND) {
        LONG highPos = 0;
        SetLastError(NO_ERROR);
        const lUInt32 lowPos =
                SetFilePointer(candidate.get(), 0, &highPos, FILE_END);
        if (lowPos == INVALID_SET_FILE_POINTER
                && GetLastError() != NO_ERROR)
            return LVERR_FAIL;
        initialPos = lowPos;
#if LVLONG_FILE_SUPPORT
        initialPos |= (static_cast<lvpos_t>(highPos) << 32);
#endif
    }
    m_hFile.reset(candidate.release());
    m_mode = openMode;
    m_size = fileSize;
    m_pos = initialPos;
#else
    int flags = openMode == LVOM_READ
            ? O_RDONLY
            : O_RDWR | O_CREAT
                    | (useSync ? O_SYNC : 0)
                    | (openMode == LVOM_WRITE ? O_TRUNC : 0);
    lString8 fn8 = UnicodeToUtf8(fname);
    ScopedDescriptor candidate(
            open(fn8.c_str(), flags, static_cast<mode_t>(0666)));
    if (!candidate.valid()) {
#ifndef ANDROID
        CRLog::error(
                "Error opening file %s for %s",
                fn8.c_str(),
                openMode == LVOM_READ ? "reading" : "read/write");
        //CRLog::error( "Error opening file %s for %s, errno=%d, msg=%s", fn8.c_str(), (mode==LVOM_READ) ? "reading" : "read/write",  (int)errno, strerror(errno) );
#endif
        return LVERR_FAIL;
    }
    struct stat stat;
    if ( fstat( candidate.get(), &stat ) ) {
        CRLog::error( "Cannot get file size for %s", fn8.c_str() );
        return LVERR_FAIL;
    }
    const lvpos_t initialPos = cr3_lseek(
            candidate.get(), 0,
            openMode == LVOM_APPEND ? SEEK_END : SEEK_SET);
    if (initialPos == static_cast<lvpos_t>(-1)) {
        CRLog::error("Cannot seek opened file %s", fn8.c_str());
        return LVERR_FAIL;
    }
    m_ownedFd.reset(candidate.release());
    m_mode = openMode;
    m_size = static_cast<lvsize_t>(stat.st_size);
    m_pos = initialPos;
#endif
    
    SetName(fname.c_str());
    return LVERR_OK;
}

LVFileStream::LVFileStream() :
    #if defined(_WIN32)
    m_hFile(),
    #else
    m_ownedFd(),
    m_borrowedFd(-1),
    #endif
    m_size(0), m_pos(0)
{
    m_mode = LVOM_ERROR;
}

LVFileStream::~LVFileStream()
{
    Close();
}

#endif
