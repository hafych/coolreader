# Unicode correctness contract

CoolReader stores parsed text as Unicode scalar values in `lString32`. Its
UTF-8 and UTF-16 encoders accept only scalar values from U+0000 through
U+10FFFF, excluding the UTF-16 surrogate range. Invalid values are replaced
with `?`, matching the engine's existing conversion failure policy.

UTF-16 decoding has the following bounded behavior:

- a valid high/low surrogate pair becomes one supplementary scalar;
- an unpaired high or low surrogate becomes `?`;
- an unpaired high surrogate does not consume the following code unit;
- fragment overloads never inspect a code unit beyond the supplied length.

The native `unicode_regression` corpus covers:

- BMP and supplementary-plane UTF-8/UTF-16 round trips;
- valid, unpaired and truncated UTF-16 surrogate sequences;
- combining-mark and emoji-ZWJ grapheme boundaries through libunibreak;
- left-to-right and Hebrew visual ordering through FriBidi;
- CJK ideograph and punctuation line-break rules;
- Latin, combining, Hebrew, Arabic, Han and emoji mixed-script round trips.

Run the corpus with:

```sh
ctest --test-dir <native-build-directory> -R '^unicode_regression$' \
  --output-on-failure
```

The target is part of both the native test suite and the first-party Clang
warning gate.
