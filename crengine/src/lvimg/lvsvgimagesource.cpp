/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2017,2021 poire-z <poire-z@users.noreply.github.com>    *
 *   Copyright (C) 2019,2021 NiLuJe <ninuje@gmail.com>                     *
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

#include "lvsvgimagesource.h"

#if (USE_NANOSVG==1)

#include "lvimagedecodercallback.h"

// Support for SVG
#include <cstring>
#include <math.h>
#include <limits>
#include <memory>
#include <stdio.h>
#include <vector>
#define NANOSVG_ALL_COLOR_KEYWORDS
#define NANOSVG_IMPLEMENTATION
#define NANOSVGRAST_IMPLEMENTATION
#define STB_IMAGE_WRITE_IMPLEMENTATION
#define STB_IMAGE_WRITE_STATIC
#include <nanosvg.h>
#include <nanosvgrast.h>
#include <stb_image_write.h> // for svg to png conversion

namespace {

struct NsvgImageDeleter {
    void operator()(NSVGimage *image) const noexcept {
        if (image)
            nsvgDelete(image);
    }
};

struct NsvgRasterizerDeleter {
    void operator()(NSVGrasterizer *rasterizer) const noexcept {
        if (rasterizer)
            nsvgDeleteRasterizer(rasterizer);
    }
};

using NsvgImageOwner = std::unique_ptr<NSVGimage, NsvgImageDeleter>;
using NsvgRasterizerOwner =
        std::unique_ptr<NSVGrasterizer, NsvgRasterizerDeleter>;

bool resizeRgbaBuffer(
        int width, int height,
        std::vector<unsigned char> &buffer) {
    if (width <= 0 || height <= 0)
        return false;
    if (width > std::numeric_limits<int>::max() / 4
            || static_cast<std::size_t>(width)
                    > std::numeric_limits<std::size_t>::max()
                            / static_cast<std::size_t>(height))
        return false;
    const std::size_t pixelCount =
            static_cast<std::size_t>(width)
            * static_cast<std::size_t>(height);
    if (pixelCount
            > std::numeric_limits<std::size_t>::max() / 4)
        return false;
    try {
        buffer.resize(pixelCount * 4);
    } catch (const std::bad_alloc &) {
        return false;
    } catch (const std::length_error &) {
        return false;
    }
    return true;
}

}

LVSvgImageSource::LVSvgImageSource( ldomNode * node, LVStreamRef stream )
        : LVNodeImageSource(node, stream)
{
}
LVSvgImageSource::~LVSvgImageSource() {}

void LVSvgImageSource::Compact() { }

bool LVSvgImageSource::CheckPattern( const lUInt8 * buf, int len)
{
    // check for <?xml or <svg
    if (len > 5 && buf[0]=='<' && buf[1]=='?' &&
            (buf[2]=='x' || buf[2] == 'X') &&
            (buf[3]=='m' || buf[3] == 'M') &&
            (buf[4]=='l' || buf[4] == 'L'))
        return true;
    if (len > 4 && buf[0]=='<' &&
            (buf[1]=='s' || buf[1] == 'S') &&
            (buf[2]=='v' || buf[2] == 'V') &&
            (buf[3]=='g' || buf[3] == 'G'))
        return true;
    return false;
}

bool LVSvgImageSource::Decode( LVImageDecoderCallback * callback )
{
    if ( _stream.isNull() )
        return false;
    lvsize_t sz = _stream->GetSize();
    // if ( sz<32 || sz>0x80000 ) return false; // do not impose (yet) a max size for svg
    std::vector<lUInt8> buf(sz + 1);
    lvsize_t bytesRead = 0;
    bool res = true;
    _stream->SetPos(0);
    if ( _stream->Read( buf.data(), sz, &bytesRead )!=LVERR_OK || bytesRead!=sz ) {
        res = false;
    }
    else {
        buf[sz] = 0;
        res = DecodeFromBuffer( buf.data(), sz, callback );
    }
    return res;
}

int LVSvgImageSource::DecodeFromBuffer(unsigned char *buf, int /*buf_size*/, LVImageDecoderCallback * callback)
{
    int w, h;
    bool res = false;

    // printf("SVG: parsing...\n");
    NsvgImageOwner image(nsvgParse((char*)buf, "px", 96.0f));
    if (!image) {
        printf("SVG: could not parse SVG stream.\n");
        return res;
    }

    if (!std::isfinite(image->width)
            || !std::isfinite(image->height)
            || image->width <= 0 || image->height <= 0
            || static_cast<double>(image->width)
                    > static_cast<double>(
                            std::numeric_limits<int>::max()) - 2.0
            || static_cast<double>(image->height)
                    > static_cast<double>(
                            std::numeric_limits<int>::max()) - 2.0) {
        printf("SVG: invalid image dimensions.\n");
        return res;
    }
    w = (int)image->width;
    h = (int)image->height;
    // The rasterizer (while antialiasing?) has a tendency to eat the last
    // right and bottom pixel. We can avoid that by adding 1 pixel around
    // each side, by increasing width and height with 2 here, and using
    // offsets of 1 in nsvgRasterize
    w += 2;
    h += 2;
    _width = w;
    _height = h;

    // int nbshapes = 0;
    // for (NSVGshape *shape = image->shapes; shape != NULL; shape = shape->next) nbshapes++;
    // printf("SVG: nb of shapes: %d\n", nbshapes);
    if (! image->shapes) {
        // If no supported shapes, it will be a blank empty image.
        // Better to let user know that with an unsupported image display (empty
        // square with borders).
        // But commented to not flood koreader's log for books with many such
        // svg images (crengine would log this at each page change)
        // printf("SVG: got image with zero supported shape.\n");
        return res;
    }

    if ( ! callback ) { // If no callback provided, only size is wanted.
        res = true;
    }
    else {
        NsvgRasterizerOwner rasterizer(nsvgCreateRasterizer());
        if (!rasterizer) {
            printf("SVG: could not init rasterizer.\n");
        }
        else {
            std::vector<unsigned char> pixels;
            if (!resizeRgbaBuffer(w, h, pixels)) {
                printf("SVG: could not alloc image buffer.\n");
            }
            else {
                // printf("SVG: rasterizing image %d x %d\n", w, h);
                nsvgRasterize(
                        rasterizer.get(), image.get(), 1, 1, 1,
                        pixels.data(), w, h, w * 4); // offsets of 1 pixel, scale = 1
                // stbi_write_png("/tmp/svg.png", w, h, 4, img, w*4); // for debug
                callback->OnStartDecode(this);
                const unsigned char *src = pixels.data();
                std::vector<lUInt32> row(_width);
                bool accepted = true;
                for (int y=0; y<_height; y++) {
                    size_t px_count = _width;
                    lUInt32 * dst = row.data();
                    while (px_count--) {
                        // nanosvg outputs straight RGBA; lvimg expects BGRA, with inverted alpha,
                        lUInt32 cl;
                        std::memcpy(&cl, src, sizeof(cl));
                        src += sizeof(cl);
                        cl ^= 0xFF000000;
                        *dst++ = ((cl<<16)&0x00FF0000) | ((cl>>16)&0x000000FF) | (cl&0xFF00FF00);
                    }
                    if (!callback->OnLineDecoded(
                                this, y, row.data())) {
                        accepted = false;
                        break;
                    }
                }
                callback->OnEndDecode(this, !accepted);
                res = accepted;
            }
        }
    }
    return res;
}

// Convenience function to convert SVG image data to PNG
unsigned char * convertSVGtoPNG(unsigned char *svg_data, int /*svg_data_size*/, float zoom_factor, int *png_data_len)
{
    int w, h, pw, ph;
    if (png_data_len)
        *png_data_len = 0;
    if (!svg_data || !png_data_len
            || !std::isfinite(zoom_factor)
            || zoom_factor <= 0
            || static_cast<double>(zoom_factor)
                    > static_cast<double>(
                            std::numeric_limits<int>::max()))
        return NULL;

    // printf("SVG: converting to PNG...\n");
    NsvgImageOwner image(
            nsvgParse((char*)svg_data, "px", 96.0f));
    if (!image) {
        printf("SVG: could not parse SVG stream.\n");
        return NULL;
    }

    if (! image->shapes) {
        printf("SVG: got image with zero supported shape.\n");
        return NULL;
    }

    if (!std::isfinite(image->width)
            || !std::isfinite(image->height)
            || image->width <= 0 || image->height <= 0
            || static_cast<double>(image->width)
                    > static_cast<double>(
                            std::numeric_limits<int>::max())
            || static_cast<double>(image->height)
                    > static_cast<double>(
                            std::numeric_limits<int>::max()))
        return NULL;
    w = (int)image->width;
    h = (int)image->height;
    // The rasterizer (while antialiasing?) has a tendency to eat some of the
    // right and bottom pixels. We can avoid that by adding N pixels around
    // each side, by increasing width and height with 2*N here, and using
    // offsets of N in nsvgRasterize. Using zoom_factor as N gives nice results.
    int offset = zoom_factor;
    const double scaledWidth =
            static_cast<double>(w) * zoom_factor + 2.0 * offset;
    const double scaledHeight =
            static_cast<double>(h) * zoom_factor + 2.0 * offset;
    if (scaledWidth <= 0 || scaledHeight <= 0
            || scaledWidth > std::numeric_limits<int>::max()
            || scaledHeight > std::numeric_limits<int>::max())
        return NULL;
    pw = static_cast<int>(scaledWidth);
    ph = static_cast<int>(scaledHeight);
    NsvgRasterizerOwner rasterizer(nsvgCreateRasterizer());
    if (!rasterizer) {
        printf("SVG: could not init rasterizer.\n");
    }
    else {
        std::vector<unsigned char> pixels;
        if (!resizeRgbaBuffer(pw, ph, pixels)) {
            printf("SVG: could not alloc image buffer.\n");
        }
        else {
            // printf("SVG: rasterizing to png image %d x %d\n", pw, ph);
            nsvgRasterize(
                    rasterizer.get(), image.get(),
                    offset, offset, zoom_factor,
                    pixels.data(), pw, ph, pw * 4);
            return stbi_write_png_to_mem(
                    pixels.data(), pw * 4,
                    pw, ph, 4, png_data_len);
        }
    }
    return NULL;
}

#endif  // (USE_NANOSVG==1)
