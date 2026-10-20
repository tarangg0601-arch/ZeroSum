# Production release readiness

**Status: not ready for public production.** No deployment was made. This page separates behavior verified from source and isolated local tests from external services and hosting settings that still need owner configuration and verification.

## Verified in the repository

- Registration does not issue a session until email ownership is verified. Verification links expire after 24 hours; password-reset and legacy-claim links expire after 30 minutes. Tokens use 32 random bytes, only SHA-256 hashes are stored, and a database update makes each token single-use. Links put the token in the URL fragment, and the frontend removes the fragment from browser history after reading it.
- Email delivery is selectable with `EMAIL_PROVIDER=smtp` (default) or `EMAIL_PROVIDER=resend`. SMTP requires STARTTLS, checks TLS server identity, and uses finite connection/read/write timeouts. Resend uses HTTPS `POST /emails` with a bearer API key. Provider failures are wrapped without returning provider response bodies or keys. Mocked Resend API tests verify the request shape, success, failure secrecy, and missing configuration; no real provider credentials are configured, so delivery has **not** been tested.
- API bearer tokens expire after 15 minutes. Logout and password reset invalidate prior tokens. Login failures are limited to five per email/IP bucket in a 15-minute window; stored bucket identifiers are hashed. Email requests for registration, password reset, and legacy claims are limited to three per email/IP/action in an hour; those bucket identifiers are also hashed.
- Email-link URLs require an HTTPS origin; HTTP is accepted only for localhost development. Userinfo, query strings, fragments, and non-root paths are rejected to avoid sending credentials or tokens to an unsafe or unexpected URL.
- Render's public ingress is configured to use the `CF-Connecting-IP` header only when `APP_TRUST_RENDER_CLIENT_IP=true`; the header value must be a single IP literal. The repository sets this flag in `render.yaml`. Local/default configuration ignores proxy headers and uses the socket address. Render documents Cloudflare at its public edge and describes forwarding the client IP; validate this assumption against the deployed ingress before relying on it. See [Render's client-IP guidance](https://render.com/articles/host-pocketbase-on-render). Never enable this setting behind an ingress that does not overwrite the header.
- Group routes require a bearer token and group membership; group administration requires the creator. IOU and settlement actions enforce participant checks. `/api/health` is public and returns healthy only after a database query succeeds.
- CORS uses configured origins, trims comma-separated entries, and does not enable cookie credentials. The checked-in Render origin is a template and must match the actual frontend URL exactly.
- Hibernate schema changes are disabled; Flyway owns migrations. The app supports separate runtime DB credentials (`DB_USERNAME`/`DB_PASSWORD`) and migration credentials (`FLYWAY_DB_USERNAME`/`FLYWAY_DB_PASSWORD`). The isolated Testcontainers suite verifies migrations and the API journey; it does not connect to Supabase.
- The Render blueprint describes a Docker API and static frontend, with an HTTP database health check, a Resend provider selection, static CSP/security headers, and secret environment variables marked `sync: false`. It does not create a PostgreSQL database. The API is still set to Render's `free` plan, which is a preview configuration, not a production plan. Static headers are source-configured but have not been observed on a deployed site.
- Stale email-throttle rows are cleaned during requests. This is per bucket and database-backed, but it is not a general volumetric abuse or bot defense; provider-side limits and edge/WAF controls are still needed.
- GitHub Actions CI is configured to run Maven tests (including Testcontainers on the hosted Docker runner), frontend tests, lint, production build, and `npm audit`. Workflow execution has not yet been observed on GitHub.

## Outstanding requirements before deployment

### 1. Choose and verify real email delivery

No SMTP settings or credentials were present in the environment checked for this task. Do not treat email delivery as working until the owner configures a provider and verifies a real mailbox end to end.

The Render blueprint selects Resend so the API calls an HTTPS provider endpoint instead of relying on outbound SMTP ports. Configure these backend variables in the hosting provider's secret/config store:

- `EMAIL_PROVIDER=resend` and `RESEND_API_KEY` (the API key is secret).
- `EMAIL_FROM` using a sender address/domain verified with the provider.
- `APP_PUBLIC_BASE_URL` set to the exact HTTPS frontend origin.

Create a provider account, select a plan within its current pricing/quotas, generate a restricted API key, and verify a sender domain/address. Publish and verify SPF and DKIM for the sending domain, and publish a monitored DMARC policy. Check that the visible From domain aligns with SPF or DKIM and inspect delivered message headers and provider delivery logs. Consult [Resend's email API](https://resend.com/features/email-api), the [Gmail sender guidelines](https://support.google.com/mail/answer/81126), and [Yahoo sender best practices](https://senders.yahooinc.com/best-practices/) for current recipient-side requirements. The adapter uses HTTPS with the Resend API; the default API host is `https://api.resend.com`. `RESEND_API_BASE_URL` exists for controlled tests/alternate compatible endpoints and should remain unset for production.

The adapter and mocked API tests do not establish real delivery. After setting the provider account, API key, and verified sender, use a controlled mailbox to exercise registration → verification → login and forgotten-password → reset → old-token rejection. Confirm inbox/spam delivery, sender authentication, and provider delivery logs. Resend may have account pricing, quotas, or verification requirements that change; check current terms before selecting a plan. The Render free API plan remains unsuitable for production independently of email transport. See [Render free instance limitations](https://render.com/docs/free).

During that live delivery check, also confirm expired and reused links fail, inspect inbox/spam placement and sender authentication, and ensure proxy, application, and provider logs do not contain message bodies, passwords, or raw tokens.

### 2. Provision a dedicated production PostgreSQL database

There is no database resource in `render.yaml`; `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are manual secret/config entries. Create a dedicated, paid managed PostgreSQL instance in the same region as the API. Do not point it at the user's existing Supabase database or any shared development database.

- Use Render's private/internal database URL and configure `DB_URL` in JDBC form, for example `jdbc:postgresql://<internal-host>:5432/<database>?sslmode=require`. Require TLS; never use an external/public database URL from the API when an internal route is available. See [Render Postgres connection guidance](https://render.com/docs/postgresql-creating-connecting).
- Create a runtime role for `DB_USERNAME`/`DB_PASSWORD` with only the table and sequence permissions the application needs. Configure a separate migration role in `FLYWAY_DB_USERNAME`/`FLYWAY_DB_PASSWORD` with schema migration rights. Flyway runs at application startup using this migration role. Keep both credentials in the host secret store and rotate them.
- Confirm backups, retention, monitoring, and recovery objectives with the selected plan. Perform a restore drill against a disposable database before production use. Document how to roll forward or restore if a migration fails.
- Render free Postgres databases expire after 30 days; use a paid database for production. See [Render free instance limitations](https://render.com/docs/free).

### 3. Complete hosting and edge configuration

- Replace the template `https://zerosum-frontend.onrender.com` and `https://zerosum-api.onrender.com` values in `render.yaml` with the actual service origins if names differ. Set `APP_CORS_ALLOWED_ORIGINS` to the exact HTTPS frontend origin, `VITE_API_BASE_URL` to the API origin, and `APP_PUBLIC_BASE_URL` to the frontend origin. Avoid wildcard origins.
- Change the API `plan: free` to a paid production plan. Render free web services spin down after inactivity and are documented for preview use. Resend avoids the SMTP egress restriction through HTTPS, but does not remove the production service availability requirement. See [Render free instance limitations](https://render.com/docs/free).
- Keep `healthCheckPath: /api/health`. It now checks database connectivity. Verify Render reports healthy after the DB is configured; Render uses HTTP health checks to decide whether a service can receive traffic. See [Render health checks](https://render.com/docs/health-checks).
- `.github/workflows/ci.yml` runs backend Maven tests with Docker available, frontend tests, lint, production build, and npm audit on pushes and pull requests. The workflow has not yet run on GitHub. Require the CI check in branch protection before merging. The Dockerfile skips Maven tests during image build, so a deployment build alone does not prove tests ran.
- Generate `APP_AUTH_TOKEN_SECRET` using a cryptographically secure generator with at least 32 bytes of entropy. Keep it in the host secret store, never in Git, and define a controlled rotation procedure (rotation invalidates all current sessions).
- `APP_TRUST_RENDER_CLIENT_IP=true` trusts the Render/Cloudflare `CF-Connecting-IP` value for login throttling and email-request throttling. Verify real ingress behavior before relying on it. Keep it false on local development and do not enable it for a different proxy unless that proxy is known to overwrite the configured client-IP header. App limits bound repeated requests per email/address/action; configure provider/edge bot and volumetric controls because distributed IPs can evade per-address limits.
- Enable monitoring and alerting for API errors, failed email delivery, database health, authentication abuse, and backup failures. Review request/application logging to ensure credentials and one-time links are never recorded. Add a documented incident and account-recovery process for legacy users without a verifiable mailbox.
- The frontend token remains in `sessionStorage`, so successful script injection can read it until its 15-minute expiry. The Render blueprint now configures CSP (`script-src 'self'`), frame/content/referrer/permissions policies, and HSTS; inline styles remain allowed for current styling. This is defense in depth, not a replacement for XSS prevention. Confirm the actual deployed response headers, make the CSP `connect-src` match the selected API origin, and test the application for CSP breakage before release. When custom front-end/API domains are chosen, update CSP, CORS, `VITE_API_BASE_URL`, and `APP_PUBLIC_BASE_URL` together.

### 4. Release verification

Latest local verification (2026-10-20): backend **35 passed, 0 failed, 0 skipped**; three Testcontainers PostgreSQL integration tests passed with Flyway migrations through v4; frontend **13 passed**; lint and production build passed; `npm audit` reported **0 vulnerabilities**; `git diff --check` passed. Integration tests mock email and do not verify provider delivery. The configured GitHub Actions workflow has not run on GitHub yet.

Rerun the full checks after the final production configuration is selected:

```powershell
$env:MAVEN_USER_HOME='C:\ZeroSum\.maven'
$env:DOCKER_HOST='npipe:////./pipe/docker_engine'
cd backend
.\mvnw.cmd "-Dmaven.repo.local=C:\ZeroSum\backend\.m2\repository" test -B -ntp
cd ..\frontend
npm run test
npm run lint
npm run build
npm audit --cache .npm-cache
cd ..
git diff --check
```

Integration tests use a disposable Testcontainers PostgreSQL instance and mock the email sender. They verify database migrations, account/token behavior, and the group-to-settlement workflow; they do not verify real provider delivery. Never replace the isolated test DB configuration with a production or Supabase URL.

## Configuration reference

| Purpose | Variables |
|---|---|
| Runtime PostgreSQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Flyway migration role | `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD` |
| Email provider | `EMAIL_PROVIDER` (`smtp` or `resend`), `EMAIL_FROM`; Resend: `RESEND_API_KEY` and optional `RESEND_API_BASE_URL`; SMTP: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` |
| Public origins | `APP_PUBLIC_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS`, frontend `VITE_API_BASE_URL` |
| Authentication / ingress | `APP_AUTH_TOKEN_SECRET`, `APP_TRUST_RENDER_CLIENT_IP` |

The API must not be declared production-ready until real email delivery, dedicated database access and restore, trusted client-IP behavior, final origins/secrets, deployed response headers, and the release review above are verified. A real Render deployment will also require a hosting account and a paid production service/database plan; email requires a provider account and a verified sender, with any applicable provider cost/quotas selected by the owner.
