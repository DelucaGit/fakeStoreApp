# fakeStoreApp (CloudStore)

CloudStore is a small cloud-based e-commerce application. Users can register, log in, browse products from FakeStore API, create orders, and view their order history.

The project is a monorepo with two Spring Boot backend services and a React frontend. The live submission version runs on AWS: the backend services run as Docker containers on separate EC2 instances, the database runs in AWS RDS PostgreSQL, and the frontend is served publicly through Nginx.

Live frontend: <http://ec2-13-49-75-31.eu-north-1.compute.amazonaws.com>

## Architecture at a glance

```
  browser
    │
    ▼
  React frontend ──► userOrderService (:8080) ──► productService (:8082)
                           │                             │
                           └────► AWS RDS PostgreSQL ◄────┘
```

- `userOrderService` owns users, auth, and orders (database `users_and_orders_db`).
- `productService` owns the product catalog and proxies a third‑party FakeStore API (database `product_db`).
- Both services share one JWT access‑token signing secret, so an access token issued by `userOrderService` is accepted by `productService`.
- `userOrderService` calls `productService` over HTTP using `PRODUCT_SERVICE_BASE_URL`.
- `frontend/` contains the React + Vite client used for the live site.

## Tech stack

- Java 21 (Temurin), Spring Boot 4.0.5
- Spring Security + `jjwt` 0.11.5 for JWT, BCrypt for password hashing
- Spring Data JPA with Hibernate, `ddl-auto=validate`
- Flyway for schema migrations (forward‑only)
- React + Vite + TypeScript frontend
- PostgreSQL locally, AWS RDS PostgreSQL in the deployed environment
- Docker + Docker Compose for local orchestration
- GitHub Actions for CI, Docker Hub for image hosting, EC2 + SSH for deploy
- Nginx for serving the production frontend on EC2

## Repository layout

```
.
├── userOrderService/        Spring Boot service: users, auth, orders
├── productService/          Spring Boot service: products
├── frontend/                React + Vite frontend
├── docker/postgres/         Init scripts (creates the product_db database)
├── docker-compose.yml       Local orchestration for all three containers
├── .env.example             Template for required env vars (copy to .env)
├── PROJECT_LOG.md           Project state, deployment notes, and recovery notes
├── REFLEKTIONSRAPPORT.md    Written reflection report for submission
└── .github/workflows/       CI/CD per service (build, image push, EC2 deploy)
```

## Configuration

All secrets are loaded from environment variables. Nothing sensitive is committed.

1. Copy the template next to `docker-compose.yml`:

   ```bash
   cp .env.example .env
   ```

2. Open `.env` and fill in real values for at least:
   - `POSTGRES_PASSWORD` — local Postgres password.
   - `JWT_SECRET_KEY` — long random string. The same value must be used by both services so tokens validate across them.
   - `AUTH_ENDPOINT_REGISTER`, `AUTH_ENDPOINT_USER_FETCH` — required by `userOrderService`.

3. `.env` is git‑ignored. Never commit it. See `.env.example` for the full list of supported variables and defaults.

Optional overrides exposed by the services:

| Variable | Default | Used by | Purpose |
|---|---|---|---|
| `PRODUCT_SERVICE_BASE_URL` | `http://product-service:8082` (compose) / `http://localhost:8082` (local) | userOrderService | Where to reach productService |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | both | Comma‑separated allowed origins |
| `JWT_ACCESS_EXPIRATION_MS` | `3600000` (1h) | userOrderService | Access token lifetime |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7d) | userOrderService | Refresh token lifetime |
| `FAKESTORE_SSL_INSECURE` | `false` | productService | Dev‑only TLS bypass. Production startup fails if `true`. |

## Running the backend

### Option A — Docker Compose (recommended)

Spins up Postgres and both services with one command.

```bash
docker compose up --build
```

Service URLs once healthy:
- `userOrderService`: <http://localhost:8080>
- `productService`: <http://localhost:8082>
- `postgres`: `localhost:5433` (host) → `postgres:5432` (compose network)

To stop and remove volumes (wipes the local DB):

```bash
docker compose down -v
```

### Option B — Run services locally without Docker

Useful for fast IDE‑driven dev loops.

1. Run a local Postgres 16 and create two databases: `users_and_orders_db` and `product_db`.
2. Export the env vars from `.env` in your shell (or configure them in your IDE run config).
3. Start each service:

   ```bash
   cd userOrderService && ./mvnw spring-boot:run
   cd productService   && ./mvnw spring-boot:run
   ```

Flyway will run migrations on first start for each service.

## Running the frontend

The frontend lives in `frontend/`. See `frontend/README.md` for the full setup.

For local development, start the backend services first, then run:

```bash
cd frontend
npm install
npm run dev
```

The frontend reads backend URLs from Vite environment variables:

- `VITE_USER_SERVICE_URL`
- `VITE_PRODUCT_SERVICE_URL`

For local development these usually point to `http://localhost:8080` and `http://localhost:8082`.

## Deployment

The current live deployment uses AWS:

- `userOrderService` runs as a Docker container on one EC2 instance.
- `productService` runs as a Docker container on a separate EC2 instance.
- The services communicate over HTTP; in AWS they use private networking inside the VPC.
- PostgreSQL runs in AWS RDS with two logical databases.
- The React production build is served by Nginx on port 80.
- GitHub Actions builds, tests, pushes Docker images to Docker Hub, and deploys to EC2 on push to `main`.

Note: the current live URL uses plain HTTP because the course HTTPS requirement was removed. The EC2 instances do not use Elastic IPs, so public DNS values may change if an instance is stopped and started again.

## Database & migrations

Schema is managed by Flyway, forward‑only:

- `userOrderService/src/main/resources/db/migration/`
  - `V1__init_schema.sql` — `store_user`, `user_order`, `order_item`
  - `V2__add_refresh_tokens.sql` — `refresh_token` (hashed, supports rotation and revocation)
- `productService/src/main/resources/db/migration/`
  - `V1__init_schema.sql` — `product`

JPA runs with `ddl-auto=validate`, so the app fails fast if entities and tables drift. Update the schema by adding a new `V{n}__*.sql` file, never by editing an existing one.

## Security model

- Passwords are hashed with BCrypt before persistence; the raw password is never stored or logged.
- On login/registration the user receives an **access token** (short‑lived JWT) and a **refresh token** (longer‑lived). The refresh token is persisted as a hash in `refresh_token` and can be revoked.
- Both services validate access tokens with the shared `JWT_SECRET_KEY`. `productService` only accepts authenticated calls to write endpoints; `GET /api/products` is public.
- Stateless sessions, CSRF disabled (token‑based clients), CORS allow‑list driven by `CORS_ALLOWED_ORIGINS`.
- `productService` includes a startup guard (`FakeStoreTlsProductionGuard`) that refuses to boot in production if `FAKESTORE_SSL_INSECURE=true`.

## API summary

`userOrderService` (`http://localhost:8080`):

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/users/register` | public | Create account, returns tokens |
| POST | `/api/users/login` | public | Returns tokens |
| POST | `/api/users/refresh` | public | Exchange refresh token for a new pair |
| POST | `/api/users/logout` | public | Revoke current refresh token |
| GET  | `/api/users/{id}` | bearer | Fetch a user |
| POST | `/api/orders` | bearer | Create an order (validates products upstream) |
| GET  | `/api/orders/my` | bearer | List the caller's orders |

`productService` (`http://localhost:8082`):

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/api/products` | public | List products |
| GET | `/api/products/{id}` | bearer | Fetch a product |

## Smoke test runbook

Run after both services are up. Replace placeholders in angle brackets with real values.

### 1) Register a user

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"smoke@test.com\",\"password\":\"<your-password>\",\"firstName\":\"Smoke\",\"lastName\":\"Test\"}"
```

Expected: `201 Created` with `accessToken` and `refreshToken`.

### 2) Login

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"smoke@test.com\",\"password\":\"<your-password>\"}"
```

Expected: `200 OK` with fresh `accessToken` and `refreshToken`.

### 3) List products

```bash
curl http://localhost:8082/api/products
```

Expected: `200 OK` with a list of products.

### 4) Create an order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"productId\":1,\"quantity\":2}]}"
```

Expected: `201 Created` with `orderId`, `totalAmount`, and the line items.

### 5) Fetch my orders

```bash
curl http://localhost:8080/api/orders/my \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Expected: `200 OK` containing at least the order from step 4.

### 6) Refresh tokens

```bash
curl -X POST http://localhost:8080/api/users/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<REFRESH_TOKEN>\"}"
```

Expected: `200 OK` with a new token pair. The old refresh token is revoked.

## CI/CD

Two GitHub Actions workflows live in `.github/workflows/`. They are path‑scoped, so only the affected service runs:

- `user-order-service-ci.yml`
- `product-service-ci.yml`

Each workflow does the same three stages:

1. **build‑test** — Maven `verify` against an ephemeral Postgres 16 service container, using disposable CI credentials.
2. **docker** — Build the service image and, on `push`, tag and push to Docker Hub (`:<sha>` always, `:latest` on `main`).
3. **deploy‑ec2** — On `push` to `main`, SSH to the target EC2 host, pull the new image, and restart the container with `--env-file` (the real `.env` lives on the host, never in the repo or image).

Required GitHub Actions secrets:

- `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`
- `USER_ORDER_EC2_HOST`, `USER_ORDER_EC2_USER`, `USER_ORDER_EC2_SSH_KEY`
- `PRODUCT_EC2_HOST`, `PRODUCT_EC2_USER`, `PRODUCT_EC2_SSH_KEY`

## Troubleshooting

- **Service won't start, `ddl-auto=validate` complains.** Entities drifted from the schema. Add a new Flyway migration; do not edit an existing one.
- **`401 Unauthorized` from `productService` for an authenticated request.** Both services must share the same `JWT_SECRET_KEY`. Re‑check `.env` and restart both.
- **PKIX / TLS errors hitting `fakestoreapi.com` from `productService`.** Run on a current JDK 21 (Temurin/Corretto) so its trust store is up to date. As a dev‑only escape hatch, set `FAKESTORE_SSL_INSECURE=true`; production startup refuses this value.
- **CORS preflight failures.** Add your origin to `CORS_ALLOWED_ORIGINS` (comma‑separated). Default allows only `http://localhost:5173`.
- **Compose can't bind 5433.** Another Postgres is already on that host port. Stop it or change the left side of `5433:5432` in `docker-compose.yml`.

## Conventions for changes

- Keep controllers thin (HTTP only). Business rules belong in services. Persistence belongs in repositories.
- Use DTOs at the API boundary; do not return JPA entities directly.
- Forward‑only Flyway migrations.
- Any change to env vars must also be reflected in `.env.example` and in this README's Configuration section.
