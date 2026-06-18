# Car Build Log API

A REST API for tracking modified-car builds — vehicles, the modifications made to
them, and dyno results — with a derived **build-summary** endpoint that aggregates
total spend, spend-by-category, and the latest power/torque figures.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Spring Data JPA · Docker · AWS (EC2 + RDS)

---

## Architecture

A conventional layered REST service, organised **package-by-feature**:

```
HTTP ─▶ Controller ─▶ Service ─▶ Repository (Spring Data JPA) ─▶ PostgreSQL
```

```
src/main/java/com/miguelpimenta/buildlog/
├── vehicle/        Vehicle CRUD (entity, repo, service, controller, mapper, dto/)
├── modification/   Modifications belonging to a vehicle
├── dyno/           Dyno (rolling-road) results
├── summary/        Derived build-summary endpoint (composes the modules above)
└── common/         Global exception handling, error + pagination DTOs
```

Key decisions:

- **DTOs in and out** — entities are never exposed over the wire; each feature has a
  `*Request` (validated) and `*Response` record plus a small mapper.
- **Validation at the boundary** — Jakarta Bean Validation (`@Valid`) on request bodies.
- **Consistent errors** — a single `@RestControllerAdvice` maps not-found to `404` and
  validation failures to `400`, always returning the same JSON shape.
- **Money is `BigDecimal`**, never `double`.
- **Config via environment** — the production profile reads the database connection
  entirely from env vars, so no credentials live in source.
- **Actuator health** at `/actuator/health` for load-balancer / deploy checks.
- **Verified modular monolith** — each top-level package is a [Spring Modulith](https://spring.io/projects/spring-modulith)
  module, and an architecture test (`ModularityTests`) fails the build if a module reaches into another's
  internals or if a dependency *cycle* appears. That's why the build summary is its own `summary` module:
  it depends on `vehicle`/`modification`/`dyno`, never the reverse. `./mvnw test` also regenerates C4 module
  diagrams under `target/spring-modulith-docs/`.

## API

Base path: `/api/v1`

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `POST` | `/vehicles` | Create a vehicle | `201` + `Location` |
| `GET` | `/vehicles?page=0&size=20` | List vehicles (paginated) | `200` |
| `GET` | `/vehicles/{id}` | Fetch one vehicle | `200` / `404` |
| `PUT` | `/vehicles/{id}` | Update a vehicle | `200` / `404` |
| `DELETE` | `/vehicles/{id}` | Delete a vehicle | `204` / `404` |
| `GET` | `/vehicles/{id}/summary` | **Aggregated build summary** | `200` |
| `POST` | `/vehicles/{vehicleId}/modifications` | Add a modification | `201` + `Location` |
| `GET` | `/vehicles/{vehicleId}/modifications` | List a vehicle's modifications | `200` |
| `GET` | `/modifications/{id}` | Fetch one modification | `200` / `404` |
| `DELETE` | `/modifications/{id}` | Delete a modification | `204` / `404` |
| `POST` | `/vehicles/{vehicleId}/dyno` | Record a dyno result | `201` |
| `GET` | `/vehicles/{vehicleId}/dyno` | List dyno results | `200` |
| `GET` | `/actuator/health` | Health check | `200` |

### Example

Create a vehicle:

```bash
curl -i -X POST http://localhost:8080/api/v1/vehicles \
  -H 'Content-Type: application/json' \
  -d '{"make":"Volkswagen","model":"Golf GTI","year":2016,"engineCode":"EA888","notes":"track build"}'
```

Build summary (`GET /api/v1/vehicles/{id}/summary`):

```json
{
  "vehicleId": "0b6f...",
  "totalModifications": 1,
  "totalSpend": 450.00,
  "spendByCategory": { "TUNING": 450.00 },
  "latestDyno": { "powerHp": 290, "torqueNm": 410, "measuredAt": "2024-03-11" },
  "currentPowerHp": 290,
  "currentTorqueNm": 410
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

## Running locally

**Prerequisites:** Java 21 and Docker.

**Option A — full stack in containers:**

```bash
docker compose up --build
# API on http://localhost:8080
```

**Option B — Postgres in Docker, app on the host (fast dev loop):**

```bash
docker compose up -d db
./mvnw spring-boot:run
```

Check it's up:

```bash
curl http://localhost:8080/actuator/health    # {"status":"UP"}
```

### Configuration

| Variable | Purpose | Local default |
|----------|---------|---------------|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/buildlog` |
| `DB_USER` | Database user | `app` |
| `DB_PASSWORD` | Database password | `localdev` |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` in production (requires the three vars above) | — |

## Testing

```bash
./mvnw test       # unit tests (services + summary aggregation) and a @WebMvcTest slice — no Docker needed
./mvnw verify     # also runs the Testcontainers integration test against a real PostgreSQL (needs Docker)
```

The integration test (`VehicleApiIT`) spins up PostgreSQL via Testcontainers and drives
a request end-to-end — create a vehicle, add a modification and a dyno result, then
assert the aggregated summary — exercising the full controller → service → repository → DB path.

`./mvnw test` also runs the Spring Modulith architecture checks (module boundaries + no cycles)
— pure static analysis, no Docker required.

## Deployment (AWS)

Deployed as a Docker container on **EC2**, talking to a managed **RDS PostgreSQL**
instance, with the DB connection supplied as environment variables and
`/actuator/health` used to confirm the service is live. Full step-by-step runbook
(security groups, billing alarm, teardown) in **[docs/DEPLOY.md](docs/DEPLOY.md)**.

## What I'd do next

- **Auth** — JWT / OAuth2 resource server; per-user ownership of vehicles.
- **Database migrations** — Flyway instead of `ddl-auto`, for versioned, reviewable schema changes.
- **CI/CD** — GitHub Actions to build, test (Testcontainers), and publish the image to ECR.
- **Caching** — cache the read-heavy summary endpoint.
- **Observability** — structured logging, metrics via Micrometer/Prometheus, request tracing.
