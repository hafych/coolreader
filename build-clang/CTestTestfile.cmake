# CMake generated Testfile for 
# Source directory: /Users/nazarii/coolreader
# Build directory: /Users/nazarii/coolreader/build-clang
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(core_safety "/Users/nazarii/coolreader/build-clang/core_safety_test")
set_tests_properties(core_safety PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;550;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(thread_safety "/Users/nazarii/coolreader/build-clang/thread_safety_test")
set_tests_properties(thread_safety PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;566;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(document_regression "/Users/nazarii/coolreader/build-clang/document_regression_test")
set_tests_properties(document_regression PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;577;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(unicode_regression "/Users/nazarii/coolreader/build-clang/unicode_regression_test")
set_tests_properties(unicode_regression PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;585;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(typography_regression "/Users/nazarii/coolreader/build-clang/typography_regression_test")
set_tests_properties(typography_regression PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;596;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(css_regression "/Users/nazarii/coolreader/build-clang/css_regression_test")
set_tests_properties(css_regression PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;607;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(hyphenation_regression "/Users/nazarii/coolreader/build-clang/hyphenation_regression_test")
set_tests_properties(hyphenation_regression PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;618;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(hyphenation_resource_sync "/opt/homebrew/bin/cmake" "-DSOURCE_ROOT=/Users/nazarii/coolreader" "-P" "/Users/nazarii/coolreader/crengine/tests/hyphenation_resource_sync.cmake")
set_tests_properties(hyphenation_resource_sync PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;620;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(epub3_regression "/Users/nazarii/coolreader/build-clang/epub3_regression_test")
set_tests_properties(epub3_regression PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;635;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(tinydict_regression "/Users/nazarii/coolreader/build-clang/tinydict_regression_test")
set_tests_properties(tinydict_regression PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;651;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
add_test(native_state_policy "/opt/homebrew/bin/cmake" "-DSOURCE_ROOT=/Users/nazarii/coolreader" "-P" "/Users/nazarii/coolreader/crengine/tests/native_state_policy.cmake")
set_tests_properties(native_state_policy PROPERTIES  _BACKTRACE_TRIPLES "/Users/nazarii/coolreader/CMakeLists.txt;653;add_test;/Users/nazarii/coolreader/CMakeLists.txt;0;")
subdirs("thirdparty_unman/qimagescale")
subdirs("thirdparty_unman/chmlib")
subdirs("thirdparty_unman/antiword")
subdirs("crengine")
subdirs("fb2props")
