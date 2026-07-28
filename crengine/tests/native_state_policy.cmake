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
file(READ "${SOURCE_ROOT}/crengine/include/lvfntman.h" FONT_MANAGER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfreetypeface.cpp" FREETYPE_FACE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfontglyphcache.h" GLYPH_CACHE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfontglyphcache.cpp" GLYPH_CACHE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvdocview.h" DOC_VIEW_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstring.cpp" LVSTRING_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/wordfmt.cpp" WORD_FORMAT_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/crconcurrent.cpp" CONCURRENCY_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/crlocks.h" LOCKS_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvtextparser.cpp" TEXT_PARSER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvhtmlparser.cpp" HTML_PARSER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvxmlparser.cpp" XML_PARSER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvtextbookmarkparser.cpp" BOOKMARK_PARSER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvfileparserbase.h" FILE_PARSER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvfileparserbase.cpp" FILE_PARSER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvtextfilebase.h" TEXT_FILE_BASE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvtextfilebase.cpp" TEXT_FILE_BASE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/rtfimp.h" RTF_PARSER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/rtfimp.cpp" RTF_PARSER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvimg.cpp" IMAGE_FACTORY_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifimagesource.cpp" GIF_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifimagesource.h" GIF_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifframe.cpp" GIF_FRAME_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifframe.h" GIF_FRAME_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvdrawbufimgsource.cpp" DRAWBUF_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvdrawbufimgsource.h" DRAWBUF_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvdummyimagesource.h" DUMMY_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvxpmimagesource.cpp" XPM_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvxpmimagesource.h" XPM_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvunpackedimgsource.cpp" UNPACKED_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvunpackedimgsource.h" UNPACKED_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvdrawbuf/lvimagescaleddrawcallback.cpp" SCALED_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvdrawbuf/lvimagescaleddrawcallback.h" SCALED_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvzipdecodestream.cpp" ZIP_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvzipdecodestream.h" ZIP_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvcachedstream.cpp" CACHED_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvcachedstream.h" CACHED_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvtcrstream.cpp" TCR_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvtcrstream.h" TCR_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvdefstreambuffer.cpp" DEFAULT_STREAM_BUFFER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvdefstreambuffer.h" DEFAULT_STREAM_BUFFER_HEADER)

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
require_source_text(
  "${FONT_MANAGER_HEADER}"
  "std::atomic<font_antialiasing_t> _antialiasMode"
  "font antialiasing mode must be synchronized"
)
require_source_text(
  "${FONT_MANAGER_HEADER}"
  "std::atomic<bool> _allowKerning"
  "font kerning mode must be synchronized"
)
require_source_text(
  "${FONT_MANAGER_HEADER}"
  "std::atomic<shaping_mode_t> _shapingMode"
  "font shaping mode must be synchronized"
)
require_source_text(
  "${FONT_MANAGER_HEADER}"
  "std::atomic<hinting_mode_t> _hintingMode"
  "font hinting mode must be synchronized"
)
require_source_text(
  "${FONT_MANAGER_HEADER}"
  "std::mutex _renderSettingsMutex"
  "font render setting changes must be serialized"
)
require_source_text(
  "${GLYPH_CACHE_HEADER}"
  "std::atomic<lUInt64> hit_count"
  "glyph cache hits must be observable"
)
require_source_text(
  "${GLYPH_CACHE_HEADER}"
  "std::atomic<lUInt64> miss_count"
  "glyph cache misses must be observable"
)
require_source_text(
  "${GLYPH_CACHE_HEADER}"
  "std::atomic<lUInt64> eviction_count"
  "glyph cache evictions must be observable"
)
require_source_text(
  "${GLYPH_CACHE_SOURCE}"
  "if (head != item)"
  "glyph cache hits must refresh least-recently-used order"
)
require_source_text(
  "${DOC_VIEW_HEADER}"
  "_mutex.unlock();"
  "page image cache misses must release their mutex"
)
require_source_text(
  "${DOC_VIEW_HEADER}"
  "LVLock lock( _mutex );"
  "page image cache probes must use scoped locking"
)
require_source_text(
  "${SKIN_SOURCE}"
  "_imageCache.getStats()"
  "decoded image cache must expose bounded cache counters"
)
require_source_text(
  "${SKIN_SOURCE}"
  "std::lock_guard<std::mutex> guard(_imageCacheMutex);"
  "decoded image cache access must be synchronized"
)
require_source_text(
  "${DOM_SOURCE}"
  "static std::shared_ptr<ldomDocCacheImpl> _cacheInstance"
  "document cache manager lifetime must use RAII ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "static std::mutex _cacheInstanceMutex"
  "document cache manager lifecycle must be synchronized"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "static ldomDocCacheImpl * _cacheInstance"
  "document cache manager must not use an owning raw pointer"
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

# --- Antiword bridge: serialized library and operation-local callback state ---
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "static thread_local WordImportContext *g_wordImportContext"
  "Word import callback state must be isolated per calling thread"
)
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "static thread_local LVStream *g_antiwordStream"
  "Antiword stream adaptation must be isolated per calling thread"
)
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "static std::mutex g_antiwordMutex"
  "third-party Antiword entry points must be serialized"
)
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "std::lock_guard<std::mutex> detectionLock(g_antiwordMutex)"
  "Antiword format detection must hold the library mutex"
)
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "std::lock_guard<std::mutex> importLock(g_antiwordMutex)"
  "Antiword document import must hold the library mutex"
)
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "WordImportContextGuard contextGuard(context)"
  "Word import callback context must use scoped publication"
)
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "context.insideList = 0"
  "Word list teardown must reset per-import list state"
)
forbid_source_text(
  "${WORD_FORMAT_SOURCE}"
  "static ldomDocumentWriter * writer"
  "Word import must not publish a process-wide writer"
)
forbid_source_text(
  "${WORD_FORMAT_SOURCE}"
  "static LVStream * antiword_stream"
  "Antiword stream adaptation must not use process-wide mutable state"
)
forbid_source_text(
  "${WORD_FORMAT_SOURCE}"
  "static bool inside_p"
  "Word paragraph state must remain operation-local"
)
forbid_source_text(
  "${WORD_FORMAT_SOURCE}"
  "static int image_index"
  "Word image numbering must remain operation-local"
)

# --- legacy engine locks: fallback, RAII ownership and quiescent lifecycle ---
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "class CRStdMutex : public CRMutex"
  "engine guards must have a built-in mutex fallback"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "std::recursive_mutex m_mutex"
  "built-in engine locks must preserve recursive legacy semantics"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "std::unique_ptr<CRMutex> g_refMutexOwner"
  "engine reference mutex must have explicit ownership"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "std::unique_ptr<CRMutex> g_crengineMutexOwner"
  "engine drawing mutex must have explicit ownership"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "std::mutex g_concurrencyLifecycleMutex"
  "engine mutex setup and teardown must be serialized"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "void CRShutdownEngineConcurrency()"
  "engine mutex ownership must have explicit quiescent teardown"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "clearEngineMutexViews();"
  "engine mutex views must be cleared before owner teardown"
)
require_source_text(
  "${LOCKS_HEADER}"
  "Non-owning compatibility views"
  "legacy engine mutex pointers must document non-owning status"
)
forbid_source_text(
  "${CONCURRENCY_SOURCE}"
  "_refMutex = concurrencyProvider->createMutex()"
  "engine mutex provider results must not publish raw ownership"
)
forbid_source_text(
  "${CONCURRENCY_SOURCE}"
  "if (!_refMutex)"
  "engine mutex setup must not use partial raw-pointer initialization"
)

# --- parser format detection: operation-scoped decoded buffers ---
foreach(PARSER_SOURCE
    TEXT_PARSER_SOURCE
    HTML_PARSER_SOURCE
    XML_PARSER_SOURCE
    BOOKMARK_PARSER_SOURCE)
  require_source_text(
    "${${PARSER_SOURCE}}"
    "std::vector<lChar32> chbuf"
    "format detector buffer must use scoped RAII ownership"
  )
  forbid_source_text(
    "${${PARSER_SOURCE}}"
    "new lChar32["
    "format detector buffer must not regress to owning new[]"
  )
endforeach()
require_source_text(
  "${TEXT_FILE_BASE_SOURCE}"
  "LVStreamPositionGuard positionGuard"
  "encoding detection must restore stream position with a scope guard"
)
require_source_text(
  "${TEXT_FILE_BASE_SOURCE}"
  "std::vector<unsigned char> buf"
  "encoding detection buffer must use scoped RAII ownership"
)
forbid_source_text(
  "${TEXT_FILE_BASE_SOURCE}"
  "new unsigned char[ sz ]"
  "encoding detection buffer must not regress to owning new[]"
)

# --- parser base: persistent read window and charset table ---
require_source_text(
  "${FILE_PARSER_HEADER}"
  "std::vector<lUInt8> m_buf"
  "parser read window must use RAII ownership"
)
require_source_text(
  "${FILE_PARSER_SOURCE}"
  "m_buf_len = (int)bytesRead"
  "parser read window must expose only bytes actually read"
)
forbid_source_text(
  "${FILE_PARSER_HEADER}"
  "lUInt8 * m_buf"
  "parser base must not own a raw read buffer"
)
forbid_source_text(
  "${FILE_PARSER_SOURCE}"
  "cr_realloc( m_buf"
  "parser read-window growth must remain container-managed"
)
forbid_source_text(
  "${FILE_PARSER_SOURCE}"
  "free( m_buf"
  "parser read-window teardown must remain automatic"
)
require_source_text(
  "${TEXT_FILE_BASE_HEADER}"
  "std::vector<lChar32> m_conv_table"
  "parser charset table must use RAII ownership"
)
require_source_text(
  "${TEXT_FILE_BASE_SOURCE}"
  "m_conv_table.assign(table, table + 128)"
  "parser charset table must copy caller-provided mappings"
)
forbid_source_text(
  "${TEXT_FILE_BASE_HEADER}"
  "lChar32 * m_conv_table"
  "parser base must not own a raw charset table"
)
forbid_source_text(
  "${TEXT_FILE_BASE_SOURCE}"
  "new lChar32[128]"
  "parser charset-table allocation must remain container-managed"
)
forbid_source_text(
  "${TEXT_FILE_BASE_SOURCE}"
  "delete[] m_conv_table"
  "parser charset-table teardown must remain automatic"
)
require_source_text(
  "${RTF_PARSER_HEADER}"
  "std::vector<lChar32> m_textBuffer"
  "RTF text buffer must use RAII ownership"
)
require_source_text(
  "${RTF_PARSER_SOURCE}"
  "m_textBuffer.resize(MAX_TXT_SIZE + 1)"
  "RTF parser must prepare its owned text buffer before parsing"
)
require_source_text(
  "${RTF_PARSER_SOURCE}"
  "m_textBuffer.data(), m_textPos, TXTFLG_RTF"
  "RTF callbacks must receive a non-owning view of the text buffer"
)
require_source_text(
  "${RTF_PARSER_HEADER}"
  "std::vector<std::unique_ptr<LVRtfDestination>> m_destinationOwners"
  "RTF destinations must have explicit LIFO ownership"
)
require_source_text(
  "${RTF_PARSER_HEADER}"
  "void set( std::unique_ptr<LVRtfDestination> newdest )"
  "RTF destination transitions must transfer explicit ownership"
)
require_source_text(
  "${RTF_PARSER_SOURCE}"
  "m_stack.unwindDestinations()"
  "RTF parser must close truncated destination groups before OnStop"
)
forbid_source_text(
  "${RTF_PARSER_HEADER}"
  "lChar32 * txtbuf"
  "RTF parser must not own a raw text buffer"
)
forbid_source_text(
  "${RTF_PARSER_HEADER}"
  "const lChar32 * m_conv_table"
  "unused RTF charset-table pointer must not return"
)
forbid_source_text(
  "${RTF_PARSER_SOURCE}"
  "new lChar32[ MAX_TXT_SIZE"
  "RTF text-buffer allocation must remain container-managed"
)
forbid_source_text(
  "${RTF_PARSER_SOURCE}"
  "delete[] txtbuf"
  "RTF text-buffer teardown must remain automatic"
)
forbid_source_text(
  "${RTF_PARSER_HEADER}"
  "delete dest"
  "RTF destination teardown must remain automatic"
)
forbid_source_text(
  "${RTF_PARSER_HEADER}"
  "void set( LVRtfDestination * newdest )"
  "RTF destination transitions must not accept implicit raw ownership"
)
forbid_source_text(
  "${RTF_PARSER_SOURCE}"
  "m_stack.set( new LVRtf"
  "RTF destination creation must use explicit ownership transfer"
)

# --- GIF decoder: image/frame buffers and color tables ---
require_source_text(
  "${GIF_IMAGE_HEADER}"
  "std::vector<lUInt32> m_global_color_table"
  "GIF global color table must use RAII ownership"
)
require_source_text(
  "${GIF_IMAGE_SOURCE}"
  "std::vector<lUInt8> buf"
  "GIF input buffer must use RAII ownership"
)
require_source_text(
  "${GIF_FRAME_HEADER}"
  "std::vector<lUInt32> m_local_color_table"
  "GIF local color table must use RAII ownership"
)
require_source_text(
  "${GIF_FRAME_HEADER}"
  "std::vector<unsigned char> m_buffer"
  "GIF decoded frame buffer must use RAII ownership"
)
require_source_text(
  "${GIF_FRAME_SOURCE}"
  "std::vector<unsigned char> stream_buffer"
  "GIF compressed stream buffer must use RAII ownership"
)
require_source_text(
  "${GIF_FRAME_SOURCE}"
  "std::vector<lUInt32> line"
  "GIF output row must use RAII ownership"
)
forbid_source_text(
  "${GIF_IMAGE_HEADER}"
  "LVGifFrame ** m_frames"
  "unused owning GIF frame array must not return"
)
forbid_source_text(
  "${GIF_IMAGE_SOURCE}"
  "new lUInt"
  "GIF image decoder must not use owning new[] buffers"
)
forbid_source_text(
  "${GIF_FRAME_SOURCE}"
  "new lUInt"
  "GIF frame decoder must not use owning color/row arrays"
)
forbid_source_text(
  "${GIF_FRAME_SOURCE}"
  "new unsigned char"
  "GIF frame decoder must not use owning byte arrays"
)

# --- synthetic/draw-buffer/XPM image sources ---
require_source_text(
  "${DRAWBUF_IMAGE_HEADER}"
  "std::unique_ptr<LVColorDrawBuf> _ownedBuf"
  "draw-buffer image ownership must be explicit"
)
require_source_text(
  "${DRAWBUF_IMAGE_SOURCE}"
  "std::vector<lUInt32> row"
  "draw-buffer image conversion row must use RAII ownership"
)
forbid_source_text(
  "${DRAWBUF_IMAGE_HEADER}"
  "bool _own"
  "draw-buffer image must not encode ownership as a boolean"
)
forbid_source_text(
  "${DRAWBUF_IMAGE_SOURCE}"
  "delete _buf"
  "draw-buffer image teardown must use RAII ownership"
)
require_source_text(
  "${DUMMY_IMAGE_HEADER}"
  "std::vector<lUInt32> row"
  "dummy image row must use RAII ownership"
)
forbid_source_text(
  "${DUMMY_IMAGE_HEADER}"
  "new lUInt32"
  "dummy image must not use an owning row array"
)
require_source_text(
  "${XPM_IMAGE_HEADER}"
  "std::vector<std::vector<char>> _rows"
  "XPM rows must use nested RAII ownership"
)
require_source_text(
  "${XPM_IMAGE_HEADER}"
  "std::vector<lUInt32> _palette"
  "XPM palette must use RAII ownership"
)
require_source_text(
  "${XPM_IMAGE_SOURCE}"
  "std::vector<lUInt32> row"
  "XPM output row must use RAII ownership"
)
require_source_text(
  "${XPM_IMAGE_SOURCE}"
  "_rows.clear();"
  "invalid XPM construction must release parsed rows"
)
forbid_source_text(
  "${XPM_IMAGE_SOURCE}"
  "new char"
  "XPM source must not use owning row arrays"
)
forbid_source_text(
  "${XPM_IMAGE_SOURCE}"
  "new lUInt32"
  "XPM source must not use owning palette/output arrays"
)
require_source_text(
  "${UNPACKED_IMAGE_HEADER}"
  "std::vector<lUInt8> _grayImage"
  "unpacked grayscale pixels must use RAII ownership"
)
require_source_text(
  "${UNPACKED_IMAGE_HEADER}"
  "std::vector<lUInt16> _colorImage16"
  "unpacked 16-bit pixels must use RAII ownership"
)
require_source_text(
  "${UNPACKED_IMAGE_HEADER}"
  "std::vector<lUInt32> _colorImage"
  "unpacked 32-bit pixels must use RAII ownership"
)
require_source_text(
  "${UNPACKED_IMAGE_SOURCE}"
  "std::vector<lUInt32> line(_dx)"
  "unpacked image conversion rows must use scoped RAII ownership"
)
require_source_text(
  "${UNPACKED_IMAGE_SOURCE}"
  "_valid = !errors"
  "unpacked image construction must observe decoder failures"
)
require_source_text(
  "${IMAGE_FACTORY_SOURCE}"
  "std::unique_ptr<LVUnpackedImgSource> img"
  "unpacked image candidates must have explicit ownership"
)
require_source_text(
  "${IMAGE_FACTORY_SOURCE}"
  "LVImageSourceRef(img.release())"
  "unpacked image ownership must transfer only at the reference boundary"
)
forbid_source_text(
  "${UNPACKED_IMAGE_HEADER}"
  "lUInt8 * _grayImage"
  "unpacked image source must not own a raw grayscale buffer"
)
forbid_source_text(
  "${UNPACKED_IMAGE_HEADER}"
  "lUInt16 * _colorImage16"
  "unpacked image source must not own a raw 16-bit buffer"
)
forbid_source_text(
  "${UNPACKED_IMAGE_HEADER}"
  "lUInt32 * _colorImage"
  "unpacked image source must not own a raw 32-bit buffer"
)
forbid_source_text(
  "${UNPACKED_IMAGE_SOURCE}"
  "malloc("
  "unpacked image allocation must remain container-managed"
)
forbid_source_text(
  "${UNPACKED_IMAGE_SOURCE}"
  "free("
  "unpacked image teardown must remain automatic"
)
forbid_source_text(
  "${IMAGE_FACTORY_SOURCE}"
  "LVUnpackedImgSource * img"
  "unpacked image construction must not use implicit raw ownership"
)
require_source_text(
  "${SCALED_IMAGE_HEADER}"
  "std::vector<int> xmap"
  "scaled-image horizontal maps must use RAII ownership"
)
require_source_text(
  "${SCALED_IMAGE_HEADER}"
  "std::vector<int> ymap"
  "scaled-image vertical maps must use RAII ownership"
)
require_source_text(
  "${SCALED_IMAGE_HEADER}"
  "std::vector<lUInt8> decoded"
  "smooth-scaling decoded snapshots must use RAII ownership"
)
require_source_text(
  "${SCALED_IMAGE_HEADER}"
  "static std::vector<int> GenNinePatchMap"
  "nine-patch map construction must transfer container ownership"
)
require_source_text(
  "${SCALED_IMAGE_SOURCE}"
  "std::unique_ptr<lUInt8, SmoothScaledBufferDeleter> scaled"
  "smooth-scale results must have scoped ownership"
)
require_source_text(
  "${SCALED_IMAGE_SOURCE}"
  "if ( errors || !smoothscale )"
  "failed image decodes must not render partial smooth-scale snapshots"
)
forbid_source_text(
  "${SCALED_IMAGE_HEADER}"
  "int * xmap"
  "scaled-image callback must not own a raw horizontal map"
)
forbid_source_text(
  "${SCALED_IMAGE_HEADER}"
  "int * ymap"
  "scaled-image callback must not own a raw vertical map"
)
forbid_source_text(
  "${SCALED_IMAGE_HEADER}"
  "lUInt8 * decoded"
  "scaled-image callback must not own a raw decoded snapshot"
)
forbid_source_text(
  "${SCALED_IMAGE_SOURCE}"
  "new int["
  "scaled-image maps must remain container-managed"
)
forbid_source_text(
  "${SCALED_IMAGE_SOURCE}"
  "new lUInt8["
  "smooth-scaling snapshots must remain container-managed"
)
forbid_source_text(
  "${SCALED_IMAGE_SOURCE}"
  "delete []"
  "scaled-image teardown must remain automatic"
)
forbid_source_text(
  "${SCALED_IMAGE_SOURCE}"
  "free(sdata)"
  "smooth-scale result cleanup must remain scope-bound"
)

# --- default stream region buffer ownership and rollback ---
require_source_text(
  "${DEFAULT_STREAM_BUFFER_HEADER}"
  "std::vector<lUInt8> m_buf"
  "default stream regions must use RAII buffer ownership"
)
require_source_text(
  "${DEFAULT_STREAM_BUFFER_HEADER}"
  "bool m_ready"
  "default stream regions must distinguish activation from rollback"
)
require_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "std::unique_ptr<LVDefStreamBuffer> buf"
  "default stream-buffer candidates must have explicit ownership"
)
require_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "stream->SetPos(pos)!=pos"
  "default stream-buffer seeks must validate the returned position"
)
require_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "if ( !buf->m_writeonly )"
  "write-only stream buffers must not attempt a read preload"
)
require_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "buf->m_ready = true"
  "default stream buffers must activate only after initialization"
)
require_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "LVStreamBufferRef(buf.release())"
  "default stream-buffer ownership must transfer at the reference boundary"
)
require_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "m_stream->SetPos(m_pos)!=m_pos"
  "default stream-buffer flushes must validate the returned position"
)
forbid_source_text(
  "${DEFAULT_STREAM_BUFFER_HEADER}"
  "lUInt8 * m_buf"
  "default stream buffer must not own raw storage"
)
forbid_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "LVDefStreamBuffer * buf"
  "default stream-buffer factory must not use implicit raw ownership"
)
forbid_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "malloc("
  "default stream-buffer allocation must remain container-managed"
)
forbid_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "free("
  "default stream-buffer teardown must remain automatic"
)
forbid_source_text(
  "${DEFAULT_STREAM_BUFFER_SOURCE}"
  "delete buf"
  "default stream-buffer rollback must remain scope-bound"
)

# --- TCR decoder dictionary, index and decoded buffers ---
require_source_text(
  "${TCR_STREAM_HEADER}"
  "std::vector<lUInt8> str"
  "TCR dictionary entries must use RAII ownership"
)
require_source_text(
  "${TCR_STREAM_HEADER}"
  "std::vector<lUInt32> _index"
  "TCR block index must use RAII ownership"
)
require_source_text(
  "${TCR_STREAM_HEADER}"
  "std::vector<lUInt8> _decoded"
  "TCR decoded block must use RAII ownership"
)
require_source_text(
  "${TCR_STREAM_SOURCE}"
  "_decoded.insert(_decoded.end(), item.str.begin(), item.str.end())"
  "TCR decoded-buffer growth must remain container-managed"
)
require_source_text(
  "${TCR_STREAM_SOURCE}"
  "std::unique_ptr<LVTCRStream> decoder"
  "TCR factory construction must have explicit ownership"
)
require_source_text(
  "${TCR_STREAM_SOURCE}"
  "LVStreamRef(decoder.release())"
  "TCR factory must transfer ownership only at the reference boundary"
)
forbid_source_text(
  "${TCR_STREAM_HEADER}"
  "lUInt32 * _index"
  "TCR decoder must not own a raw block index"
)
forbid_source_text(
  "${TCR_STREAM_HEADER}"
  "lUInt8 * _decoded"
  "TCR decoder must not own a raw decoded buffer"
)
forbid_source_text(
  "${TCR_STREAM_SOURCE}"
  "malloc("
  "TCR decoder allocation must remain container-managed"
)
forbid_source_text(
  "${TCR_STREAM_SOURCE}"
  "cr_realloc("
  "TCR decoded-buffer growth must not regress to manual reallocation"
)
forbid_source_text(
  "${TCR_STREAM_SOURCE}"
  "free("
  "TCR decoder teardown must remain automatic"
)

# --- ZIP decoder and cached stream buffers ---
require_source_text(
  "${ZIP_STREAM_HEADER}"
  "std::vector<lUInt8> m_inbuf"
  "ZIP decoder input buffer must use RAII ownership"
)
require_source_text(
  "${ZIP_STREAM_HEADER}"
  "std::vector<lUInt8> m_outbuf"
  "ZIP decoder output buffer must use RAII ownership"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "LVZipStreamPositionGuard positionGuard"
  "ZIP CRC fallback must restore stream position with a scope guard"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "std::vector<lUInt8> tmp_buff"
  "ZIP CRC scratch buffer must use RAII ownership"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "m_originalCRC != 0 && m_CRC != m_originalCRC"
  "ZIP streams with a missing CRC must allow fallback calculation"
)
forbid_source_text(
  "${ZIP_STREAM_HEADER}"
  "lUInt8 *    m_inbuf"
  "ZIP decoder must not own a raw input buffer"
)
forbid_source_text(
  "${ZIP_STREAM_HEADER}"
  "lUInt8 *    m_outbuf"
  "ZIP decoder must not own a raw output buffer"
)
forbid_source_text(
  "${ZIP_STREAM_SOURCE}"
  "malloc(ARC_OUTBUF_SIZE)"
  "ZIP CRC scratch allocation must not regress to malloc"
)
require_source_text(
  "${CACHED_STREAM_HEADER}"
  "std::vector<std::unique_ptr<BufItem>> m_buf"
  "cached stream slots must use RAII ownership"
)
require_source_text(
  "${CACHED_STREAM_SOURCE}"
  "std::unique_ptr<BufItem> itemOwner"
  "cached stream item creation/reuse must transfer explicit ownership"
)
require_source_text(
  "${CACHED_STREAM_SOURCE}"
  "std::vector<char> flags"
  "cached stream read flags must use RAII ownership"
)
forbid_source_text(
  "${CACHED_STREAM_HEADER}"
  "BufItem * * m_buf"
  "cached stream must not own a raw pointer array"
)
forbid_source_text(
  "${CACHED_STREAM_SOURCE}"
  "new BufItem*"
  "cached stream slot allocation must not regress to new[]"
)
forbid_source_text(
  "${CACHED_STREAM_SOURCE}"
  "delete[] flags"
  "cached stream scratch cleanup must remain automatic"
)

# --- lvstring: string literal interning tables ---
require_source_text(
  "${LVSTRING_SOURCE}"
  "static std::mutex cs8_mutex"
  "string8 interning table must be synchronized"
)
require_source_text(
  "${LVSTRING_SOURCE}"
  "static std::mutex cs32_mutex"
  "string32 interning table must be synchronized"
)
require_source_text(
  "${LVSTRING_SOURCE}"
  "std::lock_guard<std::mutex> guard(cs8_mutex)"
  "cs8 interning access must hold the mutex"
)
require_source_text(
  "${LVSTRING_SOURCE}"
  "std::lock_guard<std::mutex> guard(cs32_mutex)"
  "cs32 interning access must hold the mutex"
)

# --- lvtinydom: per-document first-body flag ---
require_source_text(
  "${DOM_HEADER}"
  "bool _firstBodyPending"
  "first-body flag must be per-document"
)
require_source_text(
  "${DOM_SOURCE}"
  "_document->isFirstBodyPending()"
  "first-body flag must be accessed through the document"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "static bool IS_FIRST_BODY"
  "first-body flag must not be file-scope static"
)
