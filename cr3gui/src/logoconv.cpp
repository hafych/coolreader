/***************************************************************************
 *   CoolReader GUI                                                        *
 *   Copyright (C) 2010 Vadim Lopatin <coolreader.org@gmail.com>           *
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

#include <crengine.h>

int main(int argc, char **argv)
{
    if ( argc<4 ) {
        printf("Usage: logocnv startlogo stoplogo outfile\n");
        return 1;
    }
    lString32 startPath = Utf8ToUnicode(lString8(argv[1]));
    LVImageSourceRef startimg = LVCreateFileCopyImageSource(startPath);
    if ( startimg.isNull() ) {
        printf("Cannot open image from file %s\n", argv[1]);
        return 1;
    }
    printf("Start image: %s %d x %d\n", argv[1], startimg->GetWidth(), startimg->GetHeight());
    LVGrayDrawBuf buf1( 600, 800, 3 );
    buf1.Draw(startimg, 0, 0, 600, 800, true);
    lString32 stopPath = Utf8ToUnicode(lString8(argv[2]));
    LVImageSourceRef stopimg = LVCreateFileCopyImageSource(stopPath);
    if ( stopimg.isNull() ) {
        printf("Cannot open image from file %s\n", argv[2]);
        return 1;
    }
    printf(
            "Stop image: %s %d x %d\n",
            argv[2], stopimg->GetWidth(), stopimg->GetHeight());
    LVGrayDrawBuf buf2( 600, 800, 3 );
    buf2.Draw(stopimg, 0, 0, 600, 800, true);

    lString32 outputPath = Utf8ToUnicode(lString8(argv[3]));
    LVStreamRef out = LVOpenFileStream(outputPath.c_str(), LVOM_WRITE);
    if ( out.isNull() ) {
        printf("Cannot create output file %s", argv[3]);
        return 1;
    }
    const lvsize_t startSize =
            static_cast<lvsize_t>(buf1.GetRowSize())
            * static_cast<lvsize_t>(buf1.GetHeight());
    const lvsize_t stopSize =
            static_cast<lvsize_t>(buf2.GetRowSize())
            * static_cast<lvsize_t>(buf2.GetHeight());
    lvsize_t startWritten = 0;
    lvsize_t stopWritten = 0;
    if ( out->Write(
                buf1.GetScanLine(0), startSize, &startWritten) != LVERR_OK
            || startWritten != startSize
            || out->Write(
                buf2.GetScanLine(0), stopSize, &stopWritten) != LVERR_OK
            || stopWritten != stopSize
            || out->Flush(true) != LVERR_OK) {
        printf("Cannot write complete output file %s\n", argv[3]);
        return 1;
    }
    printf(
            "%llu bytes written to file %s\n",
            static_cast<unsigned long long>(startWritten + stopWritten),
            argv[3]);
    return 0;
}
