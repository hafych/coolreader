/***************************************************************************
 *   CoolReader GUI                                                        *
 *   Copyright (C) 2008-2011 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2008,2009 Alexander V. Nikolaev <avn@daemon.hole.ru>    *
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
// C++ Interface: settings
//

#ifndef MAINWND_H_INCLUDED
#define MAINWND_H_INCLUDED

#include <crengine.h>
#include <crgui.h>
#include <crtrace.h>
#include "fsmenu.h"
#include "settings.h"
#include "t9encoding.h"

#ifndef WITH_DICT
#define WITH_DICT
#endif


#include "viewdlg.h"

#if defined(_WIN32) || !defined(CR_USE_XCB)

//define key codes here
#define XK_Return   0xFF0D
#define XK_Up       0xFF52
#define XK_Down     0xFF54
#define XK_Escape   0xFF1B
#define XK_KP_Add   0xffab 
#define XK_KP_Subtract 0xffad
#define XK_Left     0xFF51
#define XK_Right    0xFF53
#define XK_Prior    0xFF55
#define XK_Next     0xFF56	
#define XK_KP_Enter 0xFF8D
#define XK_Menu	    0xFF67
#define XF86XK_RotateWindows    0x1008FF74
#define XF86XK_Search           0x1008FF1B
#define XF86XK_AudioPlay        0x1008FF14

#else

//use standard X11 key defs
#define XK_MISCELLANY
#include <X11/keysymdef.h>
#include <X11/XF86keysym.h>

#endif

#define MAIN_MENU_COMMANDS_START 200
// don't forget to update keydefs.ini after editing of CRMainMenuCmd
enum CRMainMenuCmd
{
    MCMD_BEGIN = MAIN_MENU_COMMANDS_START,
    MCMD_QUIT,
    MCMD_MAIN_MENU,
    MCMD_GO_PAGE,
    MCMD_GO_PAGE_APPLY,
    MCMD_SETTINGS,
    MCMD_SETTINGS_APPLY,
    MCMD_SETTINGS_FONTSIZE,
    MCMD_SETTINGS_ORIENTATION,
    MCMD_GO_LINK,
    MCMD_GO_LINK_APPLY,
    MCMD_LONG_FORWARD,
    MCMD_LONG_BACK,
    MCMD_DICT,
    MCMD_BOOKMARK_LIST,
    MCMD_RECENT_BOOK_LIST,
    MCMD_OPEN_RECENT_BOOK,
    MCMD_ABOUT,
    MCMD_CITE,
    MCMD_SEARCH,
    MCMD_SEARCH_FINDFIRST,
    MCMD_DICT_VKEYBOARD,
    MCMD_DICT_FIND,
    MCMD_KBD_NEXTLAYOUT,
    MCMD_KBD_PREVLAYOUT,
    MCMD_HELP,
    MCMD_HELP_KEYS,
    MCMD_SWITCH_TO_RECENT_BOOK,
    MCMD_NEXT_MODE,
    MCMD_PREV_MODE,
    MCMD_BOOKMARK_LIST_GO_MODE,

    MCMD_GO_PERCENT,
    MCMD_GO_PERCENT_APPLY,
    MCMD_CITES_LIST
};

class V3DocViewWin : public CRViewDialog, public LVDocViewCallback
{
protected:
    CRPropRef _props;
    CRPropRef _newProps;
    lString32 _settingsFileName;
    lString32 _historyFileName;
    lString8  _css;
    lString32 _dictConfig;
    lString32 _bookmarkDir;
	lString32 _helpFile;
    lString32 _cssDir;
    time_t _loadFileStart;
public:
    void setBookmarkDir( lString32 dir ) { _bookmarkDir = dir; }
    void flush() override;
    bool loadDocument( lString32 filename );
    bool loadDefaultCover( lString32 filename );
    bool loadCSS( lString32 filename );
    bool loadSettings( lString32 filename );
    bool loadSettings( lString16 filename )
    {
        return loadSettings(Utf16ToUnicode(filename));
    }
    bool saveSettings( lString32 filename );
    bool loadHistory( lString32 filename );
    bool loadHistory( lString16 filename )
    {
        return loadHistory(Utf16ToUnicode(filename));
    }
    bool saveHistory( lString32 filename, bool exportBookmarks = true );
    bool saveHistory( lString16 filename, bool exportBookmarks = true )
    {
        return saveHistory(Utf16ToUnicode(filename), exportBookmarks);
    }
    bool loadHistory( LVStreamRef stream );
    bool saveHistory( LVStreamRef stream );
    bool loadDictConfig( lString32 filename );
	bool setHelpFile( lString32 filename );
	/// on starting file loading
	void OnLoadFileStart( lString32 filename ) override;
	/// format detection finished
	void OnLoadFileFormatDetected( doc_format_t fileFormat ) override;
	/// file loading is finished successfully - drawCoveTo() may be called there
	void OnLoadFileEnd() override;
	/// file progress indicator, called with values 0..100
	void OnLoadFileProgress( int percent ) override;
    /// first page is loaded from file an can be formatted for preview
    void OnLoadFileFirstPagesReady() override;
	/// document formatting started
	void OnFormatStart() override;
	/// document formatting finished
	void OnFormatEnd() override;
	/// format progress, called with values 0..100
	void OnFormatProgress( int percent ) override;
	/// file load finiished with error
	void OnLoadFileError( lString32 message ) override;
    /// Override to handle external links
    void OnExternalLink( lString32 url, ldomNode * node ) override;

    /// returns current properties
    CRPropRef getProps() { return _props; }

    /// sets new properties
    void setProps( CRPropRef props )
    {
        _props = props;
        _docview->propsUpdateDefaults( _props );
    }

    explicit V3DocViewWin( CRGUIWindowManager * wm );

    void applySettings();

    void showSettingsMenu();

#if CR_INTERNAL_PAGE_ORIENTATION==1 || defined(CR_POCKETBOOK)
    void showOrientationMenu();
#endif

    void showFontSizeMenu();

    void showMainMenu();

    void showBookmarksMenu( bool goMode=false );

    void showCitesMenu();

    void showRecentBooksMenu();

    void openRecentBook( int index );

    void showAboutDialog();

    void showHelpDialog();


    bool onCommand( int command, int params ) override;

    void closing() override;
};


#endif
