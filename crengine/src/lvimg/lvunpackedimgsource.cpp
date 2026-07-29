/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2009,2010,2012 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2019 NiLuJe <ninuje@gmail.com>                          *
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

#include "lvunpackedimgsource.h"

// lvdrawbuff private stuff
#include "../lvdrawbuf/lvdrawbuf_utils.h"

#include <cstddef>
#include <limits>
#include <string.h>
#include <vector>

// aaaaaaaarrrrrrrrggggggggbbbbbbbb -> yyyyyyaa
inline lUInt8 grayPack(lUInt32 pixel)
{
    lUInt8 gray = (lUInt8)(( (pixel & 0xFF) + ((pixel>>16) & 0xFF) + ((pixel>>7)&510) ) >> 2);
    lUInt8 alpha = (lUInt8)((pixel>>24) & 0xFF);
    return (gray & 0xFC) | ((alpha >> 6) & 3);
}

// yyyyyyaa -> aaaaaaaarrrrrrrrggggggggbbbbbbbb
inline lUInt32 grayUnpack(lUInt8 pixel)
{
    lUInt32 gray = pixel & 0xFC;
    lUInt32 alpha = (pixel & 3) << 6;
    if ( alpha==0xC0 )
        alpha = 0xFF;
    return gray | (gray<<8) | (gray<<16) | (alpha<<24);
}


LVUnpackedImgSource::LVUnpackedImgSource(LVImageSourceRef src, int bpp)
    : _isGray(bpp==8)
    , _valid(false)
    , _bpp(bpp)
    , _dx(src.isNull() ? 0 : src->GetWidth())
    , _dy(src.isNull() ? 0 : src->GetHeight())
{
    if (src.isNull() || _dx <= 0 || _dy <= 0
            || (bpp != 8 && bpp != 16 && bpp != 32))
        return;
    const std::size_t unsignedWidth =
            static_cast<std::size_t>(_dx);
    const std::size_t unsignedHeight =
            static_cast<std::size_t>(_dy);
    if (unsignedWidth
            > std::numeric_limits<std::size_t>::max()
                    / unsignedHeight)
        return;
    const std::size_t pixelCount =
            unsignedWidth * unsignedHeight;
    if ( bpp==8 ) {
        _grayImage.resize(pixelCount);
    } else if ( bpp==16 ) {
        _colorImage16.resize(pixelCount);
    } else {
        _colorImage.resize(pixelCount);
    }
    if ( !src->Decode(this) )
        _valid = false;
}

void LVUnpackedImgSource::OnStartDecode(LVImageSource *)
{
    //CRLog::trace( "LVUnpackedImgSource::OnStartDecode" );
}

bool LVUnpackedImgSource::OnLineDecoded(LVImageSource *, int y, lUInt32 *data)
{
    if ( y<0 || y>=_dy || _dx<=0 || !data )
        return false;
    const std::size_t offset =
            static_cast<std::size_t>(_dx) * static_cast<std::size_t>(y);
    if ( _isGray ) {
        lUInt8 * dst = _grayImage.data() + offset;
        for ( int x=0; x<_dx; x++ ) {
            dst[x] = grayPack( data[x] );
        }
    } else if ( _bpp==16 ) {
        lUInt16 * dst = _colorImage16.data() + offset;
        for ( int x=0; x<_dx; x++ ) {
            dst[x] = rgb888to565( data[x] );
        }
    } else {
        lUInt32 * dst = _colorImage.data() + offset;
        memcpy( dst, data, sizeof(lUInt32) * _dx );
    }
    return true;
}

void LVUnpackedImgSource::OnEndDecode(LVImageSource *, bool errors)
{
    _valid = !errors;
}

bool LVUnpackedImgSource::Decode(LVImageDecoderCallback *callback)
{
    if ( !_valid || !callback || _dx<=0 || _dy<=0 )
        return false;
    const std::size_t pixelCount =
            static_cast<std::size_t>(_dx)
            * static_cast<std::size_t>(_dy);
    if ((_isGray && _grayImage.size() != pixelCount)
            || (_bpp == 16
                    && _colorImage16.size() != pixelCount)
            || (_bpp == 32
                    && _colorImage.size() != pixelCount))
        return false;
    callback->OnStartDecode( this );
    if ( _isGray ) {
        // gray
        std::vector<lUInt32> line(_dx);
        for ( int y=0; y<_dy; y++ ) {
            const std::size_t offset =
                    static_cast<std::size_t>(_dx)
                    * static_cast<std::size_t>(y);
            const lUInt8 * src = _grayImage.data() + offset;
            lUInt32 * dst = line.data();
            for ( int x=0; x<_dx; x++ )
                dst[x] = grayUnpack( src[x] );
            if (!callback->OnLineDecoded(this, y, dst)) {
                callback->OnEndDecode(this, true);
                return false;
            }
        }
    } else if ( _bpp==16 ) {
        // 16bit
        std::vector<lUInt32> line(_dx);
        for ( int y=0; y<_dy; y++ ) {
            const std::size_t offset =
                    static_cast<std::size_t>(_dx)
                    * static_cast<std::size_t>(y);
            const lUInt16 * src = _colorImage16.data() + offset;
            lUInt32 * dst = line.data();
            for ( int x=0; x<_dx; x++ )
                dst[x] = rgb565to888( src[x] );
            if (!callback->OnLineDecoded(this, y, dst)) {
                callback->OnEndDecode(this, true);
                return false;
            }
        }
    } else {
        // color
        for ( int y=0; y<_dy; y++ ) {
            const std::size_t offset =
                    static_cast<std::size_t>(_dx)
                    * static_cast<std::size_t>(y);
            if (!callback->OnLineDecoded(
                        this, y,
                        _colorImage.data() + offset)) {
                callback->OnEndDecode(this, true);
                return false;
            }
        }
    }
    callback->OnEndDecode( this, false );
    return true;
}
