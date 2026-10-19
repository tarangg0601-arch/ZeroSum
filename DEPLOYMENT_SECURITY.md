# Production deployment requirements

The checked-in Render configuration describes a public web service and static frontend. ZeroSum is not ready for public deployment: the backend currently has no authentication or authorization, and APIs accept client-supplied user IDs. The group creator is treated as the current user only in the interface. Anyone who can reach the API can create users, inspect groups, change expenses, settle payments, and read payment history. Do not deploy this configuration publicly until these controls are implemented and reviewed.

Before production:

- Add authentication and derive the acting user from a verified session or token. Never trust a caller-supplied creator, payer, participant, or settlement user ID as proof of identity.
- Enforce group membership and role checks on every read and write, including expense, recurring rule, insights, settlement, payment history, and IOU endpoints. Add authorization tests for cross-group access.
- Store password hashes only if password authentication is implemented; otherwise use a supported identity provider. Require TLS and secure cookie/token handling.
- Configure a production-only database with a least-privilege account, managed backups, restore drills, monitoring, and a documented migration/rollback process. Keep credentials in the hosting provider's secret store and rotate them.
- Set the exact production frontend origin in `APP_CORS_ALLOWED_ORIGINS`; do not use wildcard origins with credentials. Verify API and frontend URLs, headers, and error responses before exposing traffic.
- Add rate limits, request size limits, audit logs for payment and expense changes, and operational health/alerting. Remove development defaults and sample credentials from production configuration.
- Resolve the current high-severity `source-map-js` advisory reported by `npm audit`, then repeat dependency audits in CI before release.
- Run the PostgreSQL migration and full API journey suite against a disposable isolated database before release, then have a security review approve public exposure.

No public deployment has been performed.
