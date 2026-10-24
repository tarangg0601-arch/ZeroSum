# ZeroSum — Expense Sharing & Settlement Platform

ZeroSum is a full-stack expense-sharing application for roommates, hostel groups, friends, and travel companions. It helps groups track shared expenses, calculate who owes whom, and generate a settlement plan that reduces the number of payments required to balance everyone's accounts.

The application combines a React frontend, a Spring Boot REST API, and PostgreSQL to manage expenses and settlements in one place.

## Features

### Groups and Members
- Create and manage expense groups.
- Add members and join groups using invitation codes.
- View group expenses, members, and balances.
- Organize expenses for general, hostel, and trip-related groups.

### Expense Management
- Record shared expenses and select the participants.
- Split expenses equally, by exact amounts, percentages, or shares.
- Validate split amounts and handle rounding using integer paise.
- Manage expenses with appropriate validation and safeguards.

### Settlement Optimization
- Calculate each member's net balance.
- Generate payment suggestions using a greedy settlement algorithm.
- Visualize debt relationships and optimized payments.
- Preview hypothetical payments and expenses without saving the simulation.
- Record payments and update outstanding balances.
- Review settlement and payment history.

The settlement engine uses a deterministic greedy strategy to match members who owe money with members who are owed money. It produces practical settlement suggestions, but it does not guarantee the globally minimum number of transactions for every possible balance configuration.

### Recurring Expenses
- Configure recurring expense rules.
- Support daily, weekly, monthly, and yearly schedules where configured.
- Pause and resume recurring rules.
- Prevent duplicate occurrences and handle month-end scheduling.

### Financial Tracking
- Track direct IOUs between members.
- Record when an IOU is settled.
- Review payment history and outstanding balances.
- View category-based spending insights.
- Simulate hypothetical changes without modifying actual financial records.

### Authentication and Security
- Register and log in using a username and password.
- Authenticate API requests using signed bearer tokens.
- Enforce group membership and access permissions.
- Hash passwords and validate sensitive operations.
- Apply token expiry and login rate limiting.

Password recovery is not currently available through the normal user interface.

## Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React, Vite, Tailwind CSS |
| Routing | React Router |
| Backend | Java, Spring Boot |
| API | REST APIs |
| Database | PostgreSQL |
| Database migrations | Flyway |
| Persistence | Spring Data JPA, Hibernate |
| Authentication | Password hashing, signed bearer tokens |
| Backend testing | JUnit, Spring Boot Test, Testcontainers |
| Frontend testing | Vitest, Node.js tests |
| Local database | Docker Desktop, PostgreSQL |

## Project Structure

```text
ZeroSum/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       └── db/migration/
│   │   └── test/
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── pages/
│   │   └── utils/
│   ├── package.json
│   └── .env.example
├── render.yaml
├── DEPLOYMENT_SECURITY.md
└── README.md
```

## Requirements

- Java 17 or newer
- Node.js and npm
- Git
- Docker Desktop with its container engine running
- A PostgreSQL database for local development

The backend includes a Maven wrapper, so a separate Maven installation is normally unnecessary.

## Getting Started on Windows

### 1. Clone the repository

```cmd
git clone https://github.com/tarangg0601-arch/ZeroSum.git
cd ZeroSum
```

### 2. Start the local PostgreSQL database

Make sure Docker Desktop is running. Create a local development database container:

```cmd
docker run --name zerosum-dev-postgres -e POSTGRES_DB=zerosum -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=local-only-zs-password -p 5432:5432 -v zerosum-postgres-data:/var/lib/postgresql/data -d postgres:16-alpine
```

If the container already exists, start it instead:

```cmd
docker start zerosum-dev-postgres
```

Check the container:

```cmd
docker ps
```

Wait until PostgreSQL accepts connections:

```cmd
docker exec zerosum-dev-postgres pg_isready -U postgres -d zerosum
```

The local Docker database is separate from any hosted database.

### 3. Configure and start the backend

Open a new Command Prompt window:

```cmd
cd /d C:\ZeroSum\backend
```

Configure the local database connection:

```cmd
set "DB_URL=jdbc:postgresql://localhost:5432/zerosum"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=local-only-zs-password"
```

Configure the frontend origin and authentication secret:

```cmd
set "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173"
set "APP_AUTH_TOKEN_SECRET=local-development-only-secret-change-this-1234567890"
```

Start Spring Boot:

```cmd
mvnw.cmd spring-boot:run
```

Flyway applies pending database migrations during startup. The backend normally listens on port `8080`.

Keep this terminal open.

The authentication secret above is only an example for local development. Generate a strong, unique secret for each hosted environment and store it securely.

### 4. Install and start the frontend

Open another Command Prompt window:

```cmd
cd /d C:\ZeroSum\frontend
npm ci
```

Set the backend API URL:

```cmd
set "VITE_API_BASE_URL=http://localhost:8080"
```

Start the development server:

```cmd
npm run dev
```

Open the local website:

**http://localhost:5173**

Keep the backend, frontend, and database running while using the application.

## Configuration

The backend uses environment variables for its database and security configuration.

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Runtime database username |
| `DB_PASSWORD` | Runtime database password |
| `FLYWAY_DB_USERNAME` | Optional separate migration username |
| `FLYWAY_DB_PASSWORD` | Optional separate migration password |
| `APP_AUTH_TOKEN_SECRET` | Secret used to sign authentication tokens |
| `APP_CORS_ALLOWED_ORIGINS` | Permitted frontend origins |
| `PORT` | Backend listening port, normally `8080` |
| `VITE_API_BASE_URL` | Backend API origin used by the frontend |

Configure environment variables in the terminal or hosting provider's settings. Do not assume Spring Boot automatically reads `.env` files.

Variables prefixed with `VITE_` are exposed to frontend code. Never put database passwords, authentication secrets, or other confidential backend credentials in them.

## Testing

### Backend tests

Run from the backend directory:

```cmd
cd /d C:\ZeroSum\backend
mvnw.cmd test -B -ntp
```

The PostgreSQL integration tests use Testcontainers to run against disposable databases. Docker Desktop must be running for these tests. Integration tests must not target a shared development or production database.

### Frontend tests

```cmd
cd /d C:\ZeroSum\frontend
npm test
```

### Lint

```cmd
npm run lint
```

### Production build

```cmd
npm run build
```

### Dependency audit

```cmd
npm audit
```

Review actual test output and investigate failures or skipped integration tests. A skipped database test should not be treated as a successful database verification.

## Database and Data Safety

- Local development data is stored in PostgreSQL running through Docker.
- Online data requires a separately configured hosted PostgreSQL database.
- Flyway manages schema changes through versioned migrations.
- Integration tests must use disposable databases and test data.
- Never commit passwords, API keys, authentication secrets, or local environment files.
- Do not reset or delete a database containing data that must be preserved.
- Verify backups and restoration procedures before storing important real financial records.

Publishing the source code to GitHub does not publish or migrate the database.

## API Overview

The Spring Boot backend provides REST endpoints for authentication, user profiles, groups, memberships, expenses, balances, settlements, recurring expenses, IOUs, insights, and payment history.

The health endpoint is available at `/api/health`.

Protected financial data must be accessible only to authenticated users who have the required group membership or participant permissions.

## Current Limitations

- Password recovery is not available through the normal user interface.
- The application records user-confirmed payments; it does not verify bank transfers.
- The greedy settlement algorithm is deterministic but does not guarantee the globally minimum number of transactions in every case.
- Public hosting, production database backup and restoration, and live deployment behavior must be verified in their actual environments before the application is considered production-ready.

## Deployment

The frontend and backend can be deployed separately.

The frontend needs `VITE_API_BASE_URL` configured to point to the deployed backend. The backend needs a reachable PostgreSQL database, secure environment variables, correct CORS origins, and a valid authentication signing secret.

Production readiness also requires secure database credentials, HTTPS, access controls, appropriate security headers, monitoring, and verified backup and recovery procedures.

Deployment is not complete until the public website can authenticate users, create groups, record and split expenses, calculate balances, and record payments through the deployed backend and database.

For additional deployment requirements, see [DEPLOYMENT_SECURITY.md](DEPLOYMENT_SECURITY.md).
