# Clang warning policy

The Linux Clang job compiles the native project with `-Wall`, `-Wextra` and
`-Wpedantic`. The following high-confidence diagnostics are errors in
CoolReader-owned targets:

- C and C++: missing return values, signed/unsigned comparisons, and
  variables that are set but never read;
- C: implicit function declarations and incompatible pointer types;
- C++: uninitialized values and deleting through a base without a virtual
  destructor;
- C++: incomplete enum switches and anonymous non-C-compatible types used for
  linkage;
- C++: misleading indentation;
- C++: constructor member-initialization order mismatches.

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

Source-scoped promotion (`set_property(SOURCE ... COMPILE_OPTIONS -Werror=<name>)`)
is allowed as a stepping stone while a diagnostic still exists in other files.
Once the class is clean across all first-party code, widen it to the full
`USE_CLANG_WARNING_GATE` target list and remove the per-file override.
