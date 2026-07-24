# Fork versioning policy

`release-version.json` is the single release-version source. The current fork
format is:

- upstream relationship: exact upstream version and full base commit;
- public version: `<upstream-version>-next.<fork-release>`, for example
  `3.2.59-next.1`;
- Git tag: `v<public-version>`, for example `v3.2.59-next.1`;
- Android version code: `YYMMDDRR0`.

`RR` is the release sequence for that UTC date (`01` through `99`). The final
zero reserves values `1` through `7` for the legacy per-ABI APK outputs, so a
new base version code is always at least ten greater than the previous one.
Version codes must remain globally increasing, unique and no greater than
Google Play's limit. Never reuse a code after an artifact has reached any Play
track.

The fork release counter restarts at 1 only when the recorded upstream base
version/commit changes. Backports that remain on an older upstream base
increment the fork release. A hotfix increments both the fork release and
Android version code; it never moves or recreates a published tag.

Before a release:

1. Update `release-version.json` once.
2. Run `python3 tools/verify_release_version.py`.
3. Update `CHANGELOG.md` and `FORK_DELTA.md`.
4. Commit the version change.
5. Create the exact verified tag only after the release commit is reviewed.

Android Gradle reads the JSON directly. The legacy Eclipse manifest and native
engine header remain duplicated for compatibility, and the verifier prevents
them from drifting.
