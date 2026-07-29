/***************************************************************************
 *   CoolReader GUI                                                        *
 *   Copyright (C) 2009,2010 Vadim Lopatin <coolreader.org@gmail.com>      *
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

//
// C++ Interface: TOC dialog
//

#ifndef TOCDLG_H_INCLUDED
#define TOCDLG_H_INCLUDED

#include "mainwnd.h"
#include "numedit.h"

class CRTOCDialog : public CRNumberEditDialog
{
    protected:
        LVPtrVector<LVTocItem, false> _items;
        LVFontRef _font;
        int _itemHeight;
        int _topItem;
        int _pageItems;
        LVDocView * _docview;

        void draw() override;
    public:
        CRTOCDialog(
                CRGUIWindowManager * wm, lString32 title,
                int resultCmd, int pageCount, LVDocView * docview);
        ~CRTOCDialog() override
        {
        }
        bool digitEntered( lChar32 c );
        int getCurItemIndex();

        /// returns true if command is processed
        bool onCommand( int command, int params ) override;
};


#endif
