/***************************************************************************
 *   CoolReader GUI                                                        *
 *   Copyright (C) 2009,2010 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2011 Konstantin Potapov <pkbo@users.sourceforge.net>    *
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
// C++ Interface: bookmarks dialog
//

#ifndef BMKDLG_H_INCLUDED
#define BMKDLG_H_INCLUDED

#include "fsmenu.h"

class CRBookmarkMenuItem : public CRMenuItem
{
private:
    CRBookmark * _bookmark;
    int _page;
public:
    CRBookmarkMenuItem( CRMenu * menu, int shortcut, CRBookmark * bookmark, int page );
    void Draw(
            LVDrawBuf & buf, lvRect & rc, CRRectSkinRef skin,
            CRRectSkinRef valueSkin, bool selected) override;
    CRBookmark * getBookmark() { return _bookmark; }
    void setBookmark(CRBookmark *bookmark) { _bookmark = bookmark; _itemDirty = true; }
    int getPage() { return _page; }
};

class CRBookmarkMenu : public CRFullScreenMenu
{
protected:
    bool _goToMode; // true for goTo mode, false for addMode
    LVDocView * _docview;
public:
    /// returns index of selected item, -1 if no item selected
    int getSelectedItemIndex() override;
    void setMode( bool goToMode );
    CRBookmarkMenu(CRGUIWindowManager * wm, LVDocView * docview, int numItems, lvRect & rc, bool goToMode=false);
#ifdef CR_POCKETBOOK
    int getDefaultSelectionIndex() override;
    void showContextMenu();
    void handleContextMenu(int index);
#endif
    bool onCommand( int command, int params ) override;
};

class CRCitesMenu : public CRFullScreenMenu
{
protected:
    LVDocView * _docview;
    void goToCitePage(int selecteditem);
    void createDefaultItem();
public:
    /// returns index of selected item, -1 if no item selected
    int getSelectedItemIndex() override;
    CRCitesMenu(CRGUIWindowManager * wm, LVDocView * docview, int numItems, lvRect & rc);
#ifdef CR_POCKETBOOK
    void showContextMenu();
    void handleContextMenu(int index);
#endif
    bool onCommand( int command, int params ) override;
};

#endif
