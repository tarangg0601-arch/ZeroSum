# ZeroSum

ZeroSum is a React and Spring Boot expense sharing app for groups. It stores money as integer paise, calculates each member's net balance, and uses a deterministic greedy settlement engine to produce a practical payment plan with an explanation.

## Current functionality

- Create and manage groups, members, and expenses through the REST API.
- Register with an email address, verify ownership by link, sign in, and recover a forgotten password by email. Existing accounts with an email can be claimed only by verifying access to that mailbox. Legacy rows without a verified email are not automatically linked.
- Group and financial routes require a signed bearer token and group membership. Only the group creator can add members or change/delete group settings. IOU and payment actions enforce participant permissions. Logout and password reset revoke earlier tokens.
- Split expenses equally, by exact paise amounts, percentage, or share units. Percentage inputs are integer basis points (`10000` = `100%`); exact inputs are paise. Percentage and share allocations distribute leftover paise in participant order.
- Calculate balances and optimized payments. The heap-based greedy method is deterministic and uses at most `n - 1` transfers; it is not guaranteed to find the globally minimum transfer count in every case.
- Preview a hypothetical payment at `POST /api/groups/{groupId}/settlement/whatif`. This recalculates in memory and does not persist the simulation.
- Manage daily, weekly, monthly, and yearly recurring rules at `/api/groups/{groupId}/recurring`; due expenses are generated once per rule/date by the scheduled worker.
- Record direct IOUs and mark them settled, separately from shared group expense balances.
- View date-filtered category spending insights and record completed settlement payments; completed payments adjust future balances and appear in payment history.
- Configure the browser API URL and backend CORS origins through environment variables.

SMTP delivery and deployed hosting require configuration and have not been verified here. Payments marked complete represent user-confirmed transfers; the app does not verify bank transactions.

## Requirements

- Java 17 or newer and Maven (or use the included Maven wrapper).
- Node.js and npm.
- PostgreSQL for a locally running backend.
- Docker Desktop for the isolated PostgreSQL Testcontainers integration test.

## Run locally

1. Create an empty local PostgreSQL database and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for the backend. Flyway applies the schema migrations at startup. Set `APP_AUTH_TOKEN_SECRET` to a random value of at least 32 bytes. Do not point local development at a shared database.
2. Start the backend from `backend/` with `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). The API listens on port `8080` by default; `PORT` can override it.
3. Start the frontend from `frontend/` with `npm ci` and `npm run dev`. `VITE_API_BASE_URL` is the backend origin, without `/api`; it defaults to `http://localhost:8080`.
4. Set `APP_CORS_ALLOWED_ORIGINS` as a comma-separated list of browser origins when hosting the API. The default is `http://localhost:5173`.
5. Configure email delivery with `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `EMAIL_FROM`, and `APP_PUBLIC_BASE_URL`. Use a provider sandbox for local testing and never put credentials in source control.

## API overview

- `GET/POST /api/groups`
- `GET/PUT/DELETE /api/groups/{groupId}`
- `GET/POST /api/groups/{groupId}/members`
- `GET/POST /api/groups/{groupId}/expenses`
- `GET/PUT/DELETE /api/groups/{groupId}/expenses/{expenseId}`
- `GET /api/groups/{groupId}/balances`
- `GET /api/groups/{groupId}/settlement`
- `POST /api/groups/{groupId}/settlement/whatif`
- `POST /api/groups/{groupId}/settlement/payments` and `GET /api/groups/{groupId}/settlement/history`
- `GET/POST /api/groups/{groupId}/recurring`
- `PATCH /api/groups/{groupId}/recurring/{ruleId}` with `{"active": false}` to pause or `true` to resume.
- `GET /api/groups/{groupId}/insights?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `POST /api/ious`, `GET /api/groups/{groupId}/ious`, and `PATCH /api/ious/{iouId}` with `{"settled": true}`.
- `GET /api/health`
- `POST /api/auth/register`, `POST /api/auth/login`, and `POST /api/auth/logout`.
- `POST /api/auth/verify-email`, `POST /api/auth/forgot-password`, and `POST /api/auth/reset-password`.
- `POST /api/auth/claim-legacy-account` and `POST /api/auth/complete-legacy-claim`.
- `GET /api/users/me` for the authenticated account.

Expense create/update requests include `participantIds`; `splitType` is `equal`, `exact`, `percentage`, or `shares`. For non-equal splits, provide `splits: [{"userId": 1, "value": 2500}]`. `value` is paise for exact, basis points for percentage, and integer weight for shares.

What-if request example:

```json
{"fromUserId": 2, "toUserId": 1, "amountPaise": 5000}
```

To preview a hypothetical expense without saving it, send an `expense` instead:

```json
{"expense":{"description":"Hotel","amountPaise":12000,"category":"travel","expenseDate":"2026-10-20","paidBy":2,"participantIds":[1,2],"splitType":"equal"}}
```

The payer must currently have a debt and the recipient must currently be owed money. The amount cannot exceed either balance.

Recurring rule creation accepts `description`, `amountPaise`, `category`, `frequency` (`DAILY`, `WEEKLY`, `MONTHLY`, or `YEARLY`), `nextRunDate`, and `paidBy`. Generated occurrences split equally among the group members on the due date. The worker catches up missed dates and uniquely keys generated expenses by recurring rule and expense date.

## Verification

- Backend unit and isolated PostgreSQL tests: `cd backend && ./mvnw test` (Windows PowerShell: `$env:DOCKER_HOST='npipe:////./pipe/docker_engine'; .\mvnw.cmd test`). The suite starts a temporary `postgres:16-alpine` Testcontainers database; it does not read `DB_URL` or access Supabase.
- Frontend production build: `cd frontend && npm ci && npm run build`.
- Frontend lint: `cd frontend && npm run lint`.
- Frontend split-input tests: `cd frontend && npm test`.

## Deployment readiness

`backend/Dockerfile` and `render.yaml` prepare a Render API and static frontend. Configure a dedicated PostgreSQL database, `APP_CORS_ALLOWED_ORIGINS`, `APP_AUTH_TOKEN_SECRET`, SMTP variables, `EMAIL_FROM`, and the HTTPS `APP_PUBLIC_BASE_URL` in the host's secret/configuration store. Verify actual email delivery, proxy IP handling, account recovery, database backup/restore, and monitoring before deployment. See [DEPLOYMENT_SECURITY.md](DEPLOYMENT_SECURITY.md). Deployment has not been performed and the app is not declared production-ready.
