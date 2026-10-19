# Production deployment requirements

The checked-in Render configuration describes a public web service and static frontend. ZeroSum now requires a registered account and a signed, short-lived bearer token for API access. Group routes require group membership, group creation uses the authenticated account, and IOU/payment recording requires the caller to be involved. The signing secret is supplied through `APP_AUTH_TOKEN_SECRET`; it must contain at least 32 bytes and must be generated and stored in the host's secret store.

Public deployment still needs additional identity and operations work. Email ownership is not verified, there is no password recovery or login rate limiting, and existing anonymous user rows do not have credentials. Legacy accounts cannot be safely claimed automatically; supply a reviewed user-linking migration or perform identity-verified manual linking before production data is exposed. Do not deploy publicly until these issues and the database integration run are resolved.

Before production:

- Add email ownership verification, password recovery, and rate limiting around account registration and login. Keep the existing group-membership gate and add a security review of every future endpoint.
- Never trust a caller-supplied creator ID as identity. Keep bearer tokens short-lived, use HTTPS, rotate the host-generated signing secret, and avoid logging tokens or passwords.
- Configure a production-only database with a least-privilege account, managed backups, restore drills, monitoring, and a documented migration/rollback process. Keep credentials in the hosting provider's secret store and rotate them.
- Set the exact production frontend origin in `APP_CORS_ALLOWED_ORIGINS`; do not use wildcard origins with credentials. Verify API and frontend URLs, headers, and error responses before exposing traffic.
- Add rate limits, request size limits, audit logs for payment and expense changes, and operational health/alerting. Remove development defaults and sample credentials from production configuration.
- Run dependency audits in CI before release. The reported high-severity `source-map-js` advisory has been resolved by the npm-supported patch update to 1.2.2.
- Run the PostgreSQL migration and full API journey suite against a disposable isolated database before release, then have a security review approve public exposure.

## Local database verification

The integration test uses Testcontainers and starts a disposable `postgres:16-alpine` database. On Windows, install Docker Desktop, start its Linux container engine, and confirm `docker info` reports both Client and Server. Then run the backend Maven test suite. Testcontainers creates and removes its own database container; the test never reads the application `DB_URL` and does not connect to Supabase.

No public deployment has been performed.
