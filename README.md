# ZeroSum

ZeroSum is a React and Spring Boot expense sharing app for groups. It stores money as integer paise, calculates each member's net balance, and uses a deterministic greedy settlement engine to produce a practical payment plan with an explanation.

## Current functionality

- Create and manage groups, members, and expenses through the REST API.
- Register and sign in using a unique, case-insensitive username and password. A successful registration creates an active account and session immediately; email, SMTP, Resend, and verification links are not used.
- Passwords use Spring Security PBKDF2 hashing. JWTs are signed with `APP_AUTH_TOKEN_SECRET`, expire after 15 minutes, and logout revokes active tokens. Login failures are limited to five per username/address bucket in 15 minutes. User IDs from the browser are never used as identity proof.
- Forgotten passwords cannot currently be recovered through the UI. There is no unauthenticated password-reset endpoint. Users should keep their password safe.
- Existing user rows, group memberships, and financial data are retained. V7 adds a nullable username column and a case-insensitive unique index. Legacy rows are deliberately left without usernames; they cannot use the new login until an administrator has separately verified the account holder and assigned a username through an approved migration process. The application never auto-assigns legacy usernames or allows an unverified user to claim financial history.
- Group and financial routes require a signed bearer token and group membership. Only the group creator can add members or change/delete group settings. IOU and payment actions enforce participant permissions. Group joins use the authenticated JWT identity and rate-limit codes per caller address. Concurrent payments lock the group and recheck balances, so stale duplicate submissions are rejected.
- Split expenses equally, by exact paise amounts, percentage, or share units. Percentage inputs are integer basis points (`10000` = `100%`); exact inputs are paise. Uneven paise allocations use stable user-ID order.
- Calculate balances and optimized payments. The heap-based greedy method is deterministic and uses at most `n - 1` transfers; it is not guaranteed to find the globally minimum transfer count in every case.
- Preview hypothetical payments and expenses without saving them. Manage daily, weekly, monthly, and yearly recurring rules; record direct IOUs; view payment history and date-filtered category insights.
- Configure the browser API URL and backend CORS origins through environment variables.

Payments marked complete represent user-confirmed transfers; the app does not verify bank transactions.

## Requirements

- Java 17 or newer and Maven (or use the included Maven wrapper).
- Node.js and npm.
- PostgreSQL for a locally running backend.
- Docker Desktop for the isolated PostgreSQL Testcontainers integration test.

## Run locally

1. Create an empty local PostgreSQL database and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for the backend. Flyway applies the schema migrations at startup. For separate migration credentials, set `FLYWAY_DB_USERNAME` and `FLYWAY_DB_PASSWORD`; otherwise Flyway uses the runtime DB credentials. Set `APP_AUTH_TOKEN_SECRET` to a random value of at least 32 bytes. Do not point local development at a shared database.
2. Start the backend from `backend/` with `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). No email-provider credentials or SMTP service are required. The API listens on port `8080` by default; `PORT` can override it.
3. Start the frontend from `frontend/` with `npm ci` and `npm run dev`. `VITE_API_BASE_URL` is the backend origin, without `/api`; it defaults to `http://localhost:8080`.
4. Set `APP_CORS_ALLOWED_ORIGINS` as a comma-separated list of browser origins when hosting the API. The default is `http://localhost:5173`.

## API overview

- `GET/POST /api/groups`
- `POST /api/groups/join`
- `GET/PUT/DELETE /api/groups/{groupId}`
- `GET/POST /api/groups/{groupId}/members` (group owner adds an existing account by username)
- `GET/POST /api/groups/{groupId}/expenses`
- `GET/PUT/DELETE /api/groups/{groupId}/expenses/{expenseId}`
- `GET /api/groups/{groupId}/balances` and `GET /api/groups/{groupId}/settlement`
- `POST /api/groups/{groupId}/settlement/whatif`
- `POST /api/groups/{groupId}/settlement/payments` and `GET /api/groups/{groupId}/settlement/history`
- `GET/POST /api/groups/{groupId}/recurring` and `PATCH /api/groups/{groupId}/recurring/{ruleId}`
- `GET /api/groups/{groupId}/insights?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `POST /api/ious`, `GET /api/groups/{groupId}/ious`, and `PATCH /api/ious/{iouId}`
- `GET /api/health`
- `POST /api/auth/register` and `POST /api/auth/login` accept `username` and `password`; registration also accepts optional `name`. Both return a bearer token. `POST /api/auth/logout` revokes the current session.
- `GET /api/users/me` returns the authenticated account profile.

Expense create/update requests include `participantIds`; `splitType` is `equal`, `exact`, `percentage`, or `shares`. For non-equal splits, provide `splits: [{"userId": 1, "value": 2500}]`. `value` is paise for exact, basis points for percentage, and integer weight for shares.

What-if settlement plans are read-only projections; only recorded payments create financial history. The initial schema included an unused `settlement_runs` table, which V6 renames to `settlement_runs_legacy` while preserving its rows and foreign keys. V5 adds recurring schedule anchors and V7 adds usernames without changing legacy user rows.

## Verification

- Backend unit and isolated PostgreSQL tests: from `backend/`, set `$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'` on Windows, then run `.\mvnw.cmd test -B -ntp`. The suite starts a disposable `postgres:16-alpine` Testcontainers database; it does not read `DB_URL` or access Supabase.
- Frontend checks from `frontend/`: `npm ci`, `npm test`, `npm run lint`, `npm run build`, `npm audit --cache .npm-cache`.
- Run `git diff --check` from the repository root.

See [DEPLOYMENT_SECURITY.md](DEPLOYMENT_SECURITY.md) for production security requirements and the legacy-account migration limitation.
