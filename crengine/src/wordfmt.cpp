/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2010-2015 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2018,2020 Aleksey Chernov <valexlin@gmail.com>          *
 *   Copyright (C) 2020 poire-z <poire-z@users.noreply.github.com>         *
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
#include "../include/lvstring.h"
#include "../include/lvstreamutils.h"
#include "../include/lvtinydom.h"
#include "../include/crlog.h"
#include <mutex>
#include <vector>

//#ifndef ENABLE_ANTIWORD
//#define ENABLE_ANTIWORD 1
//#endif


#if ENABLE_ANTIWORD==1
#if defined(_DEBUG) && !defined(DEBUG)
#define DEBUG
#endif
#if defined(_NDEBUG) && !defined(NDEBUG)
#define NDEBUG
#endif
#if !defined(DEBUG) && !defined(NDEBUG)
#define NDEBUG
#endif
#include "../include/wordfmt.h"

#ifdef _WIN32
#if defined(_MSC_VER) || (defined(__MINGW64_VERSION_MAJOR) && defined(NO_OLDNAMES)) || (defined(__MINGW32__) && !defined(__MINGW64_VERSION_MAJOR) && !defined(_EMULATE_GLIBC))
extern "C" {
	int strcasecmp(const char *s1, const char *s2) {
        return _stricmp(s1,s2);
	}
//char	*optarg = NULL;
//	int	optind = 0;
}
#endif
#endif	// _WIN32

#ifdef _DEBUG
#define TRACE(...) CRLog::trace(__VA_ARGS__)
#else
#define TRACE(...)
#endif

// Antiword Output handling
extern "C" {
#include "antiword.h"
}

#define LFAIL(x) \
    if ((x)) crFatalError(1111, "assertion failed: " #x)

struct WordImportContext {
    explicit WordImportContext(ldomDocumentWriter *documentWriter)
        : writer(documentWriter)
        , imageIndex(0)
        , insideParagraph(false)
        , insideTable(false)
        , tableColumnCount(0)
        , insideList(0)
        , alignment(0)
        , insideListItem(false)
        , lastSpaceChar(false)
        , leftIndent(0)
        , firstLineIndent(0)
        , rightIndent(0)
        , beforeIndent(0)
        , afterIndent(0)
    {
    }

    ldomDocumentWriter *writer;
    int imageIndex;
    bool insideParagraph;
    bool insideTable;
    int tableColumnCount;
    int insideList; // 0=none, 1=ul, 2=ol
    int alignment;
    bool insideListItem;
    bool lastSpaceChar;
    short leftIndent;
    short firstLineIndent;
    short rightIndent;
    int beforeIndent;
    int afterIndent;
};

static thread_local WordImportContext *g_wordImportContext = NULL;
static std::mutex g_antiwordMutex;

static WordImportContext &wordImportContext()
{
    LFAIL(g_wordImportContext == NULL);
    return *g_wordImportContext;
}

class WordImportContextGuard {
private:
    WordImportContext *m_previous;

public:
    explicit WordImportContextGuard(WordImportContext &context)
        : m_previous(g_wordImportContext)
    {
        g_wordImportContext = &context;
    }

    ~WordImportContextGuard()
    {
        g_wordImportContext = m_previous;
    }
};


static lString32 picasToPercent( const lChar32 * prop, int p, int minvalue, int maxvalue ) {
    int identPercent = 100 * p / 5000;
    if ( identPercent>maxvalue )
        identPercent = maxvalue;
    if ( identPercent<minvalue )
        identPercent = minvalue;
	//if ( identPercent!=0 )
    return lString32(prop) << fmt::decimal(identPercent) << "%; ";
	//return lString32::empty_str;
}

static lString32 picasToPx( const lChar32 * prop, int p, int minvalue, int maxvalue ) {
    int v = 600 * p / 5000;
    if ( v>maxvalue ) {
        v = maxvalue;
    }
    if ( v<minvalue ) {
        v = minvalue;
    }
    if ( v!=0 ) {
        return lString32(prop) << fmt::decimal(v) << "px; ";
    }
    return lString32::empty_str;
}

static lString32 fontSizeToPercent( const lChar32 * prop, int p, int minvalue, int maxvalue ) {
    int v = 100 * p / 20;
    if ( v>maxvalue ) {
        v = maxvalue;
    }
    if ( v<minvalue ) {
        v = minvalue;
    }
    if ( v!=0 ) {
        return lString32(prop) << fmt::decimal(v) << "%; ";
    }
    return lString32::empty_str;
}

static void setOptions() {
    options_type tOptions = {
        DEFAULT_SCREEN_WIDTH,
        conversion_xml,
        TRUE,
        TRUE,
        FALSE,
        encoding_utf_8,
        INT_MAX,
        INT_MAX,
        level_default,
    };

    //vGetOptions(&tOptions);
    vSetOptions(&tOptions);
}

/*
 * vPrologue1 - get options and call a specific initialization
 */
static void
vPrologue1(diagram_type *pDiag, const char *szTask, const char * /*szFilename*/)
{
    LFAIL(pDiag == NULL);
    LFAIL(szTask == NULL || szTask[0] == '\0');

    WordImportContext &context = wordImportContext();

    TRACE("antiword::vPrologue1()");
    //vPrologueXML(pDiag, &tOptions);

    lString32 title("Word document");
    context.writer->OnTagOpen(NULL, U"?xml");
    context.writer->OnAttribute(NULL, U"version", U"1.0");
    context.writer->OnAttribute(NULL, U"encoding", U"utf-8");
    context.writer->OnEncoding(U"utf-8", NULL);
    context.writer->OnTagBody();
    context.writer->OnTagClose(NULL, U"?xml");
    context.writer->OnTagOpenNoAttr(NULL, U"FictionBook");
    // DESCRIPTION
    context.writer->OnTagOpenNoAttr(NULL, U"description");
    context.writer->OnTagOpenNoAttr(NULL, U"title-info");
    context.writer->OnTagOpenNoAttr(NULL, U"book-title");
    context.writer->OnText(title.c_str(), title.length(), 0);
    context.writer->OnTagClose(NULL, U"book-title");
    context.writer->OnTagOpenNoAttr(NULL, U"title-info");
    context.writer->OnTagClose(NULL, U"description");
    // BODY
    context.writer->OnTagOpenNoAttr(NULL, U"body");
} /* end of vPrologue1 */


/*
 * vEpilogue - clean up after everything is done
 */
static void
vEpilogue(diagram_type * /*pDiag*/)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vEpilogue()");
    //vEpilogueTXT(pDiag->pOutFile);
    //vEpilogueXML(pDiag);
    if ( context.insideParagraph )
        context.writer->OnTagClose(NULL, U"p");
    context.writer->OnTagClose(NULL, U"body");
} /* end of vEpilogue */

/*
 * vImagePrologue - perform image initialization
 */
void
vImagePrologue(diagram_type *pDiag, const imagedata_type *pImg)
{
    TRACE("antiword::vImagePrologue()");
    CR_UNUSED2(pDiag, pImg);
    //vImageProloguePS(pDiag, pImg);
} /* end of vImagePrologue */

/*
 * vImageEpilogue - clean up an image
 */
void
vImageEpilogue(diagram_type *pDiag)
{
    CR_UNUSED(pDiag);
    TRACE("antiword::vImageEpilogue()");
    //vImageEpiloguePS(pDiag);
} /* end of vImageEpilogue */

/*
 * bAddDummyImage - add a dummy image
 *
 * return TRUE when successful, otherwise FALSE
 */
BOOL
bAddDummyImage(diagram_type *pDiag, const imagedata_type *pImg)
{
    CR_UNUSED2(pDiag, pImg);
    TRACE("antiword::vImageEpilogue()");
    //return bAddDummyImagePS(pDiag, pImg);
	return FALSE;
} /* end of bAddDummyImage */

/*
 * pCreateDiagram - create and initialize a diagram
 *
 * remark: does not return if the diagram can't be created
 */
diagram_type *
pCreateDiagram(const char *szTask, const char *szFilename)
{
    TRACE("antiword::pCreateDiagram()");
    diagram_type	*pDiag;

    LFAIL(szTask == NULL || szTask[0] == '\0');

    /* Get the necessary memory */
    pDiag = (diagram_type *)xmalloc(sizeof(diagram_type));
    /* Initialization */
    pDiag->pOutFile = stdout;
    vPrologue1(pDiag, szTask, szFilename);
    /* Return success */
    return pDiag;
} /* end of pCreateDiagram */

/*
 * vDestroyDiagram - remove a diagram by freeing the memory it uses
 */
void
vDestroyDiagram(diagram_type *pDiag)
{
    TRACE("antiword::vDestroyDiagram()");

    LFAIL(pDiag == NULL);

    if (pDiag == NULL) {
        return;
    }
    vEpilogue(pDiag);
    pDiag = (diagram_type *)xfree(pDiag);
} /* end of vDestroyDiagram */

/*
 * vPrologue2 - call a specific initialization
 */
void
vPrologue2(diagram_type *pDiag, int iWordVersion)
{
    TRACE("antiword::vDestroyDiagram()");
    CR_UNUSED2(pDiag, iWordVersion);
//    vCreateBookIntro(pDiag, iWordVersion);
//    vCreateInfoDictionary(pDiag, iWordVersion);
//    vAddFontsPDF(pDiag);
} /* end of vPrologue2 */

/*
 * vMove2NextLine - move to the next line
 */
void
vMove2NextLine(diagram_type *pDiag, drawfile_fontref /*tFontRef*/,
    USHORT usFontSize)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vMove2NextLine()");
    LFAIL(pDiag == NULL);
    LFAIL(pDiag->pOutFile == NULL);
    LFAIL(usFontSize < MIN_FONT_SIZE || usFontSize > MAX_FONT_SIZE);

    if ( (context.insideParagraph || context.insideListItem)
            && !context.lastSpaceChar )
        context.writer->OnText(U" ", 1, 0);
    //writer->OnTagOpenAndClose(NULL, U"br");
    //vMove2NextLineXML(pDiag);
} /* end of vMove2NextLine */

/*
 * vSubstring2Diagram - put a sub string into a diagram
 */
void
vSubstring2Diagram(diagram_type *pDiag,
    char *szString, size_t tStringLength, long lStringWidth,
    UCHAR /*ucFontColor*/, USHORT usFontstyle, drawfile_fontref /*tFontRef*/,
    USHORT usFontSize, USHORT /*usMaxFontSize*/)
{
    WordImportContext &context = wordImportContext();
    lString32 s( szString, (int)tStringLength);
#ifdef _LINUX
    TRACE("antiword::vSubstring2Diagram(%s)", LCSTR(s));
#else
    TRACE("antiword::vSubstring2Diagram()");
#endif
    s.trimDoubleSpaces(!context.lastSpaceChar, true, false);
    context.lastSpaceChar = (s.lastChar()==' ');
//    vSubstringXML(pDiag, szString, tStringLength, lStringWidth,
//            usFontstyle);
    if ( !context.insideParagraph && !context.insideListItem ) {
        context.writer->OnTagOpenNoAttr(NULL, U"p");
        context.insideParagraph = true;
    }
    bool styleBold = bIsBold(usFontstyle);
    bool styleItalic = bIsItalic(usFontstyle);
    lString32 style;
	style << fontSizeToPercent( U"font-size: ", usFontSize, 30, 300 );
    if ( !style.empty() ) {
        context.writer->OnTagOpen(NULL, U"span");
        context.writer->OnAttribute(NULL, U"style", style.c_str());
        context.writer->OnTagBody();
    }
    if ( styleBold )
        context.writer->OnTagOpenNoAttr(NULL, U"b");
    if ( styleItalic )
        context.writer->OnTagOpenNoAttr(NULL, U"i");
    //=================
    context.writer->OnText(s.c_str(), s.length(), 0);
    //=================
    if ( styleItalic )
        context.writer->OnTagClose(NULL, U"i");
    if ( styleBold )
        context.writer->OnTagClose(NULL, U"b");
    if ( !style.empty() )
        context.writer->OnTagClose(NULL, U"span");

    pDiag->lXleft += lStringWidth;
} /* end of vSubstring2Diagram */

extern "C" {
    void vStoreStyle(diagram_type *pDiag, output_type *pOutput,
        const style_block_type *pStyle);
}

/*
 * vStoreStyle - store a style
 */
void
vStoreStyle(diagram_type *pDiag, output_type *pOutput,
    const style_block_type *pStyle)
{
    WordImportContext &context = wordImportContext();
    //size_t	tLen;
    //char	szString[120];

    LFAIL(pDiag == NULL);
    LFAIL(pOutput == NULL);
    LFAIL(pStyle == NULL);

    context.alignment = pStyle->ucAlignment;
    context.leftIndent = pStyle->sLeftIndent;
    context.firstLineIndent = pStyle->sLeftIndent1;
    context.rightIndent = pStyle->sRightIndent;
    context.beforeIndent = pStyle->usBeforeIndent;
    context.afterIndent = pStyle->usAfterIndent;

    TRACE("antiword::vStoreStyle(al=%d, li1=%d, li=%d, ri=%d)",
            context.alignment, context.firstLineIndent,
            context.leftIndent, context.rightIndent);
    //styleBold = pStyle->style_block_tag

} /* end of vStoreStyle */
/*
 * Create a start of paragraph (phase 1)
 * Before indentation, list numbering, bullets etc.
 */
void
vStartOfParagraph1(diagram_type *pDiag, long /*lBeforeIndentation*/)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vStartOfParagraph1()");
    LFAIL(pDiag == NULL);
    context.lastSpaceChar = false;
} /* end of vStartOfParagraph1 */

/*
 * Create a start of paragraph (phase 2)
 * After indentation, list numbering, bullets etc.
 */
void
vStartOfParagraph2(diagram_type *pDiag)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vStartOfParagraph2()");
    LFAIL(pDiag == NULL);

    lString32 style;
    if ( !context.insideParagraph
            && !context.insideList && !context.insideListItem ) {
        context.writer->OnTagOpen(NULL, U"p");
        if ( context.alignment==ALIGNMENT_CENTER )
            style << "text-align: center; ";
        else if ( context.alignment==ALIGNMENT_RIGHT )
            style << "text-align: right; ";
        else if ( context.alignment==ALIGNMENT_JUSTIFY )
            style << "text-align: justify; text-indent: 1.3em; ";
        else
            style << "text-align: left; ";
        //if ( context.firstLineIndent!=0 )
        //style << picasToPercent(
        //        U"text-indent: ", context.firstLineIndent, 0, 20);
        if ( context.leftIndent!=0 )
            style << picasToPercent(
                    U"margin-left: ", context.leftIndent, 0, 40);
        if ( context.rightIndent!=0 )
            style << picasToPercent(
                    U"margin-right: ", context.rightIndent, 0, 30);
        if ( context.beforeIndent!=0 )
            style << picasToPx(
                    U"margin-top: ", context.beforeIndent, 0, 20);
        if ( context.afterIndent!=0 )
            style << picasToPx(
                    U"margin-bottom: ", context.afterIndent, 0, 20);
        if ( !style.empty() )
            context.writer->OnAttribute(NULL, U"style", style.c_str());
        context.writer->OnTagBody();
        context.insideParagraph = true;
    }
    //vStartOfParagraphXML(pDiag, 1);
} /* end of vStartOfParagraph2 */

/*
 * Create an end of paragraph
 */
void
vEndOfParagraph(diagram_type *pDiag,
    drawfile_fontref /*tFontRef*/, USHORT usFontSize, long lAfterIndentation)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vEndOfParagraph()");
    LFAIL(pDiag == NULL);
    LFAIL(pDiag->pOutFile == NULL);
    LFAIL(usFontSize < MIN_FONT_SIZE || usFontSize > MAX_FONT_SIZE);
    LFAIL(lAfterIndentation < 0);
    //vEndOfParagraphXML(pDiag, 1);
    if ( context.insideParagraph ) {
        context.writer->OnTagClose(NULL, U"p");
        context.insideParagraph = false;
    }
} /* end of vEndOfParagraph */

/*
 * Create an end of page
 */
void
vEndOfPage(diagram_type * /*pDiag*/, long /*lAfterIndentation*/, BOOL /*bNewSection*/)
{
    TRACE("antiword::vEndOfPage()");
    //vEndOfPageXML(pDiag);
} /* end of vEndOfPage */

/*
 * vSetHeaders - set the headers
 */
void
vSetHeaders(diagram_type * /*pDiag*/, USHORT /*usIstd*/)
{
    TRACE("antiword::vEndOfPage()");
    //vSetHeadersXML(pDiag, usIstd);
} /* end of vSetHeaders */

/*
 * Create a start of list
 */
void
vStartOfList(diagram_type *pDiag, UCHAR ucNFC, BOOL bIsEndOfTable)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vStartOfList()");

    if ( bIsEndOfTable!=0 )
        vEndOfTable(pDiag);

    if ( context.insideList==0 ) {
        switch( ucNFC ) {
        case LIST_BULLETS:
            context.insideList = 1;
            context.writer->OnTagOpenNoAttr(NULL, U"ul");
            break;
        default:
            context.insideList = 2;
            context.writer->OnTagOpenNoAttr(NULL, U"ol");
            break;
        }
    }
    context.insideListItem = false;

    //vStartOfListXML(pDiag, ucNFC, bIsEndOfTable);
} /* end of vStartOfList */

/*
 * Create an end of list
 */
void
vEndOfList(diagram_type * /*pDiag*/)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vEndOfList()");

    if ( context.insideListItem ) {
        context.writer->OnTagClose(NULL, U"li");
        context.insideListItem = false;
    }
    if ( context.insideList==1 )
        context.writer->OnTagClose(NULL, U"ul");
    else if ( context.insideList==2 )
        context.writer->OnTagClose(NULL, U"ol");
    context.insideList = 0;

    //vEndOfListXML(pDiag);
} /* end of vEndOfList */

/*
 * Create a start of a list item
 */
void
vStartOfListItem(diagram_type * /*pDiag*/, BOOL /*bNoMarks*/)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vStartOfListItem()");
    if ( context.insideListItem ) {
        context.writer->OnTagClose(NULL, U"li");
    }
    context.insideListItem = true;
    context.writer->OnTagOpenNoAttr(NULL, U"li");
    //vStartOfListItemXML(pDiag, bNoMarks);
} /* end of vStartOfListItem */

/*
 * Create an end of a table
 */
void
vEndOfTable(diagram_type * /*pDiag*/)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::vEndOfTable()");
    if ( context.insideTable ) {
        context.writer->OnTagClose(NULL, U"table");
        context.insideTable = false;
        context.tableColumnCount = 0;
    }
} /* end of vEndOfTable */

/*
 * Add a table row
 *
 * Returns TRUE when conversion type is XML
 */
BOOL
bAddTableRow(diagram_type * /*pDiag*/, char **aszColTxt,
    int iNbrOfColumns, const short *asColumnWidth, UCHAR /*ucBorderInfo*/)
{
    WordImportContext &context = wordImportContext();
    TRACE("antiword::bAddTableRow()");
//        vAddTableRowXML(pDiag, aszColTxt,
//                iNbrOfColumns, asColumnWidth,
//                ucBorderInfo);
    if ( context.tableColumnCount!=iNbrOfColumns ) {
        if (context.insideTable)
            context.writer->OnTagClose(NULL, U"table");
        context.writer->OnTagOpenNoAttr(NULL, U"table");
        context.insideTable = true;
		int totalWidth = 0;
		int i;
		for ( i=0; i<iNbrOfColumns; i++ )
			totalWidth += asColumnWidth[i];
		if ( totalWidth>0 ) {
			for ( i=0; i<iNbrOfColumns; i++ ) {
				int cw = asColumnWidth[i] * 100 / totalWidth;
                context.writer->OnTagOpen(NULL, U"col");
                if ( cw>=0 )
                    context.writer->OnAttribute(
                            NULL, U"width",
                            (lString32::itoa(cw) + "%").c_str());
                context.writer->OnTagBody();
                context.writer->OnTagClose(NULL, U"col");
			}
		}
        context.tableColumnCount = iNbrOfColumns;
	}
    if (!context.insideTable) {
        context.writer->OnTagOpenNoAttr(NULL, U"table");
        context.insideTable = true;
    }
    context.writer->OnTagOpenNoAttr(NULL, U"tr");
    for ( int i=0; i<iNbrOfColumns; i++ ) {
        context.writer->OnTagOpenNoAttr(NULL, U"td");
        lString32 text = lString32(aszColTxt[i]);
        context.writer->OnText(text.c_str(), text.length(), 0);
        context.writer->OnTagClose(NULL, U"td");
    }
    context.writer->OnTagClose(NULL, U"tr");
    return TRUE;
    //return FALSE;
} /* end of bAddTableRow */


static thread_local LVStream *g_antiwordStream = NULL;

class AntiwordStreamGuard {
private:
    LVStream *m_previous;

public:
    explicit AntiwordStreamGuard(const LVStreamRef &stream)
        : m_previous(g_antiwordStream)
    {
        g_antiwordStream = stream.get();
    }

    ~AntiwordStreamGuard()
    {
        g_antiwordStream = m_previous;
    }

    operator FILE * () const
    {
        return (FILE*)g_antiwordStream;
    }
};

void aw_rewind(FILE * pFile)
{
    if ( (void*)pFile==(void*)g_antiwordStream ) {
        g_antiwordStream->SetPos(0);
    } else {
        rewind(pFile);
    }
}

int aw_getc(FILE * pFile)
{
    if ( (void*)pFile==(void*)g_antiwordStream ) {
        int b = g_antiwordStream->ReadByte();
        if ( b>=0 )
            return b;
        return EOF;
    } else {
        return getc(pFile);
    }
}

/*
 * bReadBytes
 * This function reads the specified number of bytes from the specified file,
 * starting from the specified offset.
 * Returns TRUE when successfull, otherwise FALSE
 */
BOOL
bReadBytes(UCHAR *aucBytes, size_t tMemb, ULONG ulOffset, FILE *pFile)
{
    LFAIL(aucBytes == NULL || pFile == NULL || ulOffset > (ULONG)LONG_MAX);

    if ( (void*)pFile==(void*)g_antiwordStream ) {
        // use CoolReader stream
        LVStream * stream = (LVStream*)pFile;
        // default implementation from Antiword
        if (ulOffset > (ULONG)LONG_MAX) {
            return FALSE;
        }
        if (stream->SetPos(ulOffset)!=ulOffset ) {
            return FALSE;
        }
        lvsize_t bytesRead=0;
        if ( stream->Read(aucBytes, tMemb*sizeof(UCHAR), &bytesRead)!=LVERR_OK || bytesRead != (lvsize_t)tMemb ) {
            return FALSE;
        }
    } else {
        // default implementation from Antiword
        if (ulOffset > (ULONG)LONG_MAX) {
            return FALSE;
        }
        if (fseek(pFile, (long)ulOffset, SEEK_SET) != 0) {
            return FALSE;
        }
        if (fread(aucBytes, sizeof(UCHAR), tMemb, pFile) != tMemb) {
            return FALSE;
        }
    }
    return TRUE;
} /* end of bReadBytes */

/*
 * bTranslateImage - translate the image
 *
 * This function reads the type of the given image and and gets it translated.
 *
 * return TRUE when sucessful, otherwise FALSE
 */
BOOL
bTranslateImage(diagram_type *pDiag, FILE *pFile, BOOL bMinimalInformation,
        ULONG ulFileOffsetImage, const imagedata_type *pImg)
{
    WordImportContext &context = wordImportContext();
    options_type    tOptions;

    DBG_MSG("bTranslateImage");

    fail(pDiag == NULL);
    fail(pFile == NULL);
    fail(ulFileOffsetImage == FC_INVALID);
    fail(pImg == NULL);
    fail(pImg->iHorSizeScaled <= 0);
    fail(pImg->iVerSizeScaled <= 0);

    vGetOptions(&tOptions);
    fail(tOptions.eImageLevel == level_no_images);

    if (bMinimalInformation) {
        return bAddDummyImage(pDiag, pImg);
    }

    switch (pImg->eImageType) {
    case imagetype_is_jpeg:
    case imagetype_is_png:
        {
            lUInt32 offset = (lUInt32)(ulFileOffsetImage + pImg->tPosition);
            lUInt32 len = lUInt32(pImg->tLength - pImg->tPosition);

            if (!bSetDataOffset(pFile, offset)) {
                return FALSE;
            }

            std::vector<lUInt8> image(len);
            for (size_t index = 0; index < len; ++index) {
                int iByte = iNextByte(pFile);
                if (iByte == EOF) {
                    return FALSE;
                }
                image[index] = static_cast<UCHAR>(iByte);
            }

            // add Image BLOB
            lString32 name(BLOB_NAME_PREFIX); // U"@blob#"
            name << "image";
            name << fmt::decimal(context.imageIndex++);
            name << (pImg->eImageType==imagetype_is_jpeg ? ".jpg" : ".png");
            context.writer->OnBlob(name, image.data(), len);
            context.writer->OnTagOpen(LXML_NS_NONE, U"img");
            context.writer->OnAttribute(
                    LXML_NS_NONE, U"src", name.c_str());
            context.writer->OnTagClose(
                    LXML_NS_NONE, U"img", true);

            return TRUE;
        }
     case imagetype_is_dib:
     case imagetype_is_emf:
     case imagetype_is_wmf:
     case imagetype_is_pict:
     case imagetype_is_external:
         /* FIXME */
         return bAddDummyImage(pDiag, pImg);
     case imagetype_is_unknown:
     default:
         DBG_DEC(pImg->eImageType);
         return bAddDummyImage(pDiag, pImg);
     }
} /* end of bTranslateImage */


bool DetectWordFormat( LVStreamRef stream )
{
    std::lock_guard<std::mutex> detectionLock(g_antiwordMutex);
    AntiwordStreamGuard file(stream);

    setOptions();

    lUInt32 lFilesize = (lUInt32)stream->GetSize();
    int iWordVersion = iGuessVersionNumber(file, lFilesize);
    if (iWordVersion < 0 || iWordVersion == 3) {
        if (bIsRtfFile(file)) {
//            CRLog::trace("not a Word Document."
//                " It is probably a Rich Text Format file");
        } if (bIsWordPerfectFile(file)) {
//            CRLog::trace("not a Word Document."
//                " It is probably a Word Perfect file");
        } else {
            //CRLog::error("not a Word Document");
        }
        return FALSE;
    }
    return true;
}

bool ImportWordDocument( LVStreamRef stream, ldomDocument * m_doc, LVDocViewCallback * /*progressCallback*/, CacheLoadingCallback * /*formatCallback*/ )
{
    std::lock_guard<std::mutex> importLock(g_antiwordMutex);
    AntiwordStreamGuard file(stream);

    setOptions();

    BOOL bResult = 0;
    diagram_type	*pDiag;
    int		iWordVersion;

    lUInt32 lFilesize = (lUInt32)stream->GetSize();
    iWordVersion = iGuessVersionNumber(file, lFilesize);
    if (iWordVersion < 0 || iWordVersion == 3) {
        if (bIsRtfFile(file)) {
            CRLog::error("not a Word Document."
                " It is probably a Rich Text Format file");
        } if (bIsWordPerfectFile(file)) {
            CRLog::error("not a Word Document."
                " It is probably a Word Perfect file");
        } else {
            CRLog::error("not a Word Document");
        }
        return FALSE;
    }
    /* Reset any reading done during file testing */
    stream->SetPos(0);


    ldomDocumentWriter w(m_doc);
    WordImportContext context(&w);
    WordImportContextGuard contextGuard(context);

    pDiag = pCreateDiagram("cr3", "filename.doc");
    if (pDiag == NULL) {
        return false;
    }

    bResult = bWordDecryptor(file, lFilesize, pDiag);
    vDestroyDiagram(pDiag);

#ifdef _DEBUG
#define SAVE_COPY_OF_LOADED_DOCUMENT 1//def _DEBUG
#endif
    if ( bResult!=0 ) {
#ifdef SAVE_COPY_OF_LOADED_DOCUMENT //def _DEBUG
        LVStreamRef ostream = LVOpenFileStream( "/tmp/test_save_source.xml", LVOM_WRITE );
		if ( !ostream.isNull() )
			m_doc->saveToStream( ostream, "utf-16" );
#endif
    }

    return bResult!=0;
}


#endif //ENABLE_ANTIWORD==1
