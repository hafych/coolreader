/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2013 Vadim Lopatin <coolreader.org@gmail.com>           *
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

#include "lvcolortransformimgsource.h"
#include "lvcolordrawbuf.h"

static inline lUInt32 limit256(int n) {
    if (n < 0)
        return 0;
    else if (n > 0xFF)
        return 0xFF;
    else
        return (lUInt32)n;
}


LVColorTransformImgSource::LVColorTransformImgSource(LVImageSourceRef src, lUInt32 addRGB, lUInt32 multiplyRGB)
    : _src( src )
    , _add(addRGB)
    , _multiply(multiplyRGB)
    , _callback(NULL)
    , _decodeStarted(false)
    , _sumR(0)
    , _sumG(0)
    , _sumB(0)
    , _countPixels(0)
{
}

LVColorTransformImgSource::~LVColorTransformImgSource() = default;

void LVColorTransformImgSource::OnStartDecode(LVImageSource *)
{
    std::unique_ptr<LVColorDrawBuf> candidate(
            new LVColorDrawBuf(
                    _src->GetWidth(), _src->GetHeight(), 32));
    _drawbuf.swap(candidate);
    _sumR = _sumG = _sumB = _countPixels = 0;
    _decodeStarted = true;
    _callback->OnStartDecode(this);
}

bool LVColorTransformImgSource::OnLineDecoded(LVImageSource *obj, int y, lUInt32 *data) {
    CR_UNUSED(obj);
    if (!_decodeStarted || !_drawbuf || !data
            || y < 0 || y >= _src->GetHeight())
        return false;
    int dx = _src->GetWidth();
    
    lUInt32 * row = (lUInt32*)_drawbuf->GetScanLine(y);
    for (int x = 0; x < dx; x++) {
        lUInt32 cl = data[x];
        row[x] = cl;
        if (((cl >> 24) & 0xFF) < 0xC0) { // count non-transparent pixels only
            _sumR += (cl >> 16) & 0xFF;
            _sumG += (cl >> 8) & 0xFF;
            _sumB += (cl >> 0) & 0xFF;
            _countPixels++;
        }
    }
    return true;
    
}

void LVColorTransformImgSource::OnEndDecode(
        LVImageSource *obj, bool errors)
{
    if (!_decodeStarted || !_callback) {
        _drawbuf.reset();
        _decodeStarted = false;
        return;
    }
    if (errors || !_drawbuf) {
        _drawbuf.reset();
        _decodeStarted = false;
        _callback->OnEndDecode(this, true);
        return;
    }
    int dx = _src->GetWidth();
    int dy = _src->GetHeight();
    // simple add
    int ar = (((_add >> 16) & 0xFF) - 0x80) * 2;
    int ag = (((_add >> 8) & 0xFF) - 0x80) * 2;
    int ab = (((_add >> 0) & 0xFF) - 0x80) * 2;
    // fixed point * 256
    int mr = ((_multiply >> 16) & 0xFF) << 3;
    int mg = ((_multiply >> 8) & 0xFF) << 3;
    int mb = ((_multiply >> 0) & 0xFF) << 3;
    
    int avgR = _countPixels > 0 ? _sumR / _countPixels : 128;
    int avgG = _countPixels > 0 ? _sumG / _countPixels : 128;
    int avgB = _countPixels > 0 ? _sumB / _countPixels : 128;
    
    for (int y = 0; y < dy; y++) {
        lUInt32 * row = (lUInt32*)_drawbuf->GetScanLine(y);
        for ( int x=0; x<dx; x++ ) {
            lUInt32 cl = row[x];
            lUInt32 a = cl & 0xFF000000;
            if (a != 0xFF000000) {
                int r = (cl >> 16) & 0xFF;
                int g = (cl >> 8) & 0xFF;
                int b = (cl >> 0) & 0xFF;
                r = (((r - avgR) * mr) >> 8) + avgR + ar;
                g = (((g - avgG) * mg) >> 8) + avgG + ag;
                b = (((b - avgB) * mb) >> 8) + avgB + ab;
                row[x] = a | (limit256(r) << 16) | (limit256(g) << 8) | (limit256(b) << 0);
            }
        }
        _callback->OnLineDecoded(obj, y, row);
    }
    _drawbuf.reset();
    _decodeStarted = false;
    _callback->OnEndDecode(this, false);
}

bool LVColorTransformImgSource::Decode(LVImageDecoderCallback *callback)
{
    if (!callback || _src.isNull())
        return false;
    _callback = callback;
    _decodeStarted = false;
    bool result = false;
    try {
        result = _src->Decode(this);
        if (_decodeStarted) {
            _drawbuf.reset();
            _decodeStarted = false;
            _callback->OnEndDecode(this, true);
            result = false;
        }
    } catch (...) {
        _drawbuf.reset();
        _decodeStarted = false;
        _callback = NULL;
        throw;
    }
    _drawbuf.reset();
    _callback = NULL;
    return result;
}
