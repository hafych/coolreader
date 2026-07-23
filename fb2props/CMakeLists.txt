#libfb2props.so plugin for Hanlon Jinke V3/V5 Bookshelf app
SET (FB2PROPS_SOURCES 
    fb2props.cpp
)

SET (CR3_BIN_DESTINATION usr/lib)

ADD_LIBRARY(fb2props SHARED ${FB2PROPS_SOURCES})
TARGET_LINK_LIBRARIES(fb2props crengine qimagescale ${EXTRA_LIBS} ${STD_LIBS})
INSTALL( TARGETS fb2props LIBRARY DESTINATION ${CR3_BIN_DESTINATION} )

ADD_EXECUTABLE(fb2propstest fb2propstest.cpp)
# fb2props already links crengine and qimagescale. Linking those static
# libraries into the test executable again creates a second copy of crengine
# globals and corrupts the string allocator on platforms with symbol
# interposition (notably macOS).
TARGET_LINK_LIBRARIES(fb2propstest fb2props ${STD_LIBS})
INSTALL( TARGETS fb2propstest RUNTIME DESTINATION ${CR3_BIN_DESTINATION} )

if(BUILD_TESTING)
    add_test(
        NAME fb2props_metadata_smoke
        COMMAND fb2propstest ${CMAKE_SOURCE_DIR}/android/res/raw/help_template_en.fb2
    )
    if(UNIX)
        add_executable(core_safety_test
            ${CMAKE_SOURCE_DIR}/crengine/tests/core_safety_test.cpp
        )
        target_link_libraries(core_safety_test
            crengine qimagescale ${EXTRA_LIBS} ${STD_LIBS}
        )
        add_test(NAME core_safety COMMAND core_safety_test)

        add_executable(thread_safety_test
            ${CMAKE_SOURCE_DIR}/crengine/tests/thread_safety_test.cpp
        )
        target_include_directories(thread_safety_test PRIVATE
            ${CMAKE_SOURCE_DIR}/crengine/include
        )
        target_compile_definitions(thread_safety_test PRIVATE
            CR_USE_THREADS=1
            _LINUX=1
        )
        add_test(NAME thread_safety COMMAND thread_safety_test)
    endif()
endif()
