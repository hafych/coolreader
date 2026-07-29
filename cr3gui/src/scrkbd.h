/***************************************************************************
 *   CoolReader GUI                                                        *
 *   Copyright (C) 2009 Vadim Lopatin <coolreader.org@gmail.com>           *
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
// C++ Interface: on-screen keyboard
//

#ifndef SCRKBD_H_INCLUDED
#define SCRKBD_H_INCLUDED

#include "mainwnd.h"


class CRScreenKeyboard : public CRGUIWindowBase
{
protected:
    lString32 & _buffer;
    lString32 _value;
    CRMenuSkinRef _skin;
    lString32 _title;
    int _resultCmd;
    lChar32 _lastDigit;
    int _rows;
    int _cols;
    lString32Collection _keymap;
    void draw() override;
    virtual lChar32 digitsToChar( lChar32 digit1, lChar32 digit2 );
    bool digitEntered( lChar32 c );
public:
	void setDefaultLayout();
    void setLayout( CRKeyboardLayoutRef layout );
    CRScreenKeyboard(CRGUIWindowManager * wm, int id,
                     const lString32 & caption, lString32 & buffer,
                     lvRect & rc);

    ~CRScreenKeyboard() override { }

    //virtual const lvRect & getRect();

    //virtual lvPoint getSize();

    /// returns true if command is processed
    bool onCommand( int command, int params ) override;

};


#endif
