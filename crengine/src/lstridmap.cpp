/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2007-2009,2012 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2018,2020 poire-z <poire-z@users.noreply.github.com>    *
 *   Copyright (C) 2020 Konstantin Potapov <pkbo@users.sourceforge.net>    *
 *   Copyright (C) 2018,2020 Aleksey Chernov <valexlin@gmail.com>          *
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
 * \file lstridmap.cpp
 * \brief Name to Id map
 */

#include "../include/lstridmap.h"
#include "../include/dtddef.h"
#include "../include/lvtinydom.h"

#include <algorithm>

LDOMNameIdMapItem::LDOMNameIdMapItem(lUInt16 _id, const lString32 & _value, const css_elem_def_props_t * _data)
    : data(_data
            ? std::unique_ptr<css_elem_def_props_t>(
                    new css_elem_def_props_t(*_data))
            : std::unique_ptr<css_elem_def_props_t>())
    , id(_id)
    , value(_value)
{
}

LDOMNameIdMapItem::LDOMNameIdMapItem(const LDOMNameIdMapItem & item)
    : data(item.data
            ? std::unique_ptr<css_elem_def_props_t>(
                    new css_elem_def_props_t(*item.data))
            : std::unique_ptr<css_elem_def_props_t>())
    , id(item.id)
    , value(item.value)
{
}

LDOMNameIdMapItem &LDOMNameIdMapItem::operator=(
        const LDOMNameIdMapItem &item)
{
    if (this == &item)
        return *this;
    std::unique_ptr<css_elem_def_props_t> dataCopy;
    if (item.data)
        dataCopy.reset(new css_elem_def_props_t(*item.data));
    id = item.id;
    value = item.value;
    data = std::move(dataCopy);
    return *this;
}

static const char id_map_item_magic[] = "IDMI";

/// serialize to byte array
void LDOMNameIdMapItem::serialize( SerialBuf & buf )
{
    if ( buf.error() ) {
        return;
    }
    buf.putMagic( id_map_item_magic );
	buf << id;
	buf << value;
	if ( data ) {
		buf << (lUInt8)1;
		buf << (lUInt8)data->display;
		buf << (lUInt8)data->white_space;
		buf << data->allow_text;
		buf << data->is_object;
	} else {
		buf << (lUInt8)0;
	}
}

/// deserialize from byte array
std::unique_ptr<LDOMNameIdMapItem> LDOMNameIdMapItem::deserialize(
        SerialBuf &buf)
{
    if ( buf.error() ) {
        return std::unique_ptr<LDOMNameIdMapItem>();
    }
    if ( !buf.checkMagic( id_map_item_magic ) ) {
        return std::unique_ptr<LDOMNameIdMapItem>();
    }
	lUInt16 id;
	lString32 value;
    lUInt8 flgData;
    buf >> id >> value >> flgData;
    if (buf.error() || id == 0 || id >= MAX_TYPE_ID || flgData > 1)
        return std::unique_ptr<LDOMNameIdMapItem>();
    if ( flgData ) {
        css_elem_def_props_t props = {};
        lUInt8 display;
        lUInt8 white_space;
        buf >> display >> white_space >> props.allow_text >> props.is_object;
        if (buf.error()
                || display > css_d_none
                || white_space > css_ws_break_spaces)
            return std::unique_ptr<LDOMNameIdMapItem>();
        props.display = (css_display_t)display;
        props.white_space = (css_white_space_t)white_space;
        return std::unique_ptr<LDOMNameIdMapItem>(
                new LDOMNameIdMapItem(id, value, &props));
    }
    return std::unique_ptr<LDOMNameIdMapItem>(
            new LDOMNameIdMapItem(id, value, NULL));
}

LDOMNameIdMapItem::~LDOMNameIdMapItem() = default;

static const char id_map_magic[] = "IMAP";

/// serialize to byte array (pointer will be incremented by number of bytes written)
void LDOMNameIdMap::serialize( SerialBuf & buf )
{
    if ( buf.error() )
        return;
    if (!m_sorted)
        Sort();
    int start = buf.pos();
	buf.putMagic( id_map_magic );
    buf << static_cast<lUInt16>(m_by_name.size());
    for (std::size_t i = 0; i < m_by_id.size(); i++) {
        if ( m_by_id[i] )
            m_by_id[i]->serialize( buf );
    }
    buf.putCRC( buf.pos() - start );
    m_changed = false;
}

/// deserialize from byte array (pointer will be incremented by number of bytes read)
bool LDOMNameIdMap::deserialize( SerialBuf & buf )
{
    if ( buf.error() )
        return false;
    int start = buf.pos();
    if ( !buf.checkMagic( id_map_magic ) ) {
        buf.seterror();
        return false;
    }
    lUInt16 count;
    buf >> count;
    if (buf.error() || count > m_by_id.size()) {
        buf.seterror();
        return false;
    }
    LDOMNameIdMap candidate(0);
    candidate.m_by_id.resize(m_by_id.size());
    candidate.m_by_name.reserve(count);
    for (lUInt16 i = 0; i < count; i++) {
        std::unique_ptr<LDOMNameIdMapItem> item =
                LDOMNameIdMapItem::deserialize(buf);
        if (!item || (item->id < candidate.m_by_id.size()
                    && candidate.m_by_id[item->id])) {
            buf.seterror();
            return false;
        }
        candidate.AddItem(std::move(item));
    }
    if (candidate.m_by_name.size() != count) {
        buf.seterror();
        return false;
    }
    candidate.Sort();
    buf.checkCRC( buf.pos() - start );
    if (buf.error())
        return false;
    swap(candidate);
    m_changed = false;
    return true;
}


LDOMNameIdMap::LDOMNameIdMap(lUInt16 maxId)
    : m_by_id(static_cast<std::size_t>(maxId) + 1)
    , m_by_name()
    , m_sorted(true)
    , m_changed(false)
{
    m_by_name.reserve(m_by_id.size());
}

/// Copy constructor
LDOMNameIdMap::LDOMNameIdMap(const LDOMNameIdMap &map)
    : m_by_id(map.m_by_id.size())
    , m_by_name()
    , m_sorted(map.m_sorted)
    , m_changed(false)
{
    for (std::size_t i = 0; i < map.m_by_id.size(); i++) {
        if (map.m_by_id[i])
            m_by_id[i].reset(
                    new LDOMNameIdMapItem(*map.m_by_id[i]));
    }
    m_by_name.reserve(map.m_by_name.size());
    for (LDOMNameIdMapItem *item : map.m_by_name) {
        if (item && item->id < m_by_id.size() && m_by_id[item->id])
            m_by_name.push_back(m_by_id[item->id].get());
    }
}

LDOMNameIdMap &LDOMNameIdMap::operator=(const LDOMNameIdMap &map)
{
    if (this != &map) {
        LDOMNameIdMap copy(map);
        swap(copy);
    }
    return *this;
}

void LDOMNameIdMap::swap(LDOMNameIdMap &map)
{
    m_by_id.swap(map.m_by_id);
    m_by_name.swap(map.m_by_name);
    std::swap(m_sorted, map.m_sorted);
    std::swap(m_changed, map.m_changed);
}

void LDOMNameIdMap::Sort()
{
    std::sort(
            m_by_name.begin(), m_by_name.end(),
            [](const LDOMNameIdMapItem *left,
                    const LDOMNameIdMapItem *right) {
                return left->value.compare(right->value) < 0;
            });
    m_sorted = true;
}

const LDOMNameIdMapItem * LDOMNameIdMap::findItem( const lChar32 * name )
{
    if (m_by_name.empty() || !name || !*name)
        return NULL;
    if (!m_sorted)
        Sort();
    const std::vector<LDOMNameIdMapItem *>::const_iterator found =
            std::lower_bound(
                    m_by_name.begin(), m_by_name.end(), name,
                    [](const LDOMNameIdMapItem *item,
                            const lChar32 *value) {
                        return lStr_cmp(
                                item->value.c_str(), value) < 0;
                    });
    return found != m_by_name.end()
                    && lStr_cmp((*found)->value.c_str(), name) == 0
            ? *found
            : NULL;
}

const LDOMNameIdMapItem * LDOMNameIdMap::findItem( const lChar8 * name )
{
    if (m_by_name.empty() || !name || !*name)
        return NULL;
    if (!m_sorted)
        Sort();
    const std::vector<LDOMNameIdMapItem *>::const_iterator found =
            std::lower_bound(
                    m_by_name.begin(), m_by_name.end(), name,
                    [](const LDOMNameIdMapItem *item,
                            const lChar8 *value) {
                        return lStr_cmp(
                                item->value.c_str(), value) < 0;
                    });
    return found != m_by_name.end()
                    && lStr_cmp((*found)->value.c_str(), name) == 0
            ? *found
            : NULL;
}

void LDOMNameIdMap::AddItem(
        std::unique_ptr<LDOMNameIdMapItem> item)
{
    if (!item)
        return;
    if ( item->id==0 ) {
        return;
    }
    if (item->id >= m_by_id.size())
        m_by_id.resize(static_cast<std::size_t>(item->id) + 1);
    if (m_by_id[item->id])
        return; // already exists
    LDOMNameIdMapItem *itemView = item.get();
    m_by_name.push_back(itemView);
    m_by_id[itemView->id] = std::move(item);
    m_sorted = false;
    if (!m_changed) {
        m_changed = true;
        //CRLog::trace("new ID for %s is %d", LCSTR(item->value), item->id);
    }
}

void LDOMNameIdMap::AddItem( lUInt16 id, const lString32 & value, const css_elem_def_props_t * data )
{
    if (id==0)
        return;
    std::unique_ptr<LDOMNameIdMapItem> item(
            new LDOMNameIdMapItem(id, value, data));
    AddItem(std::move(item));
}


void LDOMNameIdMap::Clear()
{
    m_by_name.clear();
    for (std::unique_ptr<LDOMNameIdMapItem> &item : m_by_id)
        item.reset();
    m_sorted = true;
}

void LDOMNameIdMap::dumpUnknownItems( FILE * f, int start_id )
{
    if (start_id < 0)
        start_id = 0;
    for (std::size_t i = static_cast<std::size_t>(start_id);
            i < m_by_id.size(); i++)
    {
        if (m_by_id[i] != NULL)
        {
            lString8 s8( m_by_id[i]->value.c_str() );
            fprintf( f, "%d %s\n", m_by_id[i]->id, s8.c_str() );
        }
    }
}

lString32 LDOMNameIdMap::getUnknownItems( int start_id )
{
    lString32 items;
    if (start_id < 0)
        start_id = 0;
    for (std::size_t i = static_cast<std::size_t>(start_id);
            i < m_by_id.size(); i++) {
        if (m_by_id[i] != NULL) {
            if ( !items.empty() )
                items << " ";
            items << m_by_id[i]->value;
        }
    }
    return items;
}
