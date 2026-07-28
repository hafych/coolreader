/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2010-2015 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2012 Daniel Savard <daniels@xsoli.com>                  *
 *   Copyright (C) 2018-2021 Aleksey Chernov <valexlin@gmail.com>          *
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

#include "../include/crsetup.h"
//#define CHM_SUPPORT_ENABLED 1
#if CHM_SUPPORT_ENABLED==1
#include "../include/chmfmt.h"
#include "chmfmt_internal.h"
#include "../include/lvnamedstream.h"
#include "../include/lvstreamutils.h"
#include "../include/lvnamedcontainer.h"
#include "../include/crtxtenc.h"
#include "../include/lvxmlutils.h"
#include "../include/lvhtmlparser.h"
#include "../include/crlog.h"
#include <chm_lib.h>

#include <memory>

#define DUMP_CHM_DOC 0

struct crChmExternalFileStream : public chmExternalFileStream {
    /** returns file size, in bytes, if opened successfully */
    //LONGUINT64 (open)( chmExternalFileStream * instance );
    /** reads bytes to buffer */
    //LONGINT64 (read)( chmExternalFileStream * instance, unsigned char * buf, LONGUINT64 pos, LONGINT64 len );
    /** closes file */
    //int (close)( chmExternalFileStream * instance );
    LVStreamRef stream;
    static LONGUINT64 cr_open( chmExternalFileStream * instance )
    {
        return (LONGINT64)((crChmExternalFileStream*)instance)->stream->GetSize();
    }
    /** reads bytes to buffer */
    static LONGINT64 cr_read( chmExternalFileStream * instance, unsigned char * buf, LONGUINT64 pos, LONGINT64 len )
    {
        lvsize_t bytesRead = 0;
        if ( ((crChmExternalFileStream*)instance)->stream->SetPos( (lvpos_t)pos )!= pos )
            return 0;
        if ( ((crChmExternalFileStream*)instance)->stream->Read( buf, (lvsize_t)len, &bytesRead ) != LVERR_OK )
            return false;
        return bytesRead;
    }
    /** closes file */
    static int cr_close( chmExternalFileStream * instance )
    {
        ((crChmExternalFileStream*)instance)->stream.Clear();
		return 0;
    }
    crChmExternalFileStream( LVStreamRef s )
    : stream(s)
    {
        open = cr_open;
        read = cr_read;
        close = cr_close;
    }
};

class LVCHMFile : public LVRefCounter
{
    crChmExternalFileStream _stream;
    /// Owned external handle, paired with chm_close() in the destructor.
    chmFile *_file;
public:
    explicit LVCHMFile(LVStreamRef stream)
        : _stream(stream), _file(NULL)
    {
    }
    ~LVCHMFile()
    {
        if (_file)
            chm_close(_file);
    }
    bool open()
    {
        _file = chm_open(&_stream);
        return _file != NULL;
    }
    chmFile *get() const
    {
        return _file;
    }
private:
    LVCHMFile(const LVCHMFile &) = delete;
    LVCHMFile &operator=(const LVCHMFile &) = delete;
};

typedef LVFastRef<LVCHMFile> LVCHMFileRef;

class LVCHMStream : public LVNamedStream
{
protected:
    LVCHMFileRef _file;
    chmUnitInfo m_ui;
    lvpos_t m_pos;
    lvpos_t m_size;
public:
    explicit LVCHMStream(LVCHMFileRef file)
            : _file(file), m_pos(0), m_size(0)
    {
    }
    bool open( const char * name )
    {
        memset(&m_ui, 0, sizeof(m_ui));
        if ( CHM_RESOLVE_SUCCESS
                == chm_resolve_object(_file->get(), name, &m_ui) ) {
            m_size = (lvpos_t)m_ui.length;
            return true;
        }
        return false;
    }

    lverror_t Seek( lvoffset_t offset, lvseek_origin_t origin,
                    lvpos_t * pNewPos ) override
    {
        //
        lvpos_t newpos = m_pos;
        switch ( origin )
        {
        case LVSEEK_SET:
            newpos = offset;
            break;
        case LVSEEK_CUR:
            newpos += offset;
            break;
        case LVSEEK_END:
            newpos = m_size + offset;
            break;
        }
        if ( newpos>m_size )
            return LVERR_FAIL;
        if ( pNewPos!=NULL )
            *pNewPos = newpos;
        m_pos = newpos;
        return LVERR_OK;
    }

    /// Tell current file position
    /**
        \param pNewPos points to place to store file position
        \return lverror_t status: LVERR_OK if success
    */
    lverror_t Tell( lvpos_t * pPos ) override
    {
        *pPos = m_pos;
        return LVERR_OK;
    }

    lvpos_t SetPos(lvpos_t p) override
    {
        if ( p<=m_size ) {
            m_pos = p;
            return m_pos;
        }
        return (lvpos_t)(~0);
    }

    /// Get file position
    /**
        \return lvpos_t file position
    */
    lvpos_t GetPos() override
    {
        return m_pos;
    }

    /// Get file size
    /**
        \return lvsize_t file size
    */
    lvsize_t GetSize() override
    {
        return m_size;
    }

    lverror_t GetSize( lvsize_t * pSize ) override
    {
        *pSize = m_size;
        return LVERR_OK;
    }

    lverror_t Read(
            void * buf, lvsize_t count, lvsize_t * nBytesRead) override
    {
        int cnt = (int)count;
        if ( m_pos + cnt > m_size )
            cnt = (int)(m_size - m_pos);
        if ( cnt <= 0 )
            return LVERR_FAIL;
        LONGINT64 gotBytes = chm_retrieve_object(
                _file->get(), &m_ui, (unsigned char *)buf, m_pos, cnt);
        m_pos += (lvpos_t)gotBytes;
        if (nBytesRead)
            *nBytesRead = (lvsize_t)gotBytes;
        return LVERR_OK;
    }


    lverror_t Write( const void * /*buf*/, lvsize_t /*count*/,
                     lvsize_t * /*nBytesWritten*/ ) override
    {
        return LVERR_FAIL;
    }

    bool Eof() override
    {
        return (m_pos >= m_size);
    }

    lverror_t SetSize( lvsize_t size ) override
    {
        CR_UNUSED(size);
        // support only size grow
        return LVERR_FAIL;
    }


};

class LVCHMContainer : public LVNamedContainer
{
protected:
    /// Retained only until LVCHMFile adopts an anchoring stream reference.
    LVStreamRef _source;
    LVCHMFileRef _file;
public:
    LVStreamRef OpenStream(
            const lChar32 * fname, lvopen_mode_t mode) override
    {
        LVStreamRef stream;
        if ( mode!=LVOM_READ )
            return stream;

        std::unique_ptr<LVCHMStream> candidate(new LVCHMStream(_file));
        lString32 fn(fname);
        if (fn.empty())
            return stream;
        if ( fn[0]!='/' )
            fn = cs32("/") + fn;
        if ( !candidate->open(UnicodeToUtf8(lString32(fn)).c_str()) )
            return stream;
        stream = LVStreamRef(candidate.release());
        stream->SetName( fname );
        return stream;
    }
    LVContainer * GetParentContainer() override
    {
        return NULL;
    }
    const LVContainerItemInfo * GetObjectInfo(int index) override
    {
        if (index>=0 && index<m_list.length())
            return m_list[index];
        return NULL;
    }
    int GetObjectCount() const override
    {
        return m_list.length();
    }
    lverror_t GetSize( lvsize_t * pSize ) override
    {
        if (m_fname.empty())
            return LVERR_FAIL;
        *pSize = GetObjectCount();
        return LVERR_OK;
    }
    explicit LVCHMContainer(LVStreamRef source) : _source(source)
    {
    }
    ~LVCHMContainer() override = default;

    void addFileItem( const char * filename, LONGUINT64 len )
    {
        std::unique_ptr<LVCommonContainerItemInfo> item(
                new LVCommonContainerItemInfo());
        item->SetItemInfo( lString32(filename), (lvsize_t)len, 0, false );
        //CRLog::trace("CHM file item: %s [%d]", filename, (int)len);
        Add(item.release());
    }

    static int CHM_ENUMERATOR_CALLBACK (struct chmFile * /*h*/,
                              struct chmUnitInfo *ui,
                              void *context)
    {
        LVCHMContainer * c = (LVCHMContainer*)context;
        if ( (ui->flags & CHM_ENUMERATE_FILES) && (ui->flags & CHM_ENUMERATE_NORMAL) ) {
            c->addFileItem( ui->path, ui->length );
        }
        return CHM_ENUMERATOR_CONTINUE;
    }

    bool open()
    {
        std::unique_ptr<LVCHMFile> file(new LVCHMFile(_source));
        if ( !file->open() )
            return false;
        _file = LVCHMFileRef(file.release());
        _source.Clear();
        chm_enumerate( _file->get(),
                  CHM_ENUMERATE_ALL,
                  CHM_ENUMERATOR_CALLBACK,
                  this);
        return true;
    }
private:
    LVCHMContainer(const LVCHMContainer &) = delete;
    LVCHMContainer &operator=(const LVCHMContainer &) = delete;
};

/// opens CHM container
LVContainerRef LVOpenCHMContainer( LVStreamRef stream )
{
    std::unique_ptr<LVCHMContainer> chm(new LVCHMContainer(stream));
    if ( !chm->open() )
        return LVContainerRef();
    chm->SetName( stream->GetName() );
    return LVContainerRef(chm.release());
}

bool DetectCHMFormat( LVStreamRef stream )
{
    stream->SetPos(0);
    LVContainerRef cont = LVOpenCHMContainer( stream );
    if ( !cont.isNull() ) {
        return true;
    }
    return false;
}

class CHMBinaryReader {
    LVStreamRef _stream;
public:
    CHMBinaryReader( LVStreamRef stream ) : _stream(stream) {
    }
    bool setPos( int offset ) {
        return (int)_stream->SetPos(offset) == offset;
    }
    bool eof() {
        return _stream->Eof();
    }

    lUInt32 readInt32( bool & error ) {
        int b1 = _stream->ReadByte();
        int b2 = _stream->ReadByte();
        int b3 = _stream->ReadByte();
        int b4 = _stream->ReadByte();
        if ( b1==-1 || b2==-1  || b3==-1  || b4==-1 ) {
            error = true;
            return 0;
        }
        return (lUInt32)(b1 | (b2<<8) | (b3<<16) | (b4<<24));
    }
    lUInt16 readInt16( bool & error ) {
        int b1 = _stream->ReadByte();
        int b2 = _stream->ReadByte();
        if ( b1==-1 || b2==-1 ) {
            error = true;
            return 0;
        }
        return (lUInt16)(b1 | (b2<<8));
    }
    lUInt8 readInt8( bool & error ) {
        int b = _stream->ReadByte();
        if ( b==-1 ) {
            error = true;
            return 0;
        }
        return (lUInt8)(b & 0xFF);
    }
    int bytesLeft() {
        return (int)(_stream->GetSize() - _stream->GetPos());
    }

    bool readBytes( LVArray<lUInt8> & bytes, int offset, int length ) {
        bytes.clear();
        bytes.reserve(length);
        if ( offset>=0 )
            if ((int)_stream->SetPos(offset) != offset)
                return false;
        for (int i = 0; i < length; i++) {
            int b = _stream->ReadByte();
            if ( b==-1 )
                return false;
            bytes[i] = (lUInt8)b;
        }
        return true;
    }

    // offset==-1 to avoid changing position, length==-1 to use 0-terminated
    lString8 readString( int offset, int length ) {
        if ( length==0 )
            return lString8::empty_str;
        if ( offset>=0 )
            if ((int)_stream->SetPos(offset) != offset)
                return lString8::empty_str;
        lString8 res;
        if ( length>0 )
            res.reserve(length);
        bool zfound = false;
        for ( int i=0; i<length || length==-1; i++ ) {
            int b = _stream->ReadByte();
            if (zfound || (b==0 && length>=0)) {
                zfound = true;
                continue;
            }
            if ( b==-1 || b==0 )
                break;
            res.append(1, (lUInt8)b);
        }
        return res;
    }
    // offset==-1 to avoid changing position, length==-1 to use 0-terminated
    lString32 readStringUtf16( int offset, int length ) {
        if ( length==0 )
            return lString32::empty_str;
        if ( offset>=0 )
            if ((int)_stream->SetPos(offset) != offset)
                return lString32::empty_str;
        lString32 res;
        if ( length>0 )
            res.reserve(length);
        for ( int i=0; i<length || length==-1; i++ ) {
            int b1 = _stream->ReadByte();
            if ( b1==-1 || b1==0 )
                break;
            int b2 = _stream->ReadByte();
            if ( b2==-1 || b2==0 )
                break;
            res.append(1, (lChar32)(b1 | (b2<<16)));
        }
        return res;
    }
    lInt64 readEncInt() {
        lInt64 res = 0;
        int shift = 0;
        int b = 0;
        do {
            b = _stream->ReadByte();
            if ( b==-1 )
                return 0;
            res |= ( ((lInt64)(b&0x7F)) << shift );
            shift+=7;
        } while ( b&0x80 );
        return res;
    }
};

class CHMUrlStrEntry {
public:
    lUInt32 offset;
    lString8 url;
};

const int URLSTR_BLOCK_SIZE = 0x1000;
class CHMUrlStr {
    LVContainerRef _container;
    CHMBinaryReader _reader;
    LVPtrVector<CHMUrlStrEntry> _table;

    CHMUrlStr( LVContainerRef container, LVStreamRef stream ) : _container(container), _reader(stream)
    {

    }
    lUInt32 readInt32( const lUInt8 * & data ) {
        lUInt32 res = 0;
        res = *(data++);
        res = res | (((lUInt32)(*(data++))) << 8);
        res = res | (((lUInt32)(*(data++))) << 16);
        res = res | (((lUInt32)(*(data++))) << 24);
        return res;
    }
    lString8 readString( const lUInt8 * & data, int maxlen ) {
        lString8 res;
        for ( int i=0; i<maxlen; i++ ) {
            lUInt8 b = *data++;
            if ( b==0 )
                break;
            res.append(1, b);
        }
        return res;
    }


    bool decodeBlock( const lUInt8 * ptr, lUInt32 blockOffset, int size ) {
        const lUInt8 * data = ptr;
        const lUInt8 * maxdata = ptr + size;
        while ( data + 8 < maxdata ) {
            lUInt32 offset = (lUInt32)(blockOffset + (data - ptr));
            //lUInt32 urlOffset =
            readInt32(data);
            //lUInt32 frameOffset =
            readInt32(data);
            if ( data < maxdata ) { //urlOffset > offset ) {
                std::unique_ptr<CHMUrlStrEntry> item(
                        new CHMUrlStrEntry());
                item->offset = offset;
                item->url = readString(data, (int)(maxdata - data));
                //CRLog::trace("urlstr[offs=%x, url=%s]", item->offset, item->url.c_str());
                _table.add(item.release());
            }
        }
        return true;
    }

    bool read() {
        bool err = false;
        LVArray<lUInt8> bytes;
        _reader.readInt8(err);
        lUInt32 offset = 1;
        while ( !_reader.eof() && !err ) {
            int sz = _reader.bytesLeft();
            if ( sz>URLSTR_BLOCK_SIZE )
                sz = URLSTR_BLOCK_SIZE;
            err = !_reader.readBytes(bytes, -1, sz) || err;
            if ( err )
                break;
            err = !decodeBlock( bytes.get(), offset, sz ) || err;
            offset += sz;
        }
        return !err;
    }
public:
    static std::unique_ptr<CHMUrlStr> open(LVContainerRef container) {
        LVStreamRef stream = container->OpenStream(U"#URLSTR", LVOM_READ);
        if ( stream.isNull() )
            return std::unique_ptr<CHMUrlStr>();
        std::unique_ptr<CHMUrlStr> res(new CHMUrlStr(container, stream));
        if ( !res->read() )
            return std::unique_ptr<CHMUrlStr>();
        CRLog::info("CHM URLSTR: %d entries read", res->_table.length());
        return res;
    }
    lString8 findByOffset( lUInt32 offset ) {
        for ( int i=0; i<_table.length(); i++ ) {
            if ( _table[i]->offset==offset )
                return _table[i]->url;
        }
        return lString8::empty_str;
    }
    void getUrlList( lString32Collection & urlList ) {
        for ( int i=0; i<_table.length(); i++ ) {
            lString8 s = _table[i]->url;
            if ( !s.empty() ) {
                urlList.add(Utf8ToUnicode(s));
            }
        }
    }
private:
    CHMUrlStr(const CHMUrlStr &) = delete;
    CHMUrlStr &operator=(const CHMUrlStr &) = delete;
};

class CHMUrlTableEntry {
public:
    lUInt32 offset;
    lUInt32 id;
    lUInt32 topicsIndex;
    lUInt32 urlStrOffset;
    CHMUrlTableEntry()
    : offset(0)
    , id(0)
    , topicsIndex(0)
    , urlStrOffset(0)
    {

    }
};

const int URLTBL_BLOCK_SIZE = 0x1000;
const int URLTBL_BLOCK_RECORD_COUNT = 341;
class CHMUrlTable {
    LVContainerRef _container;
    CHMBinaryReader _reader;
    LVPtrVector<CHMUrlTableEntry> _table;
    std::unique_ptr<CHMUrlStr> _strings;


    CHMUrlTable(LVContainerRef container, LVStreamRef stream)
        : _container(container), _reader(stream)
    {

    }
    lUInt32 readInt32( const lUInt8 * & data ) {
        lUInt32 res = 0;
        res = *(data++);
        res = res | (((lUInt32)(*(data++))) << 8);
        res = res | (((lUInt32)(*(data++))) << 16);
        res = res | (((lUInt32)(*(data++))) << 24);
        return res;
    }

    bool decodeBlock( const lUInt8 * data, lUInt32 offset, int size ) {
        for ( int i=0; i<URLTBL_BLOCK_RECORD_COUNT && size>0; i++ ) {
            std::unique_ptr<CHMUrlTableEntry> item(
                    new CHMUrlTableEntry());
            item->offset = offset;
            item->id = readInt32(data);
            item->topicsIndex = readInt32(data);
            item->urlStrOffset = readInt32(data);
            //CRLog::trace("urltbl[offs=%x, id=%x, ti=%x, urloffs=%x]", item->offset, item->id, item->topicsIndex, item->urlStrOffset);
            _table.add(item.release());
            offset += 4*3;
            size -= 4*3;
        }
        return true;
    }

    bool read() {
        bool err = false;
        LVArray<lUInt8> bytes;
        lUInt32 offset = 0;
        while ( !_reader.eof() && !err ) {
            int sz = _reader.bytesLeft();
            if ( sz>URLTBL_BLOCK_SIZE )
                sz = URLTBL_BLOCK_SIZE;
            err = !_reader.readBytes(bytes, -1, sz) || err;
            if ( err )
                break;
            err = !decodeBlock( bytes.get(), offset, sz ) || err;
            offset += sz;
        }
        _strings = CHMUrlStr::open(_container);
        if ( !_strings ) {
            CRLog::warn("CHM: cannot read #URLSTR");
        }
        return !err;
    }
public:
    ~CHMUrlTable() = default;

    static std::unique_ptr<CHMUrlTable> open(LVContainerRef container) {
        LVStreamRef stream = container->OpenStream(U"#URLTBL", LVOM_READ);
        if ( stream.isNull() )
            return std::unique_ptr<CHMUrlTable>();
        std::unique_ptr<CHMUrlTable> res(
                new CHMUrlTable(container, stream));
        if ( !res->read() )
            return std::unique_ptr<CHMUrlTable>();
        CRLog::info("CHM URLTBL: %d entries read", res->_table.length());
        return res;
    }

    lString8 urlById( lUInt32 id ) {
        if ( !_strings )
            return lString8::empty_str;
        for ( int i=0; i<_table.length(); i++ ) {
            if ( _table[i]->id==id )
                return _strings->findByOffset( _table[i]->urlStrOffset );
        }
        return lString8::empty_str;
    }

    CHMUrlTableEntry * findById( lUInt32 id ) {
        for ( int i=0; i<_table.length(); i++ ) {
            if ( _table[i]->id==id )
                return _table[i];
        }
        return NULL;
    }
    CHMUrlTableEntry * findByOffset( lUInt32 offset ) {
        for ( int i=0; i<_table.length(); i++ ) {
            if ( _table[i]->offset==offset )
                return _table[i];
        }
        return NULL;
    }

    void getUrlList( lString32Collection & urlList ) {
        if ( !_strings )
            return;
        _strings->getUrlList( urlList );
//        for ( int i=0; i<_table.length(); i++ ) {
//            lString8 s = _strings->findByOffset( _table[i]->urlStrOffset );
//            if ( !s.empty() ) {
//                urlList.add(Utf8ToUnicode(s));
//            }
//        }
    }
private:
    CHMUrlTable(const CHMUrlTable &) = delete;
    CHMUrlTable &operator=(const CHMUrlTable &) = delete;
};

class CHMSystem {

    LVContainerRef _container;
    CHMBinaryReader _reader;
    lUInt32 _fileVersion;
    lString8 _contentsFile;
    lString8 _indexFile;
    lString8 _defaultTopic;
    lString8 _title;
    lString8 _language;
    lString8 _defaultFont;
    lUInt32  _lcid;
    bool _dbcs;
    bool _fullTextSearch;
    bool _hasKLinks;
    bool _hasALinks;
    lUInt32 _binaryIndexURLTableId;
    lUInt32 _binaryTOCURLTableId;
    const lChar32 * _enc_table;
    lString32 _enc_name;
    std::unique_ptr<CHMUrlTable> _urlTable;

    CHMSystem( LVContainerRef container, LVStreamRef stream ) : _container(container), _reader(stream)
    , _fileVersion(0)
    , _lcid(0)
    , _dbcs(false)
    , _fullTextSearch(false)
    , _hasKLinks(false)
    , _hasALinks(false)
    , _binaryIndexURLTableId(0)
    , _binaryTOCURLTableId(0)
    , _enc_table(NULL)
    {
    }

    bool decodeEntry() {
        bool err = false;
        int code = _reader.readInt16(err);
        int length = _reader.readInt16(err);
        //CRLog::trace("CHM binary item code=%d, length=%d, bytesLeft=%d", code, length, _reader.bytesLeft());
        if ( err )
            return false;
        LVArray<lUInt8> bytes;
        switch( code ) {
        case 0:
            _contentsFile = _reader.readString(-1, length);
            break;
        case 1:
            _indexFile = _reader.readString(-1, length);
            break;
        case 2:
            _defaultTopic = _reader.readString(-1, length);
            break;
        case 3:
            _title = _reader.readString(-1, length);
            break;
        case 4:
            {
                _lcid = _reader.readInt32(err);
                int codepage = langToCodepage( _lcid );
                const lChar32 * enc_name = GetCharsetName( codepage );
                const lChar32 * table = GetCharsetByte2UnicodeTable( codepage );
		_language = langToLanguage( _lcid );
                if ( enc_name!=NULL ) {
                    _enc_table = table;
                    _enc_name = lString32(enc_name);
                    CRLog::info("CHM LCID: %08x, charset=%s", _lcid, LCSTR(_enc_name));
                } else {
                    CRLog::info("CHM LCID: %08x -- cannot find charset encoding table", _lcid);
                }
                _dbcs = _reader.readInt32(err)==1;
                _fullTextSearch = _reader.readInt32(err)==1;
                _hasKLinks = _reader.readInt32(err)==1;
                _hasALinks = _reader.readInt32(err)==1;
                err = !_reader.readBytes(bytes, -1, length - (5*4)) || err;
            }
            break;
        case 7:
            if ( _fileVersion>2 )
                _binaryIndexURLTableId = _reader.readInt32(err);
            else
                err = !_reader.readBytes(bytes, -1, length) || err;
            break;
        case 11:
            if ( _fileVersion>2 )
                _binaryTOCURLTableId = _reader.readInt32(err);
            else
                err = !_reader.readBytes(bytes, -1, length) || err;
            break;
        case 16:
            _defaultFont = _reader.readString(-1, length);
            CRLog::info("CHM default font: %s", _defaultFont.c_str());
            if ( _enc_table==NULL ) {
                for ( int i=_defaultFont.length()-1; i>0; i-- ) {
                    if ( _defaultFont[i]==',' ) {
                        int cs = _defaultFont.substr(i+1, _defaultFont.length()-i-1).atoi();
                        const lChar32 * cpname = NULL;
                        switch (cs) {
                        case 0x00: cpname = U"windows-1252"; break;
                        case 0xCC: cpname = U"windows-1251"; break;
                        case 0xEE: cpname = U"windows-1250"; break;
                        case 0xA1: cpname = U"windows-1253"; break;
                        case 0xA2: cpname = U"windows-1254"; break;
                        case 0xBA: cpname = U"windows-1257"; break;
                        case 0xB1: cpname = U"windows-1255"; break;
                        case 0xB2: cpname = U"windows-1256"; break;
                        default: break;
                        }
                        const lChar32 * table = GetCharsetByte2UnicodeTable( cpname );
                        if ( cpname!=NULL && table!=NULL ) {
                            CRLog::info("CHM charset detected from default font: %s", LCSTR(lString32(cpname)));
                            _enc_table = table;
                            _enc_name = lString32(cpname);
                        }
                        break;
                    }
                }
            }
            break;
        default:
            err = !_reader.readBytes(bytes, -1, length) || err;
            break;
        }
        return !err;
    }

    bool read() {
        bool err = false;
        _fileVersion = _reader.readInt32(err);
        int count = 0;
        while ( !_reader.eof() && !err ) {
            err = !decodeEntry() || err;
            if ( !err )
                count++;
        }

        if ( err ) {
            CRLog::error("CHM decoding error: %d blocks decoded, stream bytes left=%d", count, _reader.bytesLeft() );
            return false;
        }
        if ( _enc_table==NULL ) {
            _enc_table = GetCharsetByte2UnicodeTable( 1252 );
            _enc_name = cs32("windows-1252");
        }
        _urlTable = CHMUrlTable::open(_container);
        return !err;
    }

public:
    ~CHMSystem() = default;

    static std::unique_ptr<CHMSystem> open(LVContainerRef container) {
        LVStreamRef stream = container->OpenStream(U"#SYSTEM", LVOM_READ);
        if ( stream.isNull() )
            return std::unique_ptr<CHMSystem>();
        std::unique_ptr<CHMSystem> res(new CHMSystem(container, stream));
        if ( !res->read() )
            return std::unique_ptr<CHMSystem>();
        return res;
    }

    lString32 decodeString( const lString8 & str ) {
        return ByteToUnicode( str, _enc_table );
    }

    lString32 getTitle() {
        return decodeString(_title);
    }

    lString32 getLanguage() {
        return decodeString(_language);
    }

    lString32 getDefaultTopic() {
        return decodeString(_defaultTopic);
    }

    lString32 getEncodingName() {
        return _enc_name;
    }

    lString32 getContentsFileName() {
        if ( _binaryTOCURLTableId!=0 && _urlTable != NULL ) {
            lString8 url = _urlTable->urlById(_binaryTOCURLTableId);
            if ( !url.empty() )
                return decodeString(url);
        }
        if ( _contentsFile.empty() ) {
            lString32 hhcName;
            int bestSize = 0;
            for ( int i=0; i<_container->GetObjectCount(); i++ ) {
                const LVContainerItemInfo * item = _container->GetObjectInfo(i);
                if ( !item->IsContainer() ) {
                    lString32 name = item->GetName();
                    int sz = item->GetSize();
                    //CRLog::trace("CHM item: %s", LCSTR(name));
                    lString32 lname = name;
                    lname.lowercase();
                    if ( lname.endsWith(".hhc") ) {
                        if ( sz > bestSize ) {
                            hhcName = name;
                            bestSize = sz;
                        }
                    }
                }
            }
            if ( !hhcName.empty() )
                return hhcName;
        }
        return decodeString(_contentsFile);
    }
    void getUrlList( lString32Collection & urlList ) {
        if ( !_urlTable )
            return;
        _urlTable->getUrlList(urlList);
    }
private:
    CHMSystem(const CHMSystem &) = delete;
    CHMSystem &operator=(const CHMSystem &) = delete;
};

bool LVRunChmMetadataOwnershipRegression()
{
    class MetadataFixtureContainer : public LVContainer {
        bool _malformedSystem;
    public:
        explicit MetadataFixtureContainer(bool malformedSystem)
            : _malformedSystem(malformedSystem)
        {
        }
        LVContainer *GetParentContainer() override
        {
            return NULL;
        }
        const LVContainerItemInfo *GetObjectInfo(int) override
        {
            return NULL;
        }
        int GetObjectCount() const override
        {
            return 0;
        }
        lverror_t GetSize(lvsize_t *size) override
        {
            if (!size)
                return LVERR_FAIL;
            *size = 0;
            return LVERR_OK;
        }
        LVStreamRef OpenStream(
                const lChar32 *name, lvopen_mode_t mode) override
        {
            if (!name || mode != LVOM_READ)
                return LVStreamRef();
            static const lUInt8 validSystem[] = { 3, 0, 0, 0 };
            static const lUInt8 malformedSystem[] =
                    { 3, 0, 0, 0, 1, 0 };
            static const lUInt8 urlTable[] =
                    { 7, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0 };
            static const lUInt8 urlStrings[] =
                    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 'a', 0 };
            const lString32 streamName(name);
            const lUInt8 *bytes = NULL;
            int size = 0;
            if (streamName == U"#SYSTEM") {
                bytes = _malformedSystem
                        ? malformedSystem : validSystem;
                size = _malformedSystem
                        ? sizeof(malformedSystem) : sizeof(validSystem);
            } else if (streamName == U"#URLTBL") {
                bytes = urlTable;
                size = sizeof(urlTable);
            } else if (streamName == U"#URLSTR") {
                bytes = urlStrings;
                size = sizeof(urlStrings);
            } else {
                return LVStreamRef();
            }
            return LVCreateMemoryStream(
                    const_cast<lUInt8 *>(bytes),
                    size, true, LVOM_READ);
        }
    };

    for (int iteration = 0; iteration < 3; ++iteration) {
        LVContainerRef fixture(new MetadataFixtureContainer(false));
        std::unique_ptr<CHMSystem> system = CHMSystem::open(fixture);
        lString32Collection urls;
        if (!system)
            return false;
        system->getUrlList(urls);
        if (system->getEncodingName() != U"windows-1252"
                || urls.length() != 1 || urls[0] != U"a")
            return false;
    }
    LVContainerRef malformed(new MetadataFixtureContainer(true));
    return !CHMSystem::open(malformed);
}

ldomDocument * LVParseCHMHTMLStream( LVStreamRef stream, lString32 defEncodingName, ldomDocument * parent_doc )
{
    if ( stream.isNull() )
        return NULL;

#if 0
    // detect encondig
    stream->SetPos(0);

    std::unique_ptr<ldomDocument> encDetectionDoc(
            LVParseHTMLStream(stream));
    int encoding = 0;
    if ( encDetectionDoc!=NULL ) {
        ldomNode * node = encDetectionDoc->nodeFromXPath(U"/html/body/object[1]");
        if ( node!=NULL ) {
            for ( int i=0; i<node->getChildCount(); i++ ) {
                ldomNode * child = node->getChildNode(i);
                if (child && child->isElement() && child->getNodeName() == "param" && child->getAttributeValue(U"name") == "Font") {
                    lString32 s = child->getAttributeValue(U"value");
                    lString32 lastDigits;
                    for ( int i=s.length()-1; i>=0; i-- ) {
                        lChar32 ch = s[i];
                        if ( ch>='0' && ch<='9' )
                            lastDigits.insert(0, 1, ch);
                        else
                            break;
                    }
                    encoding = lastDigits.atoi();
                    CRLog::debug("LVParseCHMHTMLStream: encoding detected: %d", encoding);
                }
            }
        }
    }
    const lChar32 * enc = U"cp1252";
    if ( encoding==1 ) {
        enc = U"cp1251";
    }
#endif

    stream->SetPos(0);
    std::unique_ptr<ldomDocument> doc(new ldomDocument());
    doc->setDocFlags( 0 );
    doc->setAllTypesFrom(parent_doc);

    ldomDocumentWriterFilter writerFilter(
            doc.get(), false, HTML_AUTOCLOSE_TABLE);
    writerFilter.setFlags(writerFilter.getFlags() | TXTFLG_CONVERT_8BIT_ENTITY_ENCODING);

    /// HTML format
    LVHTMLParser parser(stream, &writerFilter);
    if ( !defEncodingName.empty() )
        parser.SetCharset(defEncodingName.c_str());
    if ( parser.CheckFormat() && parser.Parse() )
        return doc.release();
    return NULL;
}

static int filename_comparator(lString32 & _s1, lString32 & _s2) {
    lString32 s1 = _s1.substr(1);
    lString32 s2 = _s2.substr(1);
    if (s1.endsWith(".htm"))
        s1.erase(s1.length()-4, 4);
    else if (s1.endsWith(".html"))
        s1.erase(s1.length()-5, 5);
    if (s2.endsWith(".htm"))
        s2.erase(s2.length()-4, 4);
    else if (s2.endsWith(".html"))
        s2.erase(s2.length()-5, 5);
    if (s1 == "index")
        return -1;
    else if (s2 == "index")
        return 1;
    if (s1 == "header")
        return -1;
    else if (s2 == "header")
        return 1;
    int d1 = 0;
    int d2 = 0;
    s1.atoi(d1);
    s2.atoi(d2);
    if (d1 || d2) {
        if (d1 && d2) {
            if (d1 < d2)
                return -1;
            else if (d1 > d2)
                return 1;
            return 0;
        } else if (d1) {
            return -1;
        } else {
            return 1;
        }
    }
    return s1.compare(s2);
}

class CHMTOCReader {
    LVContainerRef _cont;
    /// Borrowed writer and document views scoped to ImportCHMDocument().
    ldomDocumentFragmentWriter * _appender;
    ldomDocument * _doc;
    /// Non-owning cursor into the document-owned TOC.
    LVTocItem * _toc;
    lString32HashedCollection _fileList;
    lString32 lastFile;
    lString32 _defEncodingName;
    bool _fakeToc;
public:
    CHMTOCReader( LVContainerRef cont, ldomDocument * doc, ldomDocumentFragmentWriter * appender )
        : _cont(cont), _appender(appender), _doc(doc), _fileList(1024)
    {
        _toc = _doc->getToc();
    }
    void addFile( const lString32 & v1 ) {
        int index = _fileList.find(v1.c_str());
        if ( index>=0 )
            return; // already added
        _fileList.add(v1.c_str());
        CRLog::trace("New source file: %s", LCSTR(v1) );
        _appender->addPathSubstitution( v1, cs32("_doc_fragment_") + fmt::decimal(_fileList.length()) );
        _appender->setCodeBase( v1 );
    }

    void addTocItem( lString32 name, lString32 url, int level )
    {
        //CRLog::trace("CHM toc level %d: '%s' : %s", level, LCSTR(name), LCSTR(url) );
        if (url.startsWith(".."))
            url = LVExtractFilename( url );
        lString32 v1, v2;
        if ( !url.split2(cs32("#"), v1, v2) )
            v1 = url;
        PreProcessXmlString( name, 0 );
        addFile(v1);
        lString32 url2 = _appender->convertHref(url);
        //CRLog::trace("new url: %s", LCSTR(url2) );
        while ( _toc->getLevel()>level && _toc->getParent() )
            _toc = _toc->getParent();
        _toc = _toc->addChild(name, ldomXPointer(), url2);
    }

    void recurseToc( ldomNode * node, int level )
    {
        lString32 nodeName = node->getNodeName();
        lUInt16 paramElemId = node->getDocument()->getElementNameIndex(U"param");
        if (nodeName == "object") {
            if ( level>0 ) {
                // process object
                if (node->getAttributeValue("type") == "text/sitemap") {
                    lString32 name, local;
                    int cnt = node->getChildCount();
                    for ( int i=0; i<cnt; i++ ) {
                        ldomNode * child = node->getChildElementNode(i, paramElemId);
                        if ( child ) {
                            lString32 paramName = child->getAttributeValue("name");
                            lString32 paramValue = child->getAttributeValue("value");
                            if (paramName == "Name")
                                name = paramValue;
                            else if (paramName == "Local")
                                local = paramValue;
                        }
                    }
                    if ( !local.empty() && !name.empty() ) {
                        // found!
                        addTocItem( name, local, level );
                    }
                }
            }
            return;
        }
        if (nodeName == "ul")
            level++;
        int cnt = node->getChildCount();
        for ( int i=0; i<cnt; i++ ) {
            ldomNode * child = node->getChildElementNode(i);
            if ( child ) {
                recurseToc( child, level );
            }
        }
    }

    bool init( LVContainerRef cont, lString32 hhcName, lString32 defEncodingName, lString32Collection & urlList, lString32 mainPageName )
    {
        if ( hhcName.empty() && urlList.length()==0 ) {
            lString32Collection htms;
            for (int i=0; i<cont->GetObjectCount(); i++) {
                const LVContainerItemInfo * item = cont->GetObjectInfo(i);
                if (item->IsContainer())
                    continue;
                lString32 name = item->GetName();
                if (name == "/bookindex.htm" || name == "/headerindex.htm")
                    continue;
                //CRLog::trace("item %d : %s", i, LCSTR(name));
                if (name.endsWith(".htm") || name.endsWith(".html"))
                    htms.add(name);
            }
            if (!htms.length())
                return false;
//            {
//                for (int j=0; j<htms.length(); j++) {
//                    CRLog::trace("unsorted %d : %s", j, LCSTR(htms[j]));
//                }
//            }
            htms.sort(filename_comparator);
//            {
//                for (int j=0; j<htms.length(); j++) {
//                    CRLog::trace("sorted %d : %s", j, LCSTR(htms[j]));
//                }
//            }
            urlList.addAll(htms);
        }
        _defEncodingName = defEncodingName;

        if ( !mainPageName.empty() )
            addFile(mainPageName);

        if ( hhcName.empty() ) {
            _fakeToc = true;
            for ( int i=0; i<urlList.length(); i++ ) {
                //lString32 name = lString32::itoa(i+1);
                lString32 name = urlList[i];
                if ( name.endsWith(".htm") )
                    name = name.substr(0, name.length()-4);
                else if ( name.endsWith(".html") )
                    name = name.substr(0, name.length()-5);
                if (name.startsWith("/"))
                    name = name.substr(1);
                addTocItem( name, urlList[i], 0 );
            }
            return true;
        } else {
            _fakeToc = false;
            LVStreamRef tocStream = cont->OpenStream(hhcName.c_str(), LVOM_READ);
            if ( tocStream.isNull() ) {
                CRLog::error("CHM: Cannot open .hhc");
                return false;
            }
            std::unique_ptr<ldomDocument> doc(
                    LVParseCHMHTMLStream(
                            tocStream, defEncodingName, _doc));
            if ( !doc ) {
                CRLog::error("CHM: Cannot parse .hhc");
                return false;
            }

#if DUMP_CHM_DOC==1
            LVStreamRef out = LVOpenFileStream(U"chm-toc.xml", LVOM_WRITE);
            if ( !out.isNull() )
                doc->saveToStream( out, NULL, true );
#endif

            ldomNode * body = doc->getRootNode(); //doc->createXPointer(cs32("/html[1]/body[1]"));
            bool res = false;
            if ( body->isElement() ) {
                // body element
                recurseToc( body, 0 );
                // add rest of pages
                for ( int i=0; i<urlList.length(); i++ ) {
                    lString32 name = urlList[i];
                    if ( name.endsWith(".htm") || name.endsWith(".html") )
                        addFile(name);
                }

                res = _fileList.length()>0;
                while ( _toc && _toc->getParent() )
                    _toc = _toc->getParent();
                if ( res && _toc && _toc->getChildCount()>0 ) {
                    lString32 name = _toc->getChild(0)->getName();
                    CRPropRef m_doc_props = _doc->getProps();
                    m_doc_props->setString(DOC_PROP_TITLE, name);
                }
            }
            return res;
        }
    }
    int appendFragments( LVDocViewCallback * progressCallback )
    {
        int appendedFragments = 0;
        time_t lastProgressTime = (time_t)time(0);
        int lastProgressPercent = -1;
        int cnt = _fileList.length();
        for ( int i=0; i<cnt; i++ ) {
            if ( progressCallback ) {
                int percent = i * 100 / cnt;
                time_t ts = (time_t)time(0);
                if ( ts>lastProgressTime && percent>lastProgressPercent ) {
                    progressCallback->OnLoadFileProgress( percent );
                    lastProgressTime = ts;
                    lastProgressPercent = percent;
                }
            }
            lString32 fname = _fileList[i];
            CRLog::trace("Import file %s", LCSTR(fname));
            LVStreamRef stream = _cont->OpenStream(fname.c_str(), LVOM_READ);
            if ( stream.isNull() )
                continue;
            _appender->setCodeBase(fname);
            LVHTMLParser parser(stream, _appender);
            parser.SetCharset(_defEncodingName.c_str());
            if ( parser.CheckFormat() && parser.Parse() ) {
                // valid
                appendedFragments++;
            } else {
                CRLog::error("Document type is not HTML for fragment %s", LCSTR(fname));
            }
            appendedFragments++;
        }
        return appendedFragments;
    }
};

bool ImportCHMDocument( LVStreamRef stream, ldomDocument * doc, LVDocViewCallback * progressCallback, CacheLoadingCallback * formatCallback )
{
    stream->SetPos(0);
    LVContainerRef cont = LVOpenCHMContainer( stream );
    if ( cont.isNull() ) {
        stream->SetPos(0);
        return false;
    }
    doc->setContainer(cont);

#if BUILD_LITE!=1
    if ( doc->openFromCache(formatCallback) ) {
        if ( progressCallback ) {
            progressCallback->OnLoadFileEnd( );
        }
        return true;
    }
#endif

    std::unique_ptr<CHMSystem> chm = CHMSystem::open(cont);
    if ( !chm )
        return false;
    lString32 tocFileName = chm->getContentsFileName();
    lString32 defEncodingName = chm->getEncodingName();
    lString32 mainPageName = chm->getDefaultTopic();
    lString32 title = chm->getTitle();
    lString32 language = chm->getLanguage();
    CRLog::info("CHM: toc=%s, enc=%s, title=%s", LCSTR(tocFileName), LCSTR(defEncodingName), LCSTR(title));
    //
    lString32Collection urlList;
    chm->getUrlList(urlList);

    int fragmentCount = 0;
    ldomDocumentWriterFilter writer(doc, false, HTML_AUTOCLOSE_TABLE);
    //ldomDocumentWriter writer(doc);
    writer.OnStart(NULL);
    writer.OnTagOpenNoAttr(U"", U"body");
    ldomDocumentFragmentWriter appender(&writer, cs32("body"), cs32("DocFragment"), lString32::empty_str );
    CHMTOCReader tocReader(cont, doc, &appender);
    if ( !tocReader.init(cont, tocFileName, defEncodingName, urlList, mainPageName) )
        return false;

    if ( !title.empty() )
        doc->getProps()->setString(DOC_PROP_TITLE, title);
    if ( !language.empty() )
        doc->getProps()->setString(DOC_PROP_LANGUAGE, language);

    fragmentCount = tocReader.appendFragments( progressCallback );
    writer.OnTagClose(U"", U"body");
    writer.OnStop();
    CRLog::debug("CHM: %d documents merged", fragmentCount);
#if DUMP_CHM_DOC==1
    LVStreamRef out = LVOpenFileStream(U"/tmp/chm.html", LVOM_WRITE);
    if ( !out.isNull() )
        doc->saveToStream( out, NULL, true );
#endif

    return fragmentCount>0;
}

#endif
