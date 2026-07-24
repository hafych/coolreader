# Clang warning policy

The Linux Clang job compiles the native project with `-Wall`, `-Wextra` and
`-Wpedantic`. The following high-confidence diagnostics are errors in
CoolReader-owned targets:

- C and C++: missing return values;
- C: implicit function declarations and incompatible pointer types;
- C++: uninitialized values and deleting through a base without a virtual
  destructor;
- C++: incomplete enum switches and anonymous non-C-compatible types used for
  linkage;
- C++: misleading indentation.

Enable the same gate locally with:

```sh
CC=clang CXX=clang++ cmake -S . -B build-clang \
  -DGUI=FB2PROPS -DCMAKE_BUILD_TYPE=Debug -DBUILD_TESTING=ON \
  -DUSE_CLANG_WARNING_GATE=ON
cmake --build build-clang --parallel
ctest --test-dir build-clang --output-on-failure
```

There is no frozen warning-count baseline. Existing non-blocking warnings stay
visible in CI. When a warning class has been fixed in first-party code, promote
that class to `-Werror=<name>` in `USE_CLANG_WARNING_GATE`; do not silence it by
adding a blanket suppression. Bundled third-party targets are outside this
gate.

Source-scoped promotion is allowed while a diagnostic still exists elsewhere.
`hyphman.cpp` currently enforces `sign-compare` and
`unused-but-set-variable`; widening either diagnostic to the full `crengine`
target requires first clearing the remaining first-party occurrences.
