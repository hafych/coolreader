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
// C++ Interface: number editor dialog
//

#ifndef NUMEDIT_H_INCLUDED
#define NUMEDIT_H_INCLUDED


#include "mainwnd.h"

class CRNumberEditDialog : public CRGUIWindowBase
{
    protected:
        lString32 _title;
        lString32 _value;
        int _minvalue;
        int _maxvalue;
        int _resultCmd;
        CRMenuSkinRef _skin;
        void draw() override;
    public:
        CRNumberEditDialog(
                CRGUIWindowManager * wm, lString32 title,
                lString32 initialValue, int resultCmd,
                int minvalue, int maxvalue);
        ~CRNumberEditDialog() override
        {
        }
        bool digitEntered( lChar32 c );
        /// returns true if command is processed
        bool onCommand( int command, int params ) override;
};


#endif
