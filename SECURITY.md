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

## Android network policy

All active production HTTP connections are opened through `SecureHttp`; CI
rejects direct connection creation or caller-specific TLS, redirect,
authorization, referrer and timeout overrides.

- Connect and read timeouts are 60 seconds; total response-body transfer time is
  limited to 15 minutes.
- Automatic redirects are disabled. OPDS may follow at most five explicit
  HTTP(S) redirects and never follows HTTPS to HTTP. LitRes API requests do not
  follow redirects.
- OPDS Basic authorization is sent only to the original HTTPS origin. Referrers
  are same-origin only and omit credentials, query parameters and fragments.
- LitRes POST parameters are sent only to the pinned
  `https://robot.litres.ru` origin. Unauthenticated trial downloads may use
  another HTTPS origin.
- Declared response lengths are parsed as 64-bit values. Decompressed OPDS
  feeds are limited to 8 MiB, LitRes API XML to 5 MiB and downloaded books to
  the 512 MiB document input budget.

Cloud sync currently has no active remote transport in this GPL build; the old
Google Drive integration is disabled. A new sync transport must define its
threat model and use the same network policy before it can be enabled.

## Diagnostic privacy

Production Java logging is routed through `org.coolreader.crengine.Log`; direct
`android.util.Log`, stdout/stderr and `printStackTrace` calls are rejected by
CI. The wrapper removes credentials, authorization values, URLs, URL
query/fragment data, local paths and private book filenames. Exception messages
and source file names are not copied into log stack traces.

The Android application installs a default uncaught-exception boundary that
passes a detached, message-free throwable to the platform handler. Native
engine messages use a conservative sink: any message containing a credential
marker, URL/query syntax, path separator or supported document filename is
replaced in full. User-exported logcat data is filtered again line by line
before it is written.

This policy intentionally favors privacy over verbose diagnostics. Sanitized
exception class/method identifiers, source line numbers and non-sensitive
engine status messages remain available. CI policy changes or redaction
suppressions require a documented security review.

## Native dynamic analysis

Linux CI builds and runs the native smoke suite under AddressSanitizer,
UndefinedBehaviorSanitizer and ThreadSanitizer. The ThreadSanitizer build uses
Clang and includes a real-thread test even though the desktop engine normally
compiles its legacy thread abstraction as synchronous. Sanitizer reports fail
the job; suppressions are not accepted without a documented root-cause review.
