[![Java CI with Maven](https://github.com/Miguel-Pimenta/car-build-log/actions/workflows/maven.yml/badge.svg)](https://github.com/Miguel-Pimenta/car-build-log/actions/workflows/maven.yml)

# Car Build Log

**▶ [Live demo](https://car-build-log-frontend.onrender.com)** — sign in as **`demo`** / **`password123`**, or register your own account.

> ⏳ Hosted on free-tier infrastructure, so the instances sleep when idle. **The first request can take up to a minute** while the frontend and API wake up — after that it's fast.
>
> API: [`car-build-log.onrender.com`](https://car-build-log.onrender.com) · [Swagger UI](https://car-build-log.onrender.com/swagger-ui.html) · [Health](https://car-build-log.onrender.com/actuator/health)

A full-stack app for tracking modified-car builds — vehicles, the modifications made
to them, and dyno results — with a derived **build-summary** endpoint that aggregates
total spend, spend-by-category, and the latest power/torque figures.

- **Backend** — a Spring Boot REST API (the bulk of the project; documented below).
- **Frontend** — a React / Next.js UI in [`frontend/`](frontend/).

**Stack:** Java 21 · Spring Boot 3.5 · Spring Security + JWT · Spring Data JPA · PostgreSQL 16 · Docker · React (Next.js) · TypeScript · TanStack Query

```
car-build-log/
├── src/, pom.xml     Spring Boot backend (REST API)
├── docs/CONCEPTS.md  Longer-form notes on the concepts used
├── docs/DEPLOY.md    Alternative AWS (EC2 + RDS) deployment runbook
└── frontend/         React / Next.js frontend (see frontend/README.md)
```

---

## Architecture

A conventional layered REST service, organised **package-by-layer** — the same shape as a typical Spring microservice (a single bounded context split by technical layer):

```
HTTP ─▶ Controller ─▶ Service ─▶ Repository (Spring Data JPA) ─▶ PostgreSQL
```

```
src/main/java/com/miguelpimenta/buildlog/
├── controller/   REST controllers (Auth, Vehicle, Modification, Dyno, VehicleSummary)
├── service/      Business logic + transaction boundaries
├── repository/   Spring Data JPA repositories (every query scoped to the caller)
├── model/        JPA entities (User, Vehicle, Modification, DynoResult)
├── dto/          Request / response records
├── mapper/       Entity <-> DTO mappers
├── security/     JWT filter, token service, user details, current-user lookup
├── config/       Security, CORS, OpenAPI
└── exception/    Global handler, error + pagination responses
```

Key decisions:

- **DTOs in and out** — entities are never exposed over the wire; each feature has a
  `*Request` (validated) and `*Response` record plus a small mapper.
- **Validation at the boundary** — Jakarta Bean Validation (`@Valid`) on request bodies.
- **Consistent errors** — a single `@RestControllerAdvice` maps not-found to `404` and
  validation failures to `400`, always returning the same JSON shape.
- **Money is `BigDecimal`**, never `double`.
- **Versioned schema migrations** — Flyway owns the schema (`src/main/resources/db/migration`),
  and Hibernate runs with `ddl-auto: validate` so a drifted entity fails the build
  instead of silently altering a production table.
- **Transactions are explicit** — services are `@Transactional(readOnly = true)` by
  default, and write methods opt in to a read-write transaction.
- **Config via environment** — the production profile reads the database connection and
  the JWT signing key entirely from env vars, so no credentials live in source.
- **Actuator health** at `/actuator/health` for load-balancer / deploy checks.

## Authentication

Register or log in to receive a **JWT**, then send it as `Authorization: Bearer <token>`
on every other request.

```
JwtAuthenticationFilter ─▶ JwtService (validate) ─▶ CustomUserDetailsService (load user)
                                                 └─▶ SecurityContext
```

- Passwords are hashed with **BCrypt**; the plaintext is never stored or logged.
- **Data is siloed per user.** Every vehicle belongs to an owner, and repository queries
  are scoped to the authenticated user's ID — so a vehicle belonging to someone else
  returns **`404`**, not `403`, to avoid confirming that the record exists.
- Public routes: `/api/v1/auth/**`, `/actuator/health`, and the Swagger UI. Everything
  else requires a valid token.

## API

Base path: `/api/v1`

| Method   | Path                                  | Description                    | Success            |
| -------- | ------------------------------------- | ------------------------------ | ------------------ |
| `POST`   | `/auth/register`                      | Create an account, get a token | `201`              |
| `POST`   | `/auth/login`                         | Exchange credentials for a JWT | `200` / `401`      |
| `POST`   | `/vehicles`                           | Create a vehicle               | `201` + `Location` |
| `GET`    | `/vehicles?page=0&size=20`            | List your vehicles (paginated) | `200`              |
| `GET`    | `/vehicles?search=golf&status=DAILY`  | Search by make/model, filter   | `200`              |
| `GET`    | `/vehicles/{id}`                      | Fetch one vehicle              | `200` / `404`      |
| `PUT`    | `/vehicles/{id}`                      | Update a vehicle               | `200` / `404`      |
| `DELETE` | `/vehicles/{id}`                      | Delete a vehicle               | `204` / `404`      |
| `GET`    | `/vehicles/{id}/summary`              | **Aggregated build summary**   | `200`              |
| `POST`   | `/vehicles/{vehicleId}/modifications` | Add a modification             | `201` + `Location` |
| `GET`    | `/vehicles/{vehicleId}/modifications` | List a vehicle's modifications | `200`              |
| `GET`    | `/modifications/{id}`                 | Fetch one modification         | `200` / `404`      |
| `DELETE` | `/modifications/{id}`                 | Delete a modification          | `204` / `404`      |
| `POST`   | `/vehicles/{vehicleId}/dyno`          | Record a dyno result           | `201`              |
| `GET`    | `/vehicles/{vehicleId}/dyno`          | List dyno results              | `200`              |
| `GET`    | `/actuator/health`                    | Health check                   | `200`              |

Interactive docs (OpenAPI 3): **[Swagger UI](https://car-build-log.onrender.com/swagger-ui.html)**,
or `http://localhost:8080/swagger-ui.html` when running locally.

### Example

Log in, then create a vehicle:

```bash
TOKEN=$(curl -s -X POST https://car-build-log.onrender.com/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"password123"}' | jq -r .token)

curl -i -X POST https://car-build-log.onrender.com/api/v1/vehicles \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"make":"Volkswagen","model":"Golf GTI Mk7","year":2016,"engineCode":"EA888","status":"PROJECT"}'
```

Build summary (`GET /api/v1/vehicles/{id}/summary`) — the endpoint the whole model builds toward:

```json
{
  "vehicleId": "684079c1-62dd-4316-8f6a-48cf5e8e2027",
  "totalModifications": 4,
  "totalSpend": 4439.50,
  "spendByCategory": {
    "INTAKE": 649.0,
    "EXHAUST": 1180.5,
    "TUNING": 720.0,
    "SUSPENSION": 1890.0
  },
  "latestDyno": { "powerHp": 301, "torqueNm": 420, "measuredAt": "2025-04-06" },
  "currentPowerHp": 301,
  "currentTorqueNm": 420
}
```

Validation error body (every handled error uses this shape):

```json
{
  "timestamp": "2026-06-18T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": { "make": "must not be blank" }
}
```

## Running the backend locally

### Option A — no database to install (fastest)

The `local` profile runs against an in-memory **H2** database in PostgreSQL-compatibility
mode, seeded on every start with a **`demo` / `password123`** user who already owns a few
sample cars:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Browse the in-memory data at `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:buildlog`, user `sa`, empty password).

### Option B — real PostgreSQL via Docker

```bash
docker compose up --build       # API on :8080, Postgres on :5432
```

Or run just the database and keep a fast app reload loop on the host:

```bash
docker compose up -d db
./mvnw spring-boot:run
```

Check it's up:

```bash
curl http://localhost:8080/actuator/health    # {"status":"UP"}
```

### Configuration

| Variable                   | Purpose                                               | Local default                               |
| -------------------------- | ----------------------------------------------------- | ------------------------------------------- |
| `DB_URL`                   | JDBC URL                                              | `jdbc:postgresql://localhost:5432/buildlog` |
| `DB_USER`                  | Database user                                         | `app`                                       |
| `DB_PASSWORD`              | Database password                                     | `localdev`                                  |
| `APP_JWT_SECRET`           | JWT signing key (min 32 bytes) — **override in prod** | dev-only placeholder                        |
| `APP_JWT_EXPIRATION_MS`    | Token lifetime                                        | `86400000` (24 h)                           |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated origins allowed to call the API       | `http://localhost:3000`                     |
| `PORT`                     | HTTP port                                             | `8080`                                      |
| `SPRING_PROFILES_ACTIVE`   | `local` (H2) or `prod` (env-supplied PostgreSQL)      | — (plain PostgreSQL)                        |

## Frontend

A React / Next.js (App Router) UI lives in [`frontend/`](frontend/), using TanStack Query
for data fetching, shadcn/ui for components, and React Hook Form + Zod for validation.

```bash
cd frontend
npm install
cp .env.local.example .env.local   # or point it at the live API
npm run dev                        # http://localhost:3000
```

The backend must be running and must allow the frontend's origin via CORS
(`APP_CORS_ALLOWED_ORIGINS`, default `http://localhost:3000`). More detail in
[frontend/README.md](frontend/README.md).

## Testing

```bash
./mvnw test       # unit tests (services + summary aggregation) and a @WebMvcTest slice — no Docker needed
./mvnw verify     # also runs the Testcontainers integration test against a real PostgreSQL (needs Docker)
```

The integration test (`VehicleApiIT`) spins up PostgreSQL via Testcontainers and drives
a request end-to-end — authenticate, create a vehicle, add a modification and a dyno
result, then assert the aggregated summary — exercising the full
controller → service → repository → DB path.

`MigrationValidationTest` applies the Flyway migrations to an empty database and lets
Hibernate validate the entities against the result, so entity/migration drift fails the
build. It runs on H2 so it needs no Docker; `VehicleApiIT` covers the same ground against
real PostgreSQL under `mvn verify`.

**CI:** every push and pull request to `main` runs `mvn verify` on GitHub Actions
(see the badge above), so the Testcontainers test runs on every change.

## Deployment

The live demo runs on **[Render](https://render.com)**:

```
Browser ──▶ Next.js frontend (Render) ──▶ Spring Boot API in Docker (Render) ──▶ PostgreSQL (Neon)
```

- The API is built from the multi-stage [`Dockerfile`](Dockerfile) — Maven builds the jar,
  and the runtime image ships only a JRE and the jar, running as an unprivileged user.
- Configuration is entirely environment-driven (`SPRING_PROFILES_ACTIVE=prod`, `DB_URL`,
  `DB_USER`, `DB_PASSWORD`, `APP_JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`), so the same
  image runs locally, in CI, and in production with no code changes.
- The database is managed **PostgreSQL on [Neon](https://neon.tech)**, reached over TLS.
- Render redeploys automatically on every push to `main`.

[`docs/DEPLOY.md`](docs/DEPLOY.md) contains a separate, step-by-step **AWS (EC2 + RDS)**
runbook — security groups, billing alarm, teardown — as an alternative host for the same
container.

## What I'd do next

- **Broader test coverage** — the auth and dyno paths are thinner than the vehicle ones.
- **Gate deploys on CI** — Render currently deploys on push independently of the Actions run.
- **Caching** — cache the read-heavy summary endpoint.
- **Observability** — structured logging, metrics via Micrometer/Prometheus, request tracing.
