/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2009,2012 Vadim Lopatin <coolreader.org@gmail.com> *
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

#include "lvxpmimagesource.h"
#include "lvimagedecodercallback.h"

#include <cerrno>
#include <cstdlib>
#include <cstring>
#include <limits>
#include <sstream>

namespace {

const lUInt8 invalidColorIndex = 0xff;

bool parseXpmColor(const char *value, lUInt32 &color)
{
    if (!value)
        return false;
    if (value[0] == '#') {
        const std::size_t digitCount = std::strlen(value + 1);
        if (digitCount != 6 && digitCount != 8)
            return false;
        errno = 0;
        char *end = NULL;
        const unsigned long parsed = std::strtoul(value + 1, &end, 16);
        if (errno == ERANGE || !end || *end != '\0'
                || parsed > std::numeric_limits<lUInt32>::max())
            return false;
        color = static_cast<lUInt32>(parsed);
        return true;
    }
    if (!std::strcmp(value, "None"))
        color = 0xFF000000;
    else if (!std::strcmp(value, "Black"))
        color = 0x000000;
    else if (!std::strcmp(value, "White"))
        color = 0xFFFFFF;
    else
        return false;
    return true;
}

} // namespace

LVXPMImageSource::LVXPMImageSource(const char **data)
    : _width(0), _height(0), _ncolors(0)
{
    _pchars.fill(invalidColorIndex);
    if (!data || !data[0])
        return;

    int width = 0;
    int height = 0;
    int colorCount = 0;
    int charsPerPixel = 0;
    std::istringstream header(data[0]);
    if (!(header >> width >> height >> colorCount >> charsPerPixel))
        return;
    header >> std::ws;
    if (!header.eof()
            || width <= 0 || width >= 255
            || height <= 0 || height >= 255
            || colorCount < 2 || colorCount >= 255
            || charsPerPixel != 1)
        return;

    std::vector<lUInt32> palette(colorCount);
    std::array<lUInt8, 256> paletteIndexes;
    paletteIndexes.fill(invalidColorIndex);
    for (int color = 0; color < colorCount; color++) {
        const char *line = data[1 + color];
        if (!line || std::strlen(line) < 5
                || line[1] != ' ' || line[2] != 'c' || line[3] != ' ')
            return;
        const unsigned char symbol =
                static_cast<unsigned char>(line[0]);
        if (paletteIndexes[symbol] != invalidColorIndex
                || !parseXpmColor(line + 4, palette[color]))
            return;
        paletteIndexes[symbol] = static_cast<lUInt8>(color);
    }

    std::vector<std::vector<char>> rows(
            height, std::vector<char>(width));
    for (int y = 0; y < height; y++) {
        const char *line = data[1 + colorCount + y];
        if (!line || std::strlen(line) < static_cast<std::size_t>(width))
            return;
        for (int x = 0; x < width; x++) {
            const unsigned char symbol =
                    static_cast<unsigned char>(line[x]);
            if (paletteIndexes[symbol] == invalidColorIndex)
                return;
            rows[y][x] = line[x];
        }
    }

    _width = width;
    _height = height;
    _ncolors = colorCount;
    _palette.swap(palette);
    _rows.swap(rows);
    _pchars = paletteIndexes;
}

LVXPMImageSource::~LVXPMImageSource() = default;

bool LVXPMImageSource::Decode(LVImageDecoderCallback *callback)
{
    if (_width <= 0 || _height <= 0
            || _rows.size() != static_cast<std::size_t>(_height)
            || _palette.size() != static_cast<std::size_t>(_ncolors))
        return false;
    if (!callback)
        return true;

    callback->OnStartDecode(this);
    std::vector<lUInt32> row(_width);
    for (int y = 0; y < _height; y++) {
        const char *src = _rows[y].data();
        for (int x = 0; x < _width; x++) {
            const lUInt8 color =
                    _pchars[static_cast<unsigned char>(src[x])];
            if (color >= _palette.size()) {
                callback->OnEndDecode(this, true);
                return false;
            }
            row[x] = _palette[color];
        }
        if (!callback->OnLineDecoded(this, y, row.data())) {
            callback->OnEndDecode(this, true);
            return false;
        }
    }
    callback->OnEndDecode(this, false);
    return true;
}
