# ZeroSum

ZeroSum is a React and Spring Boot expense sharing app for groups. It stores money as integer paise, calculates each member's net balance, and uses a deterministic greedy settlement engine to produce a practical payment plan with an explanation.

## Current functionality

- Create and manage groups, members, and expenses through the REST API.
- Create named participants from the UI, start a group with its creator, and add more members without manual database IDs. The MVP has no authentication; group creator is used as the current user in summaries.
- Split expenses equally, by exact paise amounts, percentage, or share units. Percentage inputs are integer basis points (`10000` = `100%`); exact inputs are paise. Percentage and share allocations distribute leftover paise in participant order.
- Calculate balances and optimized payments. The heap-based greedy method is deterministic and uses at most `n - 1` transfers; it is not guaranteed to find the globally minimum transfer count in every case.
- Preview a hypothetical payment at `POST /api/groups/{groupId}/settlement/whatif`. This recalculates in memory and does not persist the simulation.
- Manage daily, weekly, monthly, and yearly recurring rules at `/api/groups/{groupId}/recurring`; due expenses are generated once per rule/date by the scheduled worker.
- Record direct IOUs and mark them settled, separately from shared group expense balances.
- View date-filtered category spending insights and record completed settlement payments; completed payments adjust future balances and appear in payment history.
- Configure the browser API URL and backend CORS origins through environment variables.

Authentication and deployed hosting are not configured. Payments marked complete represent user-confirmed transfers; the app does not verify bank transactions.

## Requirements

- Java 17 or newer and Maven (or use the included Maven wrapper).
- Node.js and npm.
- PostgreSQL for a locally running backend.

## Run locally

1. Create an empty local PostgreSQL database and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for the backend. Flyway applies the schema migration at startup. Do not point local development at a shared database unless intended.
2. Start the backend from `backend/` with `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). The API listens on port `8080` by default; `PORT` can override it.
3. Start the frontend from `frontend/` with `npm ci` and `npm run dev`. `VITE_API_BASE_URL` is the backend origin, without `/api`; it defaults to `http://localhost:8080`.
4. Set `APP_CORS_ALLOWED_ORIGINS` as a comma-separated list of browser origins when hosting the API. The default is `http://localhost:5173`.

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
- `POST /api/users` to create a named participant for group workflows.

Expense create/update requests include `participantIds`; `splitType` is `equal`, `exact`, `percentage`, or `shares`. For non-equal splits, provide `splits: [{"userId": 1, "value": 2500}]`. `value` is paise for exact, basis points for percentage, and integer weight for shares.

What-if request example:

```json
{"fromUserId": 2, "toUserId": 1, "amountPaise": 5000}
```

The payer must currently have a debt and the recipient must currently be owed money. The amount cannot exceed either balance.

Recurring rule creation accepts `description`, `amountPaise`, `category`, `frequency` (`DAILY`, `WEEKLY`, `MONTHLY`, or `YEARLY`), `nextRunDate`, and `paidBy`. Generated occurrences split equally among the group members on the due date. The worker catches up missed dates and uniquely keys generated expenses by recurring rule and expense date.

## Verification

- Backend tests: `cd backend && ./mvnw test` (Windows: `mvnw.cmd test`).
- Frontend production build: `cd frontend && npm ci && npm run build`.
- Frontend lint: `cd frontend && npm run lint`.
- Frontend split-input tests: `cd frontend && npm test`.

## Deployment readiness

`backend/Dockerfile` and `render.yaml` prepare a Render API and static frontend. Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the backend host for a dedicated PostgreSQL database, and keep `APP_CORS_ALLOWED_ORIGINS` aligned with the frontend origin. Deployment has not been performed. The MVP has no authentication, so it needs an authentication layer before public production use.
