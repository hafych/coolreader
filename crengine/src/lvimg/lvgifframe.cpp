/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2008,2011,2012,2015 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2010 Kirill Erofeev <erofeev.info@gmail.com>            *
 *   Copyright (C) 2017 Yifei(Frank) ZHU <fredyifei@gmail.com>             *
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

#include "lvgifframe.h"

#if (USE_GIF==1)

#include "lvimagedecodercallback.h"
#include "lvgifimagesource.h"
#include "clzwdecoder.h"

#include <algorithm>

inline lUInt32 lRGB(lUInt32 r, lUInt32 g, lUInt32 b )
{
    return (r<<16)|(g<<8)|b;
}


lUInt32 *LVGifFrame::GetColorTable() {
    if (m_flg_ltc)
        return m_local_color_table.data();
    else
        return m_pImage->GetColorTable();
}

bool LVGifFrame::Draw(LVImageDecoderCallback *callback)
{
    if (!m_pImage || !callback)
        return false;
    int w = m_pImage->GetWidth();
    int h = m_pImage->GetHeight();
    if ( w<=0 || w>4096 || h<=0 || h>4096 )
        return false; // wrong image width
    if (m_cx <= 0 || m_cy <= 0
            || m_buffer.size() != static_cast<std::size_t>(m_cx) * m_cy)
        return false;

    lUInt32 *pColorTable = GetColorTable();
    const std::size_t colorCount = m_flg_ltc
            ? m_local_color_table.size()
            : m_pImage->m_global_color_table.size();
    const unsigned int backgroundColor = m_pImage->m_background_color;
    const unsigned int transparentColor = m_pImage->m_transparent_color;
    const bool definedTransparent = m_pImage->defined_transparent_color;
    if (!pColorTable || backgroundColor >= colorCount)
        return false;
    for (std::size_t i = 0; i < m_buffer.size(); i++) {
        const unsigned int color = m_buffer[i];
        if ((!definedTransparent || color != transparentColor)
                && color >= colorCount)
            return false;
    }

    std::vector<int> sourceRows;
    if (m_flg_interlaced) {
        sourceRows.assign(m_cy, -1);
        static const int starts[] = {0, 4, 2, 1};
        static const int steps[] = {8, 8, 4, 2};
        int sourceRow = 0;
        for (int pass = 0; pass < 4; pass++) {
            for (int destinationRow = starts[pass];
                    destinationRow < m_cy; destinationRow += steps[pass])
                sourceRows[destinationRow] = sourceRow++;
        }
        if (sourceRow != m_cy)
            return false;
    }

    callback->OnStartDecode(m_pImage);
    std::vector<lUInt32> line(w);
    for (int y = 0; y < h; y++) {
        std::fill(line.begin(), line.end(), pColorTable[backgroundColor]);
        if (y >= m_top && y < m_top + m_cy) {
            const int frameRow = y - m_top;
            const int sourceRow = m_flg_interlaced
                    ? sourceRows[frameRow] : frameRow;
            const unsigned char *pLine =
                    m_buffer.data() + sourceRow * m_cx;
            for (int x = 0; x < m_cx; x++) {
                const unsigned int color = pLine[x];
                if (definedTransparent && color == transparentColor)
                    line[x + m_left] = 0xFF000000;
                else
                    line[x + m_left] = pColorTable[color];
            }
        }
        if (!callback->OnLineDecoded(
                    m_pImage, y, line.data())) {
            callback->OnEndDecode(m_pImage, true);
            return false;
        }
    }
    callback->OnEndDecode(m_pImage, false);
    return true;
}

int LVGifFrame::DecodeFromBuffer( unsigned char * buf, int buf_size, int &bytes_read )
{
    bytes_read = 0;
    if (!m_pImage || !buf || buf_size <= 10 || buf[0] != ',')
        return 0;
    unsigned char * p = buf;
    p++;

    // read info
    m_left = p[0] + (((unsigned int)p[1])<<8);
    m_top = p[2] + (((unsigned int)p[3])<<8);
    m_cx = p[4] + (((unsigned int)p[5])<<8);
    m_cy = p[6] + (((unsigned int)p[7])<<8);

    if (m_cx<1 || m_cx>4096 ||
        m_cy<1 || m_cy>4096 ||
        m_left+m_cx>m_pImage->GetWidth() ||
        m_top+m_cy>m_pImage->GetHeight())
        return 0; // error: wrong size

    m_flg_ltc = (p[8]&0x80)?1:0;
    m_flg_interlaced = (p[8]&0x40)?1:0;
    m_bpp = (p[8]&0x7) + 1;

    if (m_bpp==1)
        m_bpp = m_pImage->m_bpp;
    else if (m_bpp!=m_pImage->m_bpp && !m_flg_ltc)
        return 0; // wrong color table

    // next
    p+=9;

    if (m_flg_ltc) {
        // read color table
        int m_color_count = 1<<m_bpp;

        if (m_color_count * 3 >= buf_size - (p - buf))
            return 0; // error

        m_local_color_table.resize(m_color_count);
        for (int i=0; i<m_color_count; i++) {
            m_local_color_table[i] = lRGB(p[i*3],p[i*3+1],p[i*3+2]);
            //m_local_color_table[i] = lRGB(p[i*3+2],p[i*3+1],p[i*3+0]);
        }
        // next
        p+=(m_color_count * 3);
    }

    // unpack image
    int stream_buffer_size = 0;

    int size_code = *p++;
    if (size_code < 2 || size_code > 8)
        return 0;

    const int blocksSize = static_cast<int>(buf_size - (p - buf));
    int blockOffset = 0;
    for (;;) {
        if (blockOffset >= blocksSize)
            return 0;
        const int blockSize = p[blockOffset++];
        if (blockSize == 0)
            break;
        if (blockSize > blocksSize - blockOffset)
            return 0;
        stream_buffer_size += blockSize;
        blockOffset += blockSize;
    }

    if (!stream_buffer_size)
        return 0; // error

    // set read bytes count
    bytes_read = static_cast<int>((p - buf) + blockOffset);

    // create stream buffer
    std::vector<unsigned char> stream_buffer(stream_buffer_size);
    // copy data to stream buffer
    int sb_index = 0;
    int sourceOffset = 0;
    while (sourceOffset < blockOffset) {
        const int blockSize = p[sourceOffset++];
        if (blockSize == 0)
            break;
        std::copy(p + sourceOffset, p + sourceOffset + blockSize,
                stream_buffer.begin() + sb_index);
        sb_index += blockSize;
        sourceOffset += blockSize;
    }


    // create image buffer
    m_buffer.resize(m_cx * m_cy);

    // decode image to buffer
    CLZWDecoder decoder;
    decoder.SetInputStream( stream_buffer.data(), stream_buffer_size );
    decoder.SetOutputStream( m_buffer.data(), m_cx*m_cy );

    int res=0;

    if (decoder.Decode(size_code)) {
        // decoded Ok
        // fill rest with transparent color
        decoder.FillRestOfOutStream( m_pImage->m_transparent_color );
        res = 1;
    } else {
        // error
        m_buffer.clear();
    }

    return res; // OK
}

LVGifFrame::LVGifFrame(LVGifImageSource * pImage)
{
    m_pImage = pImage;
    m_left = 0;
    m_top = 0;
    m_cx = 0;
    m_cy = 0;
    m_flg_ltc = 0; // GTC (gobal table of colors) flag
}

LVGifFrame::~LVGifFrame()
{
    Clear();
}

void LVGifFrame::Clear()
{
    m_buffer.clear();
    m_local_color_table.clear();
}

#endif  // (USE_GIF==1)
