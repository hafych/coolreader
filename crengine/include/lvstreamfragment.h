/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2011 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#ifndef __LVSTREAMFRAGMENT_H_INCLUDED__
#define __LVSTREAMFRAGMENT_H_INCLUDED__

#include "lvnamedstream.h"

class LVStreamFragment : public LVNamedStream
{
private:
    LVStreamRef m_stream;
    lvsize_t    m_start;
    lvsize_t    m_size;
    lvpos_t     m_pos;
    unsigned    m_containerDepth;
public:
    LVStreamFragment( LVStreamRef stream, lvsize_t start, lvsize_t size,
                      unsigned containerDepth = 0 )
        : m_stream(stream), m_start(start), m_size(size), m_pos(0),
          m_containerDepth(containerDepth != 0 || stream.isNull()
                                   ? containerDepth
                                   : stream->GetContainerDepth())
    {
    }
    ~LVStreamFragment() override = default;
    LVStreamFragment(const LVStreamFragment &) = delete;
    LVStreamFragment &operator=(const LVStreamFragment &) = delete;

    unsigned GetContainerDepth() override
    {
        return m_containerDepth;
    }
    lvopen_mode_t GetMode() override
    {
        return LVOM_READ;
    }
    bool Eof() override
    {
        return m_pos >= m_size;
    }
    lvsize_t GetSize() override
    {
        return m_size;
    }

    lverror_t Seek(
            lvoffset_t offset, lvseek_origin_t origin,
            lvpos_t *newPos) override
    {
        if (m_stream.isNull()
                || m_start > LV_INVALID_SIZE - m_size)
            return LVERR_FAIL;
        lvpos_t base = 0;
        switch (origin) {
            case LVSEEK_SET:
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

        lvpos_t target = base;
        if (offset < 0) {
            const lvsize_t magnitude =
                    static_cast<lvsize_t>(-(offset + 1)) + 1;
            if (magnitude > base)
                return LVERR_FAIL;
            target = base - magnitude;
        } else {
            const lvsize_t delta = static_cast<lvsize_t>(offset);
            if (base > m_size || delta > m_size - base)
                return LVERR_FAIL;
            target = base + delta;
        }
        if (target > m_size || m_start > LV_INVALID_SIZE - target)
            return LVERR_FAIL;

        lvpos_t absolute = 0;
        const lverror_t result = m_stream->Seek(
                m_start + target, LVSEEK_SET, &absolute);
        if (result != LVERR_OK || absolute != m_start + target)
            return LVERR_FAIL;
        m_pos = target;
        if (newPos)
            *newPos = m_pos;
        return LVERR_OK;
    }
    lverror_t Write(
            const void*, lvsize_t, lvsize_t *bytesWritten) override
    {
        if (bytesWritten)
            *bytesWritten = 0;
        return LVERR_NOTIMPL;
    }
    lverror_t Read(
            void *buf, lvsize_t size, lvsize_t *pBytesRead) override
    {
        if (pBytesRead)
            *pBytesRead = 0;
        if (m_stream.isNull()
                || m_start > LV_INVALID_SIZE - m_size
                || m_pos > m_size)
            return LVERR_FAIL;
        const lvsize_t remaining = m_size - m_pos;
        if (size > remaining)
            size = remaining;
        if (size == 0)
            return LVERR_OK;
        if (!buf || m_start > LV_INVALID_SIZE - m_pos)
            return LVERR_FAIL;

        lvsize_t bytesRead = 0;
        lvpos_t absolute = 0;
        lverror_t result = m_stream->Seek(
                m_start + m_pos, LVSEEK_SET, &absolute);
        if (result != LVERR_OK || absolute != m_start + m_pos)
            return LVERR_FAIL;
        result = m_stream->Read(buf, size, &bytesRead);
        if (result != LVERR_OK || bytesRead > size)
            return LVERR_FAIL;
        m_pos += bytesRead;
        if (pBytesRead)
            *pBytesRead = bytesRead;
        return LVERR_OK;
    }
    lverror_t SetSize(lvsize_t) override
    {
        return LVERR_NOTIMPL;
    }
};

#endif  // __LVSTREAMFRAGMENT_H_INCLUDED__
