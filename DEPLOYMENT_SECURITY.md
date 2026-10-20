# Production release readiness

**Status: not ready for public production.** No deployment was made. This page separates behavior verified from source and isolated local tests from external services and hosting settings that still need owner configuration and verification.

## Verified in the repository

- Registration does not issue a session until email ownership is verified. Verification links expire after 24 hours; password-reset and legacy-claim links expire after 30 minutes. Tokens use 32 random bytes, only SHA-256 hashes are stored, and a database update makes each token single-use. Links put the token in the URL fragment, and the frontend removes the fragment from browser history after reading it.
- SMTP transport is configured to require STARTTLS, check TLS server identity, and use finite connection/read/write timeouts. SMTP credentials are read from environment variables. No real provider credentials are configured in this environment, so delivery has **not** been tested.
- API bearer tokens expire after 15 minutes. Logout and password reset invalidate prior tokens. Login failures are limited to five per email/IP bucket in a 15-minute window; stored bucket identifiers are hashed.
- Render's public ingress is configured to use the `CF-Connecting-IP` header only when `APP_TRUST_RENDER_CLIENT_IP=true`; the header value must be a single IP literal. The repository sets this flag in `render.yaml`. Local/default configuration ignores proxy headers and uses the socket address. Render documents Cloudflare at its public edge and describes forwarding the client IP; validate this assumption against the deployed ingress before relying on it. See [Render's client-IP guidance](https://render.com/articles/host-pocketbase-on-render). Never enable this setting behind an ingress that does not overwrite the header.
- Group routes require a bearer token and group membership; group administration requires the creator. IOU and settlement actions enforce participant checks. `/api/health` is public and returns healthy only after a database query succeeds.
- CORS uses configured origins, trims comma-separated entries, and does not enable cookie credentials. The checked-in Render origin is a template and must match the actual frontend URL exactly.
- Hibernate schema changes are disabled; Flyway owns migrations. The app supports separate runtime DB credentials (`DB_USERNAME`/`DB_PASSWORD`) and migration credentials (`FLYWAY_DB_USERNAME`/`FLYWAY_DB_PASSWORD`). The isolated Testcontainers suite verifies migrations and the API journey; it does not connect to Supabase.
- The Render blueprint describes a Docker API and static frontend, with an HTTP database health check and secret environment variables marked `sync: false`. It does not create a PostgreSQL database. The API is currently set to Render's `free` plan, which is a preview configuration, not a production plan.

## Outstanding requirements before deployment

### 1. Choose and verify real email delivery

No SMTP settings or credentials were present in the environment checked for this task. Do not treat email delivery as working until the owner configures a provider and verifies a real mailbox end to end.

Configure these backend variables in the hosting provider's secret/config store:

- `SMTP_HOST`, `SMTP_PORT` (usually 587), `SMTP_USERNAME`, `SMTP_PASSWORD`, and `SMTP_AUTH=true`.
- `EMAIL_FROM` using a sender address on a domain authorized by the provider.
- `APP_PUBLIC_BASE_URL` set to the exact HTTPS frontend origin.

The application requires STARTTLS and server-identity verification; there is no environment switch to silently disable TLS. Publish and verify SPF and DKIM for the sending domain, and publish a monitored DMARC policy. Check that the visible From domain aligns with SPF or DKIM and inspect delivered message headers and provider delivery logs. Consult the [Gmail sender guidelines](https://support.google.com/mail/answer/81126) and [Yahoo sender best practices](https://senders.yahooinc.com/best-practices/) for current recipient-side requirements.

**Render limitation:** `render.yaml` currently selects a free API plan. Render states that free web services cannot send outbound traffic on SMTP ports 25, 465, or 587. Upgrade the API service to a paid plan before using this SMTP implementation, or implement and test a provider HTTPS API adapter. Do not claim verification or recovery email works on the current free-plan configuration. See [Render free instance limitations](https://render.com/docs/free).

After configuration, use a provider sandbox or controlled test accounts to exercise registration → verification → login and forgotten-password → reset → old-token rejection. Confirm expired and reused links fail, verify inbox/spam delivery and sender authentication, and ensure proxy, application, and provider logs do not contain message bodies, passwords, or raw tokens.

### 2. Provision a dedicated production PostgreSQL database

There is no database resource in `render.yaml`; `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are manual secret/config entries. Create a dedicated, paid managed PostgreSQL instance in the same region as the API. Do not point it at the user's existing Supabase database or any shared development database.

- Use Render's private/internal database URL and configure `DB_URL` in JDBC form, for example `jdbc:postgresql://<internal-host>:5432/<database>?sslmode=require`. Require TLS; never use an external/public database URL from the API when an internal route is available. See [Render Postgres connection guidance](https://render.com/docs/postgresql-creating-connecting).
- Create a runtime role for `DB_USERNAME`/`DB_PASSWORD` with only the table and sequence permissions the application needs. Configure a separate migration role in `FLYWAY_DB_USERNAME`/`FLYWAY_DB_PASSWORD` with schema migration rights. Flyway runs at application startup using this migration role. Keep both credentials in the host secret store and rotate them.
- Confirm backups, retention, monitoring, and recovery objectives with the selected plan. Perform a restore drill against a disposable database before production use. Document how to roll forward or restore if a migration fails.
- Render free Postgres databases expire after 30 days; use a paid database for production. See [Render free instance limitations](https://render.com/docs/free).

### 3. Complete hosting and edge configuration

- Replace the template `https://zerosum-frontend.onrender.com` and `https://zerosum-api.onrender.com` values in `render.yaml` with the actual service origins if names differ. Set `APP_CORS_ALLOWED_ORIGINS` to the exact HTTPS frontend origin, `VITE_API_BASE_URL` to the API origin, and `APP_PUBLIC_BASE_URL` to the frontend origin. Avoid wildcard origins.
- Change the API `plan: free` to a paid production plan. Render free web services spin down after inactivity and are documented for preview use; their SMTP egress restriction independently blocks the current email transport. See [Render free instance limitations](https://render.com/docs/free).
- Keep `healthCheckPath: /api/health`. It now checks database connectivity. Verify Render reports healthy after the DB is configured; Render uses HTTP health checks to decide whether a service can receive traffic. See [Render health checks](https://render.com/docs/health-checks).
- The blueprint has no CI pipeline. Before release, run the full backend and frontend checks and `npm audit` in CI, and require a clean diff and review. The Dockerfile skips Maven tests during image build, so a deployment build alone does not prove tests ran.
- Generate `APP_AUTH_TOKEN_SECRET` using a cryptographically secure generator with at least 32 bytes of entropy. Keep it in the host secret store, never in Git, and define a controlled rotation procedure (rotation invalidates all current sessions).
- `APP_TRUST_RENDER_CLIENT_IP=true` trusts the Render/Cloudflare `CF-Connecting-IP` value for login throttling. Verify real ingress behavior before relying on it. Keep it false on local development and do not enable it for a different proxy unless that proxy is known to overwrite the configured client-IP header. Add provider/edge controls for registration, reset, and verification-email abuse as well as login throttling.
- Enable monitoring and alerting for API errors, failed email delivery, database health, authentication abuse, and backup failures. Review request/application logging to ensure credentials and one-time links are never recorded. Add a documented incident and account-recovery process for legacy users without a verifiable mailbox.
- Review browser security headers and the sessionStorage bearer-token exposure to XSS. Confirm production CSP and other response-header policy for the static frontend before launch.

### 4. Release verification

Latest local verification on 2026-10-20: backend **28 passed, 0 failed, 0 skipped**; three Testcontainers PostgreSQL integration tests passed with Flyway migrations; frontend **13 passed**; lint and production build passed; `npm audit` reported **0 vulnerabilities**; `git diff --check` passed. Integration tests mock the email sender and do not verify SMTP delivery.

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

Integration tests use a disposable Testcontainers PostgreSQL instance and mock the email sender. They verify database migrations, account/token behavior, and the group-to-settlement workflow; they do not verify real SMTP delivery. Never replace the isolated test DB configuration with a production or Supabase URL.

## Configuration reference

| Purpose | Variables |
|---|---|
| Runtime PostgreSQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Flyway migration role | `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD` |
| SMTP | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `EMAIL_FROM` |
| Public origins | `APP_PUBLIC_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS`, frontend `VITE_API_BASE_URL` |
| Authentication / ingress | `APP_AUTH_TOKEN_SECRET`, `APP_TRUST_RENDER_CLIENT_IP` |

The API must not be declared production-ready until email delivery, dedicated database access and restore, trusted client-IP behavior, final origins/secrets, and the release review above are verified.
