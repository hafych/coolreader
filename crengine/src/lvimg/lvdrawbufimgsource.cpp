/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2010-2012 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#include "lvdrawbufimgsource.h"
#include "lvimagedecodercallback.h"
#include "lvcolordrawbuf.h"

// lvdrawbuff private stuff
#include "../lvdrawbuf/lvdrawbuf_utils.h"
#include <vector>

LVDrawBufImgSource::LVDrawBufImgSource(LVColorDrawBuf *buf, bool own)
    : _ownedBuf(own ? buf : NULL)
    , _buf(buf)
    , _dx(buf ? buf->GetWidth() : 0)
    , _dy(buf ? buf->GetHeight() : 0)
{
}

LVDrawBufImgSource::~LVDrawBufImgSource() = default;

bool LVDrawBufImgSource::Decode(LVImageDecoderCallback *callback)
{
    if (!_buf || _dx <= 0 || _dy <= 0)
        return false;
    const int bpp = _buf->GetBitsPerPixel();
    if (bpp != 16 && bpp != 32)
        return false;
    if (!callback)
        return true;
    std::vector<lUInt32> row;
    if (bpp == 16)
        row.resize(_dx);
    callback->OnStartDecode( this );
    if ( bpp==32 ) {
        // 32 bpp
        for ( int y=0; y<_dy; y++ ) {
            lUInt8 *scanline = _buf->GetScanLine(y);
            if (!scanline || !callback->OnLineDecoded(
                        this, y,
                        reinterpret_cast<lUInt32 *>(scanline))) {
                callback->OnEndDecode(this, true);
                return false;
            }
        }
    } else {
        // 16 bpp
        for ( int y=0; y<_dy; y++ ) {
            lUInt16 *src = reinterpret_cast<lUInt16 *>(
                    _buf->GetScanLine(y));
            if (!src) {
                callback->OnEndDecode(this, true);
                return false;
            }
            for ( int x=0; x<_dx; x++ )
                row[x] = rgb565to888(src[x]);
            if (!callback->OnLineDecoded(
                        this, y, row.data())) {
                callback->OnEndDecode(this, true);
                return false;
            }
        }
    }
    callback->OnEndDecode( this, false );
    return true;
}
