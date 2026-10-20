# Production deployment requirements

The checked-in Render configuration describes a public web service and static frontend. ZeroSum requires a signed, short-lived bearer token for API access. Registration does not issue a session until the email address is verified. Email verification, password reset, and legacy-account claiming use expiring, single-use tokens whose SHA-256 hashes are stored in PostgreSQL. The signing secret is supplied through `APP_AUTH_TOKEN_SECRET`; it must contain at least 32 bytes and must be generated and stored in the host's secret store. Logout and password recovery increment a per-user token version, invalidating previously issued bearer tokens.

The app includes SMTP-based verification and recovery flows and a PostgreSQL-backed login limit (five failures per email/IP pair in 15 minutes). Existing legacy rows with an email and no password can be claimed only by proving access to that mailbox; the row and its financial relationships are retained. Rows without a verified email cannot be automatically linked and must be handled through a separately reviewed, identity-verified support process. Email delivery has not been verified against a real provider in this environment. Do not deploy publicly until the provider, proxy-aware abuse controls, and identity recovery procedures are validated.

Before production:

- Configure and test SMTP delivery using `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `EMAIL_FROM`, and `APP_PUBLIC_BASE_URL`. Keep provider credentials in the host secret store. Confirm sender-domain SPF, DKIM, and DMARC settings and verify links use HTTPS.
- The login limiter is stored in PostgreSQL and applies to email/IP buckets. Confirm reverse-proxy client IP handling is trustworthy in the production host, and add provider-level controls for registration, password-reset, and verification email abuse. Keep the group-membership gate and owner-only group administration; review every future endpoint.
- Never trust a caller-supplied creator ID as identity. Keep bearer tokens short-lived, use HTTPS, rotate the host-generated signing secret, and avoid logging tokens or passwords.
- Configure a production-only database with a least-privilege account, managed backups, restore drills, monitoring, and a documented migration/rollback process. Keep credentials in the hosting provider's secret store and rotate them.
- Set the exact production frontend origin in `APP_CORS_ALLOWED_ORIGINS`; do not use wildcard origins with credentials. Verify API and frontend URLs, headers, and error responses before exposing traffic.
- Add request size limits, audit logs for payment and expense changes, operational health/alerting, and an operational token-cleanup policy. Remove development defaults and sample credentials from production configuration.
- Run dependency audits in CI before release. The reported high-severity `source-map-js` advisory has been resolved by the npm-supported patch update to 1.2.2.
- Run the PostgreSQL migration and full API journey suite against a disposable isolated database before release, then have a security review approve public exposure.

## Local database verification

The integration test uses Testcontainers and starts a disposable `postgres:16-alpine` database. On Windows, install Docker Desktop, start its Linux container engine, and confirm `docker info` reports both Client and Server. Then run the backend Maven test suite. Testcontainers creates and removes its own database container; the test never reads the application `DB_URL` and does not connect to Supabase.

No real SMTP delivery or public deployment has been performed. `APP_AUTH_TOKEN_SECRET` and all SMTP credentials must be supplied as environment/host secrets; no credentials are committed.
