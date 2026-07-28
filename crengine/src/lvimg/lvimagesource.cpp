/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007,2013 Vadim Lopatin <coolreader.org@gmail.com>      *
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

#include "lvimagesource.h"
#include "crninepatchdecoder.h"

static void fixNegative(int &n) {
    if (n < 0)
        n = 0;
}


void CR9PatchInfo::applyPadding(lvRect &dstPadding) const {
    if (dstPadding.left < padding.left)
        dstPadding.left = padding.left;
    if (dstPadding.right < padding.right)
        dstPadding.right = padding.right;
    if (dstPadding.top < padding.top)
        dstPadding.top = padding.top;
    if (dstPadding.bottom < padding.bottom)
        dstPadding.bottom = padding.bottom;
}

static void fixNegative(int n[4]) {
    int d1 = n[1] - n[0];
    int d2 = n[3] - n[2];
    if (d1 + d2 > 0) {
        n[1] = n[2] = n[0] + (n[3] - n[0]) * d1 / (d1 + d2);
    } else {
        n[1] = n[2] = (n[0] + n[3]) / 2;
    }
}

/// caclulate dst and src rectangles (src does not include layout frame)
void CR9PatchInfo::calcRectangles(const lvRect &dst, const lvRect &src, lvRect dstitems[9],
                                  lvRect srcitems[9]) const {
    for (int i = 0; i < 9; i++) {
        srcitems[i].clear();
        dstitems[i].clear();
    }
    if (dst.isEmpty() || src.isEmpty())
        return;

    int sx[4], sy[4], dx[4], dy[4];
    sx[0] = src.left;
    sx[1] = src.left + frame.left;
    sx[2] = src.right - frame.right;
    sx[3] = src.right;
    sy[0] = src.top;
    sy[1] = src.top + frame.top;
    sy[2] = src.bottom - frame.bottom;
    sy[3] = src.bottom;
    dx[0] = dst.left;
    dx[1] = dst.left + frame.left;
    dx[2] = dst.right - frame.right;
    dx[3] = dst.right;
    dy[0] = dst.top;
    dy[1] = dst.top + frame.top;
    dy[2] = dst.bottom - frame.bottom;
    dy[3] = dst.bottom;
    if (dx[1] > dx[2]) {
        // shrink horizontal
        fixNegative(dx);
    }
    if (dy[1] > dy[2]) {
        // shrink vertical
        fixNegative(dy);
    }
    // fill calculated rectangles
    for (int y = 0; y < 3; y++) {
        for (int x = 0; x < 3; x++) {
            int i = y * 3 + x;
            srcitems[i].left = sx[x];
            srcitems[i].right = sx[x + 1];
            srcitems[i].top = sy[y];
            srcitems[i].bottom = sy[y + 1];
            dstitems[i].left = dx[x];
            dstitems[i].right = dx[x + 1];
            dstitems[i].top = dy[y];
            dstitems[i].bottom = dy[y + 1];
        }
    }
}

CR9PatchInfo *LVImageSource::DetectNinePatch() {
    if (_ninePatch)
        return _ninePatch.get();
    std::unique_ptr<CR9PatchInfo> candidate(new CR9PatchInfo());
    CR9PatchInfo * info = candidate.get();
    CRNinePatchDecoder decoder(GetWidth(), GetHeight(), info);
    if (!Decode(&decoder))
        return NULL;
    if (info->frame.left > 0 && info->frame.top > 0
        && info->frame.left < info->frame.right
        && info->frame.top < info->frame.bottom) {
        // remove 1 pixel frame
        info->padding.left--;
        info->padding.top--;
        info->padding.right = GetWidth() - info->padding.right - 1;
        info->padding.bottom = GetHeight() - info->padding.bottom - 1;
        fixNegative(info->padding.left);
        fixNegative(info->padding.top);
        fixNegative(info->padding.right);
        fixNegative(info->padding.bottom);
        info->frame.left--;
        info->frame.top--;
        info->frame.right = GetWidth() - info->frame.right - 1;
        info->frame.bottom = GetHeight() - info->frame.bottom - 1;
        fixNegative(info->frame.left);
        fixNegative(info->frame.top);
        fixNegative(info->frame.right);
        fixNegative(info->frame.bottom);
    } else {
        return NULL;
    }
    _ninePatch.swap(candidate);
    return _ninePatch.get();
}

LVImageSource::~LVImageSource() = default;
