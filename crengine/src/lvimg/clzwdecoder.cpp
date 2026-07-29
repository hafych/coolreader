/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2008,2012 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2020 poire-z <poire-z@users.noreply.github.com>         *
 *   Copyright (C) 2021 NiLuJe <ninuje@gmail.com>                          *
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

#include "clzwdecoder.h"

#if (USE_GIF==1)

#include <stddef.h>

void CLZWDecoder::SetInputStream(const unsigned char *p, int sz) {
    p_in_stream = p;
    in_stream_size = sz;
    in_bit_pos = 0;
}

void CLZWDecoder::SetOutputStream(unsigned char *p, int sz) {
    p_out_stream = p;
    out_stream_size = sz;
}

int CLZWDecoder::WriteOutChar(unsigned char b) {
    if (!p_out_stream || out_stream_size <= 0)
        return 0;
    --out_stream_size;
    *p_out_stream++ = b;
    return 1;
}

int CLZWDecoder::WriteOutString(int code) {
    if (code < 0)
        return 0;
    int pos = 0;
    while (code >= 0) {
        if (code >= lastadd || code >= LSWDECODER_MAX_TABLE_SIZE
                || pos >= LSWDECODER_MAX_TABLE_SIZE / 2)
            return 0;
        rev_buf[pos++] = str_table[code];
        code = str_nextchar[code];
    }
    while (--pos>=0) {
        if (!WriteOutChar(rev_buf[pos]))
            return 0;
    }
    return 1;
}

void CLZWDecoder::FillRestOfOutStream(unsigned char b) {
    if (!p_out_stream || out_stream_size <= 0)
        return;
    for (; out_stream_size>0; out_stream_size--) {
        *p_out_stream++ = b;
    }
}

int CLZWDecoder::ReadInCode() {
    if (!p_in_stream || in_stream_size <= 0
            || in_bit_pos < 0 || in_bit_pos >= 8
            || bits <= 0 || bits > LSWDECODER_MAX_BITS)
        return -1;

    const int requiredBytes = (in_bit_pos + bits + 7) / 8;
    if (in_stream_size < requiredBytes)
        return -1;

    unsigned int code = 0;
    for (int i = 0; i < requiredBytes; i++)
        code |= static_cast<unsigned int>(p_in_stream[i]) << (i * 8);
    code >>= in_bit_pos;
    code &= (1U << bits) - 1U;

    in_bit_pos += bits;
    const int consumedBytes = in_bit_pos / 8;
    p_in_stream += consumedBytes;
    in_stream_size -= consumedBytes;
    in_bit_pos %= 8;
    return static_cast<int>(code);
}

int CLZWDecoder::AddString(int OldCode, unsigned char NewChar) {
    if (lastadd < 0 || lastadd >= LSWDECODER_MAX_TABLE_SIZE
            || OldCode < 0 || OldCode >= lastadd)
        return -1;
    if (lastadd == (1<<bits)-1) {
        // increase table size, except case when ClearCode is expected
        if (bits < LSWDECODER_MAX_BITS)
            bits++;
    }

    str_table[lastadd] = NewChar;
    str_nextchar[lastadd] = OldCode;
    last_table[lastadd] = last_table[OldCode];

    lastadd++;
    return lastadd-1;
}

CLZWDecoder::CLZWDecoder() {
    /* // ld implementation
        str_table = NULL;
        str_size = NULL;
        */
    p_in_stream = NULL;
    in_stream_size = 0;
    in_bit_pos = 0;

    p_out_stream = NULL;
    out_stream_size = 0;

    clearcode = 0;
    eoicode = 0;
    bits = 0;
    lastadd = 0;
}

void CLZWDecoder::Clear() {
    /* // old implementation
        for (int i=0; i<lastadd; i++) {
            if (str_table[i])
                delete str_table[i];
        }
        */
    lastadd=0;
}

CLZWDecoder::~CLZWDecoder() {
    Clear();
}

void CLZWDecoder::Init(int sizecode) {
    // init table
    Clear();
    if (sizecode < 0 || sizecode >= LSWDECODER_MAX_BITS) {
        bits = 0;
        clearcode = 0;
        eoicode = 0;
        return;
    }
    bits = sizecode + 1;
    //ResizeTable(1<<bits);
    for (int i=(1<<sizecode) + 1; i>=0; i--) {
        str_table[i] = i;
        last_table[i] = i;
        str_nextchar[i] = -1;
    }
    // init codes
    clearcode = (1 << sizecode);
    eoicode = clearcode + 1;
    
    str_table[clearcode] = 0;
    str_nextchar[clearcode] = -1;
    str_table[eoicode] = 0;
    str_nextchar[eoicode] = -1;
    //str_table[eoicode] = NULL;
    lastadd = eoicode + 1;
}

int CLZWDecoder::Decode(int init_code_size) {
    
    int code, oldcode;
    
    Init( init_code_size );
    if (bits == 0)
        return 0;
    
    code = ReadInCode(); // == 256, ignore
    if (code<0 || code>lastadd)
        return 0;
    
    while (1) { // 3
        
        code = ReadInCode();
        
        if (code<0 || code>lastadd)
            return 1; // allow partial image
        
        if (!WriteOutString(code))
            return 0;
        
        while (1) { // 5
            
            oldcode = code;
            
            code = ReadInCode();
            
            if (code<0 || code>lastadd)
                return 0;
            
            if (CodeExists(code)) {
                if (code == eoicode)
                    return 1;
                else if (code == clearcode) {
                    break; // clear & goto 3
                }
                
                // write  code
                if (!WriteOutString(code))
                    return 0;
                
                // add  old + code[0]
                if (AddString(oldcode, last_table[code])<0)
                    // return 0; // table overflow
                {}
                // Ignore table overflow, which seems ok, and done by Pillow:
                //   https://github.com/python-pillow/Pillow/blob/ae43af61/src/libImaging/GifDecode.c#L234-L251
                // which is fine handling this image:
                //   https://cms-assets.tutsplus.com/uploads/users/30/posts/19890/image/hanging-punctuation-example.gif
                // (Aborting on table overflow, we would fail while in the middle
                // of the last line of text in this image.)
                // (giflib/lib/dgif_lib.c is fine with this image too, but its algo is too different
                // to have an idea how it handles this situation.)
                
                
            } else {
                // write  old + old[0]
                if (!WriteOutString(oldcode))
                    return 0;
                if (!WriteOutChar(last_table[oldcode]))
                    return 0;
                
                // add  old + old[0]
                if (AddString(oldcode, last_table[oldcode])<0)
                    // return 0; // table overflow
                {}
                // Ignore table overflow, see above (might be less needed here than there?)
            }
        }
        
        Init( init_code_size );
    }
}

#endif  // (USE_GIF==1)
