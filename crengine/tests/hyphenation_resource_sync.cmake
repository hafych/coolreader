if(NOT DEFINED SOURCE_ROOT)
  message(FATAL_ERROR "SOURCE_ROOT is required")
endif()

file(GLOB desktop_patterns
  "${SOURCE_ROOT}/cr3gui/data/hyph/*.pattern"
)
file(GLOB android_patterns
  "${SOURCE_ROOT}/android/res/raw/hyph_*.pattern"
)
list(LENGTH desktop_patterns desktop_count)
list(LENGTH android_patterns android_count)
if(NOT desktop_count EQUAL android_count)
  message(FATAL_ERROR
    "Hyphenation resource count differs: desktop=${desktop_count}, "
    "Android=${android_count}"
  )
endif()

foreach(desktop_pattern IN LISTS desktop_patterns)
  get_filename_component(pattern_name "${desktop_pattern}" NAME)
  string(REPLACE "-" "_" android_name "${pattern_name}")
  string(REPLACE "," "_" android_name "${android_name}")
  set(android_pattern
    "${SOURCE_ROOT}/android/res/raw/${android_name}"
  )
  if(NOT EXISTS "${android_pattern}")
    message(FATAL_ERROR
      "Missing Android copy for ${pattern_name}: ${android_pattern}"
    )
  endif()
  file(SHA256 "${desktop_pattern}" desktop_hash)
  file(SHA256 "${android_pattern}" android_hash)
  if(NOT desktop_hash STREQUAL android_hash)
    message(FATAL_ERROR
      "Hyphenation resource differs between desktop and Android: "
      "${pattern_name}"
    )
  endif()
endforeach()
