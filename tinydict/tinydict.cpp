/***************************************************************************
 *   CoolReader, .dict dictionary file support interface                   *
 *   Copyright (C) 2009,2011,2012 Vadim Lopatin <coolreader.org@gmail.com> *
 *   Copyright (C) 2019 Aleksey Chernov <valexlin@gmail.com>               *
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

#include "tinydict.h"

#include <cstring>
#include <limits>
#include <utility>
#include <vector>

/// add word to list
void TinyDictWordList::add( TinyDictWord * word )
{
    if ( word )
        list.emplace_back(word);
}

/// clear list
void TinyDictWordList::clear()
{
    list.clear();
}

/// empty list constructor
TinyDictWordList::TinyDictWordList() : dict(NULL) { }

/// destructor
TinyDictWordList::~TinyDictWordList() = default;


///
struct TinyDictFileCloser
{
    void operator()( FILE * file ) const
    {
        if ( file )
            fclose(file);
    }
};

class TinyDictFileBase
{
protected:
    std::string fname;
    std::unique_ptr<FILE, TinyDictFileCloser> f;
    size_t size;
    void setFilename( const char * filename )
    {
        if ( filename && *filename )
            fname = filename;
        else
            fname.clear();
    }
public:
    TinyDictFileBase() : f(nullptr), size(0)
    {
    }
    virtual ~TinyDictFileBase() = default;
    virtual void close()
    {
        f.reset();
        size = 0;
    }
};

class TinyDictIndexFile : public TinyDictFileBase
{
    int    factor;
    int    count;
    TinyDictWordList list;
public:

	void compact()
	{
		// do nothing
	}

    bool find( const char * prefix, bool exactMatch, TinyDictWordList & words );

    TinyDictIndexFile() : factor( 16 ), count(0)
    {
    }

    virtual ~TinyDictIndexFile()
    {
    }

    bool open( const char * filename );

};

class TinyDictCRC
{
    unsigned crc;
public:
    void reset()
    {
        crc = crc32( 0L, Z_NULL, 0 );
    }
    unsigned get()
    {
        return crc;
    }
    unsigned update( const void * data, unsigned size )
    {
        crc = crc32( crc, (const unsigned char *)data, size );
        return crc;
    }
    TinyDictCRC()
    {
        reset();
    }
};

class TinyDictZStream
{
    FILE * f;
    TinyDictCRC crc;

    unsigned size;

    bool error;
    std::vector<unsigned short> chunks;
    std::vector<unsigned int> offsets;
    unsigned chunkLength;

    bool     zInitialized;
    z_stream zStream;
    std::vector<unsigned char> unp_buffer;
    unsigned unp_buffer_start;
    unsigned unp_buffer_len;

    unsigned int readBytes( unsigned char * buf, unsigned size )
    {
        if ( error || !f )
            return 0;
        return fread( buf, 1, size, f );
    }

    unsigned short readU16()
    {
        unsigned char buf[2];
        if ( !error && f && fread( buf, 1, 2, f )==2 ) {
            crc.update( buf, 2 );
            return (((unsigned short)buf[1]) << 8) + buf[0];
        }
        error = true;
        return 0;
    }

    unsigned char readU8()
    {
        unsigned char buf[1];
        if ( !error && f && fread( buf, 1, 1, f )==1 ) {
            crc.update( buf, 1 );
            return buf[0];
        }
        error = true;
        return 0;
    }

    bool zinit(unsigned char * next_in, unsigned avail_in, unsigned char * next_out, unsigned avail_out);

    bool zclose();

    bool readChunk( unsigned n );

public:
	/// minimize memory consumption
	void compact();
	/// get unpacked data size
    unsigned getSize() { return size; }
	/// create uninitialized stream
    TinyDictZStream();
	/// open from file
    bool open( FILE * file );
	/// read block of data
    bool read( unsigned char * buf, unsigned start, unsigned len );
	/// close stream
    ~TinyDictZStream();
};

class TinyDictDataFile : public TinyDictFileBase
{
    bool compressed;
    std::vector<char> buf;

    TinyDictZStream zstream;

public:

    void compact()
    {
        zstream.compact();
        std::vector<char>().swap(buf);
    }
	
    const char * read( const TinyDictWord * w );

    bool open( const char * filename );

    TinyDictDataFile() : compressed(false)
    {
    }

    virtual ~TinyDictDataFile() = default;
};


static int base64table[128] = { 0 };
static const char * base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

static unsigned parseBase64( const char * str )
{
    int i;
    if ( !*base64table ) {
        for ( i=0; i<128; i++ )
            base64table[i] = -1;
        for ( i=0; base64chars[i]; i++ )
            base64table[(unsigned)base64chars[i]] = i;
    }
    unsigned n = 0;
    for ( ; *str; str++ ) {
        int code = base64table[ (unsigned)*str ];
        if ( code<0 )
            return (unsigned)-1;
        n = ( n << 6 ) + code;
    }
    return n;
}

int TinyDictWord::compare( const char * str ) const
{
    return strcmp( word.c_str(), str );
}

static int my_fgets( char * buf, int size, FILE * f )
{
	int i=0;
	for ( ; i<size; i++ ) {
		int ch = fgetc( f );
		if ( ch == '\n' )
			break;
		if ( ch > 0 )
			buf[i] = (char) ch;
		else 
			break;
	}
	buf[i] = 0;
	return i;
}

/// factory - reading from index file
TinyDictWord * TinyDictWord::read( FILE * f, unsigned index )
{
    if ( !f || feof(f) )
        return NULL;
    char buf[1024];
    unsigned indexpos = ftell( f );
    int sz = my_fgets( buf, 1023, f );
	if ( !sz )
        return NULL;
    int tabc = 0;
    int tabs[2];
    for ( int i=0; buf[i]; i++ ) {
        if ( buf[i] == '\t' && tabc<2 )
            tabs[tabc++] = i;
    }
    if ( tabc!=2 )
        return NULL;
    const char * word = buf;
    const char * pos_str = buf + tabs[0] + 1;
    const char * len_str = buf + tabs[1] + 1;
    buf[tabs[0]] = 0;
    buf[tabs[1]] = 0;
    unsigned start = parseBase64( pos_str );
    unsigned len = parseBase64( len_str );
    if ( start==(unsigned)-1 || len==(unsigned)-1 )
        return NULL;
    return new TinyDictWord( index, indexpos, start, len, word );
}

int TinyDictWordList::find( const char * prefix )
{
    if ( list.empty() )
        return -1;
    int a = 0;
    int b = length();
    for ( ;a < b-1; ) {
        int c = (a + b) / 2;
        int res = list[c]->compare( prefix );
        if ( !res )
            return c;
        if ( res < 0 ) {
            a = c + 1;
        } else {
            b = c;
        }
    }
    if ( a==0 || list[a]->compare( prefix )<0 )
        return a;
    return a - 1;
}

bool TinyDictWord::match( const char * str, bool exact ) const
{
    if ( exact )
        return !strcmp( word.c_str(), str );
    int i=0;
    for ( ; str[i]; i++  ) {
        if ( str[i] != word[i] )
            return false;
    }
    return str[i]==0;
}

bool TinyDictIndexFile::find( const char * prefix, bool exactMatch, TinyDictWordList & words )
{
    words.clear();
    int n = list.find( prefix );
    if ( n<0 )
        return false;
    TinyDictWord * p = list.get( n );
    if ( !p || fseek( f.get(), p->getIndexPos(), SEEK_SET ) )
        return false;
    int index = p->getIndex();
    for ( ;; ) {
        std::unique_ptr<TinyDictWord> candidate(
                TinyDictWord::read(f.get(), index++));
        if ( !candidate )
            break;
        int res = candidate->compare( prefix );
        if ( candidate->match( prefix, exactMatch ) )
            words.add(candidate.release());
        else {
            if ( res > 0 )
                break;
        }
    }
    return true;
}

bool TinyDictIndexFile::open( const char * filename )
{
    close();
    if ( filename )
        setFilename( filename );
    if ( fname.empty() )
        return false;
    f.reset(fopen(fname.c_str(), "rb"));
    if ( !f )
        return false;
    if ( fseek( f.get(), 0, SEEK_END ) ) {
        close();
        return false;
    }
    size = ftell( f.get() );
    if ( fseek( f.get(), 0, SEEK_SET ) ) {
        close();
        return false;
    }
    // test
    list.clear();
    count = 0;
    for ( ;; count++ ) {
        std::unique_ptr<TinyDictWord> candidate(
                TinyDictWord::read(f.get(), count));
        if ( !candidate )
            break;
        if ( (count % factor) == 0 )
            list.add(candidate.release());
    }
    printf("%d words read from index\n", count);
    return true;
}

bool TinyDictZStream::zinit( unsigned char * next_in, unsigned avail_in, unsigned char * next_out, unsigned avail_out )
{
    zclose();
    if ( !zInitialized ) {
        zStream.zalloc    = NULL;
        zStream.zfree     = NULL;
        zStream.opaque    = NULL;
        zStream.next_in   = next_in;
        zStream.avail_in  = avail_in;
        zStream.next_out  = next_out;
        zStream.avail_out = avail_out;
        if (inflateInit2( &zStream, -15 ) != Z_OK ) {
            // zlib initialization failed
            return false;
        }
        zInitialized = true;
    }
	return true;
}

bool TinyDictZStream::zclose()
{
    if ( zInitialized ) {
        inflateEnd( &zStream );
        zInitialized = false;
    }
    return true;
}

void TinyDictZStream::compact()
{
    std::vector<unsigned char>().swap(unp_buffer);
    unp_buffer_start = unp_buffer_len = 0;
}

bool TinyDictZStream::readChunk( unsigned n )
{
    if ( n >= chunks.size() )
        return false;
    if ( unp_buffer.size() != chunkLength )
        unp_buffer.resize(chunkLength);
    unp_buffer_start = n * chunkLength;

    if ( fseek( f, offsets[ n ], SEEK_SET ) ) {
        printf( "cannot seek to %d position\n", offsets[n] );
        return false;
    }
    unsigned packsz = chunks[n];
    if ( !packsz )
        return false;
    std::vector<unsigned char> packedChunk(packsz);

    unsigned int bytesRead = readBytes( packedChunk.data(), packsz );
    if ( bytesRead != packsz || error ) {
        printf( "error reading packed data\n" );
        return false;
    }
    if ( !zinit(packedChunk.data(), packsz, unp_buffer.data(),
                static_cast<unsigned>(unp_buffer.size())) ) {
        printf("cannot init deflater\n");
        return false;
    }
    printf("unpacking %d bytes\n", packsz);
    int err = inflate( &zStream,  Z_PARTIAL_FLUSH );
    printf("inflate result: %d\n", err);
    if ( err != Z_OK ) {
        printf("Inflate error %s (%d). avail_in=%d, avail_out=%d \n", zStream.msg, err, (int)zStream.avail_in, (int)zStream.avail_out);
        zclose();
        return false;
    }
    if ( zStream.avail_in ) {
        printf("Inflate: not all data read, still %d bytes available\n", (int)zStream.avail_in );
        zclose();
        return false;
    }
    unp_buffer_len = static_cast<unsigned>(unp_buffer.size())
            - zStream.avail_out;

    if ( n + 1 < chunks.size() && unp_buffer_len!=chunkLength ) {
        printf("wrong chunk length\n");
        zclose();
        return false; // too short chunk data
    }


    zclose();
    return true;
}

bool TinyDictZStream::read( unsigned char * buf, unsigned start, unsigned len )
{
    if ( !buf && len )
        return false;
    while ( len ) {
        bool buffered = start >= unp_buffer_start
                && start - unp_buffer_start < unp_buffer_len;
        if ( !buffered ) {
            if ( !chunkLength || !readChunk(start / chunkLength) )
                return false;
            if ( start < unp_buffer_start
                    || start - unp_buffer_start >= unp_buffer_len )
                return false;
        }
        unsigned bufferOffset = start - unp_buffer_start;
        unsigned readyBytes = unp_buffer_len - bufferOffset;
        if ( readyBytes > len )
            readyBytes = len;
        memcpy( buf, unp_buffer.data() + bufferOffset, readyBytes );
        buf += readyBytes;
        start += readyBytes;
        len -= readyBytes;
    }
    return true;
}

TinyDictZStream::TinyDictZStream()
: f ( NULL ), size( 0 )
, error( false ), chunkLength(0)
, zInitialized(false), unp_buffer_start(0), unp_buffer_len(0)
{
    memset( &zStream, 0, sizeof(zStream) );
}

TinyDictZStream::~TinyDictZStream()
{
    zclose();
}

bool TinyDictZStream::open( FILE * file )
{
    zclose();
    chunks.clear();
    offsets.clear();
    compact();
    size = 0;
    chunkLength = 0;
    f = file;
    error = false;
    if ( !f )
        return false;
    if ( fseek( f, 0, SEEK_END ) ) {
        return false;
    }
    long fileSize = ftell(f);
    if ( fileSize < 0 )
        return false;
    if ( fseek( f, 0, SEEK_SET ) ) {
        return false;
    }

    crc.reset();
    unsigned char header[10];
    if ( fread( header, 1, sizeof(header), f )!=sizeof(header) ) {
        return false;
    }
    crc.update( header, sizeof(header) );
    if ( header[0]!=0x1f || header[1]!=0x8b ) { // 0x1F 0x8B -- GZIP magic
        return true;
    }
    if ( header[2]!=8 ) {
        // unknown compression method
        return false;
    }
    unsigned char flg = header[3];
    unsigned headerLength = 10;

    //const char FTEXT   = 1;    // Extra text
    const char FHCRC   = 2;    // Header CRC
    const char FEXTRA  = 4;    // Extra field
    const char FNAME   = 8;    // File name
    const char FCOMMENT = 16;   // File comment

    // Optional extra field
    if ( flg & FEXTRA ) {
        unsigned extraLength = readU16();
        headerLength += extraLength + 2;
        readU8(); // subfield ID 1
        readU8(); // subfield ID 2
        readU16(); // 2 bytes subfield length
        readU16(); // 2 bytes subfield version
        chunkLength = readU16(); // 2 bytes chunk length
        unsigned chunkCount = readU16(); // 2 bytes chunk count
        if ( error || !chunkLength || !chunkCount ) {
            return false;
        }
        chunks.resize(chunkCount);
        for (unsigned i=0; i<chunkCount; i++) {
            chunks[i] = readU16();
        }
        size = 0;
    } else {
        // GZIP is not supported, use DZIP
        return false;
    }
    // Skip optional file name
    if ( flg & FNAME ) {
        while (readU8() != 0 )
            headerLength++;
        headerLength++;
    }
    // Skip optional file comment
    if ( flg & FCOMMENT ) {
        while (readU8() != 0)
            headerLength++;
        headerLength++;
    }
    // Check optional header CRC
    if ( flg & FHCRC ) {
        int v = (int)crc.get() & 0xffff;
        if (readU16() != v) {
            // CRC failed
            error = true;
        }
        headerLength += 2;
    }

    offsets.resize(chunks.size());
    std::size_t payloadEnd = headerLength;
    for ( std::size_t i=0; i<chunks.size(); i++ ) {
        if ( payloadEnd > std::numeric_limits<unsigned>::max() )
            return false;
        offsets[i] = static_cast<unsigned>(payloadEnd);
        payloadEnd += chunks[i];
    }
    if ( payloadEnd > static_cast<std::size_t>(fileSize)
            || static_cast<std::size_t>(fileSize) - payloadEnd < 8 )
        return false;

    if ( fseek( f, headerLength, SEEK_SET ) ) {
        return false;
    }

    unsigned lastChunk = static_cast<unsigned>(chunks.size() - 1);
    if ( !readChunk(lastChunk) ) {
        printf("Error reading chunk %d\n", lastChunk);
        return false;
    }
    size = unp_buffer_start + unp_buffer_len;

	compact();
    return true;
}

const char * TinyDictDataFile::read( const TinyDictWord * w )
{
    if ( !f || !w ) {
        printf("article is out of file range (%d)\n", (int)size);
        return NULL;
    }
    unsigned articleStart = w->getStart();
    unsigned articleSize = w->getSize();
    if ( articleStart > size || articleSize > size - articleStart
            || articleSize == std::numeric_limits<unsigned>::max() ) {
        printf("article is out of file range (%d)\n", (int)size);
        return NULL;
    }

    buf.resize(static_cast<std::size_t>(articleSize) + 1);
    if ( !compressed ) {
        // uncompressed
        printf("reading uncompressed article\n");
        if ( fseek( f.get(), articleStart, SEEK_SET ) )
            return NULL;
        if ( fread( buf.data(), 1, articleSize, f.get() ) != articleSize )
            return NULL;
    } else {
        // compressed
        printf("reading compressed article\n");
        if ( !zstream.read(
                    reinterpret_cast<unsigned char *>(buf.data()),
                    articleStart, articleSize) )
            return NULL;
    }
    buf[articleSize] = 0;
    return buf.data();
}

bool TinyDictDataFile::open( const char * filename )
{
    close();
    if ( filename )
        setFilename( filename );
    if ( fname.empty() )
        return false;
    f.reset(fopen(fname.c_str(), "rb"));
    if ( !f )
        return false;
    if ( fseek( f.get(), 0, SEEK_END ) ) {
        close();
        return false;
    }
    size = ftell( f.get() );
    if ( fseek( f.get(), 0, SEEK_SET ) ) {
        close();
        return false;
    }


    unsigned char header[10];
    if ( fread( header, 1, sizeof(header), f.get() )!=sizeof(header) ) {
        close();
        return false;
    }

    if ( header[0]!=0x1f || header[1]!=0x8b ) { // 0x1F 0x8B -- GZIP magic
        compressed = false;
        printf("data file %s is not compressed\n", filename);
        return true;
    }

    printf("data file %s is compressed\n", filename);
    compressed = true;
    if ( !zstream.open( f.get() ) ) {
        printf("data file %s opening error\n", filename);
        close();
        return false;
    }
    size = zstream.getSize();
    return true;
}

TinyDictionary::TinyDictionary()
    : data(std::make_unique<TinyDictDataFile>())
    , index(std::make_unique<TinyDictIndexFile>())
{
}

TinyDictionary::~TinyDictionary() = default;

void TinyDictionary::compact()
{
	index->compact();
	data->compact();
}

const char * TinyDictionary::getDictionaryName()
{
    return name ? name->c_str() : NULL;
}

bool TinyDictionary::open( const char * indexfile, const char * datafile )
{
    if ( !indexfile || !datafile )
        return false;

    std::unique_ptr<TinyDictIndexFile> nextIndex =
            std::make_unique<TinyDictIndexFile>();
    std::unique_ptr<TinyDictDataFile> nextData =
            std::make_unique<TinyDictDataFile>();
    if ( !nextIndex->open(indexfile) || !nextData->open(datafile) )
        return false;

    std::optional<std::string> nextName;
    std::string indexPath(indexfile);
    std::size_t lastSlash = indexPath.find_last_of("/\\");
    std::size_t lastPoint = indexPath.find_last_of('.');
    if ( lastPoint != std::string::npos
            && (lastSlash == std::string::npos || lastPoint > lastSlash) ) {
        std::size_t nameStart = lastSlash == std::string::npos
                ? 0 : lastSlash + 1;
        nextName = indexPath.substr(nameStart, lastPoint - nameStart);
    }

    name = std::move(nextName);
    index = std::move(nextIndex);
    data = std::move(nextData);
    return true;
}

/// returns word list's dictionary name
const char * TinyDictWordList::getDictionaryName()
{
	if ( !dict )
		return NULL;
	return dict->getDictionaryName();
}

/// returns article for word by index
const char * TinyDictWordList::getArticle( int index )
{
	if ( !dict )
		return NULL;
    TinyDictWord * word = get(index);
    if ( !word )
		return NULL;
    return dict->getData()->read(word);
}

/// searches dictionary for specified word, caller is responsible for deleting of returned object
TinyDictWordList * TinyDictionary::find( const char * prefix, int options )
{
    if ( !prefix )
        return NULL;
    std::unique_ptr<TinyDictWordList> result =
            std::make_unique<TinyDictWordList>();
    result->setDict( this );
    if ( index->find( prefix,
                (TINY_DICT_OPTION_STARTS_WITH & options) == 0, *result )
            && result->length()>0 )
        return result.release();
    return NULL;
}

bool TinyDictionaryList::add( const char * indexfile, const char * datafile )
{
    std::unique_ptr<TinyDictionary> dictionary =
            std::make_unique<TinyDictionary>();
    if ( !dictionary->open(indexfile, datafile) )
        return false;
    list.push_back(std::move(dictionary));
    return true;
}

/// create empty list
TinyDictionaryList::TinyDictionaryList() = default;

/// remove all dictionaries from list
void TinyDictionaryList::clear()
{
    list.clear();
}

TinyDictionaryList::~TinyDictionaryList() = default;

/// search all dictionaries in list for specified pattern
bool TinyDictionaryList::find( TinyDictResultList & result, const char * prefix, int options )
{
    result.clear();
    if ( !prefix )
        return false;
    for ( const std::unique_ptr<TinyDictionary> &dictionary : list ) {
        std::unique_ptr<TinyDictWordList> words(
                dictionary->find(prefix, options));
        if ( words )
            result.add(words.release());
    }
    return result.length() > 0;
}


/// remove all dictionaries from list
void TinyDictResultList::clear()
{
    list.clear();
}

/// create empty list
TinyDictResultList::TinyDictResultList() = default;

/// destructor
TinyDictResultList::~TinyDictResultList() = default;

/// add item to list
void TinyDictResultList::add( TinyDictWordList * p )
{
    if ( p )
        list.emplace_back(p);
}

#ifdef TEST_APP
int main( int argc, const char * * argv )
{
    TinyDictIndexFile index;
    TinyDictDataFile data;
    TinyDictDataFile zdata;
    if ( !index.open("mueller7.index") ) {
        printf("cannot open index file mueller7.index\n");
        return -1;
    }
    if ( !data.open("mueller7.dict") ) {
        printf("cannot open data file mueller7.dict\n");
        return -1;
    }
    if ( !zdata.open("mueller7.dict.dz") ) {
        printf("cannot open data file mueller7.dict.dz\n");
        return -1;
    }
    TinyDictWordList words;
    const char * pattern = "full";
    index.find( pattern, true, words );
    printf( "%d words matched pattern %s\n", words.length(), pattern );
    for ( int i=0; i<words.length(); i++ ) {
        TinyDictWord * p = words.get(i);
        printf("%s %d %d\n", p->getWord(), p->getStart(), p->getSize() );
        const char * text = zdata.read( p );
        if ( text )
            printf( "article:\n%s\n", text );
        else
            printf( "cannot read article\n" );
    }

	{
		// create TinyDictionaryList object
		TinyDictionaryList dicts;
		// register dictionaries using 
		dicts.add( "mueller7.index", "mueller7.dict.dz" );

		// container for results
		TinyDictResultList results;
	    dicts.find(results, "empty", 0 ); // find exact match

		// for each source dictionary that matches pattern
		for ( int d = 0; d<results.length(); d++ ) {
			TinyDictWordList * words = results.get(d);
			printf("dict: %s\n", words->getDictionaryName() );
			// for each found word
			for ( int i=0; i<words->length(); i++ ) {
				TinyDictWord * word = words->get(i);
				printf("word: %s\n", word->getWord() );
				printf("article: %s\n", words->getArticle( i ) );
			}
		}
	}
#ifdef _WIN32
	printf("Press any key...");
	getchar();
#endif
    return 0;
}
#endif
