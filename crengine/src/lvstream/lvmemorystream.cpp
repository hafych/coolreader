/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2011 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2008 Alexander V. Nikolaev <avn@daemon.hole.ru>         *
 *   Copyright (C) 2010 Kirill Erofeev <erofeev.info@gmail.com>            *
 *   Copyright (C) 2018 Aleksey Chernov <valexlin@gmail.com>               *
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

#include "lvmemorystream.h"

#include <algorithm>
#include <cstring>
#include <limits>
#include <new>
#include <stdexcept>
#include <utility>

namespace {

bool checkedBufferSize(lvsize_t requested, std::size_t &result)
{
    if (requested > static_cast<lvsize_t>(
            std::numeric_limits<std::size_t>::max()))
        return false;
    result = static_cast<std::size_t>(requested);
    return true;
}

bool checkedPositionOffset(lvpos_t base, lvoffset_t offset, lvpos_t &result)
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

lverror_t LVMemoryStream::SetMode(lvopen_mode_t mode)
{
    if ( m_mode==mode )
        return LVERR_OK;
    if ( m_mode==LVOM_WRITE && mode==LVOM_READ ) {
        m_mode = LVOM_READ;
        m_pos = 0;
        return LVERR_OK;
    }
    // TODO: READ -> WRITE/APPEND
    return LVERR_FAIL;
}

lverror_t LVMemoryStream::Read(void *buf, lvsize_t count, lvsize_t *nBytesRead)
{
    if (nBytesRead)
        *nBytesRead = 0;
    if (!m_pBuffer || (!buf && count > 0)
            || m_mode==LVOM_WRITE || m_mode==LVOM_APPEND )
        return LVERR_FAIL;
    if (m_pos > m_size)
        return LVERR_FAIL;
    const lvsize_t bytesRead = std::min(count, m_size - m_pos);
    std::size_t offset = 0;
    std::size_t amount = 0;
    if (!checkedBufferSize(m_pos, offset)
            || !checkedBufferSize(bytesRead, amount))
        return LVERR_FAIL;
    if (amount > 0)
        std::memcpy(buf, m_pBuffer + offset, amount);
    if (nBytesRead)
        *nBytesRead = bytesRead;
    m_pos += bytesRead;
    return LVERR_OK;
}

lvsize_t LVMemoryStream::GetSize()
{
    if (!m_pBuffer)
        return (lvsize_t)(-1);
    if (m_size<m_pos)
        m_size = m_pos;
    return m_size;
}

lverror_t LVMemoryStream::GetSize(lvsize_t *pSize)
{
    if (!m_pBuffer || !pSize)
        return LVERR_FAIL;
    if (m_size<m_pos)
        m_size = m_pos;
    *pSize = m_size;
    return LVERR_OK;
}

lverror_t LVMemoryStream::SetBufSize(lvsize_t new_size)
{
    if (!m_pBuffer || m_mode==LVOM_READ )
        return LVERR_FAIL;
    if (new_size<=m_bufsize)
        return LVERR_OK;
    if (m_storage.empty() || m_pBuffer != m_storage.data())
        return LVERR_FAIL; // cannot resize foreign buffer
    if (new_size > (std::numeric_limits<lvsize_t>::max() - 4096) / 2)
        return LVERR_FAIL;
    const lvsize_t grownSize = new_size * 2 + 4096;
    std::size_t vectorSize = 0;
    if (!checkedBufferSize(grownSize, vectorSize)
            || vectorSize > m_storage.max_size())
        return LVERR_FAIL;
    try {
        m_storage.resize(vectorSize);
    } catch (const std::bad_alloc &) {
        return LVERR_FAIL;
    } catch (const std::length_error &) {
        return LVERR_FAIL;
    }
    m_pBuffer = m_storage.data();
    m_bufsize = grownSize;
    return LVERR_OK;
}

lverror_t LVMemoryStream::SetSize(lvsize_t size)
{
    //
    if (SetBufSize( size )!=LVERR_OK)
        return LVERR_FAIL;
    m_size = size;
    if (m_pos>m_size)
        m_pos = m_size;
    return LVERR_OK;
}

lverror_t LVMemoryStream::Write(const void *buf, lvsize_t count, lvsize_t *nBytesWritten)
{
    if (nBytesWritten)
        *nBytesWritten = 0;
    if (!m_pBuffer || !buf || m_mode==LVOM_READ )
        return LVERR_FAIL;
    if (count > std::numeric_limits<lvpos_t>::max() - m_pos)
        return LVERR_FAIL;
    const lvpos_t endPos = m_pos + count;
    if (SetBufSize(endPos) != LVERR_OK)
        return LVERR_FAIL;
    std::size_t offset = 0;
    std::size_t amount = 0;
    if (!checkedBufferSize(m_pos, offset)
            || !checkedBufferSize(count, amount))
        return LVERR_FAIL;
    if (amount>0) {
        std::memcpy(m_pBuffer + offset, buf, amount);
        m_pos = endPos;
        if (m_size<m_pos)
            m_size = m_pos;
    }
    if (nBytesWritten)
        *nBytesWritten = count;
    return LVERR_OK;
}

lverror_t LVMemoryStream::Seek(lvoffset_t offset, lvseek_origin_t origin, lvpos_t *pNewPos)
{
    if (!m_pBuffer)
        return LVERR_FAIL;
    lvpos_t base = 0;
    switch (origin) {
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
    if (!checkedPositionOffset(base, offset, newpos))
        return LVERR_FAIL;
    if (newpos>m_size)
        return LVERR_FAIL;
    m_pos = newpos;
    if (pNewPos)
        *pNewPos = m_pos;
    return LVERR_OK;
}

lverror_t LVMemoryStream::Close()
{
    std::vector<lUInt8>().swap(m_storage);
    m_pBuffer = NULL;
    m_size = 0;
    m_bufsize = 0;
    m_pos = 0;
    m_mode = LVOM_CLOSED;
    m_containerDepth = 0;
    return LVERR_OK;
}

lverror_t LVMemoryStream::Create()
{
    Close();
    try {
        m_storage.resize(4096);
    } catch (const std::bad_alloc &) {
        return LVERR_FAIL;
    } catch (const std::length_error &) {
        return LVERR_FAIL;
    }
    m_pBuffer = m_storage.data();
    m_bufsize = m_storage.size();
    m_size = 0;
    m_pos = 0;
    m_mode = LVOM_READWRITE;
    return LVERR_OK;
}

lverror_t LVMemoryStream::CreateCopy(LVStreamRef srcStream, lvopen_mode_t mode)
{
    Close();
    if ( mode!=LVOM_READ || srcStream.isNull() )
        return LVERR_FAIL;
    lvsize_t sz = srcStream->GetSize();
    if ( sz == 0 || sz > 0x200000 )
        return LVERR_FAIL;
    std::vector<lUInt8> replacement;
    try {
        replacement.resize(static_cast<std::size_t>(sz));
    } catch (const std::bad_alloc &) {
        return LVERR_FAIL;
    } catch (const std::length_error &) {
        return LVERR_FAIL;
    }
    lvsize_t bytesRead = 0;
    if (srcStream->Read(replacement.data(), sz, &bytesRead) != LVERR_OK
            || bytesRead != sz)
        return LVERR_FAIL;
    m_storage.swap(replacement);
    m_pBuffer = m_storage.data();
    m_bufsize = sz;
    m_size = sz;
    m_pos = 0;
    m_mode = mode;
    m_containerDepth = srcStream->GetContainerDepth();
    return LVERR_OK;
}

lverror_t LVMemoryStream::CreateCopy(const lUInt8 *pBuf, lvsize_t size, lvopen_mode_t mode)
{
    Close();
    m_containerDepth = 0;
    std::size_t copySize = 0;
    if ((size > 0 && !pBuf) || !checkedBufferSize(size, copySize))
        return LVERR_FAIL;
    std::vector<lUInt8> replacement;
    try {
        replacement.resize(copySize > 0 ? copySize : 1);
    } catch (const std::bad_alloc &) {
        return LVERR_FAIL;
    } catch (const std::length_error &) {
        return LVERR_FAIL;
    }
    if (copySize > 0)
        std::memcpy(replacement.data(), pBuf, copySize);
    m_storage.swap(replacement);
    m_pBuffer = m_storage.data();
    m_bufsize = m_storage.size();
    m_pos = 0;
    m_mode = mode;
    m_size = size;
    if (mode==LVOM_APPEND)
        m_pos = m_size;
    return LVERR_OK;
}

lverror_t LVMemoryStream::Open(lUInt8 *pBuf, lvsize_t size)
{
    if (!pBuf)
        return LVERR_FAIL;
    Close();
    m_pBuffer = pBuf;
    m_bufsize = size;
    // set file size and position
    m_pos = 0;
    m_size = size;
    m_mode = LVOM_READ;
    m_containerDepth = 0;
    
    return LVERR_OK;
}

LVMemoryStream::LVMemoryStream()
    : m_pBuffer(NULL), m_parent(NULL), m_size(0), m_bufsize(0),
      m_pos(0), m_containerDepth(0)
{
}
