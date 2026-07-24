# Prepared upstream contributions

These candidates are based directly on `buggins/coolreader` commit
`109e2a22fa7b38c7e388155cca0fe1fbdbbf6144`. They contain no downstream
branding, release policy or Android architecture changes.

## Honor the requested C++17 standard

- Local branch: `hafych/upstream-cmake-cxx17`
- Commit: `3e8fedb58ce7141db8d73ce171beb614b400b1d9`
- Mail patch:
  `upstream-patches/0001-fix-honor-the-requested-C-17-standard.patch`
- Suggested PR title: `Fix CMake C++17 standard selection`

Suggested PR summary:

> Remove the stray backtick from `CMAKE_CXX_STANDARD` and stop appending
> `-std=c++11` after requesting C++17. This lets CMake select the compiler's
> C++17 flag consistently.

Verification:

- clean CMake 4.3.1 configure with AppleClang 21, macOS arm64 and
  `GUI=FB2PROPS`;
- generated compile commands contain `-std=gnu++17` and no `-std=c++11`;
- the existing upstream FB2PROPS build then reaches compilation. It still fails
  on pre-existing macOS/rendering issues such as `lseek64`/`off64_t` and missing
  rendering declarations; those unrelated fixes are intentionally excluded.

## Make third-party downloads reliable

- Local branch: `hafych/upstream-thirdparty-downloads`
- Commit: `b2860a2450db14e0684c1ea5ace9ad73abfdd7d5`
- Mail patch:
  `upstream-patches/0001-build-make-third-party-downloads-reliable.patch`
- Suggested PR title: `Use reliable HTTPS third-party source downloads`

Suggested PR summary:

> Quote download inputs, name the output explicitly and retry transient curl
> failures. Use HTTPS for IJG JPEG and the official GitHub release asset for
> zlib while retaining the existing SHA-512 verification.

Verification:

- `bash -n thirdparty-deploy.sh`;
- downloaded IJG `jpegsrc.v10.tar.gz` and zlib `1.3.2` from the proposed URLs;
- both downloads match the existing repository SHA-512 values exactly.

## Applying or sending

Each patch independently passes `git apply --check` on the upstream base:

```sh
git switch -c candidate upstream/master
git am /path/to/upstream-patches/<patch>
```

Re-fetch upstream and repeat the apply/verification checks immediately before
opening a PR. Keep these as two separate PRs so either can be reviewed and
merged without the other. Creating public branches or PRs remains an explicit
repository-owner action.
