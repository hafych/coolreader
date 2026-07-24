# Telemetry and crash-reporting decision

Status: **disabled**. CoolReader Next does not collect analytics, diagnostics or
crash reports and does not include a third-party telemetry SDK.

`CrashPrivacy` is local process hygiene, not reporting: it redacts the throwable
and thread name before delegating to Android's existing uncaught-exception
handler. It has no network path, account identifier, upload queue or owner
backend. Users may voluntarily attach a separately privacy-filtered diagnostic
export to an issue or support message.

Telemetry must not be enabled by an implementation-only change. A future owner
decision must first cover all of the following:

1. privacy assessment and exact data inventory;
2. explicit opt-in consent UX, including refusal and later withdrawal;
3. self-hosted versus named third-party processor and jurisdiction;
4. data minimization, redaction and exclusion of book names, paths, text,
   catalog URLs, credentials and stable device/account identifiers;
5. retention limits, user deletion and incident handling;
6. updated privacy policy, Play Data safety form, release notes and tests.

Silence or continued app use is not consent. A crash must never bypass network
settings, retry forever, block process termination or upload historical reports
after consent is withdrawn.

`telemetry-policy.json` is the machine-readable decision record.
`tools/verify_no_telemetry.py` fails CI if known SDK coordinates appear, if the
local crash handler gains network code, or if the build/release workflows stop
enforcing this boundary.
