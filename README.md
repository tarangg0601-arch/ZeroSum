# ZeroSum — Expense Sharing & Settlement Platform

ZeroSum is a full-stack expense-sharing application built for roommates, hostel groups, friends, and travel companions. It helps groups manage shared expenses, calculate who owes whom, and generate settlement plans using a greedy algorithm.

The application brings expense tracking, flexible splitting, balance calculation, and payment settlement together in one place.

## Live Demo

- **Website:** [https://zero-sum-five.vercel.app](https://zero-sum-five.vercel.app)
- **Backend API health:** [https://zerosum-api-gh7l.onrender.com/api/health](https://zerosum-api-gh7l.onrender.com/api/health)
- **Source code:** [GitHub Repository](https://github.com/tarangg0601-arch/ZeroSum)

The frontend is hosted on Vercel, the Spring Boot backend runs on Render, and PostgreSQL is hosted on Neon.

## Features

### Groups and Members
- Create and manage expense groups.
- Add members and join groups using invitation codes.
- View group members, expenses, and balances.
- Organize expenses for general, hostel, and trip-related groups.

### Expense Management
- Record shared expenses and select the participants.
- Split expenses equally, by exact amounts, percentages, or shares.
- Validate allocations and handle rounding using integer paise.
- Manage expenses with appropriate validation and safeguards.

### Settlement Optimization
- Calculate each member's net balance.
- Generate payment suggestions using a greedy settlement algorithm.
- Visualize debt relationships and suggested payments.
- Preview hypothetical payments and expenses without saving the simulation.
- Record payments and update outstanding balances.
- Review settlement and payment history.

The settlement engine uses a deterministic greedy strategy to match members who owe money with members who are owed money. It produces practical settlement suggestions, but it does not guarantee the globally minimum number of transactions for every possible balance configuration.

### Recurring Expenses
- Configure recurring expense rules.
- Support recurring schedules such as daily, weekly, monthly, and yearly where configured.
- Pause and resume recurring rules.
- Prevent duplicate expense occurrences and handle month-end dates.

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
- Apply token expiration and login rate limiting.

Password recovery is not currently available through the normal user interface.

## Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React, Vite, Tailwind CSS |
| Routing | React Router |
| Backend | Java, Spring Boot |
| API | REST APIs |
| Database | PostgreSQL |
| Online database hosting | Neon |
| Database migrations | Flyway |
| Persistence | Spring Data JPA, Hibernate |
| Authentication | Password hashing, signed bearer tokens |
| Backend testing | JUnit, Spring Boot Test, Testcontainers |
| Frontend testing | Vitest, Node.js tests |
| Local database | Docker Desktop, PostgreSQL |
| Frontend hosting | Vercel |
| Backend hosting | Render |

## Architecture

ZeroSum separates the user interface, backend business logic, and persistent data storage.

1. **Frontend:** React handles the dashboard, groups, expenses, balances, recurring rules, insights, and user interactions.
2. **Backend:** Spring Boot exposes REST APIs, validates requests, enforces authorization, calculates balances, and runs the settlement algorithm.
3. **Database:** PostgreSQL stores users, groups, memberships, expenses, splits, recurring rules, IOUs, and payment records.
4. **Migrations:** Flyway manages database schema changes through versioned migration files.
5. **Testing:** Automated tests cover backend logic, frontend behavior, and integration flows against disposable PostgreSQL databases.

### Application flow

```text
Browser
   |
   v
React + Vite frontend (Vercel)
   |
   v
Spring Boot REST API (Render)
   |
   v
PostgreSQL database (Neon)
```

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
│   ├── mvnw.cmd
│   └── run-neon.ps1
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── pages/
│   │   └── utils/
│   ├── package.json
│   ├── vercel.json
│   └── .env.example
├── render.yaml
├── DEPLOYMENT_SECURITY.md
└── README.md
```

## Requirements

For local development, you need:

- Java 17 or newer
- Node.js and npm
- Git
- Docker Desktop with its container engine running
- PostgreSQL, either through Docker or a configured hosted database

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

Check that it is running:

```cmd
docker ps
```

Verify that PostgreSQL accepts connections:

```cmd
docker exec zerosum-dev-postgres pg_isready -U postgres -d zerosum
```

The local Docker database is separate from the online Neon database. Local data does not automatically appear in the online application.

### 3. Configure and start the backend

Open a new Command Prompt window:

```cmd
cd /d C:\ZeroSum\backend
```

Set the local database connection:

```cmd
set "DB_URL=jdbc:postgresql://localhost:5432/zerosum"
set "DB_USERNAME=postgres"
set "DB_PASSWORD=local-only-zs-password"
set "DB_SSLMODE=disable"
```

Configure the frontend origin and a local development authentication secret:

```cmd
set "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173"
set "APP_AUTH_TOKEN_SECRET=local-development-only-secret-change-this-1234567890"
```

Start the backend:

```cmd
mvnw.cmd spring-boot:run
```

Flyway applies pending migrations during application startup. The backend normally listens on port `8080`.

Keep this terminal open while using the local application.

The example authentication secret is only for local development. Generate a strong, unique secret for each hosted environment and store it securely.

### 4. Start the frontend

Open another Command Prompt window:

```cmd
cd /d C:\ZeroSum\frontend
npm ci
```

Set the local backend URL:

```cmd
set "VITE_API_BASE_URL=http://localhost:8080"
```

Start Vite:

```cmd
npm run dev
```

Open the local website:

[http://localhost:5173](http://localhost:5173)

Keep the frontend, backend, and local database running while using the local application.

## Online Deployment

The public application uses three separate services.

### Frontend — Vercel

The React/Vite frontend is deployed at:

[https://zero-sum-five.vercel.app](https://zero-sum-five.vercel.app)

The frontend uses the `VITE_API_BASE_URL` environment variable to communicate with the backend. Vite environment variables are included in browser-facing code, so they must never contain database passwords or authentication secrets.

### Backend — Render

The Spring Boot API is deployed at:

[https://zerosum-api-gh7l.onrender.com](https://zerosum-api-gh7l.onrender.com)

Health endpoint:

[https://zerosum-api-gh7l.onrender.com/api/health](https://zerosum-api-gh7l.onrender.com/api/health)

The backend requires secure environment variables for database connectivity, authentication, CORS, and other runtime settings.

The Render Free service may sleep after inactivity, so the first request after a period without traffic may take longer to respond.

### Database — Neon PostgreSQL

The online application uses a PostgreSQL database hosted by Neon.

The backend connects to Neon over TLS. Flyway manages schema migrations, and database credentials are configured outside the source code.

The online database is separate from the local Docker database. Never commit connection strings, database passwords, or other secrets to Git.

Free-tier services have resource limits and may change their terms. Check each provider's current plan limits before relying on them for important data.

## Configuration

The backend uses environment variables for its database and security configuration.

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Runtime database username |
| `DB_PASSWORD` | Runtime database password |
| `DB_SSLMODE` | PostgreSQL SSL mode |
| `FLYWAY_DB_USERNAME` | Optional separate migration username |
| `FLYWAY_DB_PASSWORD` | Optional separate migration password |
| `APP_AUTH_TOKEN_SECRET` | Secret used to sign authentication tokens |
| `APP_CORS_ALLOWED_ORIGINS` | Permitted frontend origins |
| `PORT` | Backend listening port, normally `8080` |
| `VITE_API_BASE_URL` | Backend API origin used by the frontend |

Configure environment variables in the terminal or hosting provider's settings. Do not assume Spring Boot automatically reads `.env` files.

Variables prefixed with `VITE_` are exposed to frontend code. Never store database passwords, authentication secrets, or private backend credentials in them.

For local Neon connectivity, the backend includes `run-neon.ps1`. Use the launcher's instructions to configure the connection securely, and never publish database credentials.

## Testing

### Backend tests

Run from the backend directory:

```cmd
cd /d C:\ZeroSum\backend
mvnw.cmd test -B -ntp
```

PostgreSQL integration tests use Testcontainers to start disposable databases. Docker Desktop must be running for these tests.

Never point integration tests at the shared online database.

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

Review actual test results and investigate failures or skipped integration tests. A skipped database test should not be treated as successful database verification.

## Database and Data Safety

- Local development data is stored in PostgreSQL running through Docker.
- Online application data is stored in the Neon PostgreSQL database.
- Flyway manages schema changes through versioned migrations.
- Integration tests must use disposable databases and test data.
- Never commit passwords, API keys, authentication secrets, or local environment files.
- Do not reset or delete a database containing data that must be preserved.
- Verify backups and restoration procedures before relying on the application for important financial records.

Publishing source code to GitHub does not publish or migrate the database.

## API Overview

The Spring Boot backend provides REST endpoints for authentication, user profiles, groups, memberships, expenses, balances, settlements, recurring expenses, IOUs, insights, and payment history.

The health endpoint is available at `/api/health`.

Protected financial information must only be accessible to authenticated users with the required group membership or participant permissions.

## Current Limitations

- Password recovery is not available through the normal user interface.
- The application records user-confirmed payments; it does not verify bank transfers.
- The greedy settlement algorithm produces practical deterministic settlement suggestions but does not guarantee the globally minimum number of transactions for every possible balance configuration.
- Production backups and restoration procedures should be verified before storing important real financial records.

## Deployment Security

The deployment uses separate frontend, backend, and database services. Keep the following practices in place:

- Store credentials and authentication secrets in hosting-provider environment settings.
- Keep database connections encrypted with TLS.
- Restrict CORS to the intended frontend origin.
- Use HTTPS for public access.
- Do not expose backend credentials through frontend variables or Git commits.
- Review authentication, authorization, security headers, monitoring, and backup procedures periodically.

For additional security and operational requirements, see [DEPLOYMENT_SECURITY.md](DEPLOYMENT_SECURITY.md).
