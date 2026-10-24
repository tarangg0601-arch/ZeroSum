# Production release readiness

**Status: not ready for public production.** This change removes email as an authentication dependency. No deployment or remote CI run has been performed.

## Authentication implementation

- Registration and login use a unique username and password. Usernames are trimmed, normalized to lowercase for storage, restricted to 3–32 ASCII letters, digits, and underscores, and protected by a database case-insensitive unique index. The database remains the final arbiter for concurrent duplicate registrations.
- Display names are optional. Passwords use Spring Security PBKDF2 hashing; plaintext passwords are not returned or logged. Successful registration returns a JWT and creates an active session without sending email.
- JWTs are signed using `APP_AUTH_TOKEN_SECRET`, include the account subject and auth version, and expire after 15 minutes. Logout increments the auth version and invalidates earlier tokens. Login failures are limited to five per hashed username/client-address bucket in a 15-minute window. Proxy headers affect the bucket only when `APP_TRUST_RENDER_CLIENT_IP=true` and the forwarded value is a valid IP literal.
- Group and financial endpoints continue to require JWT authentication and group membership. Backend authorization uses the authenticated JWT subject; browser-stored user IDs are not identity evidence. Group membership, payment, IOU, and owner permissions remain enforced server-side.
- The UI and API no longer include verification, email registration, password reset, or legacy-claim flows. Forgotten passwords cannot currently be recovered through the normal user interface. There is intentionally no unauthenticated reset endpoint. Users must keep passwords safe; account recovery requires a separately designed, identity-verified process.

## Database compatibility and legacy accounts

- V1–V6 are unchanged. Forward-only V7 adds a nullable `users.username`, a format check, and a case-insensitive unique index. It does not update legacy usernames, delete rows, drop columns/tables, or alter group memberships or financial records.
- Existing rows retain their email fields, password hashes, auth versions, groups, expenses, IOUs, and payment history. Existing email tokens and rate-limit tables are left in place for compatibility but are no longer used by the application. The application no longer maps or reads the old email-token records.
- Legacy accounts receive no generated username and cannot log in through username/password until identity has been verified and a username assigned through an approved migration process. Do not infer ownership from a supplied email address or user ID. Before applying V7 to a populated production database, define and communicate the identity verification, username assignment, conflict resolution, and recovery process. A bulk automatic email-to-username migration is unsafe and is not implemented.
- Hibernate schema updates remain disabled; Flyway owns schema changes. Integration tests use only a disposable PostgreSQL Testcontainers instance and do not connect to the configured Supabase database.

## Local configuration

No SMTP, Resend, sender address, or public email-link URL configuration is required. The backend no longer includes the mail starter or email sender code. Local development still requires a PostgreSQL database and `APP_AUTH_TOKEN_SECRET`.

## Verified in repository configuration

- `render.yaml` configures a Docker API, static frontend, health check, CSP/security headers, and environment references for the database, CORS, token signing, and frontend API origin. It does not provision PostgreSQL.
- The health endpoint checks database connectivity. Hibernate schema changes are disabled. Runtime DB credentials and Flyway migration credentials can be configured separately.
- CORS uses configured origins and does not enable credentialed cookies. The Render client IP header is trusted only when explicitly enabled.
- The frontend bearer token remains in `sessionStorage` and is readable by injected script until expiration. CSP and the 15-minute token lifetime reduce exposure but do not eliminate XSS risk. Backend authorization remains authoritative. Review XSS protections and verify actual deployed security headers before production.
- GitHub Actions is configured to run Maven tests with Docker/Testcontainers, frontend tests, lint, build, and dependency audit. Its hosted run has not been verified.

## Outstanding requirements before deployment

1. **Plan legacy account migration.** Identify the production user count and records, define an identity-verified username assignment process, handle username collisions, notify affected users, and test the process on a database copy. Never automatically derive usernames from legacy email addresses.
2. **Provision production PostgreSQL.** Use a dedicated managed database, TLS, private networking where available, least-privilege runtime and migration roles, backups/retention, monitoring, and a documented restore drill. Do not use the existing Supabase database unless its owner separately approves that use; these tests did not touch it.
3. **Set production secrets and origins.** Generate a random `APP_AUTH_TOKEN_SECRET` with at least 32 bytes of entropy. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD`, exact HTTPS `APP_CORS_ALLOWED_ORIGINS`, frontend `VITE_API_BASE_URL`, and the host's `APP_TRUST_RENDER_CLIENT_IP` only after validating ingress behavior. Store secrets in the hosting secret manager, never in Git.
4. **Choose and verify a hosting plan.** `render.yaml` uses a preview/free API plan and template domains. Select a suitable paid API/database plan, set the actual service domains, confirm health checks, and verify CSP `connect-src`, CORS, TLS, and response headers on the deployed app.
5. **Run CI and release checks.** Confirm GitHub Actions succeeds and protect the release branch with required checks. Test registration, duplicate usernames, login throttling, logout, expired tokens, group authorization, and account isolation against the final production-like configuration.
6. **Choose an account recovery policy.** Until a safe recovery process is implemented, communicate clearly that lost passwords cannot be reset through the app. Do not add an unauthenticated password change/reset route.
7. **Review session storage and monitoring.** Consider an HttpOnly secure cookie or another XSS-resistant session design, test CSP and script injection mitigations, and configure alerting for API, database, authentication abuse, and backup failures.

The hosting choice may require a paid provider account and production database plan. There are no email-provider accounts, sender domains, API keys, SMTP credentials, or email delivery costs required by this authentication design.

## Configuration reference

| Purpose | Variables |
|---|---|
| Runtime PostgreSQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Flyway migration role | `FLYWAY_DB_USERNAME`, `FLYWAY_DB_PASSWORD` |
| CORS / frontend API | `APP_CORS_ALLOWED_ORIGINS`, frontend `VITE_API_BASE_URL` |
| Authentication / ingress | `APP_AUTH_TOKEN_SECRET`, `APP_TRUST_RENDER_CLIENT_IP` |

## Verification commands

```powershell
$env:MAVEN_USER_HOME='C:\ZeroSum\.maven'
$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'
cd C:\ZeroSum\backend
.\mvnw.cmd "-Dmaven.repo.local=C:\ZeroSum\backend\.m2\repository" test -B -ntp
cd C:\ZeroSum\frontend
npm ci
npm test
npm run lint
npm run build
npm audit --cache .npm-cache
cd C:\ZeroSum
git diff --check
```

The PostgreSQL integration suite runs a disposable `postgres:16-alpine` container and verifies Flyway migration plus authentication and group-to-settlement behavior. It does not use production or Supabase credentials.
