/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2008,2009,2011-2013,2015 Vadim Lopatin <coolreader.org@gmail.com>
 *   Copyright (C) 2010 Kirill Erofeev <erofeev.info@gmail.com>            *
 *   Copyright (C) 2017 Yifei(Frank) ZHU <fredyifei@gmail.com>             *
 *   Copyright (C) 2018 Aleksey Chernov <valexlin@gmail.com>               *
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

#include "lvgifimagesource.h"

#if (USE_GIF==1)

#include "lvgifframe.h"

static bool skipGifExtension(unsigned char *&buf, int buf_size) {
    if (!buf || buf_size < 2 || buf[0] != '!')
        return false;

    int offset = 2;
    while (offset < buf_size) {
        const unsigned blockSize = buf[offset++];
        if (blockSize == 0)
        {
            buf += offset;
            return true;
        }
        if (blockSize > static_cast<unsigned>(buf_size - offset))
            return false;
        offset += static_cast<int>(blockSize);
    }
    return false;
}

inline lUInt32 lRGB(lUInt32 r, lUInt32 g, lUInt32 b )
{
    return (r<<16)|(g<<8)|b;
}


bool LVGifImageSource::CheckPattern(const lUInt8 *buf, int buf_size)
{
    if (!buf || buf_size < 6)
        return false;
    if (buf[0]!='G' || buf[1]!='I' || buf[2]!='F')
        return false;
    // version: '87a' or '89a'
    if (buf[3]!='8' || buf[5]!='a')
        return false;
    if (buf[4]!='7' && buf[4]!='9')
        return false; // bad version
    return true;
}

LVGifImageSource::LVGifImageSource(ldomNode *node, LVStreamRef stream)
    : LVNodeImageSource(node, stream)
{
    Clear();
}

LVGifImageSource::~LVGifImageSource()
{
    Clear();
}

lUInt32 *LVGifImageSource::GetColorTable() {
    if (m_flg_gtc)
        return m_global_color_table.data();
    else
        return NULL;
}

void LVGifImageSource::Compact()
{
    // TODO: implement compacting
}

int LVGifImageSource::DecodeFromBuffer(unsigned char *buf, int buf_size, LVImageDecoderCallback * callback)
{
    // check GIF header (6 bytes)
    // 'GIF'
    if (!buf || buf_size < 13 || !CheckPattern(buf, buf_size))
        return 0;
    if (buf[0]!='G' || buf[1]!='I' || buf[2]!='F')
        return 0;
    // version: '87a' or '89a'
    if (buf[3]!='8' || buf[5]!='a')
        return 0;
    if (buf[4]=='7')
        m_version = 7;
    else if (buf[4]=='9')
        m_version = 9;
    else
        return 0; // bad version

    // read screen descriptor
    unsigned char * p = buf+6;

    _width = p[0] + (p[1]<<8);
    _height = p[2] + (p[3]<<8);
    m_bpp = (p[4]&7)+1;
    m_flg_gtc = (p[4]&0x80)?1:0;
    m_background_color = p[5];
    defined_transparent_color = false;
    if ( !(_width>=1 && _height>=1 && _width<4096 && _height<4096 ) )
        return false;
    if ( !callback )
        return true;
    // next
    p+=7;


    // read global color table
    if (m_flg_gtc) {
        int m_color_count = 1<<m_bpp;

        if (m_color_count * 3 > buf_size - (p - buf))
            return 0; // error

        m_global_color_table.resize(m_color_count);
        for (int i=0; i<m_color_count; i++) {
            m_global_color_table[i] = lRGB(p[i*3],p[i*3+1],p[i*3+2]);
            //m_global_color_table[i] = lRGB(p[i*3+2],p[i*3+1],p[i*3+0]);
        }

        // next
        p+=(m_color_count * 3);
    }

    bool found = false;
    bool res = true;
    while (res && p - buf < buf_size) {
        // search for delimiter char ','
        int recordType = *p;

        //            while (*p != ',' && p-buf<buf_size)
        //                p++;
        switch (recordType) {
        case ',': // image descriptor, ','
            // found image descriptor!
            {
                LVGifFrame frame(this);
                int cbRead = 0;
                if (frame.DecodeFromBuffer(p, (int)(buf_size - (p - buf)), cbRead) ) {
                    found = true;
                    frame.Draw( callback );
                }
                res = false; // first frame found, stop!
            }
            break;
        case '!': // extension record
            {
                const int remaining = static_cast<int>(buf_size - (p - buf));
                const bool hasTransparentColor = remaining >= 7
                        && p[1] == 0xf9 && (p[3] & 1) != 0;
                const unsigned char transparentColor =
                        hasTransparentColor ? p[6] : 0;
                res = skipGifExtension(p, remaining);
                if (res && hasTransparentColor) {
                    m_transparent_color = transparentColor;
                    defined_transparent_color = true;
                }
            }
            break;
        case ';': // terminate record
            res = false;
            break;
        default:
            res = false;
            break;
        }
    }

    return found;
}

void LVGifImageSource::Clear()
{
    _width = 0;
    _height = 0;
    m_version = 0;
    m_bpp = 0;
    m_flg_gtc = 0;
    m_global_color_table.clear();
}

bool LVGifImageSource::Decode( LVImageDecoderCallback * callback )
{
    if ( _stream.isNull() )
        return false;
    lvsize_t sz = _stream->GetSize();
    if ( sz<32 )
        return false; // wrong size
    std::vector<lUInt8> buf(sz);
    lvsize_t bytesRead = 0;
    bool res = true;
    _stream->SetPos(0);
    if ( _stream->Read( buf.data(), sz, &bytesRead )!=LVERR_OK || bytesRead!=sz )
        res = false;

//    // for DEBUG
//    {
//        LVStreamRef out = LVOpenFileStream("/tmp/test.gif", LVOM_WRITE);
//        out->Write(buf, sz, NULL);
//    }

    res = res && DecodeFromBuffer( buf.data(), sz, callback );
    return res;
}

#endif  // (USE_GIF==1)
