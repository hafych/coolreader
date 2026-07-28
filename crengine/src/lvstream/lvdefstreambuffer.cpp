/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2009,2010 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#include "lvdefstreambuffer.h"

#include <memory>
#include <vector>

LVStreamBufferRef LVDefStreamBuffer::create(LVStreamRef stream, lvpos_t pos, lvsize_t size, bool readonly)
{
    LVStreamBufferRef res;
    if ( stream.isNull() || size==0 )
        return res;
    switch ( stream->GetMode() ) {
        case LVOM_ERROR:       ///< to indicate error state
        case LVOM_CLOSED:        ///< to indicate closed state
            return res;
        case LVOM_READ:          ///< readonly mode, use for r/o
            if ( !readonly )
                return res;
            break;
        case LVOM_WRITE:         ///< writeonly mode
        case LVOM_APPEND:        ///< append (readwrite) mode, use for r/w
        case LVOM_READWRITE:      ///< readwrite mode
            if ( readonly )
                return res;
            break;
    }
    lvsize_t sz;
    if ( stream->GetSize(&sz)!=LVERR_OK )
        return res;
    if ( pos>sz || size>sz-pos )
        return res; // wrong position/size
    std::unique_ptr<LVDefStreamBuffer> buf(
            new LVDefStreamBuffer(stream, pos, size, readonly));
    if ( stream->SetPos(pos)!=pos )
        return res;
    if ( !buf->m_writeonly ) {
        lvsize_t bytesRead = 0;
        if ( stream->Read(buf->m_buf.data(), size, &bytesRead)!=LVERR_OK
                || bytesRead!=size )
            return res;
    }
    buf->m_ready = true;
    return LVStreamBufferRef(buf.release());
}

LVDefStreamBuffer::LVDefStreamBuffer(LVStreamRef stream, lvpos_t pos, lvsize_t size, bool readonly)
    : m_stream(stream), m_buf(static_cast<std::size_t>(size)),
      m_pos(pos), m_size(size), m_readonly(readonly),
      m_writeonly(stream->GetMode()==LVOM_WRITE), m_ready(false)
{
}

bool LVDefStreamBuffer::close()
{
    bool res = true;
    if ( m_ready ) {
        if ( !m_readonly && !m_stream.isNull() ) {
            if ( m_stream->SetPos(m_pos)!=m_pos ) {
                res = false;
            } else {
                lvsize_t bytesWritten = 0;
                if ( m_stream->Write(
                            m_buf.data(), m_size, &bytesWritten)!=LVERR_OK
                        || bytesWritten!=m_size ) {
                    res = false;
                }
            }
        }
    }
    m_ready = false;
    std::vector<lUInt8>().swap(m_buf);
    m_stream.Clear();
    m_size = 0;
    m_pos = 0;
    return res;
}
