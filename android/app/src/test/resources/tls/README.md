# TLS test fixtures

These certificates and the matching private key are intentionally public test
fixtures. They must never be used by a production or staging service.

The fixture set contains:

- a private test CA;
- a valid `localhost` server certificate;
- a CA-signed certificate for `wrong.example`;
- a CA-signed `localhost` certificate whose validity ended on
  23 July 2026;
- an untrusted self-signed `localhost` certificate.

All server certificates share the test-only key so the local `SSLServerSocket`
fixture can exercise trust, expiry and hostname validation independently.
