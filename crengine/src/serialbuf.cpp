/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2009-2012 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2020 Aleksey Chernov <valexlin@gmail.com>               *
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

#include "../include/serialbuf.h"

#include <cstdlib>
#include <limits>
#include <memory>
#include <utility>

/// serialization/deserialization buffer

/// constructor of serialization buffer
SerialBuf::SerialBuf( int sz, bool autoresize )
    : _storage(sz > 0 ? static_cast<std::size_t>(sz) : 0),
      _buf(_storage.empty() ? NULL : _storage.data()),
      _error(sz < 0), _autoresize(autoresize),
      _size(sz > 0 ? sz : 0), _pos(0)
{
}
/// constructor of deserialization buffer
SerialBuf::SerialBuf( const lUInt8 * p, int sz )
    : _buf(const_cast<lUInt8 *>(p)), _error(sz < 0),
      _autoresize(false), _size(sz > 0 ? sz : 0), _pos(0)
{
}

void SerialBuf::set( lUInt8 * buf, int size )
{
    std::unique_ptr<lUInt8, decltype(&std::free)> adopted(buf, &std::free);
    std::vector<lUInt8> replacement;
    if ( size > 0 && buf )
        replacement.assign(buf, buf + size);
    _storage.swap(replacement);
    _buf = _storage.empty() ? NULL : _storage.data();
    _error = size < 0 || (size > 0 && !buf);
    _autoresize = true;
    _size = _pos = _error ? 0 : size;
}

bool SerialBuf::copyTo( lUInt8 * buf, int maxSize )
{
    if ( _pos==0 )
        return true;
    if ( _pos > maxSize )
        return false;
    memcpy( buf, _buf, _pos );
    return true;
}

/// checks whether specified number of bytes is available, returns true in case of error
bool SerialBuf::check( int reserved )
{
	if ( _error )
		return true;
    if ( reserved < 0 || _pos < 0 || _pos > _size ) {
        _error = true;
        return true;
    }
	if ( space()<reserved ) {
        if ( _autoresize ) {
            const long long grownSize =
                    (_size > 16384
                            ? static_cast<long long>(_size) * 2
                            : 16384)
                    + reserved;
            if ( grownSize > std::numeric_limits<int>::max() ) {
                _error = true;
                return true;
            }
            _storage.resize(static_cast<std::size_t>(grownSize));
            _buf = _storage.data();
            _size = static_cast<int>(grownSize);
            return false;
        } else {
		    _error = true;
		    return true;
        }
	}
	return false;
}

// write methods
/// put magic signature
void SerialBuf::putMagic( const char * s )
{
	if ( check(1) )
		return;
	while ( *s ) {
		_buf[ _pos++ ] = *s++;
		if ( check(1) )
			return;
	}
}

void SerialBuf::swap( SerialBuf & v )
{
    _storage.swap(v._storage);
    std::swap(_buf, v._buf);
    std::swap(_error, v._error);
    std::swap(_autoresize, v._autoresize);
    std::swap(_size, v._size);
    std::swap(_pos, v._pos);
}


/// add contents of another buffer
SerialBuf & SerialBuf::operator << ( const SerialBuf & v )
{
    if ( check(v.pos()) || v.pos()==0 )
		return *this;
    memcpy( _buf + _pos, v._buf, v._pos );
    _pos += v._pos;
	return *this;
}

SerialBuf & SerialBuf::operator << ( lUInt8 n )
{
	if ( check(1) )
		return *this;
	_buf[_pos++] = n;
	return *this;
}
SerialBuf & SerialBuf::operator << ( char n )
{
	if ( check(1) )
		return *this;
	_buf[_pos++] = (lUInt8)n;
	return *this;
}
SerialBuf & SerialBuf::operator << ( bool n )
{
	if ( check(1) )
		return *this;
	_buf[_pos++] = (lUInt8)(n ? 1 : 0);
	return *this;
}
SerialBuf & SerialBuf::operator << ( lUInt16 n )
{
	if ( check(2) )
		return *this;
	_buf[_pos++] = (lUInt8)(n & 255);
	_buf[_pos++] = (lUInt8)((n>>8) & 255);
	return *this;
}
SerialBuf & SerialBuf::operator << ( lInt16 n )
{
	if ( check(2) )
		return *this;
	_buf[_pos++] = (lUInt8)(n & 255);
	_buf[_pos++] = (lUInt8)((n>>8) & 255);
	return *this;
}
SerialBuf & SerialBuf::operator << ( lUInt32 n )
{
	if ( check(4) )
		return *this;
	_buf[_pos++] = (lUInt8)(n & 255);
	_buf[_pos++] = (lUInt8)((n>>8) & 255);
	_buf[_pos++] = (lUInt8)((n>>16) & 255);
	_buf[_pos++] = (lUInt8)((n>>24) & 255);
	return *this;
}
SerialBuf & SerialBuf::operator << ( lInt32 n )
{
	if ( check(4) )
		return *this;
	_buf[_pos++] = (lUInt8)(n & 255);
	_buf[_pos++] = (lUInt8)((n>>8) & 255);
	_buf[_pos++] = (lUInt8)((n>>16) & 255);
	_buf[_pos++] = (lUInt8)((n>>24) & 255);
	return *this;
}
SerialBuf & SerialBuf::operator << ( const lString32 & s )
{
	if ( check(2) )
		return *this;
	lString8 s8 = UnicodeToUtf8(s);
	lUInt16 len = (lUInt16)s8.length();
	(*this) << len;
	for ( int i=0; i<len; i++ ) {
		if ( check(1) )
			return *this;
		(*this) << (lUInt8)(s8[i]);
	}
	return *this;
}
SerialBuf & SerialBuf::operator << ( const lString8 & s8 )
{
	if ( check(2) )
		return *this;
	lUInt16 len = (lUInt16)s8.length();
	(*this) << len;
	for ( int i=0; i<len; i++ ) {
		if ( check(1) )
			return *this;
		(*this) << (lUInt8)(s8[i]);
	}
	return *this;
}

SerialBuf & SerialBuf::operator >> ( lUInt8 & n )
{
	if ( check(1) )
		return *this;
	n = _buf[_pos++];
	return *this;
}

SerialBuf & SerialBuf::operator >> ( char & n )
{
	if ( check(1) )
		return *this;
	n = (char)_buf[_pos++];
	return *this;
}

SerialBuf & SerialBuf::operator >> ( bool & n )
{
	if ( check(1) )
		return *this;
    n = _buf[_pos++] ? true : false;
	return *this;
}

SerialBuf & SerialBuf::operator >> ( lUInt16 & n )
{
	if ( check(2) )
		return *this;
	n = _buf[_pos++];
    n |= (((lUInt16)_buf[_pos++]) << 8);
	return *this;
}

SerialBuf & SerialBuf::operator >> ( lInt16 & n )
{
	if ( check(2) )
		return *this;
	n = (lInt16)(_buf[_pos++]);
    n |= (lInt16)(((lUInt16)_buf[_pos++]) << 8);
	return *this;
}

SerialBuf & SerialBuf::operator >> ( lUInt32 & n )
{
	if ( check(4) )
		return *this;
	n = _buf[_pos++];
    n |= (((lUInt32)_buf[_pos++]) << 8);
    n |= (((lUInt32)_buf[_pos++]) << 16);
    n |= (((lUInt32)_buf[_pos++]) << 24);
	return *this;
}

SerialBuf & SerialBuf::operator >> ( lInt32 & n )
{
	if ( check(4) )
		return *this;
	n = (lInt32)(_buf[_pos++]);
    n |= (((lUInt32)_buf[_pos++]) << 8);
    n |= (((lUInt32)_buf[_pos++]) << 16);
    n |= (((lUInt32)_buf[_pos++]) << 24);
	return *this;
}

SerialBuf & SerialBuf::operator >> ( lString8 & s8 )
{
	if ( check(2) )
		return *this;
    lUInt16 len = 0;
	(*this) >> len;
	s8.clear();
	s8.reserve(len);
	for ( int i=0; i<len; i++ ) {
		if ( check(1) )
			return *this;
        lUInt8 c = 0;
		(*this) >> c;
		s8.append(1, c);
	}
	return *this;
}

SerialBuf & SerialBuf::operator >> ( lString32 & s )
{
	lString8 s8;
	(*this) >> s8;
	s = Utf8ToUnicode(s8);
	return *this;
}

// read methods
bool SerialBuf::checkMagic( const char * s )
{
    if ( _error ) {
        return false;
    }
    while ( *s ) {
		if ( check(1) )
			return false;
        if ( _buf[ _pos++ ] != *s++ ) {
            seterror();
			return false;
        }
	}
	return true;
}

/// add CRC32 for last N bytes
void SerialBuf::putCRC( int size )
{
    if ( error() )
        return;
    if ( size>_pos ) {
        *this << (lUInt32)0;
        seterror();
    }
    lUInt32 n = 0;
    n = lStr_crc32( n, _buf + _pos-size, size );
    *this << n;
}

/// get CRC32 for the whole buffer
lUInt32 SerialBuf::getCRC()
{
    if (error())
        return 0;
    lUInt32 n = 0;
    n = lStr_crc32( n, _buf, _pos );
    return n;
}

/// read crc32 code, comapare with CRC32 for last N bytes
bool SerialBuf::checkCRC( int size )
{
    if ( error() )
        return false;
    if ( size>_pos ) {
        seterror();
        return false;
    }
    lUInt32 n0 = 0;
    n0 = lStr_crc32(n0, _buf + _pos-size, size);
    lUInt32 n = 0;
    *this >> n;
    if ( error() )
        return false;
    if ( n!=n0 )
        seterror();
    return !error();
}
