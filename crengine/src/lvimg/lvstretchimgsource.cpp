/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2008-2010 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#include "lvstretchimgsource.h"

LVStretchImgSource::LVStretchImgSource(LVImageSourceRef src, int newWidth, int newHeight, ImageTransform hTransform, ImageTransform vTransform, int splitX, int splitY)
    : _src( src )
    , _src_dx( src->GetWidth() )
    , _src_dy( src->GetHeight() )
    , _dst_dx( newWidth )
    , _dst_dy( newHeight )
    , _hTransform(hTransform)
    , _vTransform(vTransform)
    , _split_x( splitX )
    , _split_y( splitY )
    , _callback(NULL)
    , _decodeStarted(false)
{
    if ( _hTransform == IMG_TRANSFORM_TILE )
        if ( _split_x>=_src_dx )
            _split_x %=_src_dx;
    if ( _vTransform == IMG_TRANSFORM_TILE )
        if ( _split_y>=_src_dy )
            _split_y %=_src_dy;
    if ( _split_x<0 || _split_x>=_src_dx )
        _split_x = _src_dx / 2;
    if ( _split_y<0 || _split_y>=_src_dy )
        _split_y = _src_dy / 2;
}

LVStretchImgSource::~LVStretchImgSource() = default;

void LVStretchImgSource::OnStartDecode(LVImageSource *)
{
    if (!_callback)
        return;
    std::vector<lUInt32> candidate(_dst_dx);
    _line.swap(candidate);
    _decodeStarted = true;
    _callback->OnStartDecode(this);
}

bool LVStretchImgSource::OnLineDecoded( LVImageSource * obj, int y, lUInt32 * data )
{
    if (!_callback || !_decodeStarted || !data
            || y < 0 || y >= _src_dy
            || _line.size() != static_cast<std::size_t>(_dst_dx))
        return false;

    switch ( _hTransform ) {
    case IMG_TRANSFORM_SPLIT:
        {
            int right_pixels = (_src_dx-_split_x-1);
            int first_right_pixel = _dst_dx - right_pixels;
            int right_offset = _src_dx - _dst_dx;
            //int bottom_pixels = (_src_dy-_split_y-1);
            //int first_bottom_pixel = _dst_dy - bottom_pixels;
            for ( int x=0; x<_dst_dx; x++ ) {
                if ( x<_split_x )
                    _line[x] = data[x];
                else if ( x < first_right_pixel )
                    _line[x] = data[_split_x];
                else
                    _line[x] = data[x + right_offset];
            }
        }
        break;
    case IMG_TRANSFORM_STRETCH:
        {
            for ( int x=0; x<_dst_dx; x++ )
                _line[x] = data[
                        static_cast<int>(
                                static_cast<long long>(x)
                                * _src_dx / _dst_dx)];
        }
        break;
    case IMG_TRANSFORM_NONE:
        {
            for ( int x=0; x<_dst_dx && x<_src_dx; x++ )
                _line[x] = data[x];
        }
        break;
    case IMG_TRANSFORM_TILE:
        {
            int offset = _src_dx - _split_x;
            for ( int x=0; x<_dst_dx; x++ )
                _line[x] = data[ (x + offset) % _src_dx];
        }
        break;
    }

    switch ( _vTransform ) {
    case IMG_TRANSFORM_SPLIT:
        {
            const int bottom_pixels =
                    _src_dy - _split_y - 1;
            const int first_bottom =
                    _dst_dy - bottom_pixels;
            const int bottom_offset =
                    _src_dy - _dst_dy;
            if ( y < _split_y ) {
                if (y < _dst_dy)
                    return _callback->OnLineDecoded(
                            obj, y, _line.data());
            } else if ( y==_split_y ) {
                for (int yy = _split_y;
                        yy < first_bottom && yy < _dst_dy;
                        yy++) {
                    if (!_callback->OnLineDecoded(
                                obj, yy, _line.data()))
                        return false;
                }
            } else {
                const int yy = y - bottom_offset;
                if (yy >= _split_y && yy >= first_bottom
                        && yy >= 0 && yy < _dst_dy)
                    return _callback->OnLineDecoded(
                            obj, yy, _line.data());
            }
        }
        break;
    case IMG_TRANSFORM_STRETCH:
        {
            const int y0 = static_cast<int>(
                    static_cast<long long>(y)
                    * _dst_dy / _src_dy);
            const int y1 = static_cast<int>(
                    static_cast<long long>(y + 1)
                    * _dst_dy / _src_dy);
            for ( int yy=y0; yy<y1; yy++ ) {
                if (!_callback->OnLineDecoded(
                            obj, yy, _line.data()))
                    return false;
            }
        }
        break;
    case IMG_TRANSFORM_NONE:
        {
            if ( y<_dst_dy )
                return _callback->OnLineDecoded(
                        obj, y, _line.data());
        }
        break;
    case IMG_TRANSFORM_TILE:
        {
            int offset = _src_dy - _split_y;
            int y0 = (y + offset) % _src_dy;
            for ( int yy=y0; yy<_dst_dy; yy+=_src_dy ) {
                if (!_callback->OnLineDecoded(
                            obj, yy, _line.data()))
                    return false;
            }
        }
        break;
    }

    return true;
}

void LVStretchImgSource::OnEndDecode(LVImageSource *, bool res)
{
    std::vector<lUInt32>().swap(_line);
    if (!_callback || !_decodeStarted)
        return;
    _decodeStarted = false;
    _callback->OnEndDecode(this, res);
}

bool LVStretchImgSource::Decode(LVImageDecoderCallback *callback)
{
    if (!callback || _src.isNull()
            || _src_dx <= 0 || _src_dy <= 0
            || _dst_dx <= 0 || _dst_dy <= 0)
        return false;
    _callback = callback;
    _decodeStarted = false;
    bool result = false;
    try {
        result = _src->Decode(this);
        if (_decodeStarted) {
            std::vector<lUInt32>().swap(_line);
            _decodeStarted = false;
            _callback->OnEndDecode(this, true);
            result = false;
        }
    } catch (...) {
        std::vector<lUInt32>().swap(_line);
        _decodeStarted = false;
        _callback = NULL;
        throw;
    }
    std::vector<lUInt32>().swap(_line);
    _callback = NULL;
    return result;
}
