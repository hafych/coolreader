if(NOT DEFINED SOURCE_ROOT)
  message(FATAL_ERROR "SOURCE_ROOT is required")
endif()

file(READ "${SOURCE_ROOT}/crengine/src/lvtextfm.cpp" FORMATTER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvtextfm_internal.h" FORMATTER_INTERNAL_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvtextfm.h" FORMATTER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvrend.cpp" RENDER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvbmpbuf.cpp" BITMAP_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/crskin.cpp" SKIN_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/crgui.h" GUI_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/crgui.cpp" GUI_SOURCE)
file(READ "${SOURCE_ROOT}/cr3gui/src/cr3qt.cpp" QT_GUI_SOURCE)
file(READ "${SOURCE_ROOT}/cr3gui/src/cr3xcb.cpp" XCB_GUI_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvtinydom.cpp" DOM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvtinydom.h" DOM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvopc.h" OPC_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvopc.cpp" OPC_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/fb3fmt.h" FB3_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/fb3fmt.cpp" FB3_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/chmfmt.cpp" CHM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/chmfmt_internal.h" CHM_INTERNAL_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/epubfmt.cpp" EPUB_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/tests/epub3_regression_test.cpp" EPUB3_REGRESSION_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/odtfmt.cpp" ODT_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstsheet.cpp" STYLESHEET_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvstsheet.h" STYLESHEET_HEADER)
file(READ "${SOURCE_ROOT}/crengine/tests/css_regression_test.cpp" CSS_REGRESSION_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/wolutil.cpp" WOL_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/wolutil.h" WOL_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/cri18n.h" I18N_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/cri18n.cpp" I18N_SOURCE)
file(READ "${SOURCE_ROOT}/cr3gui/src/cr3jinke.cpp" JINKE_SOURCE)
file(READ "${SOURCE_ROOT}/cr3gui/src/cr3nanox.cpp" NANOX_SOURCE)
file(READ "${SOURCE_ROOT}/cr3gui/src/cr3pocketbook.cpp" POCKETBOOK_SOURCE)
file(READ "${SOURCE_ROOT}/cr3gui/src/cr3win.cpp" WIN_GUI_SOURCE)
string(CONCAT GUI_PLATFORM_OWNERSHIP_SOURCE
  "${QT_GUI_SOURCE}"
  "${WIN_GUI_SOURCE}"
  "${JINKE_SOURCE}"
  "${XCB_GUI_SOURCE}"
  "${POCKETBOOK_SOURCE}"
  "${NANOX_SOURCE}"
)
file(READ "${SOURCE_ROOT}/crengine/include/lvrend.h" RENDER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/hyphman.cpp" HYPH_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/hyphman.h" HYPH_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/textlang.cpp" TEXTLANG_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/textlang.h" TEXTLANG_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfntman.cpp" FONT_MANAGER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvfntman.h" FONT_MANAGER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfreetypeface.cpp" FREETYPE_FACE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfontcache.h" FONT_CACHE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfontcache.cpp" FONT_CACHE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfreetypefontman.cpp" FREETYPE_FONT_MANAGER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvembeddedfont.h" EMBEDDED_FONT_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvembeddedfont.cpp" EMBEDDED_FONT_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfontglyphcache.h" GLYPH_CACHE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvfont/lvfontglyphcache.cpp" GLYPH_CACHE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvdocview.h" DOC_VIEW_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvdocview.cpp" DOC_VIEW_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/tests/document_regression_test.cpp" DOCUMENT_REGRESSION_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/hist.h" HISTORY_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/hist.cpp" HISTORY_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/props.cpp" PROPERTIES_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstring.cpp" LVSTRING_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvstring8collection.h" STRING8_COLLECTION_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstring8collection.cpp" STRING8_COLLECTION_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvstring32collection.h" STRING32_COLLECTION_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstring32collection.cpp" STRING32_COLLECTION_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvstring32hashedcollection.h" HASHED_STRING_COLLECTION_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstring32hashedcollection.cpp" HASHED_STRING_COLLECTION_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lstridmap.h" NAME_ID_MAP_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lstridmap.cpp" NAME_ID_MAP_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvhashtable.h" HASH_TABLE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvarray.h" VALUE_ARRAY_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvref.h" REF_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvptrvec.h" PTR_VECTOR_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvpagesplitter.h" PAGE_SPLITTER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvrefcache.h" REF_CACHE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvqueue.h" QUEUE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/wordfmt.cpp" WORD_FORMAT_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/pdbfmt.cpp" PDB_FORMAT_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/pdbfmt_internal.h" PDB_FORMAT_INTERNAL_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/crconcurrent.h" CONCURRENCY_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/crconcurrent.cpp" CONCURRENCY_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/crlocks.h" LOCKS_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/crlog.h" LOG_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/crlog.cpp" LOG_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/crtxtenc.cpp" TEXT_ENCODING_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/crtxtenc_internal.h" TEXT_ENCODING_INTERNAL_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvtextparser.cpp" TEXT_PARSER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvtextlinequeue.h" TEXT_LINE_QUEUE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvxml/lvtextlinequeue.cpp" TEXT_LINE_QUEUE_SOURCE)
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
file(READ "${SOURCE_ROOT}/crengine/include/lvimagesource.h" IMAGE_SOURCE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvimagesource.cpp" IMAGE_SOURCE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvcolortransformimgsource.h" COLOR_TRANSFORM_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvcolortransformimgsource.cpp" COLOR_TRANSFORM_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifimagesource.cpp" GIF_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifimagesource.h" GIF_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifframe.cpp" GIF_FRAME_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvgifframe.h" GIF_FRAME_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvpngimagesource.cpp" PNG_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvpngimagesource.h" PNG_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvjpegimagesource.cpp" JPEG_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvjpegimagesource.h" JPEG_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvdrawbufimgsource.cpp" DRAWBUF_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvdrawbufimgsource.h" DRAWBUF_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvdummyimagesource.h" DUMMY_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvxpmimagesource.cpp" XPM_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvxpmimagesource.h" XPM_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvunpackedimgsource.cpp" UNPACKED_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvimg/lvunpackedimgsource.h" UNPACKED_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvdrawbuf/lvimagescaleddrawcallback.cpp" SCALED_IMAGE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvdrawbuf/lvimagescaleddrawcallback.h" SCALED_IMAGE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvdrawbuf/lvcolordrawbuf.cpp" COLOR_DRAW_BUFFER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvcolordrawbuf.h" COLOR_DRAW_BUFFER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvdrawbuf/lvgraydrawbuf.cpp" GRAY_DRAW_BUFFER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/lvgraydrawbuf.h" GRAY_DRAW_BUFFER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/tests/core_safety_test.cpp" CORE_SAFETY_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/tests/thread_safety_test.cpp" THREAD_SAFETY_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvzipdecodestream.cpp" ZIP_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvzipdecodestream.h" ZIP_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvcachedstream.cpp" CACHED_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvcachedstream.h" CACHED_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvtcrstream.cpp" TCR_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvtcrstream.h" TCR_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/include/lvstreamfragment.h" STREAM_FRAGMENT_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvdefstreambuffer.cpp" DEFAULT_STREAM_BUFFER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvdefstreambuffer.h" DEFAULT_STREAM_BUFFER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvblockwritestream.cpp" BLOCK_WRITE_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvblockwritestream.h" BLOCK_WRITE_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvfilemappedstream.cpp" FILE_MAPPED_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvfilemappedstream.h" FILE_MAPPED_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvfilestream.cpp" FILE_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvfilestream.h" FILE_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvdirectorycontainer.cpp" DIRECTORY_CONTAINER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvdirectorycontainer.h" DIRECTORY_CONTAINER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvarccontainerbase.h" ARCHIVE_CONTAINER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvziparc.cpp" ZIP_ARCHIVE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvziparc.h" ZIP_ARCHIVE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvrararc.cpp" RAR_ARCHIVE_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvrararc.h" RAR_ARCHIVE_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/ziphdr.h" ZIP_HEADER_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvmemorystream.cpp" MEMORY_STREAM_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvmemorystream.h" MEMORY_STREAM_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/lvstream/lvstreamutils.cpp" STREAM_UTILS_SOURCE)
file(READ "${SOURCE_ROOT}/crengine/include/serialbuf.h" SERIAL_BUFFER_HEADER)
file(READ "${SOURCE_ROOT}/crengine/src/serialbuf.cpp" SERIAL_BUFFER_SOURCE)

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
  "${FORMATTER_SOURCE}"
  "BufferStorageStaticBorrowed"
  "formatter static scratch storage must use an explicit borrowed state"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "std::vector<lChar32> m_ownedText"
  "formatter dynamic scratch text must use scoped storage"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "~LVFormatter()"
  "formatter teardown must release its workspace on every exit path"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "releaseWorkspace();"
  "formatter teardown must release its workspace on every exit path"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "LVFormatter(const LVFormatter &) = delete"
  "formatter workspace views must not be shallow-copied"
)
require_source_text(
  "${FORMATTER_INTERNAL_HEADER}"
  "bool LVRunFormatterWorkspaceOwnershipRegression()"
  "formatter workspace ownership must retain native regression coverage"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "class FormattedTextOwner : public formatted_text_fragment_t"
  "formatted text ownership must extend the stable C view without changing it"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "std::vector<std::unique_ptr<FormattedLineOwner> > m_lines"
  "formatted lines must have scoped owners"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "std::vector<std::unique_ptr<EmbeddedFloatOwner> > m_floats"
  "embedded floats must have scoped owners"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "std::vector<std::unique_ptr<lChar32[]> > m_sourceTexts"
  "owned source text must have scoped owners"
)
require_source_text(
  "${FORMATTER_SOURCE}"
  "m_links.reset(new lString32Collection())"
  "embedded float links must cross into a scoped owner"
)
require_source_text(
  "${FORMATTER_HEADER}"
  "BufferOwner m_pbuffer"
  "the C++ formatted text wrapper must own its C view explicitly"
)
require_source_text(
  "${FORMATTER_HEADER}"
  "LFormattedText(const LFormattedText &) = delete"
  "formatted text wrappers must not shallow-copy ownership"
)
require_source_text(
  "${FORMATTER_INTERNAL_HEADER}"
  "bool LVRunFormattedTextOwnershipRegression()"
  "formatted text graph ownership must retain native regression coverage"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "malloc("
  "formatter graph storage must not regress to manual allocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "calloc("
  "formatter graph storage must not regress to manual allocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "cr_realloc("
  "formatter graph storage must not regress to manual reallocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "free("
  "formatter graph teardown must remain scope-bound"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "delete "
  "formatter graph teardown must remain scope-bound"
)
forbid_source_text(
  "${FORMATTER_HEADER}"
  "formatted_text_fragment_t * m_pbuffer;"
  "the C++ formatter wrapper must not own its buffer through a raw pointer"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "m_text = cr_realloc"
  "formatter scratch text must not regress to manual reallocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "m_flags = cr_realloc"
  "formatter scratch flags must not regress to manual reallocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "m_srcs = cr_realloc"
  "formatter scratch source views must not regress to manual reallocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "m_charindex = cr_realloc"
  "formatter scratch indexes must not regress to manual reallocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "m_widths = cr_realloc"
  "formatter scratch widths must not regress to manual reallocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "m_bidi_ctypes = cr_realloc"
  "formatter bidi scratch types must not regress to manual reallocation"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "free( m_text )"
  "formatter scratch teardown must remain automatic"
)
forbid_source_text(
  "${FORMATTER_SOURCE}"
  "free( m_bidi_ctypes )"
  "formatter bidi scratch teardown must remain automatic"
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
  "${FONT_CACHE_HEADER}"
  "typedef std::vector<std::unique_ptr<LVFontCacheItem> >"
  "font-cache entries must have explicit scoped owners"
)
require_source_text(
  "${FONT_CACHE_HEADER}"
  "const LVFontCacheItemList &getInstances() const"
  "font-cache instances must expose only a borrowed collection view"
)
require_source_text(
  "${FONT_CACHE_HEADER}"
  "LVFontCache(const LVFontCache &) = delete"
  "font-cache ownership must not be shallow-copied"
)
require_source_text(
  "${FONT_CACHE_SOURCE}"
  "std::unique_ptr<LVFontCacheItem> item("
  "font-cache instance candidates must enter an owner immediately"
)
require_source_text(
  "${FONT_CACHE_SOURCE}"
  "_instance_list.push_back(std::move(item));"
  "font-cache instance publication must transfer scoped ownership"
)
require_source_text(
  "${FONT_CACHE_SOURCE}"
  "std::remove_if(_instance_list.begin(),"
  "font-cache instance removal must destroy every matching owner"
)
require_source_text(
  "${FONT_CACHE_SOURCE}"
  "std::remove_if(_registered_list.begin(),"
  "font-cache registration removal must destroy every matching owner"
)
require_source_text(
  "${FREETYPE_FONT_MANAGER_SOURCE}"
  "const LVFontCacheItemList &fonts = _cache.getInstances();"
  "FreeType settings updates must borrow the font-cache instance view"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testFontCacheOwnership()"
  "font-cache ownership must retain native lifecycle regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "font cache document removal retained owned entries"
  "font-cache regression must retain document-owner removal coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "font cache typeface removal skipped adjacent owners"
  "font-cache regression must retain adjacent-owner removal coverage"
)
require_source_text(
  "${EMBEDDED_FONT_HEADER}"
  "std::vector<std::unique_ptr<LVEmbeddedFontDef> > _items"
  "embedded-font definitions must have explicit scoped owners"
)
require_source_text(
  "${EMBEDDED_FONT_HEADER}"
  "void addOwned(std::unique_ptr<LVEmbeddedFontDef> def)"
  "embedded-font publication must accept scoped ownership"
)
require_source_text(
  "${EMBEDDED_FONT_HEADER}"
  "LVEmbeddedFontList(const LVEmbeddedFontList &list)"
  "embedded-font list copies must retain deep-copy semantics"
)
require_source_text(
  "${EMBEDDED_FONT_HEADER}"
  "LVEmbeddedFontList &operator=(const LVEmbeddedFontList &) = delete"
  "embedded-font ownership must not use implicit assignment"
)
require_source_text(
  "${EMBEDDED_FONT_SOURCE}"
  "std::unique_ptr<LVEmbeddedFontDef> candidate("
  "embedded-font definitions must enter scoped ownership before publication"
)
require_source_text(
  "${EMBEDDED_FONT_SOURCE}"
  "LVEmbeddedFontList replacement;
    replacement.addAll(list);"
  "embedded-font list replacement must be transactional"
)
require_source_text(
  "${EMBEDDED_FONT_SOURCE}"
  "LVEmbeddedFontList parsed;"
  "embedded-font deserialization must stage a complete candidate list"
)
require_source_text(
  "${EMBEDDED_FONT_SOURCE}"
  "_items.reserve(_items.size() + parsed._items.size());"
  "embedded-font deserialization must reserve before atomic publication"
)
require_source_text(
  "${DOM_SOURCE}"
  "const LVEmbeddedFontDef *item = _fontList.get(i);"
  "embedded-font registration must consume a borrowed definition view"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testEmbeddedFontOwnership()"
  "embedded-font ownership must retain native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "embedded font owners were not deep-copied"
  "embedded-font regression must retain independent-copy coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "embedded font deserialize published a partial list"
  "embedded-font regression must retain failed-parse rollback coverage"
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
forbid_source_text(
  "${FONT_CACHE_HEADER}"
  "LVPtrVector<LVFontCacheItem> _registered_list"
  "font-cache registrations must not regress to implicit raw ownership"
)
forbid_source_text(
  "${FONT_CACHE_HEADER}"
  "LVPtrVector<LVFontCacheItem> _instance_list"
  "font-cache instances must not regress to implicit raw ownership"
)
forbid_source_text(
  "${FONT_CACHE_HEADER}"
  "LVPtrVector<LVFontCacheItem> *getInstances()"
  "font-cache must not expose its mutable owner container"
)
forbid_source_text(
  "${FONT_CACHE_SOURCE}"
  "LVFontCacheItem *item = new"
  "font-cache candidates must not use raw transitional ownership"
)
forbid_source_text(
  "${FONT_CACHE_SOURCE}"
  "delete _instance_list.remove"
  "font-cache instance teardown must remain automatic"
)
forbid_source_text(
  "${FONT_CACHE_SOURCE}"
  "delete _registered_list.remove"
  "font-cache registration teardown must remain automatic"
)
forbid_source_text(
  "${FONT_CACHE_SOURCE}"
  "_instance_list.remove(i)"
  "font-cache instance removal must not leak or skip adjacent owners"
)
forbid_source_text(
  "${FONT_CACHE_SOURCE}"
  "_registered_list.remove(i)"
  "font-cache registration removal must not leak or skip adjacent owners"
)
forbid_source_text(
  "${EMBEDDED_FONT_HEADER}"
  "class LVEmbeddedFontList : public LVPtrVector<LVEmbeddedFontDef>"
  "embedded-font definitions must not regress to implicit pointer-vector ownership"
)
forbid_source_text(
  "${EMBEDDED_FONT_SOURCE}"
  "def = new LVEmbeddedFontDef"
  "embedded-font candidates must not use raw transitional ownership"
)
forbid_source_text(
  "${EMBEDDED_FONT_SOURCE}"
  "LVEmbeddedFontDef *item = new"
  "embedded-font parse candidates must not use raw transitional ownership"
)
forbid_source_text(
  "${EMBEDDED_FONT_SOURCE}"
  "delete item"
  "embedded-font candidate teardown must remain automatic"
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

# --- Word/PDB transient import buffers and factory candidates ---
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "std::vector<lUInt8> image(len)"
  "Word image blobs must use operation-scoped storage"
)
require_source_text(
  "${WORD_FORMAT_SOURCE}"
  "context.writer->OnBlob(name, image.data(), len)"
  "Word image callbacks must receive a view of scoped storage"
)
require_source_text(
  "${PDB_FORMAT_INTERNAL_HEADER}"
  "std::vector<lUInt8> &uncompressed"
  "PDB inflate output must publish through RAII storage"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "PDBInflateGuard streamGuard(&stream)"
  "PDB zlib state must use scoped teardown"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "std::vector<lUInt8> candidate"
  "PDB inflate must build candidate output before publication"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "uncompressed.swap(candidate)"
  "PDB inflate must publish only complete output"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "std::vector<unsigned char> buf(sz)"
  "PDB encoding detection must use scoped scratch storage"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "std::unique_ptr<PDBFile> pdbOwner"
  "PDB stream candidates must start with explicit ownership"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "std::unique_ptr<LVPDBContainer> containerOwner"
  "PDB container candidates must start with explicit ownership"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "LVStreamRef(pdbOwner.release())"
  "PDB stream ownership must transfer only at the reference boundary"
)
require_source_text(
  "${PDB_FORMAT_SOURCE}"
  "LVContainerRef(containerOwner.release())"
  "PDB container ownership must transfer only at the reference boundary"
)
forbid_source_text(
  "${WORD_FORMAT_SOURCE}"
  "lUInt8 *pucJpeg"
  "Word image blobs must not use raw owning pointers"
)
forbid_source_text(
  "${WORD_FORMAT_SOURCE}"
  "malloc(len)"
  "Word image blob allocation must remain container-managed"
)
forbid_source_text(
  "${WORD_FORMAT_SOURCE}"
  "free(pucJpeg)"
  "Word image blob teardown must remain automatic"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "cr_realloc(uncompressed_buf"
  "PDB inflate growth must not use realloc"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "free(uncompressed_buf)"
  "PDB inflate failure must not require manual cleanup"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "unsigned char * buf = new unsigned char"
  "PDB detection scratch storage must not use raw arrays"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "delete[] buf"
  "PDB detection scratch teardown must remain automatic"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "PDBFile * pdb = new PDBFile"
  "PDB factories must not start with implicit raw ownership"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "LVPDBContainer * container = new LVPDBContainer"
  "PDB container factories must not start with implicit raw ownership"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "delete pdb"
  "PDB factory failure must not require manual stream deletion"
)
forbid_source_text(
  "${PDB_FORMAT_SOURCE}"
  "delete container"
  "PDB factory failure must not require manual container deletion"
)

# --- process logger: scoped ownership and synchronized publication/dispatch ---
require_source_text(
  "${LOG_SOURCE}"
  "std::unique_ptr<CRLog> &loggerOwner()"
  "the process logger must have an explicit ownership boundary"
)
require_source_text(
  "${LOG_SOURCE}"
  "static std::unique_ptr<CRLog> owner"
  "the process logger owner must have function-local lifetime"
)
require_source_text(
  "${LOG_SOURCE}"
  "std::recursive_mutex &loggerMutex()"
  "logger lifecycle and dispatch must share a recursive mutex"
)
require_source_text(
  "${LOG_SOURCE}"
  "static std::recursive_mutex mutex"
  "the logger mutex must have function-local lifetime"
)
require_source_text(
  "${LOG_SOURCE}"
  "if (CRLOG == logger)"
  "publishing the active logger again must remain idempotent"
)
require_source_text(
  "${LOG_SOURCE}"
  "std::unique_ptr<CRLog> replacement(logger)"
  "logger replacement must adopt the candidate before teardown"
)
require_source_text(
  "${LOG_SOURCE}"
  "owner.reset();"
  "logger replacement must release the previous owner under the lifecycle lock"
)
require_source_text(
  "${LOG_SOURCE}"
  "CRLOG = owner.get();"
  "the legacy logger pointer must remain a borrowed owner view"
)
require_source_text(
  "${LOG_SOURCE}"
  "std::lock_guard<std::recursive_mutex> guard(loggerMutex())"
  "logger access must remain synchronized"
)
require_source_text(
  "${LOG_HEADER}"
  "replaces logger instance, taking ownership of logger"
  "the logger compatibility API must document ownership transfer"
)
require_source_text(
  "${LOG_HEADER}"
  "non-owning compatibility view of the scoped process logger"
  "the legacy logger pointer must document its borrowed status"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "static int testLoggerOwnershipAndConcurrency()"
  "logger ownership must retain native lifecycle regression coverage"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "logger dispatch was not serialized across threads"
  "logger regression must retain concurrent dispatch coverage"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "idempotent logger publication destroyed its owner"
  "logger regression must retain idempotent publication coverage"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "logger clear did not release exactly one owner"
  "logger regression must retain exact teardown coverage"
)
forbid_source_text(
  "${LOG_SOURCE}"
  "delete CRLOG"
  "the process logger must not regress to manual owner deletion"
)
forbid_source_text(
  "${LOG_SOURCE}"
  "CRLOG = logger;"
  "logger publication must not bypass scoped ownership"
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

# --- generic queue nodes and thread-executor task lifecycle ---
require_source_text(
  "${QUEUE_HEADER}"
  "typedef std::list<T> Storage"
  "generic queue nodes must remain standard-container owned"
)
require_source_text(
  "${QUEUE_HEADER}"
  "LVQueue(const LVQueue &) = delete"
  "generic queue ownership must not be duplicated"
)
require_source_text(
  "${QUEUE_HEADER}"
  "items.push_back(std::move(item))"
  "generic queue insertion must support move-only ownership"
)
require_source_text(
  "${QUEUE_HEADER}"
  "T res = std::move(items.front())"
  "generic queue removal must transfer move-only values"
)
forbid_source_text(
  "${QUEUE_HEADER}"
  "struct Item"
  "generic queue nodes must not regress to a custom raw graph"
)
forbid_source_text(
  "${QUEUE_HEADER}"
  "Item * head"
  "generic queue head ownership must not use a raw pointer"
)
forbid_source_text(
  "${QUEUE_HEADER}"
  "new Item"
  "generic queue insertion must not manually allocate nodes"
)
forbid_source_text(
  "${QUEUE_HEADER}"
  "delete p"
  "generic queue teardown must remain automatic"
)
require_source_text(
  "${CONCURRENCY_HEADER}"
  "std::atomic<bool> _stopped"
  "thread-executor stop state must remain race-free"
)
require_source_text(
  "${CONCURRENCY_HEADER}"
  "std::unique_ptr<CRMonitor> _monitor"
  "thread-executor monitor lifetime must remain explicit"
)
require_source_text(
  "${CONCURRENCY_HEADER}"
  "std::unique_ptr<CRThread> _thread"
  "thread-executor worker lifetime must remain explicit"
)
require_source_text(
  "${CONCURRENCY_HEADER}"
  "LVQueue<std::unique_ptr<CRRunnable> > _queue"
  "thread-executor queued tasks must remain uniquely owned"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "std::unique_ptr<CRRunnable> ownedTask(task)"
  "thread-executor raw API boundary must adopt tasks immediately"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "_queue.pushBack(std::move(ownedTask))"
  "thread-executor enqueue must transfer task ownership"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "std::unique_ptr<CRRunnable> task"
  "thread-executor running tasks must remain scope-owned"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "_queue.clear()"
  "thread-executor stop must release queued tasks automatically"
)
require_source_text(
  "${CONCURRENCY_SOURCE}"
  "_stopped.exchange(true, std::memory_order_acq_rel)"
  "thread-executor stop must remain idempotent"
)
require_source_text(
  "${LOCKS_HEADER}"
  "Transfers task ownership to the executor, including rejected tasks."
  "executor raw compatibility API must document ownership transfer"
)
forbid_source_text(
  "${CONCURRENCY_HEADER}"
  "volatile bool _stopped"
  "thread-executor stop state must not use volatile synchronization"
)
forbid_source_text(
  "${CONCURRENCY_HEADER}"
  "CRMonitorRef _monitor"
  "thread-executor monitor must not use the legacy auto pointer"
)
forbid_source_text(
  "${CONCURRENCY_HEADER}"
  "CRThreadRef _thread"
  "thread-executor worker must not use the legacy auto pointer"
)
forbid_source_text(
  "${CONCURRENCY_HEADER}"
  "LVQueue<CRRunnable *> _queue"
  "thread-executor queue must not contain unowned task pointers"
)
forbid_source_text(
  "${CONCURRENCY_SOURCE}"
  "delete task"
  "thread-executor running task teardown must remain automatic"
)
forbid_source_text(
  "${CONCURRENCY_SOURCE}"
  "delete p"
  "thread-executor queued task teardown must remain automatic"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "static int testQueueOwnership()"
  "generic queue ownership must retain lifecycle regression coverage"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "static int testThreadExecutorOwnership()"
  "thread-executor ownership must retain lifecycle regression coverage"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "thread executor stop retained its queued task"
  "thread-executor regression must retain queued-stop cleanup coverage"
)
require_source_text(
  "${THREAD_SAFETY_SOURCE}"
  "thread executor task ownership is inconsistent"
  "thread-executor regression must retain rejected-task cleanup coverage"
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

# --- encoding double-character statistic storage and reuse ---
require_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "std::array<std::unique_ptr<lUInt16[]>, 256> stats"
  "encoding double-character rows must have scoped sparse ownership"
)
require_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "CDoubleCharStat(const CDoubleCharStat &) = delete"
  "encoding statistic ownership must not be shallow-copied"
)
require_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "std::vector<dbl_char_stat_long_t> data(items)"
  "encoding statistic output must use scoped contiguous storage"
)
require_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "for (std::unique_ptr<lUInt16[]> &row : stats)\n            row.reset();"
  "encoding statistic reset must release every sparse row"
)
require_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "total = 0;\n        items = 0;"
  "encoding statistic reset must restore all reusable state"
)
require_source_text(
  "${TEXT_ENCODING_INTERNAL_HEADER}"
  "bool LVRunDoubleCharStatOwnershipRegression()"
  "encoding statistic ownership must expose its native regression seam"
)
require_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "bool LVRunDoubleCharStatOwnershipRegression()"
  "encoding statistic ownership must retain native lifecycle coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "double-character statistic ownership regression failed"
  "encoding statistic regression must remain wired into the native suite"
)
forbid_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "CDblCharNode"
  "unused manual encoding-statistic tree ownership must not return"
)
forbid_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "lUInt16 * * stats"
  "encoding statistic rows must not regress to a raw pointer table"
)
forbid_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "new lUInt16* [256]"
  "encoding statistic row slots must not use manual array ownership"
)
forbid_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "new dbl_char_stat_long_t[items]"
  "encoding statistic output must not use a manual candidate array"
)
forbid_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "delete[] stats"
  "encoding statistic row teardown must remain automatic"
)
forbid_source_text(
  "${TEXT_ENCODING_SOURCE}"
  "delete[] pdata"
  "encoding statistic output teardown must remain automatic"
)

# --- plain-text line queue owners and parser borrow ---
require_source_text(
  "${TEXT_LINE_QUEUE_HEADER}"
  "std::vector<std::unique_ptr<LVTextFileLine> > lines"
  "plain-text decoded lines must have explicit scoped owners"
)
require_source_text(
  "${TEXT_LINE_QUEUE_HEADER}"
  "LVTextFileBase &file;"
  "plain-text line queues must retain a non-null borrowed parser"
)
require_source_text(
  "${TEXT_LINE_QUEUE_HEADER}"
  "LVTextLineQueue(const LVTextLineQueue &) = delete"
  "plain-text line ownership must not be shallow-copied"
)
require_source_text(
  "${TEXT_LINE_QUEUE_HEADER}"
  "return get(index - first_line_index);"
  "plain-text line lookup must expose only a borrowed item view"
)
require_source_text(
  "${TEXT_LINE_QUEUE_SOURCE}"
  "std::unique_ptr<LVTextFileLine> line("
  "plain-text decoded candidates must enter scoped ownership immediately"
)
require_source_text(
  "${TEXT_LINE_QUEUE_SOURCE}"
  "lines.push_back(std::move(line));"
  "plain-text line publication must transfer scoped ownership"
)
require_source_text(
  "${TEXT_LINE_QUEUE_SOURCE}"
  "lines.erase(lines.begin(), lines.begin() + lineCount);"
  "plain-text head removal must destroy every removed owner"
)
require_source_text(
  "${TEXT_PARSER_SOURCE}"
  "LVTextLineQueue queue(*this, 2000);"
  "plain-text parsers must pass an explicit borrowed reference"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testTextLineQueueOwnership()"
  "plain-text line ownership must retain native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "text line queue head removal lost borrowed views"
  "plain-text queue regression must retain owner-removal coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "text parser rejected the RAII line queue"
  "plain-text regression must retain multi-batch parser coverage"
)
forbid_source_text(
  "${TEXT_LINE_QUEUE_HEADER}"
  "class LVTextLineQueue : public LVPtrVector<LVTextFileLine>"
  "plain-text line ownership must not regress to pointer-vector inheritance"
)
forbid_source_text(
  "${TEXT_LINE_QUEUE_HEADER}"
  "LVTextFileBase * file"
  "plain-text parser linkage must not regress to a nullable pointer"
)
forbid_source_text(
  "${TEXT_LINE_QUEUE_SOURCE}"
  "LVTextFileLine * line = new"
  "plain-text decoded candidates must not use raw transitional ownership"
)
forbid_source_text(
  "${TEXT_LINE_QUEUE_SOURCE}"
  "add( line )"
  "plain-text line publication must remain owner-aware"
)
forbid_source_text(
  "${TEXT_LINE_QUEUE_SOURCE}"
  "erase(0, lineCount)"
  "plain-text line teardown must remain container-managed"
)
forbid_source_text(
  "${TEXT_PARSER_SOURCE}"
  "LVTextLineQueue queue( this, 2000 )"
  "plain-text parsers must not pass a nullable queue source"
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

# --- PNG decoder: longjmp-safe row and pixel ownership ---
require_source_text(
  "${PNG_IMAGE_HEADER}"
  "std::vector<lUInt8> _decodePixels"
  "PNG decoded pixels must have longjmp-safe member ownership"
)
require_source_text(
  "${PNG_IMAGE_HEADER}"
  "std::vector<lUInt8 *> _decodeRows"
  "PNG row views must have longjmp-safe member ownership"
)
require_source_text(
  "${PNG_IMAGE_HEADER}"
  "bool _decodeStarted"
  "PNG callback teardown must track a completed start"
)
require_source_text(
  "${PNG_IMAGE_SOURCE}"
  "png_read_image(png_ptr, _decodeRows.data())"
  "libpng must decode into RAII-owned row storage"
)
require_source_text(
  "${PNG_IMAGE_SOURCE}"
  "if (callback && _decodeStarted)"
  "PNG error teardown must follow a started callback lifecycle"
)
require_source_text(
  "${PNG_IMAGE_SOURCE}"
  "clearDecodeBuffers();"
  "PNG decode buffers must be released on every lifecycle"
)
forbid_source_text(
  "${PNG_IMAGE_SOURCE}"
  "png_bytep *image"
  "PNG decoder must not own a raw packed row allocation"
)
forbid_source_text(
  "${PNG_IMAGE_SOURCE}"
  "malloc("
  "PNG row and pixel allocation must remain container-managed"
)
forbid_source_text(
  "${PNG_IMAGE_SOURCE}"
  "free(image)"
  "PNG error cleanup must remain automatic"
)

# --- JPEG decoder: libjpeg-pool ownership across longjmp ---
require_source_text(
  "${JPEG_IMAGE_HEADER}"
  "bool _decompressCreated"
  "JPEG decompressor teardown must track construction"
)
require_source_text(
  "${JPEG_IMAGE_HEADER}"
  "bool _decodeStarted"
  "JPEG callback teardown must track a completed start"
)
require_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "JPOOL_PERMANENT"
  "JPEG source manager and input bytes must use the libjpeg permanent pool"
)
require_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "alloc_sarray"
  "JPEG scanline storage must use the libjpeg image pool"
)
require_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "alloc_large"
  "JPEG converted rows must use the libjpeg image pool"
)
require_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "jpeg_finish_decompress"
  "JPEG success teardown must finish decompression"
)
require_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "if (callback && _decodeStarted)"
  "JPEG error teardown must follow a started callback lifecycle"
)
string(FIND "${JPEG_IMAGE_SOURCE}"
  "if (setjmp(jerr.setjmp_buffer))" JPEG_SETJMP_POSITION)
string(FIND "${JPEG_IMAGE_SOURCE}" "jpeg_create_decompress" JPEG_CREATE_POSITION)
if(JPEG_SETJMP_POSITION EQUAL -1
    OR JPEG_CREATE_POSITION EQUAL -1
    OR JPEG_SETJMP_POSITION GREATER JPEG_CREATE_POSITION)
  message(FATAL_ERROR
    "JPEG error boundary must be installed before decompressor creation")
endif()
forbid_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "new cr_jpeg_source_mgr"
  "JPEG source-manager ownership must remain pool-managed"
)
forbid_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "new JOCTET"
  "JPEG input bytes must remain pool-managed"
)
forbid_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "new lUInt8"
  "JPEG scanline storage must remain pool-managed"
)
forbid_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "new lUInt32"
  "JPEG converted rows must remain pool-managed"
)
forbid_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "delete[]"
  "JPEG decoder teardown must remain pool-managed"
)
forbid_source_text(
  "${JPEG_IMAGE_SOURCE}"
  "cr_jpeg_src_free"
  "JPEG source-manager teardown must remain part of jpeg_destroy_decompress"
)

# --- synthetic/draw-buffer/XPM image sources ---
require_source_text(
  "${IMAGE_SOURCE_HEADER}"
  "std::unique_ptr<CR9PatchInfo> _ninePatch"
  "nine-patch metadata cache must have scoped ownership"
)
require_source_text(
  "${IMAGE_SOURCE_HEADER}"
  "LVImageSource(const LVImageSource &) = delete"
  "image-source cache ownership must not be shallow-copied"
)
require_source_text(
  "${IMAGE_SOURCE_SOURCE}"
  "std::unique_ptr<CR9PatchInfo> candidate"
  "nine-patch detection must stage candidate metadata"
)
require_source_text(
  "${IMAGE_SOURCE_SOURCE}"
  "if (!Decode(&decoder))"
  "failed nine-patch decodes must not publish partial metadata"
)
forbid_source_text(
  "${IMAGE_SOURCE_SOURCE}"
  "delete _ninePatch"
  "nine-patch metadata teardown must remain automatic"
)
forbid_source_text(
  "${IMAGE_SOURCE_SOURCE}"
  "_ninePatch = new"
  "nine-patch metadata must not regress to raw ownership"
)
require_source_text(
  "${COLOR_TRANSFORM_IMAGE_HEADER}"
  "std::unique_ptr<LVColorDrawBuf> _drawbuf"
  "color-transform workspace must have scoped ownership"
)
require_source_text(
  "${COLOR_TRANSFORM_IMAGE_HEADER}"
  "non-owning view valid only during Decode()"
  "color-transform callback borrowing must stay explicit"
)
require_source_text(
  "${COLOR_TRANSFORM_IMAGE_SOURCE}"
  "if (_decodeStarted)"
  "aborted color transforms must close their callback lifecycle"
)
require_source_text(
  "${COLOR_TRANSFORM_IMAGE_SOURCE}"
  "_drawbuf.reset();"
  "color-transform workspace cleanup must be scope-independent"
)
require_source_text(
  "${IMAGE_FACTORY_SOURCE}"
  "LVCreateColorTransformImageSource(LVImageSourceRef srcImage"
  "color-transform construction must stay behind its checked factory"
)
require_source_text(
  "${IMAGE_FACTORY_SOURCE}"
  "if (srcImage.isNull())"
  "color-transform factory must reject a missing source"
)
forbid_source_text(
  "${COLOR_TRANSFORM_IMAGE_HEADER}"
  "LVColorDrawBuf * _drawbuf"
  "color-transform workspace must not use a raw owner"
)
forbid_source_text(
  "${COLOR_TRANSFORM_IMAGE_SOURCE}"
  "delete _drawbuf"
  "color-transform workspace teardown must remain automatic"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "failed nine-patch decode published partial metadata"
  "nine-patch candidate rollback must retain native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "aborted color transform retained its workspace"
  "color-transform abort cleanup must retain native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "color-transform workspace survived callback exception"
  "color-transform exception cleanup must retain native regression coverage"
)
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

# --- color/gray draw-buffer pixel backing ---
foreach(DRAW_BUFFER_HEADER
    "${COLOR_DRAW_BUFFER_HEADER}"
    "${GRAY_DRAW_BUFFER_HEADER}")
  require_source_text(
    "${DRAW_BUFFER_HEADER}"
    "std::vector<lUInt8> _ownedData"
    "draw-buffer pixel storage must have a scoped owner"
  )
  require_source_text(
    "${DRAW_BUFFER_HEADER}"
    "bool _isBorrowed"
    "draw-buffer raw scanline views must identify borrowed storage"
  )
  forbid_source_text(
    "${DRAW_BUFFER_HEADER}"
    "_ownData"
    "draw-buffer ownership must not regress to an owning boolean"
  )
endforeach()
require_source_text(
  "${COLOR_DRAW_BUFFER_HEADER}"
  "LVColorDrawBuf(const LVColorDrawBuf &) = delete"
  "color draw-buffer views must not be shallow-copied"
)
require_source_text(
  "${GRAY_DRAW_BUFFER_HEADER}"
  "LVGrayDrawBuf(const LVGrayDrawBuf &) = delete"
  "gray draw-buffer views must not be shallow-copied"
)
require_source_text(
  "${COLOR_DRAW_BUFFER_SOURCE}"
  "getColorBufferLayout"
  "color draw-buffer allocation must use checked layouts"
)
require_source_text(
  "${GRAY_DRAW_BUFFER_SOURCE}"
  "getGrayBufferLayout"
  "gray draw-buffer allocation must use checked layouts"
)
require_source_text(
  "${GRAY_DRAW_BUFFER_SOURCE}"
  "bitmap(layout.storageBytes, 0)"
  "gray bitmap conversion must allocate every output row plus its guard"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "testDrawBufferStorageOwnership"
  "draw-buffer ownership must retain native regression coverage"
)
foreach(DRAW_BUFFER_SOURCE
    "${COLOR_DRAW_BUFFER_SOURCE}"
    "${GRAY_DRAW_BUFFER_SOURCE}")
  forbid_source_text(
    "${DRAW_BUFFER_SOURCE}"
    "malloc("
    "draw-buffer pixel allocation must remain container-managed"
  )
  forbid_source_text(
    "${DRAW_BUFFER_SOURCE}"
    "calloc("
    "draw-buffer pixel allocation must remain container-managed"
  )
  forbid_source_text(
    "${DRAW_BUFFER_SOURCE}"
    "free("
    "draw-buffer pixel teardown must remain automatic"
  )
  forbid_source_text(
    "${DRAW_BUFFER_SOURCE}"
    "_ownData"
    "draw-buffer ownership must not regress to manual state"
  )
endforeach()

# --- WOL image/TOC buffers and reader result ownership ---
require_source_text(
  "${WOL_SOURCE}"
  "static const size_t WOL_MAX_IMAGE_BYTES"
  "WOL image buffers must have an explicit allocation bound"
)
require_source_text(
  "${WOL_SOURCE}"
  "static bool encodeLzssWithTerminator"
  "WOL LZSS output must be prepared transactionally"
)
require_source_text(
  "${WOL_SOURCE}"
  "return !out.getOverflow();"
  "WOL LZSS encoding must report a bounded-output overflow"
)
require_source_text(
  "${WOL_SOURCE}"
  "std::vector<wol_toc_subcatalog_item> toc"
  "WOL TOC serialization must use scoped storage"
)
require_source_text(
  "${WOL_HEADER}"
  "std::unique_ptr<LVArray<lUInt8> > getBookCover()"
  "WOL cover reads must return explicit ownership"
)
require_source_text(
  "${WOL_HEADER}"
  "std::unique_ptr<LVGrayDrawBuf> getImage( int index )"
  "WOL image reads must return explicit ownership"
)
require_source_text(
  "${WOL_SOURCE}"
  "bool LVRunWolBufferOwnershipRegression()"
  "WOL buffer ownership must retain native regression coverage"
)
forbid_source_text(
  "${WOL_SOURCE}"
  "new lUInt8"
  "WOL transient byte buffers must remain container-managed"
)
forbid_source_text(
  "${WOL_SOURCE}"
  "new wol_toc_subcatalog_item"
  "WOL TOC records must remain container-managed"
)
forbid_source_text(
  "${WOL_SOURCE}"
  "malloc"
  "WOL transient buffers must not regress to manual allocation"
)
forbid_source_text(
  "${WOL_SOURCE}"
  "free("
  "WOL transient buffer teardown must remain automatic"
)
forbid_source_text(
  "${WOL_SOURCE}"
  "delete[]"
  "WOL transient buffer teardown must remain automatic"
)
forbid_source_text(
  "${WOL_HEADER}"
  "LVArray<lUInt8> * getBookCover()"
  "WOL cover reads must not return implicit raw ownership"
)
forbid_source_text(
  "${WOL_HEADER}"
  "LVGrayDrawBuf * getImage( int index )"
  "WOL image reads must not return implicit raw ownership"
)
forbid_source_text(
  "${WOL_SOURCE}"
  "LVOpenFileStream( \"test.dat\""
  "WOL decoding must not write an implicit debug artifact"
)

# --- CSS declaration, selector-chain and snapshot ownership ---
require_source_text(
  "${STYLESHEET_HEADER}"
  "std::vector<int> _data;"
  "CSS compiled declarations must use container ownership"
)
require_source_text(
  "${STYLESHEET_HEADER}"
  "std::unique_ptr<LVCssSelectorRule> _next;"
  "CSS rule chains must own their next link explicitly"
)
require_source_text(
  "${STYLESHEET_HEADER}"
  "std::unique_ptr<LVCssSelector> _next;"
  "CSS selector chains must own their next link explicitly"
)
require_source_text(
  "${STYLESHEET_HEADER}"
  "std::unique_ptr<LVCssSelectorRule> _rules;"
  "CSS selectors must own their rule chain explicitly"
)
require_source_text(
  "${STYLESHEET_HEADER}"
  "std::vector<Snapshot> _stack;"
  "CSS stylesheet snapshots must keep count and selectors together"
)
require_source_text(
  "${STYLESHEET_SOURCE}"
  "_data.swap(parsedData);"
  "CSS declarations must publish only after complete parsing"
)
require_source_text(
  "${STYLESHEET_SOURCE}"
  "LVCssSelectorRule::~LVCssSelectorRule()"
  "CSS rule chains must have bounded iterative teardown"
)
require_source_text(
  "${STYLESHEET_SOURCE}"
  "LVCssSelector::~LVCssSelector()"
  "CSS selector chains must have bounded iterative teardown"
)
require_source_text(
  "${STYLESHEET_SOURCE}"
  "Snapshot snapshot = std::move(_stack.back());"
  "CSS snapshot restore must transfer one coherent state"
)
require_source_text(
  "${STYLESHEET_SOURCE}"
  "selector = item->takeNext();"
  "CSS selector publication must transfer each owned chain link"
)
require_source_text(
  "${CSS_REGRESSION_SOURCE}"
  "truncated CSS declaration replaced committed data"
  "CSS declaration rollback must retain regression coverage"
)
require_source_text(
  "${CSS_REGRESSION_SOURCE}"
  "const int selectorCount = 4096;"
  "CSS long-chain copy and teardown must retain regression coverage"
)
forbid_source_text(
  "${STYLESHEET_HEADER}"
  "int * _data;"
  "CSS declarations must not regress to owning raw arrays"
)
forbid_source_text(
  "${STYLESHEET_HEADER}"
  "LVCssSelectorRule * _next;"
  "CSS rule chains must not regress to owning raw links"
)
forbid_source_text(
  "${STYLESHEET_HEADER}"
  "LVCssSelector * _next;"
  "CSS selector chains must not regress to owning raw links"
)
forbid_source_text(
  "${STYLESHEET_HEADER}"
  "LVCssSelectorRule * _rules;"
  "CSS selectors must not regress to owning raw rule chains"
)
forbid_source_text(
  "${STYLESHEET_HEADER}"
  "LVPtrVector <LVCssSelector> _selectors;"
  "CSS selector buckets must retain direct RAII ownership"
)
forbid_source_text(
  "${STYLESHEET_HEADER}"
  "_selector_count_stack"
  "CSS snapshot state must not split across parallel stacks"
)
forbid_source_text(
  "${STYLESHEET_SOURCE}"
  "_data = new int["
  "CSS declaration allocation must remain container-managed"
)
forbid_source_text(
  "${STYLESHEET_SOURCE}"
  "delete selector;"
  "CSS parse rollback must remain automatic"
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
require_source_text(
  "${STRING8_COLLECTION_HEADER}"
  "std::vector<lString8> _items"
  "8-bit string collections must own their references through RAII storage"
)
require_source_text(
  "${STRING32_COLLECTION_HEADER}"
  "std::vector<lString32> _items"
  "32-bit string collections must own their references through RAII storage"
)
require_source_text(
  "${STRING32_COLLECTION_SOURCE}"
  "std::sort("
  "32-bit string sorting must use typed RAII storage"
)
require_source_text(
  "${HASHED_STRING_COLLECTION_HEADER}"
  "std::vector<std::vector<int>> _hashBuckets"
  "hashed string collision buckets must use nested RAII storage"
)
require_source_text(
  "${HASHED_STRING_COLLECTION_SOURCE}"
  "lString32Collection::clear()"
  "hashed string clearing must discard owned strings before bucket indices"
)
forbid_source_text(
  "${STRING8_COLLECTION_SOURCE}"
  "realloc("
  "8-bit string collection storage must not regress to realloc"
)
forbid_source_text(
  "${STRING8_COLLECTION_SOURCE}"
  "free("
  "8-bit string collection storage must not regress to free"
)
forbid_source_text(
  "${STRING32_COLLECTION_SOURCE}"
  "custom_lstr32_comparator_ptr"
  "string collection sorting must not publish a process-global comparator"
)
forbid_source_text(
  "${STRING32_COLLECTION_SOURCE}"
  "realloc("
  "32-bit string collection storage must not regress to realloc"
)
forbid_source_text(
  "${STRING32_COLLECTION_SOURCE}"
  "free("
  "32-bit string collection storage must not regress to free"
)
forbid_source_text(
  "${HASHED_STRING_COLLECTION_HEADER}"
  "HashPair"
  "hashed string buckets must not regress to manual collision nodes"
)
forbid_source_text(
  "${HASHED_STRING_COLLECTION_SOURCE}"
  "malloc("
  "hashed string collision storage must not regress to malloc"
)
forbid_source_text(
  "${HASHED_STRING_COLLECTION_SOURCE}"
  "free("
  "hashed string collision storage must not regress to free"
)
require_source_text(
  "${NAME_ID_MAP_HEADER}"
  "std::unique_ptr<css_elem_def_props_t> data"
  "name/id item metadata must use exclusive RAII ownership"
)
require_source_text(
  "${NAME_ID_MAP_HEADER}"
  "std::vector<std::unique_ptr<LDOMNameIdMapItem>> m_by_id"
  "name/id maps must keep one owning id index"
)
require_source_text(
  "${NAME_ID_MAP_HEADER}"
  "std::vector<LDOMNameIdMapItem *> m_by_name"
  "name/id maps must keep an explicit non-owning name index"
)
require_source_text(
  "${NAME_ID_MAP_SOURCE}"
  "LDOMNameIdMap candidate(0)"
  "name/id deserialization must build a rollback candidate"
)
require_source_text(
  "${NAME_ID_MAP_SOURCE}"
  "swap(candidate)"
  "name/id deserialization must publish only a validated candidate"
)
require_source_text(
  "${NAME_ID_MAP_SOURCE}"
  "m_by_name.push_back(itemView)"
  "name/id adoption must publish the non-owning view before ownership transfer"
)
forbid_source_text(
  "${NAME_ID_MAP_HEADER}"
  "LDOMNameIdMapItem * *"
  "name/id maps must not own raw pointer arrays"
)
forbid_source_text(
  "${NAME_ID_MAP_SOURCE}"
  "cr_realloc("
  "name/id map growth must not use sequential realloc"
)
forbid_source_text(
  "${NAME_ID_MAP_SOURCE}"
  "delete[]"
  "name/id map teardown must not manually delete index arrays"
)
forbid_source_text(
  "${NAME_ID_MAP_SOURCE}"
  "qsort("
  "name/id map sorting must stay typed"
)
require_source_text(
  "${HASH_TABLE_HEADER}"
  "std::vector<std::unique_ptr<pair>> _table"
  "hash table buckets must use RAII ownership"
)
require_source_text(
  "${HASH_TABLE_HEADER}"
  "std::unique_ptr<pair> next"
  "hash table collision links must use exclusive RAII ownership"
)
require_source_text(
  "${HASH_TABLE_HEADER}"
  "LVHashTable(const LVHashTable &table)"
  "hash table copies must rebuild independently owned nodes"
)
require_source_text(
  "${HASH_TABLE_HEADER}"
  "std::unique_ptr<pair> item = std::move(bucket)"
  "hash table resize and teardown must transfer node ownership"
)
require_source_text(
  "${HASH_TABLE_HEADER}"
  "ptr = ptr->next.get()"
  "hash iterators must traverse non-owning views of collision owners"
)
require_source_text(
  "${HASH_TABLE_HEADER}"
  "void swap(LVHashTable &table) noexcept"
  "hash-table ownership must support no-throw transactional publication"
)
forbid_source_text(
  "${HASH_TABLE_HEADER}"
  "pair ** _table"
  "hash tables must not own a raw bucket array"
)
forbid_source_text(
  "${HASH_TABLE_HEADER}"
  "new pair*"
  "hash table bucket allocation must not regress to new[]"
)
forbid_source_text(
  "${HASH_TABLE_HEADER}"
  "delete[] _table"
  "hash table bucket teardown must not use manual delete[]"
)
forbid_source_text(
  "${HASH_TABLE_HEADER}"
  "memset( _table"
  "hash table clearing must not byte-reset owned nodes"
)

# --- generic value-array backing storage and rollback ---
require_source_text(
  "${VALUE_ARRAY_HEADER}"
  "std::unique_ptr<T[]> _array"
  "value arrays must use RAII backing storage"
)
require_source_text(
  "${VALUE_ARRAY_HEADER}"
  "std::unique_ptr<T[]> storage(new T[size]())"
  "value-array growth must build scoped candidate storage"
)
require_source_text(
  "${VALUE_ARRAY_HEADER}"
  "_array = std::move(storage)"
  "value-array storage must publish only after copying succeeds"
)
require_source_text(
  "${VALUE_ARRAY_HEADER}"
  "std::unique_ptr<T[]> snapshot(new T[count]())"
  "value-array aliased appends must snapshot their source"
)
require_source_text(
  "${VALUE_ARRAY_HEADER}"
  "_array[i] = T();"
  "value-array reset and erase must release inactive values"
)
forbid_source_text(
  "${VALUE_ARRAY_HEADER}"
  "T * _array"
  "value arrays must not own a raw backing array"
)
forbid_source_text(
  "${VALUE_ARRAY_HEADER}"
  "T* new_array"
  "value-array growth must not regress to raw candidate storage"
)
forbid_source_text(
  "${VALUE_ARRAY_HEADER}"
  "delete [] _array"
  "value-array teardown must not use manual delete[]"
)

# --- reference-vector backing storage and alias safety ---
require_source_text(
  "${REF_HEADER}"
  "std::unique_ptr<LVRef<T>[]> _array"
  "reference vectors must use RAII backing storage"
)
require_source_text(
  "${REF_HEADER}"
  "std::unique_ptr<LVRef<T>[]> storage(new LVRef<T>[size])"
  "reference-vector growth must build scoped candidate storage"
)
require_source_text(
  "${REF_HEADER}"
  "std::unique_ptr<LVRef<T>[]> snapshot(new LVRef<T>[count])"
  "reference-vector aliased appends must snapshot their source"
)
require_source_text(
  "${REF_HEADER}"
  "reserve(index + 1)"
  "reference-vector set must reserve the indexed slot"
)
require_source_text(
  "${REF_HEADER}"
  "_array[i] = LVRef<T>();"
  "reference-vector erase must release inactive values"
)
forbid_source_text(
  "${REF_HEADER}"
  "LVRef<T> * _array"
  "reference vectors must not own a raw backing array"
)
forbid_source_text(
  "${REF_HEADER}"
  "LVRef<T> * newarray"
  "reference-vector growth must not use raw candidate storage"
)
forbid_source_text(
  "${REF_HEADER}"
  "T* new_array = (T*)malloc"
  "reference-vector trim must not allocate unconstructed objects"
)
forbid_source_text(
  "${REF_HEADER}"
  "delete [] _array"
  "reference-vector teardown must not use manual delete[]"
)
forbid_source_text(
  "${REF_HEADER}"
  "free( _array )"
  "reference-vector teardown must not mix allocation families"
)

# --- owning/borrowed pointer-vector slot and item lifecycle ---
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "std::vector<T *> _list"
  "pointer-vector slots must use container-backed storage"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "void discardSlot(int index)"
  "pointer-vector item disposal must use one ownership boundary"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "std::unique_ptr<T> item(_list[index])"
  "owning pointer-vector disposal must scope item ownership"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "std::vector<std::unique_ptr<T> > owners"
  "owning pointer-vector copies must scope partial clones"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "if constexpr (ownItems)"
  "pointer-vector ownership branches must be compile-time explicit"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "storage[i] = v[i]"
  "borrowed pointer-vector copies must preserve non-owning views"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "_list[_count] = NULL"
  "pointer-vector transfers must clear inactive slots"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "std::sort(_list.begin(), _list.begin() + _count"
  "pointer-vector sorting must stay typed"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "void swap(LVPtrVector &vector) noexcept"
  "pointer-vector ownership must support no-throw transactional publication"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "T * * _list"
  "pointer vectors must not own a raw slot array"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "cr_realloc( _list"
  "pointer-vector growth must not use realloc"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "free( _list"
  "pointer-vector teardown must not free raw slot storage"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "delete _list"
  "pointer-vector item disposal must remain scope-bound"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "qsort(_list"
  "pointer-vector sorting must not use an erased comparator"
)

# --- generic matrix cell storage and resize lifecycle ---
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "std::vector<_Ty> cells"
  "matrix cells must use contiguous container-backed storage"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "LVMatrix(const LVMatrix &) = default"
  "matrix copies must deep-copy their backing storage"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "const size_t maxCellCount = std::min"
  "matrix dimensions must be bounded before allocation"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "std::vector<_Ty> replacement("
  "matrix resize must build scoped candidate storage"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "std::copy_n(cells.begin() + oldOffset"
  "matrix resize must retain only the overlapping cells"
)
require_source_text(
  "${PTR_VECTOR_HEADER}"
  "cells.swap(replacement)"
  "matrix resize must publish only complete candidate storage"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "_Ty ** rows"
  "matrices must not own raw row arrays"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "free( rows"
  "matrix teardown must not manually free rows"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "cr_realloc( rows"
  "matrix resize must not reallocate raw row arrays"
)
forbid_source_text(
  "${PTR_VECTOR_HEADER}"
  "malloc( sizeof(_Ty*)"
  "matrix rows must not allocate pointer-sized cell storage"
)

# --- pagination compact-array and line-link ownership ---
require_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "std::vector<T> _list"
  "pagination compact arrays must use container-backed storage"
)
require_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "std::unique_ptr<Array> _data"
  "pagination compact-array storage must be lazily RAII-owned"
)
require_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "std::vector<T> snapshot(items, items + count)"
  "pagination compact-array appends must snapshot aliased input"
)
require_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "std::make_unique<Array>(*array._data)"
  "pagination compact-array copies must deep-copy storage"
)
require_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "std::unique_ptr<LVFootNoteList> links"
  "rendered lines must explicitly own their footnote-link list"
)
require_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "std::make_unique<LVFootNoteList>(*line.links)"
  "rendered-line copies must deep-copy link-list storage"
)
require_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "links.reset()"
  "rendered-line clear must release link-list ownership"
)
forbid_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "T * _list"
  "pagination compact arrays must not own raw backing arrays"
)
forbid_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "Array * _data"
  "pagination compact arrays must not own raw lazy storage"
)
forbid_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "cr_realloc( _list"
  "pagination compact-array growth must not use realloc"
)
forbid_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "free( _list"
  "pagination compact-array teardown must not use free"
)
forbid_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "delete _data"
  "pagination compact-array teardown must remain automatic"
)
forbid_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "LVFootNoteList * links"
  "rendered lines must not own raw link-list pointers"
)
forbid_source_text(
  "${PAGE_SPLITTER_HEADER}"
  "delete links"
  "rendered-line link teardown must remain automatic"
)

# --- reference-cache bucket, index and export ownership ---
require_source_text(
  "${REF_CACHE_HEADER}"
  "std::vector<std::unique_ptr<LVRefCacheRec> > table"
  "reference-cache buckets must use RAII ownership"
)
require_source_text(
  "${REF_CACHE_HEADER}"
  "std::unique_ptr<LVRefCacheRec> next"
  "reference-cache collision links must use exclusive ownership"
)
require_source_text(
  "${REF_CACHE_HEADER}"
  "std::vector<LVRefCacheIndexRec> index"
  "indexed reference-cache metadata must use container storage"
)
require_source_text(
  "${REF_CACHE_HEADER}"
  "LVIndexedRefCache candidate("
  "reference-cache index restoration must build a scoped candidate"
)
require_source_text(
  "${REF_CACHE_HEADER}"
  "std::unique_ptr<LVArray<ref_t> > getIndex() const"
  "reference-cache index export must return explicit ownership"
)
require_source_text(
  "${REF_CACHE_HEADER}"
  "void clearBuckets()"
  "reference-cache collision teardown must remain iterative"
)
require_source_text(
  "${REF_CACHE_HEADER}"
  "std::vector<Pair> buf"
  "bounded cache maps must use container-backed storage"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::unique_ptr<LVArray<css_style_ref_t> > list = _styles.getIndex()"
  "DOM style-index exports must retain scoped ownership"
)
forbid_source_text(
  "${REF_CACHE_HEADER}"
  "LVRefCacheRec ** table"
  "reference caches must not own raw bucket arrays"
)
forbid_source_text(
  "${REF_CACHE_HEADER}"
  "LVRefCacheRec * next"
  "reference-cache nodes must not own raw collision links"
)
forbid_source_text(
  "${REF_CACHE_HEADER}"
  "cr_realloc( index"
  "reference-cache index growth must not use realloc"
)
forbid_source_text(
  "${REF_CACHE_HEADER}"
  "delete[] table"
  "reference-cache bucket teardown must not use manual delete[]"
)
forbid_source_text(
  "${REF_CACHE_HEADER}"
  "LVArray<ref_t> * getIndex()"
  "reference-cache index export must not return implicit raw ownership"
)
forbid_source_text(
  "${REF_CACHE_HEADER}"
  "Pair * buf"
  "bounded cache maps must not own a raw backing array"
)
forbid_source_text(
  "${REF_CACHE_HEADER}"
  "delete[] buf"
  "bounded cache-map teardown must not use manual delete[]"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "delete list;"
  "DOM style-index export cleanup must remain automatic"
)

require_source_text(
  "${ZIP_STREAM_HEADER}"
  "static std::unique_ptr<LVStream> Create"
  "ZIP entry factories must return scoped ownership"
)
require_source_text(
  "${ZIP_STREAM_HEADER}"
  "LVZipDecodeStream(const LVZipDecodeStream &) = delete"
  "ZIP decoders must not copy inflater and buffer ownership"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "std::unique_ptr<LVStreamFragment> sourceFragment"
  "deflated ZIP source fragments must start with scoped ownership"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "LVStreamRef srcStream(sourceFragment.release())"
  "ZIP source fragments must transfer only into the engine reference owner"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "std::unique_ptr<LVZipDecodeStream> res"
  "ZIP decoder candidates must stay scoped during inflater initialization"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "if (!res->rewind())"
  "failed ZIP inflater initialization must roll back the decoder candidate"
)
require_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "std::unique_ptr<LVStream> stream"
  "ZIP archive entries must stay scoped until LVStreamRef adoption"
)
require_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "return LVStreamRef(stream.release())"
  "ZIP entry ownership must transfer only at the stream reference boundary"
)
require_source_text(
  "${STREAM_FRAGMENT_HEADER}"
  "LVStreamFragment(const LVStreamFragment &) = delete"
  "stream fragments must not copy source ownership and position state"
)
require_source_text(
  "${STREAM_FRAGMENT_HEADER}"
  "if (size > remaining)"
  "stream fragment reads must clamp to their declared region"
)
require_source_text(
  "${STREAM_FRAGMENT_HEADER}"
  "delta > m_size - base"
  "stream fragment seeks must reject positions outside their region"
)
require_source_text(
  "${STREAM_FRAGMENT_HEADER}"
  "m_start > LV_INVALID_SIZE - m_size"
  "stream fragment source ranges must reject overflow"
)
require_source_text(
  "${ZIP_HEADER_SOURCE}"
  "findZip64ExtInfo"
  "ZIP64 extra records must use a bounded record parser"
)
require_source_text(
  "${ZIP_HEADER_SOURCE}"
  "recordSize > extraSize - offset"
  "ZIP64 record sizes must stay within the supplied extra bytes"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "hdr.Mark != 0x04034b50"
  "ZIP entry creation must validate the local header signature"
)
require_source_text(
  "${ZIP_STREAM_SOURCE}"
  "pos > LV_INVALID_SIZE - localHeaderSize"
  "ZIP local-header offset arithmetic must reject overflow"
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
forbid_source_text(
  "${ZIP_STREAM_HEADER}"
  "static LVStream * Create"
  "ZIP entry factories must not return implicit raw ownership"
)
forbid_source_text(
  "${ZIP_STREAM_SOURCE}"
  "LVStreamFragment * fragment = new"
  "stored ZIP entry construction must not use implicit raw ownership"
)
forbid_source_text(
  "${ZIP_STREAM_SOURCE}"
  "LVZipDecodeStream * res = new"
  "ZIP decoder construction must not use implicit raw ownership"
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

# --- mapped-file region and OS-handle ownership ---
require_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "class MappedRegion"
  "mapped files must wrap mapping lifetime in an RAII type"
)
require_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "class ScopedDescriptor"
  "POSIX mapped-file descriptors must use scoped ownership"
)
require_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "class ScopedHandle"
  "Windows mapped-file handles must use scoped ownership"
)
require_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "MappedRegion m_map"
  "mapped-file stream must own its mapping region"
)
require_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "static std::unique_ptr<LVFileMappedStream> CreateFileStream"
  "mapped-file factory results must express ownership"
)
require_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "LVFileMappedStream::MappedRegion::~MappedRegion()"
  "mapped regions must release themselves at scope exit"
)
require_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "m_map.adopt"
  "successful mappings must transfer into the region owner"
)
require_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "m_fd.reset(fd)"
  "POSIX file descriptors must transfer into the scoped owner"
)
require_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "m_hFile.reset(fileHandle)"
  "Windows file handles must transfer into the scoped owner"
)
require_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "std::make_unique<LVFileMappedStream>()"
  "mapped-file candidates must stay scoped during open"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "LVFileMappedStream::CreateFileStream("
  "mapped-file utility must use the owning factory"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "LVStreamRef(stream.release())"
  "mapped-file ownership must transfer only at the reference boundary"
)
forbid_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "lUInt8* m_map"
  "mapped-file regions must not regress to raw ownership"
)
forbid_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "int m_fd"
  "mapped-file descriptors must not regress to raw ownership"
)
forbid_source_text(
  "${FILE_MAPPED_STREAM_HEADER}"
  "HANDLE m_hFile"
  "mapped-file handles must not regress to raw ownership"
)
forbid_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "::close(m_fd"
  "mapped-file descriptor cleanup must remain scope-bound"
)
forbid_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "CloseHandle(m_h"
  "mapped-file handle cleanup must remain scope-bound"
)
forbid_source_text(
  "${FILE_MAPPED_STREAM_SOURCE}"
  "LVFileMappedStream * f = new"
  "mapped-file factory rollback must remain automatic"
)

# --- block write-cache buffer and LRU ownership ---
require_source_text(
  "${BLOCK_WRITE_STREAM_HEADER}"
  "std::vector<lUInt8> buf"
  "block write-cache payloads must use RAII storage"
)
require_source_text(
  "${BLOCK_WRITE_STREAM_HEADER}"
  "std::unique_ptr<Block> next"
  "block write-cache links must transfer explicit ownership"
)
require_source_text(
  "${BLOCK_WRITE_STREAM_HEADER}"
  "std::unique_ptr<Block> _firstBlock"
  "block write-cache root must own its chain"
)
require_source_text(
  "${BLOCK_WRITE_STREAM_SOURCE}"
  "std::make_unique<Block>("
  "new write-cache blocks must start with scoped ownership"
)
require_source_text(
  "${BLOCK_WRITE_STREAM_SOURCE}"
  "if (writeBlock(last->get()) != LVERR_OK)"
  "failed block eviction must retain dirty storage"
)
require_source_text(
  "${BLOCK_WRITE_STREAM_SOURCE}"
  "if (writeBlock(_firstBlock.get()) != LVERR_OK)"
  "failed block flush must retain the dirty chain"
)
require_source_text(
  "${BLOCK_WRITE_STREAM_SOURCE}"
  "_count--"
  "block write-cache removal must keep its bounded count coherent"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "std::make_unique<LVBlockWriteStream>"
  "block write-stream factories must own candidates explicitly"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "if (blockSize <= 0 || blockCount <= 0)"
  "block write-stream factories must reject invalid cache bounds"
)
forbid_source_text(
  "${BLOCK_WRITE_STREAM_HEADER}"
  "lUInt8 * buf"
  "block write-cache payloads must not regress to raw ownership"
)
forbid_source_text(
  "${BLOCK_WRITE_STREAM_HEADER}"
  "Block * next"
  "block write-cache links must not regress to raw ownership"
)
forbid_source_text(
  "${BLOCK_WRITE_STREAM_HEADER}"
  "Block * _firstBlock"
  "block write-cache root must not regress to raw ownership"
)
forbid_source_text(
  "${BLOCK_WRITE_STREAM_SOURCE}"
  "calloc("
  "block write-cache allocation must remain container-managed"
)
forbid_source_text(
  "${BLOCK_WRITE_STREAM_SOURCE}"
  "free("
  "block write-cache teardown must remain automatic"
)
forbid_source_text(
  "${BLOCK_WRITE_STREAM_SOURCE}"
  "delete "
  "block write-cache rollback must remain scope-bound"
)

# --- owned and borrowed memory-stream storage ---
require_source_text(
  "${MEMORY_STREAM_HEADER}"
  "std::vector<lUInt8> m_storage"
  "owned memory streams must use RAII storage"
)
require_source_text(
  "${MEMORY_STREAM_HEADER}"
  "LVMemoryStream(const LVMemoryStream &) = delete"
  "memory streams must not shallow-copy their buffer view"
)
require_source_text(
  "${MEMORY_STREAM_SOURCE}"
  "m_storage.empty() || m_pBuffer != m_storage.data()"
  "borrowed memory streams must remain non-resizable views"
)
require_source_text(
  "${MEMORY_STREAM_SOURCE}"
  "m_storage.resize(vectorSize)"
  "owned memory-stream growth must remain container-managed"
)
require_source_text(
  "${MEMORY_STREAM_SOURCE}"
  "std::vector<lUInt8>().swap(m_storage)"
  "memory-stream close must release owned storage automatically"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "std::unique_ptr<LVMemoryStream> stream(new LVMemoryStream())"
  "memory-stream factories must own candidates until initialization succeeds"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "if (result != LVERR_OK)"
  "memory-stream factories must not publish failed initialization"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "LVStreamRef(stream.release())"
  "memory-stream ownership must transfer only at the reference boundary"
)
forbid_source_text(
  "${MEMORY_STREAM_HEADER}"
  "bool m_own_buffer"
  "memory-stream ownership must not depend on a raw-pointer flag"
)
forbid_source_text(
  "${MEMORY_STREAM_SOURCE}"
  "cr_realloc("
  "memory-stream growth must not regress to manual reallocation"
)
forbid_source_text(
  "${MEMORY_STREAM_SOURCE}"
  "malloc("
  "memory-stream allocation must remain container-managed"
)
forbid_source_text(
  "${MEMORY_STREAM_SOURCE}"
  "free(m_pBuffer)"
  "memory-stream teardown must remain automatic"
)
forbid_source_text(
  "${STREAM_UTILS_SOURCE}"
  "LVMemoryStream * stream = new LVMemoryStream()"
  "memory-stream factories must not use implicit raw ownership"
)

# --- serialization buffer owning/borrowed storage ---
require_source_text(
  "${SERIAL_BUFFER_HEADER}"
  "std::vector<lUInt8> _storage"
  "serialization buffers must use RAII storage ownership"
)
require_source_text(
  "${SERIAL_BUFFER_HEADER}"
  "SerialBuf( const SerialBuf & ) = delete"
  "serialization buffers must not shallow-copy their storage view"
)
require_source_text(
  "${SERIAL_BUFFER_SOURCE}"
  "std::unique_ptr<lUInt8, decltype(&std::free)> adopted"
  "legacy serialization-buffer adoption must be scope-bound"
)
require_source_text(
  "${SERIAL_BUFFER_SOURCE}"
  "_storage.resize(static_cast<std::size_t>(grownSize))"
  "serialization-buffer growth must remain container-managed"
)
forbid_source_text(
  "${SERIAL_BUFFER_HEADER}"
  "bool _ownbuf"
  "serialization-buffer ownership must not depend on a raw-pointer flag"
)
forbid_source_text(
  "${SERIAL_BUFFER_SOURCE}"
  "cr_realloc("
  "serialization-buffer growth must not regress to manual reallocation"
)
forbid_source_text(
  "${SERIAL_BUFFER_SOURCE}"
  "free( _buf"
  "serialization-buffer teardown must remain automatic"
)

# --- cache-file codec state and operation buffers ---
require_source_text(
  "${DOM_SOURCE}"
  "std::unique_ptr<zstd_comp_res_t> _zstd_comp_res"
  "cache-file ZSTD compression state must use RAII ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::unique_ptr<zstd_decomp_res_t> _zstd_decomp_res"
  "cache-file ZSTD decompression state must use RAII ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::unique_ptr<zlib_comp_res_t> _zlib_comp_res"
  "cache-file zlib compression state must use RAII ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::unique_ptr<zlib_decomp_res_t> _zlib_uncomp_res"
  "cache-file zlib decompression state must use RAII ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "bool CacheFile::readBlock("
  "cache-file block reads must use operation-scoped storage"
)
require_source_text(
  "${DOM_SOURCE}"
  "dstbuf.swap(candidate)"
  "cache-file codec results must publish transactionally"
)
require_source_text(
  "${DOM_SOURCE}"
  "buf.set(std::move(storage))"
  "cache-file serialization reads must move RAII storage"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "zstd_comp_res_t* _zstd_comp_res"
  "cache-file ZSTD state must not use an owning raw pointer"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "zlib_res_t* _zlib_comp_res"
  "cache-file zlib state must not use an owning raw pointer"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "compressed_buf = cr_realloc"
  "cache-file compression output must not use manual reallocation"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "uncompressed_buf = cr_realloc"
  "cache-file decompression output must not use manual reallocation"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "deflateEnd(z);"
  "cache-file operations must not invalidate reusable zlib state"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "inflateEnd(z);"
  "cache-file operations must not invalidate reusable zlib state"
)

# --- cache-file index snapshots and transactional publication ---
require_source_text(
  "${DOM_SOURCE}"
  "CACHE_FILE_MAX_INDEX_ITEMS"
  "cache-file indexes must enforce their item-count bound"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::vector<CacheFileItem> serializedIndex"
  "cache-file index snapshots must use typed RAII storage"
)
require_source_text(
  "${DOM_SOURCE}"
  "LVPtrVector<CacheFileItem, true> candidateIndex"
  "cache-file index loads must scope candidate item ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::adjacent_find("
  "cache-file indexes must reject duplicate live block keys"
)
require_source_text(
  "${DOM_SOURCE}"
  "_map.swap(candidateMap);\n        _freeIndex.swap(candidateFreeIndex);\n        _index.swap(candidateIndex);"
  "cache-file indexes must publish all lookup structures atomically"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "CacheFileItem * index = new CacheFileItem[count]"
  "cache-file index snapshots must not use owning raw arrays"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "delete[] index"
  "cache-file index snapshot teardown must remain automatic"
)

# --- persistent DOM node-part ownership and cache loading ---
require_source_text(
  "${DOM_HEADER}"
  "std::unique_ptr<CacheFile> _cacheFile"
  "the persistent DOM cache file must have scoped ownership"
)
require_source_text(
  "${DOM_HEADER}"
  "void adoptCacheFile(std::unique_ptr<CacheFile> cacheFile)"
  "persistent DOM cache publication must transfer explicit ownership"
)
require_source_text(
  "${DOM_HEADER}"
  "non-owning cache view supplied by tinyNodeCollection"
  "DOM cache consumers must document their borrowed status"
)
require_source_text(
  "${DOM_SOURCE}"
  "_cacheFile = std::move(cacheFile)"
  "the persistent DOM cache candidate must enter its owner before publication"
)
require_source_text(
  "${DOM_SOURCE}"
  "CacheFile *cacheView = _cacheFile.get()"
  "DOM storage managers must receive one borrowed cache view"
)
require_source_text(
  "${DOM_SOURCE}"
  "_blobCache.setCacheFile(cacheView)"
  "DOM blob storage must receive the owner-backed cache view"
)
require_source_text(
  "${DOM_SOURCE}"
  "f->setCachePath(cache_path);\n    adoptCacheFile(std::move(f));\n    return true;"
  "opened DOM caches must transfer scoped ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "f->setCachePath(cache_path);\n    adoptCacheFile(std::move(f));\n    _mapped = true;"
  "created DOM caches must transfer scoped ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "CacheFile *cacheView = cache.get();\n    collection.adoptCacheFile(std::move(cache));"
  "DOM cache ownership must retain native transfer regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "DOM node-part/cache-file ownership regression failed"
  "DOM cache ownership regression must remain wired into the native suite"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "_cacheFile = f.release()"
  "DOM cache candidates must not regress to raw ownership publication"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "_cacheFile = cache.release()"
  "DOM cache regression setup must not bypass scoped ownership"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "delete _cacheFile"
  "persistent DOM cache teardown must remain automatic"
)
require_source_text(
  "${DOM_HEADER}"
  "typedef std::unique_ptr<ldomNode, ldomNodePartDeleter>"
  "DOM node parts must use explicit malloc-compatible RAII ownership"
)
require_source_text(
  "${DOM_HEADER}"
  "typedef std::array<ldomNodePartOwner, TNC_PART_COUNT>"
  "DOM node-part catalogs must own every block automatically"
)
require_source_text(
  "${DOM_SOURCE}"
  "void ldomNodePartDeleter::operator()"
  "DOM node-part allocation must have one matching free boundary"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::vector<lUInt8> serializedPart"
  "DOM node-part cache reads must use scoped vector storage"
)
require_source_text(
  "${DOM_SOURCE}"
  "ldomNodePartList elemList;\n    ldomNodePartList textList;"
  "DOM node-part loads must stage both catalogs in RAII candidates"
)
require_source_text(
  "${DOM_SOURCE}"
  "destroyNodeParts(_elemList, _elemCount);\n    destroyNodeParts(_textList, _textCount);\n    _elemList.swap(elemList);\n    _textList.swap(textList);"
  "DOM node-part catalogs must publish only after complete validation"
)
require_source_text(
  "${DOM_SOURCE}"
  "maximumNodeCount"
  "DOM node-part indexes must enforce their address-space bound"
)
forbid_source_text(
  "${DOM_HEADER}"
  "ldomNode * _textList[TNC_PART_COUNT]"
  "DOM text-node parts must not regress to owning raw catalogs"
)
forbid_source_text(
  "${DOM_HEADER}"
  "ldomNode * _elemList[TNC_PART_COUNT]"
  "DOM element-node parts must not regress to owning raw catalogs"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "realloc(buf, TNC_PART_LEN * sizeof(ldomNode))"
  "DOM node-part restoration must not transfer realloc ownership"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "lUInt8 * &buf, int &size"
  "cache-file reads must not expose malloc-owned result buffers"
)

# --- DOM blob payload and index ownership ---
require_source_text(
  "${DOM_HEADER}"
  "std::vector<std::unique_ptr<ldomBlobItem> > _list"
  "DOM blob items must use explicit RAII ownership"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::vector<lUInt8> _data"
  "DOM blob payloads must use container-managed storage"
)
require_source_text(
  "${DOM_SOURCE}"
  "std::vector<std::unique_ptr<ldomBlobItem> > candidate"
  "DOM blob index loads must remain transactionally scoped"
)
require_source_text(
  "${DOM_SOURCE}"
  "_list.swap(candidate)"
  "DOM blob indexes must publish only after complete validation"
)
require_source_text(
  "${DOM_SOURCE}"
  "BLOB_CACHE_MAX_ITEMS"
  "DOM blob indexes must enforce their 16-bit storage bound"
)
require_source_text(
  "${DOM_SOURCE}"
  "if (size >= 4)"
  "DOM blob diagnostics must guard their four-byte preview"
)
require_source_text(
  "${DOM_SOURCE}"
  "_list.reserve(index + 1)"
  "DOM blob item capacity must be reserved before cache publication"
)
require_source_text(
  "${DOM_SOURCE}"
  "if (!_cacheFile->write("
  "DOM blob items must not publish after a failed cache write"
)
require_source_text(
  "${DOM_SOURCE}"
  "if (res)\n        res = saveIndex();"
  "DOM blob indexes must not publish after a payload write failure"
)
forbid_source_text(
  "${DOM_HEADER}"
  "LVPtrVector<ldomBlobItem> _list"
  "DOM blob items must not regress to implicit raw ownership"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "lUInt8 * _data;"
  "DOM blob payloads must not use an owning raw pointer"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "delete[] _data"
  "DOM blob payload teardown must remain automatic"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "_data = new lUInt8[size]"
  "DOM blob payload allocation must remain container-managed"
)

# --- DOM text-storage chunk ownership and cache transitions ---
require_source_text(
  "${DOM_HEADER}"
  "std::vector<lUInt8> _storage"
  "DOM text-storage chunks must own resident bytes through RAII"
)
require_source_text(
  "${DOM_HEADER}"
  "_nextRecent; /// non-owning LRU link"
  "DOM text-storage LRU links must remain explicitly non-owning"
)
require_source_text(
  "${DOM_SOURCE}"
  "// reads a block into caller-owned storage\nbool CacheFile::read("
  "cache-file reads must support direct caller-owned vector storage"
)
require_source_text(
  "${DOM_SOURCE}"
  "candidate.size() != _bufpos"
  "DOM chunk restoration must validate its indexed size"
)
require_source_text(
  "${DOM_SOURCE}"
  "_storage.swap(candidate)"
  "DOM chunk storage must publish transactionally"
)
require_source_text(
  "${DOM_SOURCE}"
  "void ldomTextStorageChunk::clearUnpacked()"
  "DOM chunk resident accounting must have one teardown path"
)
require_source_text(
  "${DOM_SOURCE}"
  "bool ldomTextStorageChunk::validRange("
  "DOM chunk raw views must enforce bounds in every build"
)
forbid_source_text(
  "${DOM_HEADER}"
  "lUInt8 * _buf;"
  "DOM text-storage chunks must not regress to owning raw buffers"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "calloc(preAllocSize"
  "DOM text-storage preallocation must remain container-managed"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "setunpacked("
  "DOM text-storage teardown must not regress to manual buffer transfer"
)

# --- file-stream owned and borrowed OS resources ---
require_source_text(
  "${FILE_STREAM_HEADER}"
  "std::unique_ptr<FILE, FileCloser> m_file"
  "ANSI file streams must scope FILE ownership"
)
require_source_text(
  "${FILE_STREAM_HEADER}"
  "class ScopedHandle"
  "Windows file streams must scope HANDLE ownership"
)
require_source_text(
  "${FILE_STREAM_HEADER}"
  "class ScopedDescriptor"
  "POSIX file streams must scope descriptor ownership"
)
require_source_text(
  "${FILE_STREAM_HEADER}"
  "ScopedDescriptor m_ownedFd"
  "owned file-stream descriptors must have an explicit owner"
)
require_source_text(
  "${FILE_STREAM_HEADER}"
  "int m_borrowedFd"
  "borrowed file-stream descriptors must remain explicit non-owning views"
)
require_source_text(
  "${FILE_STREAM_HEADER}"
  "static std::unique_ptr<LVFileStream> CreateFileStream"
  "file-stream factories must return scoped ownership"
)
require_source_text(
  "${FILE_STREAM_SOURCE}"
  "m_ownedFd.reset(candidate.release())"
  "validated descriptor candidates must transfer ownership explicitly"
)
require_source_text(
  "${FILE_STREAM_SOURCE}"
  "m_borrowedFd = candidateFd"
  "borrowed descriptor publication must not enter the owning wrapper"
)
require_source_text(
  "${FILE_STREAM_SOURCE}"
  "ftruncate(fd, nativeSize)"
  "POSIX file-stream resize must update the underlying file"
)
require_source_text(
  "${FILE_STREAM_SOURCE}"
  "(useSync ? O_SYNC : 0)"
  "file-stream sync flags must be read before open-mode masking"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "LVFileStream::CreateFileStream(fd, mode, autoClose)"
  "descriptor factories must stay scoped until the LVStreamRef boundary"
)
forbid_source_text(
  "${FILE_STREAM_HEADER}"
  "bool                   m_autoClose"
  "file-stream ownership must not depend on a descriptor flag"
)
forbid_source_text(
  "${FILE_STREAM_HEADER}"
  "FILE * m_file"
  "ANSI file streams must not regress to raw FILE ownership"
)
forbid_source_text(
  "${FILE_STREAM_HEADER}"
  "HANDLE m_hFile"
  "Windows file streams must not regress to raw HANDLE ownership"
)
forbid_source_text(
  "${FILE_STREAM_HEADER}"
  "int m_fd;"
  "POSIX file streams must not regress to a raw owning descriptor"
)
forbid_source_text(
  "${FILE_STREAM_SOURCE}"
  "LVFileStream * f = new LVFileStream"
  "file-stream factory rollback must remain scope-bound"
)
forbid_source_text(
  "${STREAM_UTILS_SOURCE}"
  "LVFileStream * stream = LVFileStream::CreateFileStream"
  "file-stream utilities must not reintroduce implicit raw ownership"
)

# --- directory-container scan and item ownership ---
require_source_text(
  "${DIRECTORY_CONTAINER_HEADER}"
  "static std::unique_ptr<LVDirectoryContainer> OpenDirectory"
  "directory factories must return scoped ownership"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "class ScopedFindHandle"
  "Windows directory enumeration must scope its search handle"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "FindClose(_handle)"
  "Windows directory search handles must close automatically"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "using ScopedDirectory = std::unique_ptr<DIR, DirectoryCloser>"
  "POSIX directory enumeration must scope DIR ownership"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "closedir(directory)"
  "POSIX directory handles must close automatically"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "std::unique_ptr<LVDirectoryContainerItemInfo> item"
  "directory items must stay scoped until container adoption"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "directory.Add(item.release())"
  "directory item ownership must transfer only at the owning list boundary"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "if (stat(fpath.c_str(), &st) != 0)"
  "failed directory metadata reads must not publish uninitialized items"
)
require_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "if (errno != 0)"
  "directory enumeration errors must roll back the candidate container"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "std::unique_ptr<LVDirectoryContainer> dir"
  "directory utilities must scope candidates until LVContainerRef adoption"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "return LVContainerRef(dir.release())"
  "directory ownership must transfer only at the reference boundary"
)
forbid_source_text(
  "${DIRECTORY_CONTAINER_HEADER}"
  "static LVDirectoryContainer * OpenDirectory"
  "directory factories must not return implicit raw ownership"
)
forbid_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "LVDirectoryContainer * dir = new"
  "directory factory rollback must remain scope-bound"
)
forbid_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "LVDirectoryContainerItemInfo * item = new"
  "directory item construction must not use implicit raw ownership"
)
forbid_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "DIR * d = opendir"
  "directory scans must not regress to raw DIR ownership"
)
forbid_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "FindClose( hFind )"
  "Windows directory cleanup must remain guard-owned"
)
forbid_source_text(
  "${DIRECTORY_CONTAINER_SOURCE}"
  "delete dir"
  "directory factory failure must not require manual deletion"
)

# --- archive-container factory, item and stream-state ownership ---
require_source_text(
  "${ARCHIVE_CONTAINER_HEADER}"
  "LVArcContainerBase(const LVArcContainerBase &) = delete"
  "archive containers must not copy their owning stream and item list"
)
require_source_text(
  "${ZIP_ARCHIVE_HEADER}"
  "static std::unique_ptr<LVZipArc> OpenArchieve"
  "ZIP factories must return scoped ownership"
)
require_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "std::unique_ptr<LVCommonContainerItemInfo> item"
  "ZIP entry metadata must stay scoped until list adoption"
)
require_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "m_list.add(item.release())"
  "ZIP entry ownership must transfer only at the owning list boundary"
)
require_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "std::unique_ptr<LVZipArc> arc"
  "ZIP factory candidates must remain scope-owned during parser retry"
)
require_source_text(
  "${RAR_ARCHIVE_HEADER}"
  "static std::unique_ptr<LVRarArc> OpenArchieve"
  "RAR factory parity must preserve scoped ownership"
)
require_source_text(
  "${RAR_ARCHIVE_SOURCE}"
  "std::unique_ptr<LVRarArc> arc"
  "RAR factory candidates must remain scope-owned"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "class LVStreamPositionGuard"
  "archive probing must scope caller stream-position state"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "_stream->Seek(_position, LVSEEK_SET, NULL)"
  "archive probing must restore caller stream position"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "std::unique_ptr<LVZipArc> zip"
  "ZIP candidates must stay scoped until the container reference boundary"
)
require_source_text(
  "${STREAM_UTILS_SOURCE}"
  "return LVContainerRef(zip.release())"
  "ZIP ownership must transfer only into LVContainerRef"
)
forbid_source_text(
  "${ZIP_ARCHIVE_HEADER}"
  "static LVArcContainerBase * OpenArchieve"
  "ZIP factories must not return implicit raw ownership"
)
forbid_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "LVCommonContainerItemInfo *item = new"
  "ZIP entries must not regress to implicit raw ownership"
)
forbid_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "LVZipArc * arc = new"
  "ZIP factory rollback must remain automatic"
)
forbid_source_text(
  "${ZIP_ARCHIVE_SOURCE}"
  "delete arc"
  "ZIP factory failure must not require manual deletion"
)
forbid_source_text(
  "${RAR_ARCHIVE_SOURCE}"
  "LVRarArc * arc = new"
  "RAR factory rollback must remain automatic"
)
forbid_source_text(
  "${RAR_ARCHIVE_SOURCE}"
  "delete arc"
  "RAR factory failure must not require manual deletion"
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

# --- history parser and synchronization-record ownership ---
require_source_text(
  "${HISTORY_HEADER}"
  "std::unique_ptr<CRBookmark> _bookmark"
  "change records must own bookmark snapshots explicitly"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "std::unique_ptr<CRBookmark> _curr_bookmark"
  "history parsing must scope the current bookmark candidate"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "std::unique_ptr<CRFileHistRecord> _curr_file"
  "history parsing must scope the current file candidate"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "_hist->getRecords().add( _curr_file.release() )"
  "history file ownership must transfer only at the owning-list boundary"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "_curr_file->getBookmarks().add(_curr_bookmark.release())"
  "history bookmark ownership must transfer only at the owning-list boundary"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "_records.swap(candidate._records)"
  "history loads must publish only a complete candidate snapshot"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "std::unique_ptr<ChangeInfo> ci"
  "change record factories must keep parse candidates scope-owned"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "return ci.release()"
  "change record ownership must transfer only at the legacy factory boundary"
)
require_source_text(
  "${HISTORY_SOURCE}"
  "char ch = text[i]"
  "change record escape decoding must read from encoded input"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testHistoryOwnership()"
  "history candidate ownership must retain native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "failed history load published its valid prefix"
  "history regression must retain failure rollback coverage"
)
forbid_source_text(
  "${HISTORY_HEADER}"
  "CRBookmark * _bookmark"
  "change records must not own bookmarks through raw pointers"
)
forbid_source_text(
  "${HISTORY_SOURCE}"
  "CRBookmark * _curr_bookmark"
  "history parsing must not own bookmark candidates through raw pointers"
)
forbid_source_text(
  "${HISTORY_SOURCE}"
  "CRFileHistRecord * _curr_file"
  "history parsing must not own file candidates through raw pointers"
)
forbid_source_text(
  "${HISTORY_SOURCE}"
  "delete _curr_bookmark"
  "history candidate teardown must remain automatic"
)
forbid_source_text(
  "${HISTORY_SOURCE}"
  "ChangeInfo * ci = new"
  "change record factory rollback must remain automatic"
)

# --- global i18n translator ownership and publication ---
require_source_text(
  "${I18N_HEADER}"
  "static std::unique_ptr<CRI18NTranslator> _translator"
  "the active translator slot must own its object explicitly"
)
require_source_text(
  "${I18N_HEADER}"
  "static std::unique_ptr<CRI18NTranslator> _defTranslator"
  "the fallback translator slot must own its object explicitly"
)
require_source_text(
  "${I18N_SOURCE}"
  "_translator.reset(translator)"
  "active translator replacement must release the previous owner automatically"
)
require_source_text(
  "${I18N_SOURCE}"
  "_defTranslator.reset(translator)"
  "fallback translator replacement must release the previous owner automatically"
)
require_source_text(
  "${I18N_SOURCE}"
  "_translator = std::move(_defTranslator)"
  "moving a fallback translator active must preserve exclusive ownership"
)
require_source_text(
  "${I18N_SOURCE}"
  "_defTranslator = std::move(_translator)"
  "moving an active translator to fallback must preserve exclusive ownership"
)
require_source_text(
  "${JINKE_SOURCE}"
  "std::unique_ptr<CRMoFileTranslator> t"
  "Jinke translator candidates must remain scope-owned until publication"
)
require_source_text(
  "${JINKE_SOURCE}"
  "setTranslator( t.release() )"
  "Jinke translation ownership must transfer only at the global slot boundary"
)
require_source_text(
  "${NANOX_SOURCE}"
  "std::unique_ptr<CRMoFileTranslator> t"
  "NanoX translator candidates must remain scope-owned until publication"
)
require_source_text(
  "${NANOX_SOURCE}"
  "setTranslator( t.release() )"
  "NanoX translation ownership must transfer only at the global slot boundary"
)
require_source_text(
  "${POCKETBOOK_SOURCE}"
  "std::unique_ptr<CRMoFileTranslator> t"
  "PocketBook translator candidates must remain scope-owned until publication"
)
require_source_text(
  "${POCKETBOOK_SOURCE}"
  "setTranslator( t.release() )"
  "PocketBook translation ownership must transfer only at the global slot boundary"
)
require_source_text(
  "${WIN_GUI_SOURCE}"
  "std::unique_ptr<CRMoFileTranslator> translator"
  "Windows translator candidates must remain scope-owned until publication"
)
require_source_text(
  "${WIN_GUI_SOURCE}"
  "setTranslator( translator.release() )"
  "Windows translation ownership must transfer only at the global slot boundary"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testTranslatorOwnerLifecycle()"
  "translator slot ownership must retain native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "translator slot transfer duplicated exclusive ownership"
  "translator regression must retain cross-slot ownership coverage"
)
forbid_source_text(
  "${I18N_HEADER}"
  "static CRI18NTranslator * _translator"
  "the active translator slot must not own a raw pointer"
)
forbid_source_text(
  "${I18N_HEADER}"
  "static CRI18NTranslator * _defTranslator"
  "the fallback translator slot must not own a raw pointer"
)
forbid_source_text(
  "${I18N_SOURCE}"
  "delete _translator"
  "active translator teardown must remain automatic"
)
forbid_source_text(
  "${I18N_SOURCE}"
  "delete _defTranslator"
  "fallback translator teardown must remain automatic"
)
forbid_source_text(
  "${JINKE_SOURCE}"
  "delete t"
  "Jinke translator failure cleanup must remain automatic"
)
forbid_source_text(
  "${NANOX_SOURCE}"
  "delete t"
  "NanoX translator failure cleanup must remain automatic"
)
forbid_source_text(
  "${POCKETBOOK_SOURCE}"
  "delete t"
  "PocketBook translator failure cleanup must remain automatic"
)

# --- property stream buffer, candidate and output ownership ---
require_source_text(
  "${PROPERTIES_SOURCE}"
  "std::vector<char> buf"
  "property input bytes must use scoped container ownership"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "sz > ParseBudgetLimits::defaults().maxInputBytes"
  "property input allocation must remain bounded"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "bytesRead != sz"
  "property loading must reject short successful reads"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "CRPropRef candidate = clone()"
  "property loading must stage updates in a scoped candidate"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "candidate->setString("
  "property parsing must not mutate the live container incrementally"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "set(candidate)"
  "property loading must publish only its complete candidate"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "if ( i + 1 >= str.length() )"
  "property escape decoding must bound a trailing backslash"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "lString8 snapshot("
  "property saving must stage output in owned string storage"
)
require_source_text(
  "${PROPERTIES_SOURCE}"
  "bytesWritten == static_cast<lvsize_t>(snapshot.length())"
  "property saving must require an exact target write"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testPropertyStreamOwnership()"
  "property stream ownership must retain native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "short property read published a partial snapshot"
  "property regression must retain input rollback coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "property save accepted a short target write"
  "property regression must retain exact output coverage"
)
forbid_source_text(
  "${PROPERTIES_SOURCE}"
  "char * buf = new char"
  "property input must not regress to manual array ownership"
)
forbid_source_text(
  "${PROPERTIES_SOURCE}"
  "delete[] buf"
  "property input cleanup must remain automatic"
)
forbid_source_text(
  "${PROPERTIES_SOURCE}"
  "LVPumpStream( targetStream, stream )"
  "property saving must not hide target write failures behind an unchecked pump"
)

# --- XML/HTML document factories and FB3/OPC ownership ---
require_source_text(
  "${DOM_SOURCE}"
  "std::unique_ptr<ldomDocument> doc(new ldomDocument())"
  "XML and HTML document factories must retain parse candidates automatically"
)
require_source_text(
  "${DOM_SOURCE}"
  "LVXMLParser parser(stream, &writer)"
  "the XML document factory must scope its parser"
)
require_source_text(
  "${DOM_SOURCE}"
  "LVHTMLParser parser(stream, &writerFilter)"
  "the HTML document factory must scope its parser"
)
require_source_text(
  "${DOM_SOURCE}"
  "return doc.release()"
  "document factories must transfer ownership only at their legacy raw-return boundary"
)
require_source_text(
  "${OPC_HEADER}"
  "std::vector<std::unique_ptr<RelationTable> > m_relationOwners"
  "OPC relation tables must have one explicit owner collection"
)
require_source_text(
  "${OPC_HEADER}"
  "LVHashTable<lString32, RelationTable *> m_relations"
  "the OPC relation hash must remain a non-owning lookup index"
)
require_source_text(
  "${OPC_HEADER}"
  "std::unique_ptr<OpcPart> createPart"
  "OPC part factories must retain candidates until reference transfer"
)
require_source_text(
  "${OPC_SOURCE}"
  "std::vector<std::unique_ptr<RelationTable> > relationOwners"
  "OPC relation parsing must build a scoped ownership candidate"
)
require_source_text(
  "${OPC_SOURCE}"
  "m_relationOwners.swap(relationOwners)"
  "OPC relation owners must publish only after complete parsing"
)
require_source_text(
  "${OPC_SOURCE}"
  "m_relations.swap(relations)"
  "OPC relation lookup views must publish with their owners"
)
require_source_text(
  "${OPC_SOURCE}"
  "std::unique_ptr<ldomDocument> propertiesDoc"
  "OPC core-property parse documents must remain scope-owned"
)
require_source_text(
  "${FB3_HEADER}"
  "std::unique_ptr<ldomDocument> m_descDoc"
  "the FB3 import context must own its cached description document"
)
require_source_text(
  "${FB3_SOURCE}"
  "m_descDoc.reset(LVParseXMLStream(descStream))"
  "the FB3 description factory result must enter its owner immediately"
)
require_source_text(
  "${FB3_SOURCE}"
  "return m_descDoc.get()"
  "the FB3 description accessor must expose only a non-owning view"
)
require_source_text(
  "${FB3_SOURCE}"
  "LVXMLParser parser(bookStream, &fb3Writer)"
  "the FB3 body parser must use automatic lifetime"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testOpcFb3Ownership()"
  "FB3/OPC ownership must retain end-to-end native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "FB3 parser published a depth-budget failure"
  "FB3 ownership regression must retain failure cleanup coverage"
)
forbid_source_text(
  "${OPC_HEADER}"
  "LVHashTable<lString32, LVHashTable<lString32, lString32> *> m_relations"
  "OPC relation tables must not regress to raw owning hash values"
)
forbid_source_text(
  "${OPC_SOURCE}"
  "delete relationsTable"
  "OPC relation teardown must remain automatic"
)
forbid_source_text(
  "${FB3_HEADER}"
  "ldomDocument *m_descDoc"
  "the FB3 description cache must not own a raw document pointer"
)
forbid_source_text(
  "${FB3_SOURCE}"
  "delete  m_descDoc"
  "FB3 description teardown must remain automatic"
)
forbid_source_text(
  "${FB3_SOURCE}"
  "LVFileFormatParser * parser = new LVXMLParser"
  "FB3 parser teardown must remain automatic"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "LVFileFormatParser * parser = new LVXMLParser(stream, &writer)"
  "the XML document factory must not regress to a manually deleted parser"
)
forbid_source_text(
  "${DOM_SOURCE}"
  "LVFileFormatParser * parser = new LVHTMLParser(stream, &writerFilter)"
  "the HTML document factory must not regress to a manually deleted parser"
)

# --- CHM file, metadata and document ownership ---
require_source_text(
  "${CHM_SOURCE}"
  "class LVCHMFile : public LVRefCounter"
  "the external CHM handle must have one reference-counted lifetime anchor"
)
require_source_text(
  "${CHM_SOURCE}"
  "chm_close(_file)"
  "the CHM handle owner must pair external teardown in its destructor"
)
require_source_text(
  "${CHM_SOURCE}"
  "typedef LVFastRef<LVCHMFile> LVCHMFileRef"
  "CHM containers and entry streams must share the engine lifetime anchor"
)
require_source_text(
  "${CHM_SOURCE}"
  "std::unique_ptr<LVCHMStream> candidate"
  "CHM entry factories must retain candidates until stream publication"
)
require_source_text(
  "${CHM_SOURCE}"
  "_file = LVCHMFileRef(file.release())"
  "the CHM handle must transfer only after external open succeeds"
)
require_source_text(
  "${CHM_SOURCE}"
  "std::unique_ptr<LVCHMContainer> chm"
  "CHM container factories must retain candidates until reference transfer"
)
require_source_text(
  "${CHM_SOURCE}"
  "std::unique_ptr<CHMUrlStr> _strings"
  "the CHM URL table must own its string table explicitly"
)
require_source_text(
  "${CHM_SOURCE}"
  "std::unique_ptr<CHMUrlTable> _urlTable"
  "the CHM system metadata must own its URL table explicitly"
)
require_source_text(
  "${CHM_SOURCE}"
  "static std::unique_ptr<CHMUrlStr> open"
  "CHM URL-string parsing must return a scoped candidate"
)
require_source_text(
  "${CHM_SOURCE}"
  "static std::unique_ptr<CHMUrlTable> open"
  "CHM URL-table parsing must return a scoped candidate"
)
require_source_text(
  "${CHM_SOURCE}"
  "static std::unique_ptr<CHMSystem> open"
  "CHM system parsing must return a scoped candidate"
)
require_source_text(
  "${CHM_SOURCE}"
  "LVHTMLParser parser(stream, &writerFilter)"
  "the CHM HTML document parser must use automatic lifetime"
)
require_source_text(
  "${CHM_SOURCE}"
  "return doc.release()"
  "the CHM HTML factory must transfer only a complete document"
)
require_source_text(
  "${CHM_SOURCE}"
  "std::unique_ptr<ldomDocument> doc("
  "CHM TOC and HTML parse documents must remain scope-owned"
)
require_source_text(
  "${CHM_SOURCE}"
  "std::unique_ptr<CHMSystem> chm = CHMSystem::open(cont)"
  "CHM import metadata must remain owned through value extraction"
)
require_source_text(
  "${CHM_INTERNAL_HEADER}"
  "bool LVRunChmMetadataOwnershipRegression()"
  "CHM metadata ownership must expose its native regression seam"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testChmOwnership()"
  "CHM ownership must retain end-to-end native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "CHM entry did not retain its CHM file owner"
  "CHM regression must retain detached entry lifetime coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "CHM HTML factory published a rejected candidate"
  "CHM regression must retain document factory rollback coverage"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "LVCHMStream * p = new LVCHMStream"
  "CHM entry creation must not regress to a raw candidate"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "LVCHMContainer * chm = new LVCHMContainer"
  "CHM container creation must not regress to a raw candidate"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "CHMUrlStr * _strings"
  "the CHM URL string table must not be a raw owning field"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "CHMUrlTable * _urlTable"
  "the CHM URL table must not be a raw owning field"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "CHMSystem * chm = CHMSystem::open"
  "CHM import metadata must not be a raw owning candidate"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "LVFileFormatParser * parser = new LVHTMLParser"
  "CHM HTML parser teardown must remain automatic"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "delete _strings"
  "CHM URL-string teardown must remain automatic"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "delete _urlTable"
  "CHM URL-table teardown must remain automatic"
)
forbid_source_text(
  "${CHM_SOURCE}"
  "delete chm"
  "CHM container and metadata teardown must remain automatic"
)

# --- EPUB encrypted-container, stream-key and parse-candidate ownership ---
require_source_text(
  "${EPUB_SOURCE}"
  "LVArray<lUInt8> _key"
  "EPUB font demangling streams must own a stable key snapshot"
)
require_source_text(
  "${EPUB_SOURCE}"
  "_base->Read(buf, count, &bytesRead)"
  "EPUB font demangling must observe the base stream's actual read count"
)
require_source_text(
  "${EPUB_SOURCE}"
  "i < bytesRead"
  "EPUB font demangling must transform only bytes returned by the base stream"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::vector<std::unique_ptr<EncryptedItem> > _items"
  "EPUB encryption parsing must retain item candidates automatically"
)
require_source_text(
  "${EPUB_SOURCE}"
  "uri = attrvalue"
  "EPUB encryption parsing must retain the cipher-reference URI"
)
require_source_text(
  "${EPUB_SOURCE}"
  "algorithm = attrvalue"
  "EPUB encryption parsing must retain the encryption method"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::vector<std::unique_ptr<EncryptedItem> > _list"
  "EPUB encrypted-item storage must have one explicit owner collection"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::vector<std::unique_ptr<EncryptedItem> > items"
  "EPUB encryption metadata must parse into a scoped candidate"
)
require_source_text(
  "${EPUB_SOURCE}"
  "_list.swap(items)"
  "EPUB encryption metadata must publish only a complete candidate"
)
require_source_text(
  "${EPUB_SOURCE}"
  "lString32 actual = item->_uri"
  "EPUB encrypted-item matching must normalize the stored URI"
)
require_source_text(
  "${EPUB_SOURCE}"
  "if (actual == expected)"
  "EPUB encrypted-item matching must compare the URI with the requested path"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::unique_ptr<EncryptedDataContainer> decryptor"
  "EPUB decryptor candidates must remain scoped until container publication"
)
require_source_text(
  "${EPUB_SOURCE}"
  "EncryptedDataContainer *decryptorView = decryptor.get()"
  "EPUB imports must label the decryptor access retained by the container as borrowed"
)
require_source_text(
  "${EPUB_SOURCE}"
  "LVContainerRef m_arc(decryptor.release())"
  "EPUB decryptors must transfer ownership only into LVContainerRef"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::unique_ptr<ldomDocument> navDoc"
  "EPUB navigation DOM candidates must remain scope-owned"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::unique_ptr<ldomDocument> ncxdoc"
  "EPUB NCX DOM candidates must remain scope-owned"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::unique_ptr<ldomDocument> pagemapdoc"
  "EPUB page-map DOM candidates must remain scope-owned"
)
require_source_text(
  "${EPUB_SOURCE}"
  "std::unique_ptr<EpubItem> epubItem"
  "EPUB manifest item candidates must remain scope-owned"
)
require_source_text(
  "${EPUB_SOURCE}"
  "epubItems.reserve(epubItems.length() + 1)"
  "EPUB manifest owners must reserve before crossing the legacy list boundary"
)
require_source_text(
  "${EPUB_SOURCE}"
  "epubItems.add(epubItem.release())"
  "EPUB manifest ownership must transfer only at the owning-list boundary"
)
require_source_text(
  "${EPUB3_REGRESSION_SOURCE}"
  "static int testEncryptedFontOwnership()"
  "EPUB encrypted stream ownership must retain native regression coverage"
)
require_source_text(
  "${EPUB3_REGRESSION_SOURCE}"
  "font demangling stream did not retain its key snapshot"
  "EPUB regression must retain detached demangling-stream coverage"
)
require_source_text(
  "${EPUB3_REGRESSION_SOURCE}"
  "failed encryption parse published its valid prefix"
  "EPUB regression must retain failed-parse rollback coverage"
)
require_source_text(
  "${EPUB3_REGRESSION_SOURCE}"
  "EPUB cover factory did not retain its stream owner"
  "EPUB regression must retain detached cover-stream coverage"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "LVArray<lUInt8> & _key"
  "EPUB demangling streams must not borrow a container-owned key"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "EncryptedItemCallback"
  "EPUB encryption parsing must not publish entries incrementally"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "LVPtrVector<EncryptedItem> _list"
  "EPUB encrypted items must not regress to implicit legacy ownership"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "EncryptedDataContainer * decryptor = new"
  "EPUB decryptor rollback must remain automatic"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "ldomDocument * doc = LVParseXMLStream"
  "EPUB document candidates must not regress to manual ownership"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "ldomDocument * navDoc = LVParseXMLStream"
  "EPUB navigation documents must not regress to manual ownership"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "ldomDocument * ncxdoc = LVParseXMLStream"
  "EPUB NCX documents must not regress to manual ownership"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "ldomDocument * pagemapdoc = LVParseXMLStream"
  "EPUB page-map documents must not regress to manual ownership"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "EpubItem * epubItem = new"
  "EPUB manifest candidates must not regress to raw ownership"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "delete doc"
  "EPUB document teardown must remain automatic"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "delete navDoc"
  "EPUB navigation document teardown must remain automatic"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "delete ncxdoc"
  "EPUB NCX document teardown must remain automatic"
)
forbid_source_text(
  "${EPUB_SOURCE}"
  "delete pagemapdoc"
  "EPUB page-map document teardown must remain automatic"
)

# --- LVDocView primary document ownership and lifecycle ---
require_source_text(
  "${DOC_VIEW_HEADER}"
  "std::unique_ptr<ldomDocument> m_doc"
  "LVDocView must own its primary document explicitly"
)
require_source_text(
  "${DOC_VIEW_HEADER}"
  "return m_doc.get()"
  "LVDocView must expose only a borrowed document view"
)
require_source_text(
  "${DOC_VIEW_HEADER}"
  "LVDocView(const LVDocView &) = delete"
  "LVDocView must not duplicate primary document ownership"
)
require_source_text(
  "${DOC_VIEW_HEADER}"
  "LVDocView &operator=(const LVDocView &) = delete"
  "LVDocView must not assign primary document ownership"
)
require_source_text(
  "${DOC_VIEW_SOURCE}"
  "m_doc.reset();"
  "LVDocView clear must release its document idempotently"
)
require_source_text(
  "${DOC_VIEW_SOURCE}"
  "m_doc.reset(new ldomDocument())"
  "LVDocView replacement documents must enter their owner immediately"
)
require_source_text(
  "${DOC_VIEW_SOURCE}"
  "m_stream, m_doc.get(), m_callback, this"
  "LVDocView importers must receive an explicitly borrowed document view"
)
require_source_text(
  "${DOC_VIEW_SOURCE}"
  "ldomDocumentWriter writer(m_doc.get())"
  "LVDocView writers must receive an explicitly borrowed document view"
)
require_source_text(
  "${DOCUMENT_REGRESSION_SOURCE}"
  "static int testDocViewDocumentOwnership()"
  "LVDocView document ownership must retain lifecycle regression coverage"
)
require_source_text(
  "${DOCUMENT_REGRESSION_SOURCE}"
  "LVDocView replacement retained nodes from its released document"
  "LVDocView regression must retain clean replacement coverage"
)
require_source_text(
  "${DOCUMENT_REGRESSION_SOURCE}"
  "LVDocView repeated Clear lost document-owner idempotence"
  "LVDocView regression must retain repeated teardown coverage"
)
forbid_source_text(
  "${DOC_VIEW_HEADER}"
  "ldomDocument * m_doc"
  "LVDocView must not own its primary document through a raw pointer"
)
forbid_source_text(
  "${DOC_VIEW_HEADER}"
  "return m_doc;"
  "LVDocView must not expose its owner without an explicit borrowed boundary"
)
forbid_source_text(
  "${DOC_VIEW_SOURCE}"
  "delete m_doc"
  "LVDocView document teardown must remain automatic"
)
forbid_source_text(
  "${DOC_VIEW_SOURCE}"
  "m_doc = new ldomDocument()"
  "LVDocView document replacement must remain scope-bound"
)

# --- ODT metadata DOM, style candidates and borrowed parse context ---
require_source_text(
  "${ODT_SOURCE}"
  "std::unique_ptr<ldomDocument> metaDoc"
  "ODT metadata documents must remain scope-owned"
)
require_source_text(
  "${ODT_SOURCE}"
  "odtImportContext &m_context"
  "ODT handlers must retain non-null borrowed context references"
)
require_source_text(
  "${ODT_SOURCE}"
  "static bool parseStyles(odtImportContext &context)"
  "ODT style parsing must label its context as borrowed"
)
require_source_text(
  "${ODT_SOURCE}"
  "m_styleRef = odx_StyleRef( new odx_Style );
        break;"
  "ODT paragraph styles must not fall through into unrelated list ownership"
)
require_source_text(
  "${ODT_SOURCE}"
  "m_context.addStyle(m_styleRef)"
  "ODT style candidates must publish through their intrusive owner"
)
require_source_text(
  "${ODT_SOURCE}"
  "m_context.addListStyle(m_ListStyleRef)"
  "ODT list-style candidates must publish through their intrusive owner"
)
require_source_text(
  "${ODT_SOURCE}"
  "m_ListLevelStyleRef.Clear()"
  "ODT list-level handler ownership must end after publication"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testOdtOwnership()"
  "ODT import ownership must retain end-to-end native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "ODT style candidates survived a rejected parse"
  "ODT regression must retain failed-style cleanup coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "ODT rejected parse retained shared ownership state"
  "ODT regression must retain clean repeated-import coverage"
)
forbid_source_text(
  "${ODT_SOURCE}"
  "ldomDocument * metaDoc = LVParseXMLStream"
  "ODT metadata parsing must not regress to raw document ownership"
)
forbid_source_text(
  "${ODT_SOURCE}"
  "delete metaDoc"
  "ODT metadata document teardown must remain automatic"
)
forbid_source_text(
  "${ODT_SOURCE}"
  "odx_Style *m_style"
  "ODT style handlers must not shadow their intrusive owner with a raw field"
)
forbid_source_text(
  "${ODT_SOURCE}"
  "odt_ListStyle *m_ListStyle"
  "ODT list-style handlers must not shadow their intrusive owner with a raw field"
)
forbid_source_text(
  "${ODT_SOURCE}"
  "odt_ListLevelStyle *m_ListLevelStyle"
  "ODT list-level handlers must not shadow their intrusive owner with a raw field"
)
forbid_source_text(
  "${ODT_SOURCE}"
  "odtImportContext *"
  "ODT parse handlers must not use nullable context pointers"
)

# --- Skin DOM and factory/icon/button parse candidates ---
require_source_text(
  "${SKIN_SOURCE}"
  "std::unique_ptr<ldomDocument> _doc"
  "skin XML documents must remain scope-owned"
)
require_source_text(
  "${SKIN_SOURCE}"
  "std::unique_ptr<ldomDocument> doc(LVParseXMLStream(stream))"
  "skin XML parse results must enter their owner immediately"
)
require_source_text(
  "${SKIN_SOURCE}"
  "CRIconSkinRef icon(new CRIconSkin())"
  "skin icon candidates must remain in their intrusive owner"
)
require_source_text(
  "${SKIN_SOURCE}"
  "LVRef<CRButtonSkin> button(new CRButtonSkin())"
  "skin button candidates must remain in their intrusive owner"
)
require_source_text(
  "${SKIN_SOURCE}"
  "std::unique_ptr<CRSkinImpl> skin(new CRSkinImpl())"
  "skin factory candidates must remain scope-owned until publication"
)
require_source_text(
  "${SKIN_SOURCE}"
  "return CRSkinRef(skin.release())"
  "skin factories must publish only successfully opened candidates"
)
require_source_text(
  "${SKIN_SOURCE}"
  "std::unique_ptr<CRSkinListItem> item(new CRSkinListItem())"
  "skin-list candidates must remain scope-owned until legacy publication"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testSkinOwnership()"
  "skin ownership must retain end-to-end native regression coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "skin DOM survived a rejected parse"
  "skin ownership regression must retain failed-DOM cleanup coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "rejected skin parse retained shared ownership state"
  "skin ownership regression must retain repeated-load coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "skin container/list owners did not publish valid state"
  "skin ownership regression must retain directory/list factory coverage"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "LVAutoPtr<ldomDocument> _doc"
  "skin XML ownership must not regress to the legacy auto pointer"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "ldomDocument * doc = LVParseXMLStream"
  "skin XML parsing must not expose a raw owning document"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "CRIconSkin * icon = new CRIconSkin()"
  "skin icon candidates must not use manual cleanup"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "CRButtonSkin * button = new CRButtonSkin()"
  "skin button candidates must not use manual cleanup"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "CRSkinImpl * skin = new CRSkinImpl()"
  "skin factory candidates must not use raw ownership"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "delete icon"
  "skin icon candidate teardown must remain automatic"
)
forbid_source_text(
  "${SKIN_SOURCE}"
  "delete button"
  "skin button candidate teardown must remain automatic"
)

require_source_text(
  "${GUI_HEADER}"
  "std::unique_ptr<CRGUIScreen> _ownedScreen"
  "GUI window managers must keep an explicit screen owner"
)
require_source_text(
  "${GUI_HEADER}"
  "std::unique_ptr<CRGUIScreen> _ownedScreen;
        std::vector<std::unique_ptr<CRGUIWindow> > _windows;"
  "the GUI screen owner must outlive dependent window state"
)
require_source_text(
  "${GUI_HEADER}"
  "Borrowed compatibility view, backed by _ownedScreen when non-null"
  "the legacy GUI screen pointer must remain documented as borrowed"
)
require_source_text(
  "${GUI_HEADER}"
  "void setOwnedScreen( std::unique_ptr<CRGUIScreen> screen )"
  "GUI screen adoption must use one explicit ownership boundary"
)
require_source_text(
  "${GUI_HEADER}"
  "_screen = screen.get();"
  "GUI screen adoption must publish only an owner-backed view"
)
require_source_text(
  "${GUI_HEADER}"
  "_ownedScreen = std::move( screen );"
  "GUI screen adoption must transfer exclusive ownership"
)
require_source_text(
  "${GUI_HEADER}"
  "void setBorrowedScreen( CRGUIScreen * screen )"
  "GUI managers must distinguish borrowed screens explicitly"
)
require_source_text(
  "${GUI_HEADER}"
  "_ownedScreen.reset();"
  "switching to a borrowed GUI screen must release the previous owner"
)
require_source_text(
  "${GUI_HEADER}"
  "CRGUIWindowManager( const CRGUIWindowManager & ) = delete"
  "GUI screen ownership must not be shallow-copied"
)
require_source_text(
  "${QT_GUI_SOURCE}"
  "setOwnedScreen( std::unique_ptr<CRGUIScreen>("
  "Qt window managers must adopt their created screen"
)
require_source_text(
  "${WIN_GUI_SOURCE}"
  "setOwnedScreen( std::unique_ptr<CRGUIScreen>("
  "Win32 window managers must adopt their created screen"
)
require_source_text(
  "${JINKE_SOURCE}"
  "setOwnedScreen( std::unique_ptr<CRGUIScreen>("
  "Jinke window managers must adopt their created screen"
)
require_source_text(
  "${XCB_GUI_SOURCE}"
  "setOwnedScreen( std::unique_ptr<CRGUIScreen>("
  "XCB window managers must adopt their created screen"
)
require_source_text(
  "${POCKETBOOK_SOURCE}"
  "setOwnedScreen( std::unique_ptr<CRGUIScreen>("
  "PocketBook window managers must adopt their created screen"
)
require_source_text(
  "${NANOX_SOURCE}"
  "setOwnedScreen( std::unique_ptr<CRGUIScreen>("
  "NanoX window managers must adopt a newly created screen"
)
require_source_text(
  "${NANOX_SOURCE}"
  "setBorrowedScreen( CRJinkeScreen::instance );"
  "NanoX window managers must borrow an existing singleton screen"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testGuiScreenOwnership()"
  "GUI screen ownership must retain native lifecycle coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "GUI manager destroyed its borrowed screen"
  "GUI screen regression must retain borrowed teardown coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "GUI manager did not replace its owned screen"
  "GUI screen regression must retain owned replacement coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "GUI manager did not release ownership before borrowing"
  "GUI screen regression must retain owned-to-borrowed coverage"
)
forbid_source_text(
  "${GUI_HEADER}"
  "_ownScreen"
  "GUI screen ownership must not regress to a boolean side channel"
)
forbid_source_text(
  "${GUI_HEADER}"
  "delete _screen"
  "GUI screen teardown must remain automatic"
)
forbid_source_text(
  "${QT_GUI_SOURCE}"
  "_ownScreen"
  "Qt GUI screen ownership must not use the legacy boolean"
)
forbid_source_text(
  "${WIN_GUI_SOURCE}"
  "_ownScreen"
  "Win32 GUI screen ownership must not use the legacy boolean"
)
forbid_source_text(
  "${JINKE_SOURCE}"
  "_ownScreen"
  "Jinke GUI screen ownership must not use the legacy boolean"
)
forbid_source_text(
  "${XCB_GUI_SOURCE}"
  "_ownScreen"
  "XCB GUI screen ownership must not use the legacy boolean"
)
forbid_source_text(
  "${POCKETBOOK_SOURCE}"
  "_ownScreen"
  "PocketBook GUI screen ownership must not use the legacy boolean"
)
forbid_source_text(
  "${NANOX_SOURCE}"
  "_ownScreen"
  "NanoX GUI screen ownership must not use the legacy boolean"
)
forbid_source_text(
  "${NANOX_SOURCE}"
  "_screen = new CRJinkeScreen"
  "NanoX GUI screen creation must not bypass the owner"
)
forbid_source_text(
  "${GUI_PLATFORM_OWNERSHIP_SOURCE}"
  "_screen ="
  "platform GUI managers must use explicit owned or borrowed screen transitions"
)

require_source_text(
  "${GUI_HEADER}"
  "std::vector<std::unique_ptr<CRGUIWindow> > _windows"
  "GUI window managers must own window stack entries explicitly"
)
require_source_text(
  "${GUI_HEADER}"
  "std::vector<std::unique_ptr<CRGUIEvent> > _events"
  "GUI window managers must own queued events explicitly"
)
require_source_text(
  "${GUI_HEADER}"
  "std::unique_ptr<CRGUIEvent> takeEvent()"
  "GUI event dispatch must transfer a scoped event owner"
)
require_source_text(
  "${GUI_HEADER}"
  "return takeEvent().release();"
  "the legacy GUI event API must remain an explicit transfer boundary"
)
require_source_text(
  "${GUI_SOURCE}"
  "std::unique_ptr<CRGUIWindow> owner( window );"
  "GUI window activation must adopt new windows before publication"
)
require_source_text(
  "${GUI_SOURCE}"
  "owner = std::move( _windows[static_cast<size_t>( index )] );"
  "GUI window close must retain ownership through lifecycle callbacks"
)
require_source_text(
  "${GUI_SOURCE}"
  "owner->closing();
    owner.reset();"
  "GUI window close must destroy its scoped owner before refocusing"
)
require_source_text(
  "${GUI_SOURCE}"
  "std::unique_ptr<CRGUIEvent> owner( event );"
  "GUI event posting must adopt its raw argument immediately"
)
require_source_text(
  "${GUI_SOURCE}"
  "_events.erase( _events.begin() + i );"
  "GUI event deduplication must release replaced owners automatically"
)
require_source_text(
  "${GUI_SOURCE}"
  "std::unique_ptr<CRGUIEvent> event = takeEvent();"
  "GUI event dispatch must keep each event scoped through handling"
)
require_source_text(
  "${GUI_HEADER}"
  "std::unique_ptr<LVDocView> _docview"
  "GUI document windows must own their document view explicitly"
)
require_source_text(
  "${GUI_HEADER}"
  "return _docview.get();"
  "the GUI document-view getter must expose only a borrowed view"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "static int testGuiRuntimeOwnership()"
  "GUI runtime ownership must retain native lifecycle coverage"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "GUI manager teardown leaked its final window"
  "GUI window teardown regression coverage must be retained"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "GUI manager teardown leaked a queued event"
  "GUI event teardown regression coverage must be retained"
)
require_source_text(
  "${CORE_SAFETY_SOURCE}"
  "GUI document window leaked its document view"
  "GUI document-view teardown regression coverage must be retained"
)
forbid_source_text(
  "${GUI_HEADER}"
  "LVPtrVector<CRGUIWindow, true> _windows"
  "GUI window ownership must not regress to a raw-pointer container"
)
forbid_source_text(
  "${GUI_HEADER}"
  "LVPtrVector<CRGUIEvent, true> _events"
  "GUI event ownership must not regress to a raw-pointer container"
)
forbid_source_text(
  "${GUI_SOURCE}"
  "delete window"
  "GUI window teardown must remain automatic"
)
forbid_source_text(
  "${GUI_SOURCE}"
  "delete event"
  "GUI event teardown must remain automatic"
)
forbid_source_text(
  "${GUI_HEADER}"
  "delete _docview"
  "GUI document-view teardown must remain automatic"
)
