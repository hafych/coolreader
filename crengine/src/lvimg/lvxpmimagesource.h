/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007 Vadim Lopatin <coolreader.org@gmail.com>           *
 *   Copyright (C) 2021 NiLuJe <ninuje@gmail.com>                          *
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

#ifndef __LVXPMIMAGESOURCE_H_INCLUDED__
#define __LVXPMIMAGESOURCE_H_INCLUDED__

#include "lvimagesource.h"
#include <array>
#include <vector>

class LVXPMImageSource : public LVImageSource
{
protected:
    std::vector<std::vector<char>> _rows;
    std::vector<lUInt32> _palette;
    std::array<lUInt8, 256> _pchars;
    int _width;
    int _height;
    int _ncolors;
public:
    LVXPMImageSource( const char ** data );
    virtual ~LVXPMImageSource();

    ldomNode * GetSourceNode() { return NULL; }
    virtual LVStream * GetSourceStream() { return NULL; }
    virtual void   Compact() { }
    virtual int    GetWidth() const { return _width; }
    virtual int    GetHeight() const { return _height; }
    virtual bool   Decode( LVImageDecoderCallback * callback );
};

#endif  // __LVXPMIMAGESOURCE_H_INCLUDED__
