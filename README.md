# ZeroSum

ZeroSum is a React and Spring Boot expense sharing app for groups. It stores money as integer paise, calculates each member's net balance, and uses a deterministic greedy settlement engine to produce a practical payment plan with an explanation.

## Current functionality

- Create and manage groups, members, and expenses through the REST API.
- Split expenses equally, by exact paise amounts, percentage, or share units. Percentage inputs are integer basis points (`10000` = `100%`); exact inputs are paise. Percentage and share allocations distribute leftover paise in participant order.
- Calculate balances and optimized payments. The heap-based greedy method is deterministic and uses at most `n - 1` transfers; it is not guaranteed to find the globally minimum transfer count in every case.
- Preview a hypothetical payment at `POST /api/groups/{groupId}/settlement/whatif`. This recalculates in memory and does not persist the simulation.
- Configure the browser API URL and backend CORS origins through environment variables.

The database migration also defines tables for recurring rules, IOUs, and settlement history. Their user-facing workflows and APIs are not implemented yet. Authentication and deployed hosting are also not configured.

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
- `GET /api/health`

Expense create/update requests include `participantIds`; `splitType` is `equal`, `exact`, `percentage`, or `shares`. For non-equal splits, provide `splits: [{"userId": 1, "value": 2500}]`. `value` is paise for exact, basis points for percentage, and integer weight for shares.

What-if request example:

```json
{"fromUserId": 2, "toUserId": 1, "amountPaise": 5000}
```

The payer must currently have a debt and the recipient must currently be owed money. The amount cannot exceed either balance.

## Verification

- Backend tests: `cd backend && ./mvnw test` (Windows: `mvnw.cmd test`).
- Frontend production build: `cd frontend && npm ci && npm run build`.
- Frontend lint: `cd frontend && npm run lint`.

## Deployment readiness

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the backend host, `PORT` as provided by the host, and `APP_CORS_ALLOWED_ORIGINS` to the deployed frontend origin. Build the frontend with `VITE_API_BASE_URL` set to the backend origin. No deployment files or hosted environment are currently configured; deployment has not been performed.
