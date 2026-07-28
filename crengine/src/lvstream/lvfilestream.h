/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2009,2010 Vadim Lopatin <coolreader.org@gmail.com> *
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

#ifndef __LVFILESTREAM_H_INCLUDED__
#define __LVFILESTREAM_H_INCLUDED__

#include "crsetup.h"
#include "lvnamedstream.h"
#include "lvstream_types.h"

#include <memory>

//#ifdef _LINUX
#undef USE_ANSI_FILES
//#endif

#if (USE_ANSI_FILES==1)

#include <stdio.h>

class LVFileStream : public LVNamedStream
{
private:
    struct FileCloser
    {
        void operator()(FILE *file) const;
    };

    std::unique_ptr<FILE, FileCloser> m_file;
public:
    lverror_t Seek( lvoffset_t offset, lvseek_origin_t origin, lvpos_t * pNewPos ) override;
    lverror_t SetSize( lvsize_t ) override;
    lverror_t Read( void * buf, lvsize_t count, lvsize_t * nBytesRead ) override;
    lverror_t Write( const void * buf, lvsize_t count, lvsize_t * nBytesWritten ) override;
    /// flushes unsaved data from buffers to file, with optional flush of OS buffers
    lverror_t Flush( bool sync ) override;
    bool Eof() override;
    static std::unique_ptr<LVFileStream> CreateFileStream(
            lString32 fname, lvopen_mode_t mode );
    lverror_t OpenFile( lString32 fname, lvopen_mode_t mode );
    LVFileStream();
    ~LVFileStream() override = default;
    LVFileStream(const LVFileStream &) = delete;
    LVFileStream &operator=(const LVFileStream &) = delete;
};

#else   // (USE_ANSI_FILES==1)

#if !defined(__SYMBIAN32__) && defined(_WIN32)
extern "C" {
#include <windows.h>
}
#endif

class LVFileStream : public LVNamedStream
{
    friend class LVDirectoryContainer;
private:
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
        HANDLE release();
        bool reset(HANDLE handle = NULL);
    };

    ScopedHandle m_hFile;
#else
    class ScopedDescriptor
    {
        int _fd;
    public:
        ScopedDescriptor();
        explicit ScopedDescriptor(int fd);
        ~ScopedDescriptor();
        ScopedDescriptor(const ScopedDescriptor &) = delete;
        ScopedDescriptor &operator=(const ScopedDescriptor &) = delete;

        int get() const { return _fd; }
        bool valid() const { return _fd >= 0; }
        int release();
        bool reset(int fd = -1);
    };

    ScopedDescriptor m_ownedFd;
    int m_borrowedFd;
    int descriptor() const {
        return m_ownedFd.valid() ? m_ownedFd.get() : m_borrowedFd;
    }
#endif
    lvsize_t m_size;
    lvpos_t m_pos;
public:
    /// flushes unsaved data from buffers to file, with optional flush of OS buffers
    lverror_t Flush( bool sync ) override;
    bool Eof() override;
    lverror_t Read( void * buf, lvsize_t count, lvsize_t * nBytesRead ) override;
    lverror_t GetSize( lvsize_t * pSize ) override;
    lvsize_t GetSize() override;
    lverror_t SetSize( lvsize_t size ) override;
    lverror_t Write( const void * buf, lvsize_t count, lvsize_t * nBytesWritten ) override;
    lverror_t Seek( lvoffset_t offset, lvseek_origin_t origin, lvpos_t * pNewPos ) override;
    lverror_t Close();
    static std::unique_ptr<LVFileStream> CreateFileStream(
            lString32 fname, lvopen_mode_t mode );
    static std::unique_ptr<LVFileStream> CreateFileStream(
            int fd, lvopen_mode_t mode = LVOM_READ, bool autoClose = true );
    lverror_t OpenFile( lString32 fname, int mode );
    lverror_t OpenFile( int fd, int mode, bool autoClose = true );
    LVFileStream();
    ~LVFileStream() override;
    LVFileStream(const LVFileStream &) = delete;
    LVFileStream &operator=(const LVFileStream &) = delete;
};
#endif  // (USE_ANSI_FILES==1)

#endif  // __LVFILESTREAM_H_INCLUDED__
