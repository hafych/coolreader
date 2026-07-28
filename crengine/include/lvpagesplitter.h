/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2012 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2020 Konstantin Potapov <pkbo@users.sourceforge.net>    *
 *   Copyright (C) 2020 Aleksey Chernov <valexlin@gmail.com>               *
 *   Copyright (C) 2020 Jellby <jellby@yahoo.com>                          *
 *   Copyright (C) 2018-2021 poire-z <poire-z@users.noreply.github.com>    *
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

/**
 * \file lvpagesplitter.h
 * \brief page splitter interface
 */

#ifndef __LV_PAGESPLITTER_H_INCLUDED__
#define __LV_PAGESPLITTER_H_INCLUDED__

#include <algorithm>
#include <climits>
#include <memory>
#include <stdexcept>
#include <time.h>
#include <utility>
#include <vector>
#include "lvtypes.h"
#include "lvarray.h"
#include "lvptrvec.h"
#include "lvref.h"
#include "lvstring.h"
#include "lvhashtable.h"
#include "crtimerutil.h"
#include "lvstring32collection.h"

#ifndef RENDER_PROGRESS_INTERVAL_MILLIS
#define RENDER_PROGRESS_INTERVAL_MILLIS 300
#endif
#ifndef RENDER_PROGRESS_INTERVAL_PERCENT
#define RENDER_PROGRESS_INTERVAL_PERCENT 2
#endif

/// &7 values
#define RN_SPLIT_AUTO   0
#define RN_SPLIT_AVOID  1
#define RN_SPLIT_ALWAYS 2
/// right-shift
#define RN_SPLIT_BEFORE 0
#define RN_SPLIT_AFTER  3

#define RN_SPLIT_BEFORE_AUTO   (RN_SPLIT_AUTO<<RN_SPLIT_BEFORE)
#define RN_SPLIT_BEFORE_AVOID  (RN_SPLIT_AVOID<<RN_SPLIT_BEFORE)
#define RN_SPLIT_BEFORE_ALWAYS (RN_SPLIT_ALWAYS<<RN_SPLIT_BEFORE)
#define RN_SPLIT_AFTER_AUTO    (RN_SPLIT_AUTO<<RN_SPLIT_AFTER)
#define RN_SPLIT_AFTER_AVOID   (RN_SPLIT_AVOID<<RN_SPLIT_AFTER)
#define RN_SPLIT_AFTER_ALWAYS  (RN_SPLIT_ALWAYS<<RN_SPLIT_AFTER)

#define RN_SPLIT_BOTH_AUTO      RN_SPLIT_BEFORE_AUTO | RN_SPLIT_AFTER_AUTO
#define RN_SPLIT_BOTH_AVOID    RN_SPLIT_BEFORE_AVOID | RN_SPLIT_AFTER_AVOID

#define RN_SPLIT_FOOT_NOTE 0x100
#define RN_SPLIT_FOOT_LINK 0x200

#define RN_SPLIT_DISCARD_AT_START 0x400

#define RN_LINE_IS_RTL 0x1000

#define RN_GET_SPLIT_BEFORE(flags) (flags & 0x7)
#define RN_GET_SPLIT_AFTER(flags) (flags >> 3)

#define RN_PAGE_TYPE_NORMAL           0x01
#define RN_PAGE_TYPE_COVER            0x02
#define RN_PAGE_MOSTLY_RTL            0x10
#define RN_PAGE_FOOTNOTES_MOSTLY_RTL  0x20

class SerialBuf;

/// footnote fragment inside page
class LVPageFootNoteInfo {
public:
    int start;
    int height;
    LVPageFootNoteInfo()
    : start(0), height(0)
    { }
    LVPageFootNoteInfo( int s, int h )
    : start(s), height(h) 
    { }
};

template <typename T, int RESIZE_MULT, int RESIZE_ADD> class CompactArray
{
    static_assert(RESIZE_MULT >= 1,
            "CompactArray growth multiplier must be positive");
    static_assert(RESIZE_ADD > 0,
            "CompactArray growth increment must be positive");

    struct Array {
        std::vector<T> _list;

        size_t requiredSize(int count) const
        {
            const size_t maxCount = std::min(
                    _list.max_size(), static_cast<size_t>(INT_MAX));
            const size_t added = static_cast<size_t>(count);
            if (added > maxCount
                    || _list.size() > maxCount - added)
                throw std::length_error("CompactArray size overflow");
            return _list.size() + added;
        }

        void ensureCapacity(size_t required, bool grow)
        {
            if (required <= _list.capacity())
                return;
            size_t capacity = required;
            if (grow) {
                const size_t maxCount = std::min(
                        _list.max_size(), static_cast<size_t>(INT_MAX));
                const size_t multiplier =
                        static_cast<size_t>(RESIZE_MULT);
                const size_t increment =
                        static_cast<size_t>(RESIZE_ADD);
                if (increment > maxCount
                        || _list.capacity()
                        > (maxCount - increment) / multiplier) {
                    capacity = maxCount;
                } else {
                    capacity = _list.capacity() * multiplier
                            + increment;
                }
                if (capacity < required)
                    capacity = required;
            }
            _list.reserve(capacity);
        }

        void add( T item )
        {
            ensureCapacity(requiredSize(1), true);
            _list.push_back(std::move(item));
        }

        void add( T * items, int count )
        {
            if (!items || count <= 0)
                return;
            const size_t required = requiredSize(count);
            std::vector<T> snapshot(items, items + count);
            ensureCapacity(required, false);
            _list.insert(_list.end(),
                    snapshot.begin(), snapshot.end());
        }

        void reserve( int count )
        {
            if (count <= 0)
                return;
            ensureCapacity(requiredSize(count), false);
        }

        int length() const
        {
            return static_cast<int>(_list.size());
        }

        T get( int index ) const
        {
            return _list[index];
        }

        const T & operator [] (int index) const
        {
            return _list[index];
        }

        T & operator [] (int index)
        {
            return _list[index];
        }
    };

    std::unique_ptr<Array> _data;
public:
    CompactArray() : _data()
    {
    }

    CompactArray(const CompactArray &array)
        : _data(array._data
                ? std::make_unique<Array>(*array._data)
                : nullptr)
    {
    }

    CompactArray &operator=(const CompactArray &array)
    {
        if (this == &array)
            return *this;
        std::unique_ptr<Array> replacement = array._data
                ? std::make_unique<Array>(*array._data)
                : nullptr;
        _data = std::move(replacement);
        return *this;
    }

    CompactArray(CompactArray &&) noexcept = default;
    CompactArray &operator=(CompactArray &&) noexcept = default;
    ~CompactArray() = default;

    void add( T item )
    {
        if (_data) {
            _data->add(std::move(item));
            return;
        }
        std::unique_ptr<Array> replacement =
                std::make_unique<Array>();
        replacement->add(std::move(item));
        _data = std::move(replacement);
    }

    void add( T * items, int count )
    {
        if (!items || count <= 0)
            return;
        if (_data) {
            _data->add(items, count);
            return;
        }
        std::unique_ptr<Array> replacement =
                std::make_unique<Array>();
        replacement->add(items, count);
        _data = std::move(replacement);
    }

    void add( LVArray<T> & items )
    {
        if (items.length() <= 0)
            return;
        add(&(items[0]), items.length());
    }

    void reserve( int count )
    {
        if (count <= 0)
            return;
        if (_data) {
            _data->reserve(count);
            return;
        }
        std::unique_ptr<Array> replacement =
                std::make_unique<Array>();
        replacement->reserve(count);
        _data = std::move(replacement);
    }

    void clear()
    {
        _data.reset();
    }

    int length() const
    {
        return _data ? _data->length() : 0;
    }

    T get( int index ) const
    {
        return _data->get(index);
    }

    const T & operator [] (int index) const
    {
        return _data->operator [](index);
    }

    T & operator [] (int index)
    {
        return _data->operator [](index);
    }

    bool empty() const { return !_data || _data->length() == 0; }
};

/// rendered page splitting info
class LVRendPageInfo {
public:
    int start; /// start of page
    int index;  /// index of page
    lInt16 height; /// height of page, does not include footnotes
    lInt8 flags;   /// RN_PAGE_*
    CompactArray<LVPageFootNoteInfo, 1, 4> footnotes; /// footnote fragment list for page
    lUInt16 flow;
    LVRendPageInfo(int pageStart, lUInt16 pageHeight, int pageIndex)
    : start(pageStart), index(pageIndex), height(pageHeight), flags(RN_PAGE_TYPE_NORMAL), flow(0) {}
    LVRendPageInfo(lUInt16 coverHeight)
    : start(0), index(0), height(coverHeight), flags(RN_PAGE_TYPE_COVER), flow(0) {}
    LVRendPageInfo() 
    : start(0), index(0), height(0), flags(RN_PAGE_TYPE_NORMAL), flow(0) {}
    bool serialize( SerialBuf & buf );
    bool deserialize( SerialBuf & buf );
};

class LVRendPageList : public LVPtrVector<LVRendPageInfo>
{
    bool has_nonlinear_flows;
public:
    LVRendPageList() : has_nonlinear_flows(false) {}
    int FindNearestPage( int y, int direction );
    void setHasNonLinearFlows() { has_nonlinear_flows=true; }
    bool hasNonLinearFlows() { return has_nonlinear_flows; }
    bool serialize( SerialBuf & buf );
    bool deserialize( SerialBuf & buf );
};

class LVFootNote;

class LVFootNoteList;

class LVFootNoteList : public LVArray<LVFootNote*> {
public: 
    LVFootNoteList() {}
};


class LVRendLineInfo {
    friend struct PageSplitState;
    std::unique_ptr<LVFootNoteList> links;
    int start;              // 4 bytes
    int height;             // 4 bytes (we may get extra tall lines with tables TR)
public:
    lUInt16 flags;          // 2 bytes
    lUInt16 flow;           // 2 bytes (should be enough)
    int getSplitBefore() const { return (flags>>RN_SPLIT_BEFORE)&7; }
    int getSplitAfter() const { return (flags>>RN_SPLIT_AFTER)&7; }
/*
    LVRendLineInfo & operator = ( const LVRendLineInfoBase & v )
    {
        start = v.start;
        end = v.end;
        flags = v.flags;
        return *this;
    }
*/
    bool empty() const { 
        return start==-1; 
    }

    void clear() { 
        start = -1; height = 0; flags = 0;
        links.reset();
    }

    inline int getEnd() const { return start + height; }
    inline int getStart() const { return start; }
    inline int getHeight() const { return height; }
    inline lUInt16 getFlags() const { return flags; }

    LVRendLineInfo()
    : links(), start(-1), height(0), flags(0), flow(0) { }
    LVRendLineInfo( int line_start, int line_end, lUInt16 line_flags )
    : links(), start(line_start), height(line_end-line_start), flags(line_flags), flow(0)
    {
    }
    LVRendLineInfo( int line_start, int line_end, lUInt16 line_flags, int flow )
    : links(), start(line_start), height(line_end-line_start), flags(line_flags), flow(flow)
    {
    }

    LVRendLineInfo(const LVRendLineInfo &line)
    : links(line.links
            ? std::make_unique<LVFootNoteList>(*line.links)
            : nullptr),
      start(line.start),
      height(line.height),
      flags(line.flags),
      flow(line.flow)
    {
    }

    LVRendLineInfo &operator=(const LVRendLineInfo &line)
    {
        if (this == &line)
            return *this;
        std::unique_ptr<LVFootNoteList> replacement = line.links
                ? std::make_unique<LVFootNoteList>(*line.links)
                : nullptr;
        links = std::move(replacement);
        start = line.start;
        height = line.height;
        flags = line.flags;
        flow = line.flow;
        return *this;
    }

    LVRendLineInfo(LVRendLineInfo &&) noexcept = default;
    LVRendLineInfo &operator=(LVRendLineInfo &&) noexcept = default;
    ~LVRendLineInfo() = default;

    LVFootNoteList * getLinks() { return links.get(); }
    const LVFootNoteList * getLinks() const { return links.get(); }

    int getLinksCount() const
    {
        if (!links)
            return 0;
        return links->length();
    }
    void addLink( LVFootNote * note, int pos=-1 )
    {
        if (!links)
            links = std::make_unique<LVFootNoteList>();
        if ( pos >= 0 ) // insert at pos
            links->insert( pos, note );
        else // append
            links->add( note );
        flags |= RN_SPLIT_FOOT_LINK;
    }
};


typedef LVFastRef<LVFootNote> LVFootNoteRef;

class LVFootNote : public LVRefCounter {
    lString32 id;
    CompactArray<LVRendLineInfo*, 2, 4> lines;
public:
    LVFootNote( lString32 noteId )
        : id(noteId)
    {
    }
    void addLine( LVRendLineInfo * line )
    {
        lines.add( line );
    }
    CompactArray<LVRendLineInfo*, 2, 4> & getLines() { return lines; }
    bool empty() { return lines.empty(); }
    void clear() { lines.clear(); }
    lString32 getId() { return id; }
};

class LVDocViewCallback;
class LVRendPageContext
{


    LVPtrVector<LVRendLineInfo> lines;

    LVDocViewCallback * callback;
    int totalFinalBlocks;
    int renderedFinalBlocks;
    int lastPercent;
    CRTimerUtil progressTimeout;


    // page start line
    //LVRendLineInfoBase pagestart;
    // page end candidate line
    //LVRendLineInfoBase pageend;
    // next line after page end candidate
    //LVRendLineInfoBase next;
    // last fit line
    //LVRendLineInfoBase last;
    // page list to fill
    LVRendPageList * page_list;
    // page height
    int page_h;
    // document default font size (= root node font size)
    int doc_font_size;
    // Whether to gather lines or not (only footnote links will be gathered if not)
    bool gather_lines;
    // Links gathered when !gather_lines
    lString32Collection link_ids;
    // current flow being processed
    int current_flow;
    // maximum flow encountered so far
    int max_flow;

    LVHashTable<lString32, LVFootNoteRef> footNotes;

    LVFootNote * curr_note;

    LVFootNoteRef getOrCreateFootNote( lString32 id )
    {
        LVFootNoteRef ref = footNotes.get(id);
        if ( ref.isNull() ) {
            ref = LVFootNoteRef( new LVFootNote( id ) );
            footNotes.set( id, ref );
        }
        return ref;
    }

    void split();
public:


    void setCallback(LVDocViewCallback * cb, int _totalFinalBlocks) {
        callback = cb; totalFinalBlocks=_totalFinalBlocks;
        progressTimeout.restart(RENDER_PROGRESS_INTERVAL_MILLIS);
    }
    bool updateRenderProgress( int numFinalBlocksRendered );

    bool wantsLines() { return gather_lines; }

    void newFlow( bool nonlinear );

    /// Get the number of links in the current line links list, or
    // in link_ids when !gather_lines
    int getCurrentLinksCount();

    /// append or insert footnote link to last added line
    void addLink( lString32 id, int pos=-1 );

    /// get gathered links when !gather_lines
    // (returns a reference to avoid lString32Collection destructor from
    // being called twice and a double free crash)
    lString32Collection * getLinkIds() { return &link_ids; }

    /// mark start of foot note
    void enterFootNote( lString32 id );

    /// mark end of foot note
    void leaveFootNote();

    /// returns page height
    int getPageHeight() { return page_h; }

    /// returns document font size
    int getDocFontSize() { return doc_font_size; }

    /// returns page list pointer
    LVRendPageList * getPageList() { return page_list; }

    /// constructor (docFontSize is only needed for with main context actually used to split pages)
    LVRendPageContext(LVRendPageList * pageList, int pageHeight, int docFontSize=0, bool gatherLines=true);

    /// add source line
    void AddLine( int starty, int endy, int flags );

    LVPtrVector<LVRendLineInfo> * getLines() {
        return &lines;
    };
    void Finalize();
};

#endif
