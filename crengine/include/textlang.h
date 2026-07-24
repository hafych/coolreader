/***************************************************************************
 *   CoolReader engine                                                     *
 *   Copyright (C) 2020,2021 poire-z <poire-z@users.noreply.github.com>    *
 *   Copyright (C) 2020 Jellby <jellby@yahoo.com>                          *
 *   Copyright (C) 2020-2022 Aleksey Chernov <valexlin@gmail.com>          *
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

#ifndef __TEXTLANG_H_INCLUDED__
#define __TEXTLANG_H_INCLUDED__

#include "crsetup.h"
#include "lvptrvec.h"
#include "lvstring.h"
#include "cssdef.h"

#include <atomic>

#if USE_HARFBUZZ==1
#include <hb.h>
#include <hb-ft.h>
#endif

#if USE_LIBUNIBREAK==1
    #ifdef __cplusplus
    extern "C" {
    #endif
#include <linebreak.h>
#include <linebreakdef.h>
    #ifdef __cplusplus
    }
    #endif
#endif

// Be similar to HyphMan default state with "hyph-en-us.pattern"
#define TEXTLANG_DEFAULT_MAIN_LANG              "en"   // for LVDocView
#define TEXTLANG_DEFAULT_MAIN_LANG_32           U"en"  // for textlang.cpp
#define TEXTLANG_DEFAULT_EMBEDDED_LANGS_ENABLED false
#define TEXTLANG_DEFAULT_HYPHENATION_ENABLED    true
#define TEXTLANG_DEFAULT_HYPH_SOFT_HYPHENS_ONLY false
#define TEXTLANG_DEFAULT_HYPH_FORCE_ALGORITHMIC false
#define TEXTLANG_FALLBACK_HYPH_DICT_ID          U"hyph-en-us.pattern" // For languages without specific hyph dicts

class TextLangCfg;
class HyphMethod;
struct ldomNode;

class TextLangMan
{
    friend class TextLangCfg;
    enum RuntimeOption {
        RUNTIME_HYPHENATION_ENABLED = 1U << 0,
        RUNTIME_HYPH_FORCE_ALGORITHMIC = 1U << 1,
        RUNTIME_HYPH_SOFT_HYPHENS_ONLY = 1U << 2,
        RUNTIME_EMBEDDED_LANGS_ENABLED = 1U << 3
    };

    static lString32 _main_lang;
    static LVPtrVector<TextLangCfg> _lang_cfg_list;

    static std::atomic<lUInt32> _runtime_options;
    static HyphMethod * _no_hyph_method;       // instance of hyphman NoHyph
    static HyphMethod * _soft_hyphens_method;  // instance of hyphman SoftHyphensHyph
    static HyphMethod * _algo_hyph_method;     // instance of hyphman AlgoHyph

    static lUInt32 getRuntimeOptions() {
        return _runtime_options.load(std::memory_order_relaxed);
    }
    static void setRuntimeOption(lUInt32 option, bool enabled);
public:
    static void uninit();
    static lUInt32 getHash();

    static void setMainLang( lString32 lang_tag ) { _main_lang = lang_tag; }
    static void setMainLangFromHyphDict( lString32 id ); // For HyphMan legacy methods
    static lString32 getMainLang() { return _main_lang; }

    static void setEmbeddedLangsEnabled( bool enabled ) {
        setRuntimeOption(RUNTIME_EMBEDDED_LANGS_ENABLED, enabled);
    }
    static bool getEmbeddedLangsEnabled() {
        return (getRuntimeOptions() & RUNTIME_EMBEDDED_LANGS_ENABLED) != 0;
    }

    static bool getHyphenationEnabled() {
        return (getRuntimeOptions() & RUNTIME_HYPHENATION_ENABLED) != 0;
    }
    static void setHyphenationEnabled( bool enabled ) {
        setRuntimeOption(RUNTIME_HYPHENATION_ENABLED, enabled);
    }

    static bool getHyphenationSoftHyphensOnly() {
        return (getRuntimeOptions() & RUNTIME_HYPH_SOFT_HYPHENS_ONLY) != 0;
    }
    static void setHyphenationSoftHyphensOnly( bool enabled ) {
        setRuntimeOption(RUNTIME_HYPH_SOFT_HYPHENS_ONLY, enabled);
    }

    static bool getHyphenationForceAlgorithmic() {
        return (getRuntimeOptions() & RUNTIME_HYPH_FORCE_ALGORITHMIC) != 0;
    }
    static void setHyphenationForceAlgorithmic( bool enabled ) {
        setRuntimeOption(RUNTIME_HYPH_FORCE_ALGORITHMIC, enabled);
    }

    static TextLangCfg * getTextLangCfg(); // get LangCfg for _main_lang
    static TextLangCfg * getTextLangCfg( lString32 lang_tag );
    static TextLangCfg * getTextLangCfg( ldomNode * node );
    static int getLangNodeIndex( ldomNode * node );

    static HyphMethod * getMainLangHyphMethod(); // For HyphMan::hyphenate()

    static void resetCounters();

    // For frontend info about TextLangMan status and seen langs
    static LVPtrVector<TextLangCfg> * getLangCfgList() {
        return &_lang_cfg_list;
    }

    TextLangMan();
    ~TextLangMan();
};

#define MAX_NB_LB_PROPS_ITEMS 20 // for our statically sized array (increase if needed)

#if USE_LIBUNIBREAK==1
typedef lChar32 (*lb_char_sub_func_t)(struct LineBreakContext *lbpCtx, const lChar32 * text, int pos, int next_usable);
#endif

class TextLangCfg
{
    friend class TextLangMan;
    lString32 _lang_tag;
    HyphMethod * _hyph_method;

    lString32 _open_quote1;
    lString32 _close_quote1;
    lString32 _open_quote2;
    lString32 _close_quote2;
    int _quote_nesting_level;

    #if USE_HARFBUZZ==1
    hb_language_t _hb_language;
    #endif

    #if USE_LIBUNIBREAK==1
    lb_char_sub_func_t _lb_char_sub_func;
    struct LineBreakProperties _lb_props[MAX_NB_LB_PROPS_ITEMS];
    #endif

    bool _duplicate_real_hyphen_on_next_line;
    bool _is_ja_zh;

    void resetCounters();

public:
    lString32 getLangTag() const { return _lang_tag; }

    HyphMethod * getHyphMethod() const {
        lUInt32 options = TextLangMan::getRuntimeOptions();
        if ( !(options & TextLangMan::RUNTIME_HYPHENATION_ENABLED) )
            return TextLangMan::_no_hyph_method;
        if ( options & TextLangMan::RUNTIME_HYPH_SOFT_HYPHENS_ONLY )
            return TextLangMan::_soft_hyphens_method;
        if ( options & TextLangMan::RUNTIME_HYPH_FORCE_ALGORITHMIC )
            return TextLangMan::_algo_hyph_method;
        return _hyph_method;
    }
    HyphMethod * getDefaultHyphMethod() const {
        return _hyph_method;
    }

    lString32 & getOpeningQuote( bool update_level=true );
    lString32 & getClosingQuote( bool update_level=true );

    int getHyphenHangingPercent();
    int getHangingPercent( bool right_hanging, bool & check_font, const lChar32 * text, int pos, int next_usable );

    #if USE_HARFBUZZ==1
    hb_language_t getHBLanguage() const { return _hb_language; }
    #endif

    #if USE_LIBUNIBREAK==1
    bool hasLBCharSubFunc() const { return _lb_char_sub_func != NULL; }
    lb_char_sub_func_t getLBCharSubFunc() const { return _lb_char_sub_func; }
    struct LineBreakProperties * getLBProps() const { return (struct LineBreakProperties *)_lb_props; }
    lChar32 getCssLbCharSub(css_line_break_t css_linebreak, css_word_break_t css_wordbreak,
                struct LineBreakContext *lbpCtx, const lChar32 * text, int pos, int next_usable, lChar32 tweaked_ch);
    #endif

    bool duplicateRealHyphenOnNextLine() const { return _duplicate_real_hyphen_on_next_line; }

    TextLangCfg( lString32 lang_tag );
    ~TextLangCfg();
};

#endif
