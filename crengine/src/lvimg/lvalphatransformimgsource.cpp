/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2013,2014 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#include "lvalphatransformimgsource.h"

LVAlphaTransformImgSource::LVAlphaTransformImgSource(LVImageSourceRef src, int alpha)
    : _src( src )
    , _callback(NULL)
    , _opacity(alpha <= 0 ? 0xFF
            : alpha >= 0xFF ? 0 : 0xFF - alpha)
    , _decodeStarted(false)
{
}

LVAlphaTransformImgSource::~LVAlphaTransformImgSource() = default;

void LVAlphaTransformImgSource::OnStartDecode(LVImageSource *)
{
    if (!_callback)
        return;
    _decodeStarted = true;
    _callback->OnStartDecode(this);
}

bool LVAlphaTransformImgSource::OnLineDecoded(LVImageSource *obj, int y, lUInt32 *data) {
    if (!_callback || !_decodeStarted || !data
            || y < 0 || y >= _src->GetHeight())
        return false;
    int dx = _src->GetWidth();
    
    for (int x = 0; x < dx; x++) {
        const lUInt32 color = data[x];
        const int sourceOpacity =
                0xFF - static_cast<int>(color >> 24);
        const int outputOpacity =
                (sourceOpacity * _opacity + 0x7F) / 0xFF;
        const lUInt32 outputTransparency =
                static_cast<lUInt32>(0xFF - outputOpacity);
        data[x] = (color & 0x00FFFFFF)
                | (outputTransparency << 24);
    }
    return _callback->OnLineDecoded(obj, y, data);
}

void LVAlphaTransformImgSource::OnEndDecode(LVImageSource *obj, bool res)
{
    CR_UNUSED(obj);
    if (!_callback || !_decodeStarted)
        return;
    _decodeStarted = false;
    _callback->OnEndDecode(this, res);
}

bool LVAlphaTransformImgSource::Decode(LVImageDecoderCallback *callback)
{
    if (!callback || _src.isNull())
        return false;
    _callback = callback;
    _decodeStarted = false;
    bool result = false;
    try {
        result = _src->Decode(this);
        if (_decodeStarted) {
            _decodeStarted = false;
            _callback->OnEndDecode(this, true);
            result = false;
        }
    } catch (...) {
        _decodeStarted = false;
        _callback = NULL;
        throw;
    }
    _callback = NULL;
    return result;
}
