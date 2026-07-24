# Font selection contract

CoolReader resolves fonts in two scopes:

- process fonts discovered from the platform or registered by the application;
- document fonts registered with a document ID from an EPUB/container.

A document font with the requested CSS family wins only for its own document
ID. Removing that document's fonts restores process-font selection. This keeps
embedded fonts from leaking into later books while allowing an EPUB to
override a system family deliberately.

Fallback families are an ordered, de-duplicated list. Unknown families are
discarded, the remaining order is preserved, and each fallback instance has a
distinct traversal mask so missing glyph lookup terminates deterministically.

The native `typography_regression` test uses only vendored HarfBuzz font
fixtures. It verifies:

- loading and shaping a font that retains OpenType variation axes;
- document-scoped precedence over a process font with the same CSS family;
- restoration of the process font after document-font teardown;
- ordered fallback filtering, masks and CJK glyph resolution.

Run it with:

```sh
ctest --test-dir <native-build-directory> -R '^typography_regression$' \
  --output-on-failure
```
