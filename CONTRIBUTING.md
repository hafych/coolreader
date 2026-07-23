# Contributing to CoolReader Next

Thank you for helping improve this downstream fork. Small, testable changes are
preferred because CoolReader spans Android/Java/JNI, a mature C++ rendering
engine and several desktop and E-Ink frontends.

## Before opening an issue

- Search both this repository and
  [`buggins/coolreader`](https://github.com/buggins/coolreader/issues).
- Include the platform, OS or Android version, device model and the affected
  ebook format.
- Reduce problematic documents when possible. Do not upload copyrighted or
  private books; create a minimal reproducer or describe how maintainers can
  obtain a legal public sample.
- Remove account credentials, OPDS tokens, personal library paths and other
  private data from logs.

Security vulnerabilities should be reported privately as described in
[SECURITY.md](SECURITY.md), not in a public issue.

## Development workflow

1. Create a focused branch from the current default branch.
2. Keep unrelated formatting and generated files out of the change.
3. Add or update a regression test.
4. Run the checks that cover the changed area.
5. Explain user-visible behavior, compatibility risk and upstream relevance in
   the pull request.

### Android checks

```bash
cd android
./gradlew assembleDebug lintDebug testDebugUnitTest
```

### Native checks

```bash
cmake -S . -B build -DGUI=FB2PROPS -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON
cmake --build build --parallel
ctest --test-dir build --output-on-failure
```

For parser, archive, image, cache, JNI or memory-safety changes, also run an
AddressSanitizer/UndefinedBehaviorSanitizer build when your platform supports it.

## Fork discipline

Classify downstream changes in [FORK_DELTA.md](FORK_DELTA.md):

- `upstreamable` for generally useful fixes suitable for the original project;
- `temporary-delta` for compatibility bridges with a clear removal condition;
- `fork-only` for intentional downstream policy or product choices.

Do not rewrite upstream history. Upstream syncs should be isolated, reviewed and
followed by Android, native and sanitizer verification.

## License and authorship

Contributions must be compatible with GPL-2.0-or-later. Third-party components
must use a compatible license and retain their notices. See the
[GNU license list](https://www.gnu.org/licenses/license-list.html).

Follow existing source-file attribution conventions. Add yourself to
[AUTHORS](AUTHORS) when appropriate, using a name and contact address you are
comfortable publishing.
