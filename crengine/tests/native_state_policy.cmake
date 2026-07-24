if(NOT DEFINED SOURCE_ROOT)
  message(FATAL_ERROR "SOURCE_ROOT is required")
endif()

file(READ "${SOURCE_ROOT}/crengine/src/lvtextfm.cpp" FORMATTER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvrend.cpp" RENDER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvbmpbuf.cpp" BITMAP_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/crskin.cpp" SKIN_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvtinydom.cpp" DOM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvtinydom.h" DOM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvrend.h" RENDER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/hyphman.cpp" HYPH_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/hyphman.h" HYPH_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/textlang.cpp" TEXTLANG_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/textlang.h" TEXTLANG_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfntman.cpp" FONT_MANAGER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfreetypeface.cpp" FREETYPE_FACE_SOURCE)

function(require_source_text SOURCE_VALUE EXPECTED DESCRIPTION)
  string(FIND "${SOURCE_VALUE}" "${EXPECTED}" POSITION)
  if(POSITION EQUAL -1)
    message(FATAL_ERROR "${DESCRIPTION}: missing '${EXPECTED}'")
  endif()
endfunction()

function(forbid_source_text SOURCE_VALUE FORBIDDEN DESCRIPTION)
  string(FIND "${SOURCE_VALUE}" "${FORBIDDEN}" POSITION)
  if(NOT POSITION EQUAL -1)
    message(FATAL_ERROR "${DESCRIPTION}: found '${FORBIDDEN}'")
  endif()
endfunction()

require_source_text(
  "${FORMATTER_SOURCE}"
  "static thread_local bool m_staticBufs_inUse"
  "formatter re-entry state must be isolated per thread"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "thread_local bool LVFormatter::m_staticBufs_inUse"
  "formatter re-entry state definition must be isolated per thread"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "std::call_once(m_libunibreak_init_once"
  "libunibreak initialization must be synchronized"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "static thread_local lChar32 m_static_text"
  "formatter scratch text must be isolated per thread"
)
require_source_text(
  "${RENDER_SOURCE}"
  "static thread_local lUInt16 widths[MAX_TEXT_CHUNK_SIZE+1]"
  "render measurement scratch must be isolated per thread"
)
require_source_text(
  "${RENDER_SOURCE}"
  "static std::atomic<int> rend_font_base_weight"
  "process-wide render base weight must be synchronized"
)
require_source_text(
  "${RENDER_SOURCE}"
  "static std::atomic<int> g_render_dpi"
  "process-wide render DPI must be synchronized"
)
require_source_text(
  "${RENDER_SOURCE}"
  "static std::atomic<bool> g_render_scale_font_with_dpi"
  "process-wide font DPI scaling must be synchronized"
)
require_source_text(
  "${BITMAP_SOURCE}"
  "static thread_local lUInt8 glyph_buf[16384]"
  "bitmap glyph scratch must be isolated per thread"
)
require_source_text(
  "${DOM_HEADER}"
  "static std::atomic<ldomDocument *>"
  "document registry slots must be atomic"
)
require_source_text(
  "${DOM_SOURCE}"
  "compare_exchange_strong"
  "document registry updates must be atomic"
)
require_source_text(
  "${HYPH_HEADER}"
  "static std::atomic<int> _OverriddenLeftHyphenMin"
  "left hyphen minimum must be synchronized"
)
require_source_text(
  "${HYPH_HEADER}"
  "static std::atomic<int> _OverriddenRightHyphenMin"
  "right hyphen minimum must be synchronized"
)
require_source_text(
  "${HYPH_HEADER}"
  "static std::atomic<int> _TrustSoftHyphens"
  "soft-hyphen policy must be synchronized"
)
require_source_text(
  "${HYPH_HEADER}"
  "static std::unique_ptr<HyphDictionaryList> _dictList"
  "hyphenation dictionary list must have explicit ownership"
)
require_source_text(
  "${HYPH_HEADER}"
  "static std::unique_ptr<HyphDataLoader> _dataLoader"
  "hyphenation data loader must have explicit ownership"
)
require_source_text(
  "${HYPH_SOURCE}"
  "std::vector<std::unique_ptr<HyphMethod>> _owned"
  "loaded hyphenation methods must have explicit ownership"
)
require_source_text(
  "${HYPH_SOURCE}"
  "std::lock_guard<std::mutex> guard(g_hyph_method_cache_mutex)"
  "loaded hyphenation method cache must be synchronized"
)
require_source_text(
  "${TEXTLANG_HEADER}"
  "static std::atomic<lUInt32> _runtime_options"
  "text-language runtime options must be synchronized"
)
require_source_text(
  "${TEXTLANG_SOURCE}"
  "_runtime_options.compare_exchange_weak"
  "text-language option updates must preserve one coherent snapshot"
)
require_source_text(
  "${TEXTLANG_HEADER}"
  "static std::vector<std::unique_ptr<TextLangCfg>> _lang_cfg_list"
  "text-language configurations must have explicit ownership"
)
require_source_text(
  "${TEXTLANG_HEADER}"
  "static std::mutex _lang_cfg_mutex"
  "text-language configuration cache must be synchronized"
)
require_source_text(
  "${TEXTLANG_SOURCE}"
  "std::lock_guard<std::mutex> guard(_lang_cfg_mutex)"
  "text-language cache access must be serialized"
)
require_source_text(
  "${TEXTLANG_SOURCE}"
  "return lString32(_main_lang.c_str(), _main_lang.length())"
  "main-language reads must not expose a shared desktop string chunk"
)
require_source_text(
  "${TEXTLANG_SOURCE}"
  "_main_lang = lString32(lang_tag.c_str(), lang_tag.length())"
  "main-language writes must not retain a caller-owned desktop string chunk"
)
require_source_text(
  "${FONT_MANAGER_SOURCE}"
  "std::unique_ptr<LVFontManager> g_font_manager_owner"
  "the process-wide font manager must have explicit ownership"
)
require_source_text(
  "${FONT_MANAGER_SOURCE}"
  "std::mutex g_font_manager_lifecycle_mutex"
  "font-manager lifecycle operations must be serialized"
)
require_source_text(
  "${FONT_MANAGER_SOURCE}"
  "std::atomic<int> g_font_gamma_index"
  "the process-wide font gamma index must be synchronized"
)
require_source_text(
  "${FONT_MANAGER_SOURCE}"
  "std::mutex g_font_gamma_mutex"
  "font gamma changes and cache invalidation must be serialized"
)

forbid_source_text(
  "${FORMATTER_SOURCE}"
  "m_libunibreak_init_done"
  "unsynchronized libunibreak initialization is forbidden"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "static int counter"
  "skin recursion depth must not be process-wide"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "static int _nextDocumentIndex"
  "document registry index must not have unsynchronized writes"
)
forbid_source_text(
  "${RENDER_SOURCE}"
  "static int rend_font_base_weight"
  "render base weight must not have unsynchronized writes"
)
forbid_source_text(
  "${RENDER_SOURCE}"
  "int gRenderDPI"
  "render DPI must not expose unsynchronized storage"
)
forbid_source_text(
  "${RENDER_SOURCE}"
  "bool gRenderScaleFontWithDPI"
  "font DPI scaling must not expose unsynchronized storage"
)
forbid_source_text(
  "${RENDER_HEADER}"
  "gRootFontSize"
  "unused render root-font global declaration must not return"
)
forbid_source_text(
  "${RENDER_HEADER}"
  "gRenderDPI"
  "render DPI storage must remain private"
)
forbid_source_text(
  "${RENDER_HEADER}"
  "gRenderScaleFontWithDPI"
  "font DPI scaling storage must remain private"
)
forbid_source_text(
  "${HYPH_SOURCE}"
  "int HyphMan::_OverriddenLeftHyphenMin"
  "left hyphen minimum must not have unsynchronized storage"
)
forbid_source_text(
  "${HYPH_SOURCE}"
  "int HyphMan::_OverriddenRightHyphenMin"
  "right hyphen minimum must not have unsynchronized storage"
)
forbid_source_text(
  "${HYPH_SOURCE}"
  "int HyphMan::_TrustSoftHyphens"
  "soft-hyphen policy must not have unsynchronized storage"
)
forbid_source_text(
  "${HYPH_SOURCE}"
  "delete _dictList"
  "hyphenation dictionary list must use RAII teardown"
)
forbid_source_text(
  "${HYPH_SOURCE}"
  "delete _dataLoader"
  "hyphenation data loader must use RAII teardown"
)
forbid_source_text(
  "${HYPH_SOURCE}"
  "delete pair->value"
  "loaded hyphenation methods must use RAII teardown"
)
forbid_source_text(
  "${TEXTLANG_HEADER}"
  "static bool _embedded_langs_enabled"
  "embedded-language mode must not have unsynchronized storage"
)
forbid_source_text(
  "${TEXTLANG_HEADER}"
  "static bool _hyphenation_enabled"
  "text-language hyphenation mode must not have unsynchronized storage"
)
forbid_source_text(
  "${TEXTLANG_HEADER}"
  "static bool _hyphenation_soft_hyphens_only"
  "soft-hyphen-only mode must not have unsynchronized storage"
)
forbid_source_text(
  "${TEXTLANG_HEADER}"
  "static bool _hyphenation_force_algorithmic"
  "algorithmic hyphenation mode must not have unsynchronized storage"
)
forbid_source_text(
  "${TEXTLANG_HEADER}"
  "static LVPtrVector<TextLangCfg> _lang_cfg_list"
  "text-language configuration ownership must not regress to raw pointers"
)
forbid_source_text(
  "${TEXTLANG_HEADER}"
  "getLangCfgList()"
  "the mutable text-language cache container must not be exposed"
)
forbid_source_text(
  "${FONT_MANAGER_SOURCE}"
  "delete fontMan"
  "font-manager shutdown must use RAII ownership"
)
forbid_source_text(
  "${FONT_MANAGER_SOURCE}"
  "static double gammaLevel"
  "font gamma level must be derived from one atomic index"
)
forbid_source_text(
  "${FONT_MANAGER_SOURCE}"
  "int gammaIndex ="
  "font gamma index storage must remain private and synchronized"
)
forbid_source_text(
  "${FREETYPE_FACE_SOURCE}"
  "extern int gammaIndex"
  "glyph rendering must use the synchronized font gamma API"
)
