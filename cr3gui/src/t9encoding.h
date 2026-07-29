/***************************************************************************
 *   CoolReader GUI                                                        *
 *   Copyright (C) 2008,2009,2012 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2008 Alexander V. Nikolaev <avn@daemon.hole.ru>         *
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

#ifndef __T9ENCODIG
#define __T9ENCODIG 1

#include "lvstring.h"
#include "lvstring32collection.h"

// T9-like encoding table
class TEncoding {
    lString32Collection keytable_;
public:

	void set( const lString32Collection & items )
	{
		keytable_.clear();
		keytable_.addAll( items );
	}

    int length() const { return keytable_.length(); }

    const lString32 & operator [] ( int index ) const { return keytable_[index]; }

    TEncoding() { }

    TEncoding( const lChar32 * const * defs )
    {
        init( defs );
    }

    TEncoding(const TEncoding& other) : keytable_(other.keytable_) {}

    virtual ~TEncoding() {}

    void init( const lChar32 * const * defs )
    {
        keytable_.clear();
        for (; *defs; defs++ ) {
            assert(keytable_.length() <= 10);
            keytable_.add(*defs);
        }
    }

    int encode(lChar32 ch) const
    {
        assert(keytable_.length() <= 10);
        for (int i = 0; i < keytable_.length(); i++) {
            const lString32 &ref = keytable_[i];
            for( int j = 0; j < ref.length(); j ++ ) {
                if (ref[j] == ch) {
                    return i;
                }
            }
        }
        return 0;
    }

    lString8
    encode_string( const lString16 &s ) const
    {
        lString32 normalized = Utf16ToUnicode(s);
        normalized.lowercase();
        lString8 result;
        for (int i = 0; i < normalized.length(); i ++) {
            result.append(
                    1, static_cast<lChar8>(
                            '0' + encode(normalized[i])));
        }
        return result;
    }

protected:
   void defkey(const lChar32 *chars) {
        assert(keytable_.length() <= 10);
        keytable_.add(chars);
   }
};

class T9ClassicEncoding : public TEncoding {
       //T9 drawn on my Siemens S55 ;)
public:
   T9ClassicEncoding () : TEncoding() {
       defkey(U".,"); // 0 STUB
       defkey(U" "); // 1 are STUBs
       defkey(U"abc"); // 2
       defkey(U"def"); // 3
       defkey(U"ghi"); // 4
       defkey(U"jkl"); // 5
       defkey(U"mno"); // 6
       defkey(U"pqrs"); // 7
       defkey(U"tuv"); // 8
       defkey(U"wxyz"); // 9
    }
};

class T9Encoding : public TEncoding {
   // T9 by LV
public:
   T9Encoding () : TEncoding() {
       defkey(U" .,"); // 0 STUB
       defkey(U"abc"); // 1
       defkey(U"def"); // 2
       defkey(U"ghi"); // 3
       defkey(U"jkl"); // 4
       defkey(U"mno"); // 5
       defkey(U"pqrs"); // 6
       defkey(U"tuv"); // 7
       defkey(U"wxyz"); // 8
       defkey(U""); // 9 are STUBs
    }
};
#endif
