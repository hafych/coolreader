## Summary

Describe the user-visible change and why it is needed.

## Fork classification

Select exactly one and update `FORK_DELTA.md` when the permanent delta changes.

- [ ] `upstreamable` — generally useful fix suitable for `buggins/coolreader`
- [ ] `temporary-delta` — compatibility bridge with a stated removal condition
- [ ] `fork-only` — intentional downstream product or policy choice

Upstream issue/PR or reason it is not applicable:

## Verification

- [ ] Relevant regression test added or updated
- [ ] Android: `assembleDebug lintDebug testDebugUnitTest`
- [ ] Native: configure, build and `ctest`
- [ ] Sanitizers run for parser/archive/image/cache/JNI/memory-safety changes
- [ ] Manual device or desktop check, if applicable

Checks not run and why:

## Security and privacy

- [ ] No credentials, tokens, private books or local paths were committed
- [ ] Untrusted input, permissions, logging and data migration were considered
- [ ] Documentation/changelog updated for user-visible or operational changes
