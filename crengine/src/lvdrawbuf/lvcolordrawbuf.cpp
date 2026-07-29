/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2014 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2011 Konstantin Potapov <pkbo@users.sourceforge.net>    *
 *   Copyright (C) 2015,2016 Yifei(Frank) ZHU <fredyifei@gmail.com>        *
 *   Copyright (C) 2018-2020 poire-z <poire-z@users.noreply.github.com>    *
 *   Copyright (C) 2019-2021 NiLuJe <ninuje@gmail.com>                     *
 *   Copyright (C) 2021 Aleksey Chernov <valexlin@gmail.com>               *
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

#include "lvcolordrawbuf.h"
#include "lvimagescaleddrawcallback.h"
#include "lvdrawbuf_utils.h"

#include <limits>
#include <new>
#include <stdexcept>
#include <vector>

namespace {

static const std::size_t DRAW_BUFFER_MAX_BYTES =
        static_cast<std::size_t>(512) * 1024 * 1024;

struct ColorBufferLayout {
    int width;
    int height;
    int rowSize;
    std::size_t byteCount;
};

static ColorBufferLayout getColorBufferLayout(
        int width, int height, int bitsPerPixel)
{
    if (width < 0 || height < 0)
        throw std::length_error("negative color draw-buffer size");
    if (bitsPerPixel != 16 && bitsPerPixel != 32)
        throw std::invalid_argument(
                "color draw buffer supports only 16 or 32 bpp");
    if (width == 0 || height == 0)
        return ColorBufferLayout{0, 0, 0, 0};

    const std::size_t bytesPerPixel =
            static_cast<std::size_t>(bitsPerPixel >> 3);
    const std::size_t rowSize =
            static_cast<std::size_t>(width) * bytesPerPixel;
    if (rowSize > static_cast<std::size_t>(
                std::numeric_limits<int>::max())
            || static_cast<std::size_t>(height)
                > DRAW_BUFFER_MAX_BYTES / rowSize)
        throw std::length_error("color draw buffer is too large");
    const std::size_t byteCount =
            rowSize * static_cast<std::size_t>(height);
    if (byteCount > DRAW_BUFFER_MAX_BYTES)
        throw std::length_error("color draw buffer is too large");
    return ColorBufferLayout{
            width, height, static_cast<int>(rowSize), byteCount};
}

#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
class ScopedBitmap {
    HBITMAP m_value;
public:
    explicit ScopedBitmap(HBITMAP value = NULL)
        : m_value(value) {}
    ~ScopedBitmap() {
        if (m_value)
            DeleteObject(m_value);
    }
    HBITMAP get() const { return m_value; }
    void reset(HBITMAP value) {
        if (m_value)
            DeleteObject(m_value);
        m_value = value;
    }
    HBITMAP release() {
        HBITMAP value = m_value;
        m_value = NULL;
        return value;
    }
    ScopedBitmap(const ScopedBitmap &) = delete;
    ScopedBitmap & operator=(const ScopedBitmap &) = delete;
};

class ScopedCompatibleDC {
    HDC m_value;
public:
    explicit ScopedCompatibleDC(HDC value = NULL)
        : m_value(value) {}
    ~ScopedCompatibleDC() {
        if (m_value)
            DeleteDC(m_value);
    }
    HDC get() const { return m_value; }
    void reset(HDC value) {
        if (m_value)
            DeleteDC(m_value);
        m_value = value;
    }
    HDC release() {
        HDC value = m_value;
        m_value = NULL;
        return value;
    }
    ScopedCompatibleDC(const ScopedCompatibleDC &) = delete;
    ScopedCompatibleDC & operator=(const ScopedCompatibleDC &) = delete;
};
#endif

} // namespace


// Blend mono 1-bit bitmap (8 pixels per byte)
static inline void blendBitmap_monoTo16bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt16 color) {
    lUInt16 * dst;
    lUInt16 * dstline;
    const lUInt8* src;
    unsigned char mask;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt16*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        mask = 0x80;
        for (int xx = width; xx>0; --xx) {
            if (*src & mask)
                *dst = color;
            mask >>= 1;
            if (0 == mask) {
                mask = 0x80;
                src++;
            }
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend gray 8-bit bitmap (1 byte per pixel)
static inline void blendBitmap_grayTo16bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt16 color) {
    lUInt16 tr = (color>>11);
    lUInt16 tg = ((color>>5) & 0x003F);
    lUInt16 tb = (color & 0x001F);
    lUInt16 * dst;
    lUInt16 * dstline;
    const lUInt8* src;
    lUInt16 alpha;
    lUInt16 alpha_r, alpha_g, alpha_b;
    lUInt16 r, g, b;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt16*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        for (int xx = width; xx>0; --xx) {
            alpha = *(src++);
            alpha_r = alpha >> 3;
            alpha_g = alpha >> 2;
            alpha_b = alpha >> 3;
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (31 - alpha_r)*((*dst)>>11) + 15)/31;
            g = (alpha_g*tg + (63 - alpha_g)*(((*dst)>>5) & 0x003F) + 31)/63;
            b = (alpha_b*tb + (31 - alpha_b)*((*dst) & 0x00001F) + 15)/31;
            *dst = (r << 11) | (g << 5) | b;
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel)
static inline void blendBitmap_rgbTo16bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt16 color) {
    lUInt16 tr = (color>>11);
    lUInt16 tg = ((color>>5) & 0x003F);
    lUInt16 tb = (color & 0x001F);
    lUInt16 * dst;
    lUInt16 * dstline;
    const lUInt8* src;
    lUInt16 alpha_r, alpha_g, alpha_b;
    lUInt16 r, g, b;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt16*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        for (int xx = width; xx>0; --xx) {
            alpha_r = *(src++) >> 3;
            alpha_g = *(src++) >> 2;
            alpha_b = *(src++) >> 3;
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (31 - alpha_r)*((*dst)>>11) + 15)/31;
            g = (alpha_g*tg + (63 - alpha_g)*(((*dst)>>5) & 0x003F) + 31)/63;
            b = (alpha_b*tb + (31 - alpha_b)*((*dst) & 0x001F) + 15)/31;
            *dst = (r << 11) | (g << 5) | b;
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel)
static inline void blendBitmap_bgrTo16bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt16 color) {
    lUInt16 tr = (color>>11);
    lUInt16 tg = ((color>>5) & 0x003F);
    lUInt16 tb = (color & 0x001F);
    lUInt16 * dst;
    lUInt16 * dstline;
    const lUInt8* src;
    lUInt16 alpha_r, alpha_g, alpha_b;
    lUInt16 r, g, b;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt16*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        for (int xx = width; xx>0; --xx) {
            alpha_b = *(src++) >> 3;
            alpha_g = *(src++) >> 2;
            alpha_r = *(src++) >> 3;
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (31 - alpha_r)*((*dst)>>11) + 15)/31;
            g = (alpha_g*tg + (63 - alpha_g)*(((*dst)>>5) & 0x003F) + 31)/63;
            b = (alpha_b*tb + (31 - alpha_b)*((*dst) & 0x001F) + 15)/31;
            *dst = (r << 11) | (g << 5) | b;
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel, vertical)
static inline void blendBitmap_rgbvTo16bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt16 color) {
    lUInt16 tr = (color>>11);
    lUInt16 tg = ((color>>5) & 0x003F);
    lUInt16 tb = (color & 0x001F);
    lUInt16 * dst;
    lUInt16 * dstline;
    const lUInt8* src;
    lUInt16 alpha_r, alpha_g, alpha_b;
    lUInt16 r, g, b;
    for (int xx = 0; xx<width; xx++) {
        src = bitmap;
        for (int yy = 0; yy < height; yy++) {
            dstline = ((lUInt16*)d->GetScanLine(y+yy)) + x + xx;
            dst = dstline;
            if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
                break;
            }
            alpha_r = src[xx] >> 3;
            alpha_g = src[xx + bmp_pitch] >> 2;
            alpha_b = src[xx + 2*bmp_pitch] >> 3;
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (31 - alpha_r)*((*dst)>>11) + 15)/31;
            g = (alpha_g*tg + (63 - alpha_g)*(((*dst)>>5) & 0x003F) + 31)/63;
            b = (alpha_b*tb + (31 - alpha_b)*((*dst) & 0x001F) + 15)/31;
            *dst = (r << 11) | (g << 5) | b;
            /* next pixel */
            //dst++;
            /* new dest line */
            src += 3*bmp_pitch;
        }
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel, vertical)
static inline void blendBitmap_bgrvTo16bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt16 color) {
    lUInt16 tr = (color>>11);
    lUInt16 tg = ((color>>5) & 0x003F);
    lUInt16 tb = (color & 0x001F);
    lUInt16 * dst;
    lUInt16 * dstline;
    const lUInt8* src;
    lUInt16 alpha_r, alpha_g, alpha_b;
    lUInt16 r, g, b;
    for (int xx = 0; xx<width; xx++) {
        src = bitmap;
        for (int yy = 0; yy < height; yy++) {
            dstline = ((lUInt16*)d->GetScanLine(y+yy)) + x + xx;
            dst = dstline;
            if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
                break;
            }
            alpha_b = src[xx] >> 3;
            alpha_g = src[xx + bmp_pitch] >> 2;
            alpha_r = src[xx + 2*bmp_pitch] >> 3;
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (31 - alpha_r)*((*dst)>>11) + 15)/31;
            g = (alpha_g*tg + (63 - alpha_g)*(((*dst)>>5) & 0x003F) + 31)/63;
            b = (alpha_b*tb + (31 - alpha_b)*((*dst) & 0x001F) + 15)/31;
            *dst = (r << 11) | (g << 5) | b;
            /* next pixel */
            //dst++;
            /* new dest line */
            src += 3*bmp_pitch;
        }
    }
}



// Blend mono 1-bit bitmap (8 pixels per byte)
static inline void blendBitmap_monoTo32bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt32 color) {
    lUInt32 * dst;
    lUInt32 * dstline;
    const lUInt8* src;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt32*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        unsigned char mask = 0x80;
        for (int xx = width; xx>0; --xx) {
            if (*src & mask)
                *dst = color;
            mask >>= 1;
            if (0 == mask) {
                mask = 0x80;
                src++;
            }
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend gray 8-bit bitmap (1 byte per pixel)
static inline void blendBitmap_grayTo32bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt32 color) {
    lUInt32 tr = (color>>16);
    lUInt32 tg = ((color>>8) & 0x00FF);
    lUInt32 tb = (color & 0x00FF);
    lUInt32 * dst;
    lUInt32 * dstline;
    const lUInt8* src;
    lUInt32 alpha;
    lUInt32 r, g, b;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt32*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        for (int xx = width; xx>0; --xx) {
            alpha = *(src++);
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha*tr + (255 - alpha)*((*dst)>>16) + 127)/255;
            g = (alpha*tg + (255 - alpha)*(((*dst)>>8) & 0x00FF) + 127)/255;
            b = (alpha*tb + (255 - alpha)*((*dst) & 0x00FF) + 127)/255;
            *dst = (r << 16) | (g << 8) | b;
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel)
static inline void blendBitmap_rgbTo32bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt32 color) {
    lUInt32 tr = (color>>16);
    lUInt32 tg = ((color>>8) & 0x00FF);
    lUInt32 tb = (color & 0x00FF);
    lUInt32 * dst;
    lUInt32 * dstline;
    const lUInt8* src;
    lUInt32 alpha_r, alpha_g, alpha_b;
    lUInt32 r, g, b;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt32*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        for (int xx = width; xx>0; --xx) {
            alpha_r = *(src++);
            alpha_g = *(src++);
            alpha_b = *(src++);
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (255 - alpha_r)*((*dst)>>16) + 127)/255;
            g = (alpha_g*tg + (255 - alpha_g)*(((*dst)>>8) & 0x00FF) + 127)/255;
            b = (alpha_b*tb + (255 - alpha_b)*((*dst) & 0x00FF) + 127)/255;
            *dst = (r << 16) | (g << 8) | b;
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel)
static inline void blendBitmap_bgrTo32bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt32 color) {
    lUInt32 tr = (color>>16);
    lUInt32 tg = ((color>>8) & 0x00FF);
    lUInt32 tb = (color & 0x00FF);
    lUInt32 * dst;
    lUInt32 * dstline;
    const lUInt8* src;
    lUInt32 alpha_r, alpha_g, alpha_b;
    lUInt32 r, g, b;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt32*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        for (int xx = width; xx>0; --xx) {
            alpha_b = *(src++);
            alpha_g = *(src++);
            alpha_r = *(src++);
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (255 - alpha_r)*((*dst)>>16) + 127)/255;
            g = (alpha_g*tg + (255 - alpha_g)*(((*dst)>>8) & 0x00FF) + 127)/255;
            b = (alpha_b*tb + (255 - alpha_b)*((*dst) & 0x00FF) + 127)/255;
            *dst = (r << 16) | (g << 8) | b;
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel, vertical)
static inline void blendBitmap_rgbvTo32bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt32 color) {
    lUInt32 tr = (color>>16);
    lUInt32 tg = ((color>>8) & 0x00FF);
    lUInt32 tb = (color & 0x00FF);
    lUInt32 * dst;
    lUInt32 * dstline;
    const lUInt8* src;
    lUInt32 alpha_r, alpha_g, alpha_b;
    lUInt32 r, g, b;
    for (int xx = 0; xx<width; xx++) {
        src = bitmap;
        for (int yy = 0; yy < height; yy++) {
            dstline = ((lUInt32*)d->GetScanLine(y+yy)) + x + xx;
            dst = dstline;
            if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
                break;
            }
            alpha_r = src[xx];
            alpha_g = src[xx + bmp_pitch];
            alpha_b = src[xx + 2*bmp_pitch];
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (255 - alpha_r)*((*dst)>>16) + 127)/255;
            g = (alpha_g*tg + (255 - alpha_g)*(((*dst)>>8) & 0x00FF) + 127)/255;
            b = (alpha_b*tb + (255 - alpha_b)*((*dst) & 0x00FF) + 127)/255;
            *dst = (r << 16) | (g << 8) | b;
            /* next pixel */
            //dst++;
            /* new dest line */
            src += 3*bmp_pitch;
        }
    }
}

// Blend color 24-bit bitmap (3 bytes per pixel, vertical)
static inline void blendBitmap_bgrvTo32bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch, lUInt32 color) {
    lUInt32 tr = (color>>16);
    lUInt32 tg = ((color>>8) & 0x00FF);
    lUInt32 tb = (color & 0x00FF);
    lUInt32 * dst;
    lUInt32 * dstline;
    const lUInt8* src;
    lUInt32 alpha_r, alpha_g, alpha_b;
    lUInt32 r, g, b;
    for (int xx = 0; xx<width; xx++) {
        src = bitmap;
        for (int yy = 0; yy < height; yy++) {
            dstline = ((lUInt32*)d->GetScanLine(y+yy)) + x + xx;
            dst = dstline;
            if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
                break;
            }
            alpha_b = src[xx];
            alpha_g = src[xx + bmp_pitch];
            alpha_r = src[xx + 2*bmp_pitch];
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha_r*tr + (255 - alpha_r)*((*dst)>>16) + 127)/255;
            g = (alpha_g*tg + (255 - alpha_g)*(((*dst)>>8) & 0x00FF) + 127)/255;
            b = (alpha_b*tb + (255 - alpha_b)*((*dst) & 0x00FF) + 127)/255;
            *dst = (r << 16) | (g << 8) | b;
            /* next pixel */
            //dst++;
            /* new dest line */
            src += 3*bmp_pitch;
        }
    }
}

// Blend color 32-bit bitmap (4 bytes per pixel, BGRA)
static inline void blendBitmap_bgraTo32bpp(LVDrawBuf* d, int x, int y, const lUInt8 * bitmap, int width, int height, int bmp_pitch) {
    lUInt32 * dst;
    lUInt32 * dstline;
    const lUInt8* src;
    lUInt32 alpha;
    lUInt32 r, g, b;
    for (;height;height--) {
        src = bitmap;
        dstline = ((lUInt32*)d->GetScanLine(y++)) + x;
        dst = dstline;
        if ( !dst ) { // Should not happen, but avoid clang-tidy warning below
            break;
        }
        for (int xx = width; xx>0; --xx) {
            b = *(src++);
            g = *(src++);
            r = *(src++);
            alpha = *(src++);
            // BGRA bitmap from FreeType are premultipled
            // Unpremultiply:
            if (alpha > 0 && alpha < 255) {
                r = 255*r/alpha;
                g = 255*g/alpha;
                b = 255*b/alpha;
            }
            // blending function (OVER operator)
            // See https://www.freetype.org/freetype2/docs/reference/ft2-base_interface.html#ft_render_glyph
            r = (alpha*r + (255 - alpha)*((*dst)>>16) + 127)/255;
            g = (alpha*g + (255 - alpha)*(((*dst)>>8) & 0x00FF) + 127)/255;
            b = (alpha*b + (255 - alpha)*((*dst) & 0x00FF) + 127)/255;
            *dst = (r << 16) | (g << 8) | b;
            /* next pixel */
            dst++;
        }
        /* new dest line */
        bitmap += bmp_pitch;
    }
}


/// rotates buffer contents by specified angle
void LVColorDrawBuf::Rotate( cr_rotate_angle_t angle )
{
    if ( angle==CR_ROTATE_ANGLE_0 )
        return;
    if (!_data || _dx <= 0 || _dy <= 0)
        return;
    if ( _bpp==16 ) {
        int sz = (_dx * _dy);
        if ( angle==CR_ROTATE_ANGLE_180 ) {
            lUInt16 * buf = (lUInt16 *) _data;
            for ( int i=sz/2-1; i>=0; i-- ) {
                lUInt16 tmp = buf[i];
                buf[i] = buf[sz-i-1];
                buf[sz-i-1] = tmp;
            }
            return;
        }
        const ColorBufferLayout layout =
                getColorBufferLayout(_dy, _dx, _bpp);
        std::vector<lUInt8> rotated(layout.byteCount, 0);
        lUInt16 * dst =
                reinterpret_cast<lUInt16 *>(rotated.data());
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
        bool cw = angle!=CR_ROTATE_ANGLE_90;
#else
        bool cw = angle==CR_ROTATE_ANGLE_90;
#endif
        for ( int y=0; y<_dy; y++ ) {
            lUInt16 * src = (lUInt16*)_data + _dx*y;
            int nx, ny;
            if ( cw ) {
                nx = _dy - 1 - y;
            } else {
                nx = y;
            }
            for ( int x=0; x<_dx; x++ ) {
                if ( cw ) {
                    ny = x;
                } else {
                    ny = _dx - 1 - x;
                }
                dst[ _dy*ny + nx ] = src[ x ];
            }
        }
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
        if (_drawbmp) {
            memcpy(_data, rotated.data(), layout.byteCount);
        } else {
            _ownedData.swap(rotated);
            _data = _ownedData.data();
            _isBorrowed = false;
        }
#else
        _ownedData.swap(rotated);
        _data = _ownedData.data();
        _isBorrowed = false;
#endif
        _dx = layout.width;
        _dy = layout.height;
        _rowsize = layout.rowSize;
    } else {
        int sz = (_dx * _dy);
        if ( angle==CR_ROTATE_ANGLE_180 ) {
            lUInt32 * buf = (lUInt32 *) _data;
            for ( int i=sz/2-1; i>=0; i-- ) {
                lUInt32 tmp = buf[i];
                buf[i] = buf[sz-i-1];
                buf[sz-i-1] = tmp;
            }
            return;
        }
        const ColorBufferLayout layout =
                getColorBufferLayout(_dy, _dx, _bpp);
        std::vector<lUInt8> rotated(layout.byteCount, 0);
        lUInt32 * dst =
                reinterpret_cast<lUInt32 *>(rotated.data());
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
        bool cw = angle!=CR_ROTATE_ANGLE_90;
#else
        bool cw = angle==CR_ROTATE_ANGLE_90;
#endif
        for ( int y=0; y<_dy; y++ ) {
            lUInt32 * src = (lUInt32*)_data + _dx*y;
            int nx, ny;
            if ( cw ) {
                nx = _dy - 1 - y;
            } else {
                nx = y;
            }
            for ( int x=0; x<_dx; x++ ) {
                if ( cw ) {
                    ny = x;
                } else {
                    ny = _dx - 1 - x;
                }
                dst[ _dy*ny + nx ] = src[ x ];
            }
        }
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
        if (_drawbmp) {
            memcpy(_data, rotated.data(), layout.byteCount);
        } else {
            _ownedData.swap(rotated);
            _data = _ownedData.data();
            _isBorrowed = false;
        }
#else
        _ownedData.swap(rotated);
        _data = _ownedData.data();
        _isBorrowed = false;
#endif
        _dx = layout.width;
        _dy = layout.height;
        _rowsize = layout.rowSize;
    }
}

/// invert image
void  LVColorDrawBuf::Invert()
{
}

/// get buffer bits per pixel
int  LVColorDrawBuf::GetBitsPerPixel() const
{
    return _bpp;
}

void LVColorDrawBuf::Draw( LVImageSourceRef img, int x, int y, int width, int height, bool dither )
{
    //fprintf( stderr, "LVColorDrawBuf::Draw( img(%d, %d), %d, %d, %d, %d\n", img->GetWidth(), img->GetHeight(), x, y, width, height );
    if (img.isNull() || width <= 0 || height <= 0
            || img->GetWidth() <= 0 || img->GetHeight() <= 0
            || _dx <= 0 || _dy <= 0 || !_data)
        return;
    LVImageScaledDrawCallback drawcb( this, img, x, y, width, height, dither, _invertImages, _smoothImages );
    img->Decode( &drawcb );
    recordDrawnImage(width, height);
}

/// fills buffer with specified color
void LVColorDrawBuf::Clear( lUInt32 color )
{
    if ( _bpp==16 ) {
        lUInt16 cl16 = rgb888to565(color);
        for (int y=0; y<_dy; y++)
        {
            lUInt16 * dst = (lUInt16 *)GetScanLine(y);
            size_t px_count = _dx;
            while (px_count--)
            {
                *dst++ = cl16;
            }
        }
    } else {
        lUInt32 cl32 = color;
        for (int y=0; y<_dy; y++)
        {
            lUInt32 * dst = (lUInt32 *)GetScanLine(y);
            size_t px_count = _dx;
            while (px_count--)
            {
                *dst++ = cl32;
            }
        }
    }
}


/// get pixel value
lUInt32 LVColorDrawBuf::GetPixel( int x, int y ) const
{
    if (!_data || y<0 || x<0 || y>=_dy || x>=_dx)
        return 0;
    if ( _bpp==16 )
        return rgb565to888(((lUInt16*)GetScanLine(y))[x]);
    return ((lUInt32*)GetScanLine(y))[x];
}

/// fills rectangle with specified color
void LVColorDrawBuf::FillRect( int x0, int y0, int x1, int y1, lUInt32 color )
{
    if (x0<_clip.left)
        x0 = _clip.left;
    if (y0<_clip.top)
        y0 = _clip.top;
    if (x1>_clip.right)
        x1 = _clip.right;
    if (y1>_clip.bottom)
        y1 = _clip.bottom;
    if (x0>=x1 || y0>=y1)
        return;
    int alpha = (color >> 24) & 0xFF;
    if ( _bpp==16 ) {
        lUInt16 cl16 = rgb888to565(color);
        for (int y=y0; y<y1; y++)
        {
            lUInt16 * line = (lUInt16 *)GetScanLine(y);
            for (int x=x0; x<x1; x++)
            {
                if (alpha)
                    ApplyAlphaRGB565(line[x], cl16, alpha);
                else
                    line[x] = cl16;
            }
        }
    } else {
        for (int y=y0; y<y1; y++)
        {
            lUInt32 * line = (lUInt32 *)GetScanLine(y);
            for (int x=x0; x<x1; x++)
            {
                if (alpha)
                    ApplyAlphaRGB(line[x], color, alpha);
                else
                    line[x] = color;
            }
        }
    }
}

void LVColorDrawBuf::DrawLine(int x0,int y0,int x1,int y1,lUInt32 color0 ,int length1,int length2,int direction)
{
    if (x0<_clip.left)
        x0 = _clip.left;
    if (y0<_clip.top)
        y0 = _clip.top;
    if (x1>_clip.right)
        x1 = _clip.right;
    if (y1>_clip.bottom)
        y1 = _clip.bottom;
    if (x0>=x1 || y0>=y1)
        return;
    if ( _bpp==16 ) {
        lUInt16 cl16 = rgb888to565(color0);
        for (int y=y0; y<y1; y++)
        {
            lUInt16 * line = (lUInt16 *)GetScanLine(y);
            for (int x=x0; x<x1; x++)
            {
                if (direction==0 &&x%(length1+length2)<length1)line[x] = cl16;
                if (direction==1 &&y%(length1+length2)<length1)line[x] = cl16;
            }
        }
    } else {
        for (int y=y0; y<y1; y++)
        {
            lUInt32 * line = (lUInt32 *)GetScanLine(y);
            for (int x=x0; x<x1; x++)
            {
                if (direction==0 &&x%(length1+length2)<length1)line[x] = color0;
                if (direction==1 &&y%(length1+length2)<length1)line[x] = color0;
            }
        }
    }
}

/// fills rectangle with specified color
void LVColorDrawBuf::FillRectPattern( int x0, int y0, int x1, int y1, lUInt32 color0, lUInt32 color1, lUInt8 * pattern )
{
    if (x0<_clip.left)
        x0 = _clip.left;
    if (y0<_clip.top)
        y0 = _clip.top;
    if (x1>_clip.right)
        x1 = _clip.right;
    if (y1>_clip.bottom)
        y1 = _clip.bottom;
    if (x0>=x1 || y0>=y1)
        return;
    if ( _bpp==16 ) {
        lUInt16 cl16_0 = rgb888to565(color0);
        lUInt16 cl16_1 = rgb888to565(color1);
        for (int y=y0; y<y1; y++)
        {
            lUInt8 patternMask = pattern[y & 3];
            lUInt16 * line = (lUInt16 *)GetScanLine(y);
            for (int x=x0; x<x1; x++)
            {
                lUInt8 patternBit = (patternMask << (x&7)) & 0x80;
                line[x] = patternBit ? cl16_1 : cl16_0;
            }
        }
    } else {
        for (int y=y0; y<y1; y++)
        {
            lUInt8 patternMask = pattern[y & 3];
            lUInt32 * line = (lUInt32 *)GetScanLine(y);
            for (int x=x0; x<x1; x++)
            {
                lUInt8 patternBit = (patternMask << (x&7)) & 0x80;
                line[x] = patternBit ? color1 : color0;
            }
        }
    }
}

/// sets new size
void LVColorDrawBuf::Resize( int dx, int dy )
{
    if ( dx==_dx && dy==_dy ) {
    	//CRLog::trace("LVColorDrawBuf::Resize : no resize, not changed");
    	return;
    }
    if ( _isBorrowed ) {
    	//CRLog::trace("LVColorDrawBuf::Resize : no resize, own data");
        return;
    }
    const ColorBufferLayout layout =
            getColorBufferLayout(dx, dy, _bpp);
    //CRLog::trace("LVColorDrawBuf::Resize : resizing %d x %d to %d x %d", _dx, _dy, dx, dy);
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
    ScopedBitmap candidateBitmap;
    ScopedCompatibleDC candidateDC;
    lUInt8 * candidateData = NULL;
    if (layout.byteCount > 0) {
        BITMAPINFO bmi = { 0 };
        bmi.bmiHeader.biSize = sizeof(bmi.bmiHeader);
        bmi.bmiHeader.biWidth = layout.width;
        bmi.bmiHeader.biHeight = layout.height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = BI_RGB;
        bmi.bmiHeader.biSizeImage = 0;
        bmi.bmiHeader.biXPelsPerMeter = 1024;
        bmi.bmiHeader.biYPelsPerMeter = 1024;
        bmi.bmiHeader.biClrUsed = 0;
        bmi.bmiHeader.biClrImportant = 0;

        candidateBitmap.reset(CreateDIBSection(
                NULL, &bmi, DIB_RGB_COLORS,
                reinterpret_cast<void **>(&candidateData), NULL, 0));
        if (!candidateBitmap.get() || !candidateData)
            throw std::bad_alloc();
        candidateDC.reset(CreateCompatibleDC(NULL));
        if (!candidateDC.get())
            throw std::bad_alloc();
        SelectObject(candidateDC.get(), candidateBitmap.get());
        memset(candidateData, 0, layout.byteCount);
    }
    if (_drawdc)
        DeleteDC(_drawdc);
    if (_drawbmp)
        DeleteObject(_drawbmp);
    _drawdc = candidateDC.release();
    _drawbmp = candidateBitmap.release();
    std::vector<lUInt8> emptyStorage;
    _ownedData.swap(emptyStorage);
    _data = candidateData;
#else
    std::vector<lUInt8> candidate(layout.byteCount, 0);
    _ownedData.swap(candidate);
    _data = _ownedData.empty() ? NULL : _ownedData.data();
#endif
    _dx = layout.width;
    _dy = layout.height;
    _rowsize = layout.rowSize;
    SetClipRect( NULL );
}

void LVColorDrawBuf::InvertRect(int x0, int y0, int x1, int y1)
{
    if (x0<_clip.left)
        x0 = _clip.left;
    if (y0<_clip.top)
        y0 = _clip.top;
    if (x1>_clip.right)
        x1 = _clip.right;
    if (y1>_clip.bottom)
        y1 = _clip.bottom;
    if (x0>=x1 || y0>=y1)
        return;

    if (_bpp==16) {
        for (int y=y0; y<y1; y++) {
            lUInt16 * line = (lUInt16 *)GetScanLine(y);
            for (int x=x0; x<x1; x++) {
                line[x] ^= 0xFFFF;
            }
        }
    }
    else {
        for (int y=y0; y<y1; y++) {
            lUInt32 * line = (lUInt32 *)GetScanLine(y);
            for (int x=x0; x<x1; x++) {
                line[x] ^= 0x00FFFFFF;
            }
        }
    }
}

/// blend font bitmap using specified palette
void LVColorDrawBuf::BlendBitmap(int x, int y, const lUInt8 * bitmap, FontBmpPixelFormat bitmap_fmt, int width, int height, int bmp_pitch, lUInt32 * palette )
{
    if ( !_data )
        return;
    int initial_height = height;
    int bx = 0;
    int by = 0;
    lUInt32 bmpcl = palette?palette[0]:GetTextColor();

    if (x<_clip.left)
    {
        width += x-_clip.left;
        bx -= x-_clip.left;
        x = _clip.left;
        if (width<=0)
            return;
    }
    if (y<_clip.top)
    {
        height += y-_clip.top;
        by -= y-_clip.top;
        y = _clip.top;
        if (_hidePartialGlyphs && height<=initial_height/2) // HIDE PARTIAL VISIBLE GLYPHS
            return;
        if (height<=0)
            return;
    }
    if (x + width > _clip.right)
    {
        width = _clip.right - x;
    }
    if (width<=0 || !bitmap)
        return;
    if (y + height > _clip.bottom)
    {
        if (_hidePartialGlyphs && height<=initial_height/2) // HIDE PARTIAL VISIBLE GLYPHS
            return;
        int clip_bottom = _clip.bottom;
        if (_hidePartialGlyphs )
            clip_bottom = this->_dy;
        if ( y+height > clip_bottom)
            height = clip_bottom - y;
    }
    if (height<=0)
        return;

    bitmap += bx + by*bmp_pitch;

    if ( _bpp==16 ) {
        switch (bitmap_fmt) {
        case BMP_PIXEL_FORMAT_MONO:
            blendBitmap_monoTo16bpp(this, x, y, bitmap, width, height, bmp_pitch, rgb888to565(bmpcl));
            break;
        case BMP_PIXEL_FORMAT_GRAY:
            blendBitmap_grayTo16bpp(this, x, y, bitmap, width, height, bmp_pitch, rgb888to565(bmpcl));
            break;
        case BMP_PIXEL_FORMAT_GRAY2:
            // TODO: implement this
            break;
        case BMP_PIXEL_FORMAT_GRAY4:
            // TODO: implement this
            break;
        case BMP_PIXEL_FORMAT_RGB:
            blendBitmap_rgbTo16bpp(this, x, y, bitmap, width, height, bmp_pitch, rgb888to565(bmpcl));
            break;
        case BMP_PIXEL_FORMAT_BGR:
            blendBitmap_bgrTo16bpp(this, x, y, bitmap, width, height, bmp_pitch, rgb888to565(bmpcl));
            break;
        case BMP_PIXEL_FORMAT_RGB_V:
            blendBitmap_rgbvTo16bpp(this, x, y, bitmap, width, height, bmp_pitch, rgb888to565(bmpcl));
            break;
        case BMP_PIXEL_FORMAT_BGR_V:
            blendBitmap_bgrvTo16bpp(this, x, y, bitmap, width, height, bmp_pitch, rgb888to565(bmpcl));
            break;
        case BMP_PIXEL_FORMAT_BGRA:
            // TODO: implement this
            break;
        }
    } else {
        switch (bitmap_fmt) {
        case BMP_PIXEL_FORMAT_MONO:
            blendBitmap_monoTo32bpp(this, x, y, bitmap, width, height, bmp_pitch, bmpcl);
            break;
        case BMP_PIXEL_FORMAT_GRAY:
            blendBitmap_grayTo32bpp(this, x, y, bitmap, width, height, bmp_pitch, bmpcl);
            break;
        case BMP_PIXEL_FORMAT_GRAY2:
            // TODO: implement this
            break;
        case BMP_PIXEL_FORMAT_GRAY4:
            // TODO: implement this
            break;
        case BMP_PIXEL_FORMAT_RGB:
            blendBitmap_rgbTo32bpp(this, x, y, bitmap, width, height, bmp_pitch, bmpcl);
            break;
        case BMP_PIXEL_FORMAT_BGR:
            blendBitmap_bgrTo32bpp(this, x, y, bitmap, width, height, bmp_pitch, bmpcl);
            break;
        case BMP_PIXEL_FORMAT_RGB_V:
            blendBitmap_rgbvTo32bpp(this, x, y, bitmap, width, height, bmp_pitch, bmpcl);
            break;
        case BMP_PIXEL_FORMAT_BGR_V:
            blendBitmap_bgrvTo32bpp(this, x, y, bitmap, width, height, bmp_pitch, bmpcl);
            break;
        case BMP_PIXEL_FORMAT_BGRA:
            blendBitmap_bgraTo32bpp(this, x, y, bitmap, width, height, bmp_pitch);
            break;
        }
    }
}

#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
/// draws buffer content to DC doing color conversion if necessary
void LVColorDrawBuf::DrawTo( HDC dc, int x, int y, int options, lUInt32 * palette )
{
    if (dc!=NULL && _drawdc!=NULL)
        BitBlt( dc, x, y, _dx, _dy, _drawdc, 0, 0, SRCCOPY );
}
#endif

/// draws buffer content to another buffer doing color conversion if necessary
void LVColorDrawBuf::DrawTo( LVDrawBuf * buf, int x, int y, int options, lUInt32 * palette )
{
    CR_UNUSED(options);
    CR_UNUSED(palette);
    //
    if ( !_data )
        return;
    lvRect clip;
    buf->GetClipRect(&clip);
    int bpp = buf->GetBitsPerPixel();
    for (int yy=0; yy<_dy; yy++) {
        if (y+yy >= clip.top && y+yy < clip.bottom) {
            if ( _bpp==16 ) {
                lUInt16 * src = (lUInt16 *)GetScanLine(yy);
                if (bpp == 1) {
                    int shift = x & 7;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>3);
                    for (int xx=0; xx<_dx; xx++) {
                        if (x + xx >= clip.left && x + xx < clip.right) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0x8000)^0x8000) >> (shift+8);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0x8000)) >> (shift+8);
#endif
                            *dst |= cl;
                        }
                        if (!((shift = (shift + 1) & 7)))
                            dst++;
                        src++;
                    }
                } else if (bpp == 2) {
                    int shift = x & 3;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>2);
                    for (int xx=0; xx < _dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0xC000)^0xC000) >> ((shift<<1) + 8);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0xC000)) >> ((shift<<1) + 8);
#endif
                            *dst |= cl;
                        }
                        if (!((shift = ((shift + 1) & 3))))
                            dst++;
                        src++;
                    }
                } else if (bpp<=8) {
                    lUInt8 * dst = buf->GetScanLine(y+yy) + x;
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
                            *dst = (lUInt8)(*src >> 8);
                        }
                        dst++;
                        src++;
                    }
                } else if (bpp == 16) {
                    lUInt16 * dst = ((lUInt16 *)buf->GetScanLine(y + yy)) + x;
                    for (int xx=0; xx < _dx; xx++) {
                        if (x + xx >= clip.left && x + xx < clip.right) {
                            *dst = *src;
                        }
                        dst++;
                        src++;
                    }
                } else if (bpp == 32) {
                    lUInt32 * dst = ((lUInt32 *)buf->GetScanLine(y + yy)) + x;
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            *dst = rgb565to888( *src );
                        }
                        dst++;
                        src++;
                    }
                }
            } else {
                lUInt32 * src = (lUInt32 *)GetScanLine(yy);
                if (bpp==1) {
                    int shift = x & 7;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>3);
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0x80)^0x80) >> (shift);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0x80)) >> (shift);
#endif
                            *dst |= cl;
                        }
                        if (!((shift = (shift + 1) & 7)))
                            dst++;
                        src++;
                    }
                } else if (bpp==2) {
                    int shift = x & 3;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>2);
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0xC0)^0xC0) >> (shift<<1);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0xC0)) >> (shift<<1);
#endif
                            *dst |= cl;
                        }
                        if (!((shift = (shift + 1) & 3)))
                            dst++;
                        src++;
                    }
                } else if (bpp<=8) {
                    lUInt8 * dst = buf->GetScanLine(y + yy) + x;
                    for (int xx=0; xx<_dx; xx++) {
                        if (x + xx >= clip.left && x + xx < clip.right) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
                            *dst = (lUInt8)*src;
                        }
                        dst++;
                        src++;
                    }
                } else if (bpp == 32) {
                    lUInt32 * dst = ((lUInt32 *)buf->GetScanLine(y + yy)) + x;
                    for (int xx = 0; xx < _dx; xx++) {
                        if (x+xx >= clip.left && x + xx < clip.right) {
                            *dst = *src;
                        }
                        dst++;
                        src++;
                    }
                }
            }
        }
    }
}
/// draws buffer content on top of another buffer doing color conversion if necessary
void LVColorDrawBuf::DrawOnTop( LVDrawBuf * buf, int x, int y)
{
    //
    if ( !_data )
        return;
    lvRect clip;
    buf->GetClipRect(&clip);
    int bpp = buf->GetBitsPerPixel();
    for (int yy=0; yy<_dy; yy++) {
        if (y+yy >= clip.top && y+yy < clip.bottom) {
            if ( _bpp==16 ) {
                lUInt16 * src = (lUInt16 *)GetScanLine(yy);
                if (bpp == 1) {
                    int shift = x & 7;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>3);
                    for (int xx=0; xx<_dx; xx++) {
                        if (x + xx >= clip.left && x + xx < clip.right) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0x8000)^0x8000) >> (shift+8);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0x8000)) >> (shift+8);
#endif
                            if(cl!=0) *dst |= cl;
                        }
                        if (!((shift = (shift + 1) & 7)))
                            dst++;
                        src++;
                    }
                } else if (bpp == 2) {
                    int shift = x & 3;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>2);
                    for (int xx=0; xx < _dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0xC000)^0xC000) >> ((shift<<1) + 8);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0xC000)) >> ((shift<<1) + 8);
#endif
                            if(cl!=0) *dst |= cl;
                        }
                        if (!((shift = ((shift + 1) & 3))))
                            dst++;
                        src++;
                    }
                } else if (bpp<=8) {
                    lUInt8 * dst = buf->GetScanLine(y+yy) + x;
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
                            if(src!=0) *dst = (lUInt8)(*src >> 8);
                        }
                        dst++;
                        src++;
                    }
                } else if (bpp == 16) {
                    lUInt16 * dst = ((lUInt16 *)buf->GetScanLine(y + yy)) + x;
                    for (int xx=0; xx < _dx; xx++) {
                        if (x + xx >= clip.left && x + xx < clip.right) {
                            if(src!=0) *dst = *src;
                        }
                        dst++;
                        src++;
                    }
                } else if (bpp == 32) {
                    lUInt32 * dst = ((lUInt32 *)buf->GetScanLine(y + yy)) + x;
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            if(src!=0) *dst = rgb565to888( *src );
                        }
                        dst++;
                        src++;
                    }
                }
            } else {
                lUInt32 * src = (lUInt32 *)GetScanLine(yy);
                if (bpp==1) {
                    int shift = x & 7;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>3);
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0x80)^0x80) >> (shift);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0x80)) >> (shift);
#endif
                            if(*src!=0) *dst |= cl;
                        }
                        if (!((shift = (shift + 1) & 7)))
                            dst++;
                        src++;
                    }
                } else if (bpp==2) {
                    int shift = x & 3;
                    lUInt8 * dst = buf->GetScanLine(y+yy) + (x>>2);
                    for (int xx=0; xx<_dx; xx++) {
                        if ( x+xx >= clip.left && x+xx < clip.right ) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
#if (GRAY_INVERSE==1)
                            lUInt8 cl = (((lUInt8)(*src)&0xC0)^0xC0) >> (shift<<1);
#else
                            lUInt8 cl = (((lUInt8)(*src)&0xC0)) >> (shift<<1);
#endif
                            if(*src!=0) *dst |= cl;
                        }
                        if (!((shift = (shift + 1) & 3)))
                            dst++;
                        src++;
                    }
                } else if (bpp<=8) {
                    lUInt8 * dst = buf->GetScanLine(y + yy) + x;
                    for (int xx=0; xx<_dx; xx++) {
                        if (x + xx >= clip.left && x + xx < clip.right) {
                            //lUInt8 mask = ~((lUInt8)0xC0>>shift);
                            if(*src!=0) *dst = (lUInt8)*src;
                        }
                        dst++;
                        src++;
                    }
                } else if (bpp == 16) {
                    lUInt16 * dst = ((lUInt16 *)buf->GetScanLine(y + yy)) + x;
                    for (int xx=0; xx < _dx; xx++) {
                        if (x + xx >= clip.left && x + xx < clip.right) {
                            if(src!=0) *dst = rgb888to565(*src);
                        }
                        dst++;
                        src++;
                    }
                } else if (bpp == 32) {
                    lUInt32 * dst = ((lUInt32 *)buf->GetScanLine(y + yy)) + x;
                    for (int xx = 0; xx < _dx; xx++) {
                        if (x+xx >= clip.left && x + xx < clip.right) {
                            if(*src!=0) *dst = *src;
                        }
                        dst++;
                        src++;
                    }
                }
            }
        }
    }
}

/// draws rescaled buffer content to another buffer doing color conversion if necessary
void LVColorDrawBuf::DrawRescaled(LVDrawBuf * src, int x, int y, int dx, int dy, int options)
{
    CR_UNUSED(options);
    if (dx < 1 || dy < 1)
        return;
    lvRect clip;
    GetClipRect(&clip);
    int srcdx = src->GetWidth();
    int srcdy = src->GetHeight();
    bool linearInterpolation = (srcdx <= dx || srcdy <= dy);
	for (int yy=0; yy<dy; yy++) {
		if (y+yy >= clip.top && y+yy < clip.bottom)	{
			if (linearInterpolation) {
				// linear interpolation
				int srcy16 = srcdy * yy * 16 / dy;
				for (int xx=0; xx<dx; xx++)	{
					if ( x+xx >= clip.left && x+xx < clip.right ) {
						int srcx16 = srcdx * xx * 16 / dx;
						lUInt32 cl = src->GetInterpolatedColor(srcx16, srcy16);
                        if (_bpp == 16) {
							lUInt16 * dst = (lUInt16 *)GetScanLine(y + yy);
							dst[x + xx] = rgb888to565(cl);
						} else {
							lUInt32 * dst = (lUInt32 *)GetScanLine(y + yy);
							dst[x + xx] = cl;
						}
					}
				}
			} else {
				// area average
				lvRect srcRect;
				srcRect.top = srcdy * yy * 16 / dy;
				srcRect.bottom = srcdy * (yy + 1) * 16 / dy;
				for (int xx=0; xx<dx; xx++)	{
					if ( x+xx >= clip.left && x+xx < clip.right ) {
						srcRect.left = srcdx * xx * 16 / dx;
						srcRect.right = srcdx * (xx + 1) * 16 / dx;
						lUInt32 cl = src->GetAvgColor(srcRect);
                        if (_bpp == 16) {
							lUInt16 * dst = (lUInt16 *)GetScanLine(y + yy);
							dst[x + xx] = rgb888to565(cl);
						} else {
							lUInt32 * dst = (lUInt32 *)GetScanLine(y + yy);
							dst[x + xx] = cl;
						}
					}
				}
			}
		}
	}
}

/// returns scanline pointer
lUInt8 * LVColorDrawBuf::GetScanLine( int y ) const
{
    if (!_data || y<0 || y>=_dy)
        return NULL;
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
    return _data + _rowsize * (_dy-1-y);
#else
    return _data + _rowsize * y;
#endif
}

/// returns white pixel value
lUInt32 LVColorDrawBuf::GetWhiteColor() const
{
    return 0xFFFFFF;
}
/// returns black pixel value
lUInt32 LVColorDrawBuf::GetBlackColor() const
{
    return 0x000000;
}


/// constructor
LVColorDrawBuf::LVColorDrawBuf(int dx, int dy, int bpp)
:     LVBaseDrawBuf()
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
    ,_drawdc(NULL)
    ,_drawbmp(NULL)
#endif
    ,_bpp(bpp)
    ,_isBorrowed(false)
{
    Resize( dx, dy ); // NOLINT: Call to virtual function during construction
}

/// creates wrapper around external RGBA buffer
LVColorDrawBuf::LVColorDrawBuf(int dx, int dy, lUInt8 * externalBuffer, int bpp )
:     LVBaseDrawBuf()
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
    ,_drawdc(NULL)
    ,_drawbmp(NULL)
#endif
    ,_bpp(bpp)
    ,_isBorrowed(true)
{
    const ColorBufferLayout layout =
            getColorBufferLayout(dx, dy, _bpp);
    _dx = layout.width;
    _dy = layout.height;
    _rowsize = layout.rowSize;
    _data = layout.byteCount == 0 ? NULL : externalBuffer;
    SetClipRect( NULL );
}

/// destructor
LVColorDrawBuf::~LVColorDrawBuf()
{
#if !defined(__SYMBIAN32__) && defined(_WIN32) && !defined(QT_GL)
    if (_drawdc)
        DeleteDC(_drawdc);
    if (_drawbmp)
        DeleteObject(_drawbmp);
#endif
    _data = NULL;
}

/// convert to 1-bit bitmap
void LVColorDrawBuf::ConvertToBitmap(bool flgDither)
{
    // not implemented
    CR_UNUSED(flgDither);
}
