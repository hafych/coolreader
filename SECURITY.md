# Security policy

## Supported versions

Security fixes are developed on the current default branch. Until the fork
publishes a stable release, older commits and unofficial APKs are not supported.

## Reporting a vulnerability

Please do not open a public issue for an undisclosed vulnerability.

Use GitHub's private vulnerability reporting for this repository:

1. Open the **Security** tab.
2. Choose **Report a vulnerability**.
3. Include the affected commit, platform, reproduction steps, impact and any
   proposed mitigation.

If private vulnerability reporting is unavailable, open a public issue that asks
the maintainer to provide a private contact channel without including exploit
details.

Do not send credentials, private books or personal library data. A minimal,
legally shareable test document is preferred.

The maintainer will acknowledge a complete report when possible, validate its
scope, coordinate a fix and credit the reporter unless anonymity is requested.

## Document parsing limits

Untrusted books are processed under a shared Java/native `ParseBudget`.
Java/native error codes and the 512 MiB document ingress limit are checked for
parity in CI.

| Resource | Default limit | Error code |
| --- | ---: | --- |
| Input document | 512 MiB | `1001 input-bytes` |
| Decoded XML/HTML text | 128 Mi characters | `1002 text-characters` |
| XML/HTML nesting | 256 elements | `1003 xml-depth` |
| ZIP entries | 10,000 | `1101 archive-entry-count` |
| One expanded ZIP entry | 256 MiB | `1102 archive-entry-bytes` |
| All expanded ZIP entries | 1 GiB | `1103 archive-total-bytes` |
| ZIP compression ratio above 1 MiB | 200:1 | `1104 archive-compression-ratio` |
| ZIP path depth | 64 segments | `1105 archive-path-depth` |
| Recursive archive depth | 4 containers | `1106 container-depth` |
| Image dimensions | 16,384 per side and 64 Mi pixels | `1201 image-dimensions` |

Unsafe paths and duplicate archive entries use codes `1107` and `1108`.
Limit failures must not include document contents, credentials or local paths.
