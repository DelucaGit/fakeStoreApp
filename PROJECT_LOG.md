# CloudStore — Project Log

This file is the working memory of the project. If you stop touching the code for a week or two, read this first. It captures the current state, what was built, how to bring it back up, and what is left.

Secrets are never committed here. Passwords, JWT secrets, RDS master credentials and `.pem` keys are intentionally **not** in this file. Placeholders look like `<your_db_password>`.

Last meaningful update: 2026-06-04.

## 1. Project snapshot

- Repo: `fakeStoreApp` (monorepo).
- Backend: two Spring Boot services (`userOrderService`, `productService`).
- Frontend: React + Vite + TypeScript (`frontend/`).
- Database: AWS RDS PostgreSQL, two logical databases.
- Hosting: **two EC2 instances (VG split COMPLETE as of 2026-06-04).**
  - **EC2 #1 (`cloudstore-app`, the original):** runs `userOrderService` (:8080) ONLY, still as a `systemd` JAR. The old `productService` systemd unit here is now stopped and disabled.
  - **EC2 #2 (`cloudstore-product`, NEW):** runs `productService` (:8082) ONLY, as a **Docker container** pulled from Docker Hub. Live, returns `200` on `GET /api/products`.
  - `userOrderService` reaches `productService` over the **private DNS** of EC2 #2 inside the VPC. Verified by a successful end-to-end order creation.
- Frontend: not yet hosted; runs locally via `npm run dev` and points at the EC2 backends through env vars. `VITE_PRODUCT_SERVICE_URL` now targets the new product EC2 public DNS.
- Status: two-EC2 split is functionally done and tested end-to-end (register → login → products → create order → my orders). See section 16 for full detail. **Next candidates: CI/CD deploy secrets to both EC2s, or HTTPS.**

Outstanding items (from `userOrderService/kursinlämning.md`) are listed in section 12.

## 2. Architecture overview

```
[ browser ]
    │
    │ http://localhost:5173 (React dev server)
    ▼
[ frontend (Vite, React) ]
    │
    │  http://<ec2-public-dns>:8080  (user/order/auth)
    │  http://<ec2-public-dns>:8082  (products)
    ▼
[ EC2  ip-172-31-20-136 ]
    ├── userOrderService (:8080)  ─┐
    └── productService    (:8082) ─┤
                                    ▼
                          [ RDS PostgreSQL 18 ]
                          ├── users_and_orders_db
                          └── product_db
```

Notes:

- `userOrderService` calls `productService` over HTTP for product price lookups during order creation. The user's access JWT is forwarded as `Authorization: Bearer ...`.
- `productService` validates that JWT using the same `JWT_SECRET_KEY` that `userOrderService` uses to sign it. Both services must always share the same secret.
- The frontend never talks to RDS directly. Only the two backend services do.

## 3. Repo layout

```
fakeStoreApp/
├── userOrderService/       Spring Boot service: users, JWT auth, refresh tokens, orders
│   ├── src/main/...
│   ├── src/main/resources/db/migration/   (Flyway: V1__init_schema.sql, V2__add_refresh_tokens.sql)
│   ├── Dockerfile
│   ├── pom.xml
│   └── kursinlämning.md    (course requirements + AWS handoff notes)
├── productService/         Spring Boot service: product catalog via FakeStore API
│   ├── src/main/...
│   ├── src/main/resources/db/migration/   (Flyway: V1__init_schema.sql)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/               React + Vite SPA
│   ├── src/api/endpoints.ts        URLs read from VITE_* env vars
│   ├── src/api/client.ts           apiFetch with auto-refresh on 401
│   ├── src/context/AuthContext.tsx
│   ├── src/pages/ (Login, Register, ProductList, CreateOrder, MyOrders)
│   ├── .env.example                committed template
│   └── .env.local                  git-ignored; real URLs live here
├── docker/postgres/        Init script: creates product_db on first start
├── docker-compose.yml      Local: postgres + both backends
├── .env.example            Backend env template (root, used by docker-compose)
├── .github/workflows/      CI/CD per backend service
├── README.md               Backend developer docs
└── PROJECT_LOG.md          This file
```

## 4. Backend services in detail

### 4.1 userOrderService (port 8080)

Owns users, login, refresh tokens, and orders.

Key paths:


| Method | Path                  | Auth                 | Notes                                                  |
| ------ | --------------------- | -------------------- | ------------------------------------------------------ |
| POST   | `/api/users/register` | public               | Creates user, returns access + refresh tokens          |
| POST   | `/api/users/login`    | public               | Returns fresh access + refresh tokens                  |
| POST   | `/api/users/refresh`  | public               | Rotates the refresh token                              |
| POST   | `/api/users/logout`   | public (bearer body) | Revokes refresh token                                  |
| GET    | `/api/users/{id}`     | bearer               | Fetch a user                                           |
| POST   | `/api/orders`         | bearer               | Create an order, validates products via productService |
| GET    | `/api/orders/my`      | bearer               | List my orders                                         |


Important files:

- `userOrderService/src/main/java/.../controller/UserController.java`
- `userOrderService/src/main/java/.../controller/OrderController.java`
- `userOrderService/src/main/java/.../service/UserService.java`
- `userOrderService/src/main/java/.../service/AuthTokenService.java`
- `userOrderService/src/main/java/.../service/OrderService.java`
- `userOrderService/src/main/java/.../client/ProductServiceClient.java`
- `userOrderService/src/main/java/.../config/SecurityConfig.java`
- `userOrderService/src/main/java/.../util/JwtUtil.java`

Required env vars:

```
DB_URL_LOCAL=jdbc:postgresql://<rds-endpoint>:5432/users_and_orders_db?sslmode=require
DB_USERNAME_LOCAL=<db_user>
DB_PASSWORD_LOCAL=<db_password>
JWT_SECRET_KEY=<long_random_secret>
AUTH_ENDPOINT_REGISTER=/api/users/register
AUTH_ENDPOINT_USER_FETCH=/api/users/**
PRODUCT_SERVICE_BASE_URL=http://127.0.0.1:8082
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### 4.2 productService (port 8082)

Proxies the public FakeStore API and exposes a small REST surface. Despite having a `product` table in Flyway, products are currently served live from FakeStore, not from the local table. The table is reserved for future caching/extension.


| Method | Path                 | Auth   | Notes                                               |
| ------ | -------------------- | ------ | --------------------------------------------------- |
| GET    | `/api/products`      | public | List products from FakeStore                        |
| GET    | `/api/products/{id}` | bearer | Product by id; called by user-order during checkout |


Required env vars:

```
DB_URL_LOCAL=jdbc:postgresql://<rds-endpoint>:5432/product_db?sslmode=require
DB_USERNAME_LOCAL=<db_user>
DB_PASSWORD_LOCAL=<db_password>
JWT_SECRET_KEY=<same_as_user_order_service>
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Optional dev-only: `FAKESTORE_SSL_INSECURE=true` to bypass TLS for FakeStore. Production startup refuses this.

## 5. Frontend

Pages: `Login`, `Register`, `ProductList`, `CreateOrder`, `MyOrders`. Routing in `frontend/src/App.tsx`. Authenticated routes are wrapped by `ProtectedRoute`.

Auth flow:

- `AuthProvider` exposes `login`, `register`, `logout`, and `isAuthenticated`.
- Tokens are stored in `localStorage`. This is OK for the school project but has XSS risk in real production.
- `apiFetch` (in `frontend/src/api/client.ts`) automatically attaches `Authorization: Bearer <accessToken>` and, on `401`, transparently calls `/api/users/refresh` and retries.

Backend URLs (since 2026-05-28) are no longer hardcoded. They are read at build/dev time from Vite env vars:

```
VITE_USER_SERVICE_URL
VITE_PRODUCT_SERVICE_URL
```

`frontend/src/api/endpoints.ts` calls `requireEnv(...)` and throws a clear error if either var is missing. This stops the old footgun where the app silently called `localhost` in a deployed build.

`frontend/.env.example` is committed (template). `frontend/.env.local` is git-ignored (via the existing `*.local` rule) and holds the real EC2 URLs.

## 6. Database and migrations

Two databases on a single RDS PostgreSQL 18 instance:

- `users_and_orders_db`: tables `store_user`, `user_order`, `order_item`, `refresh_token`.
- `product_db`: table `product` (not currently used at runtime).

Flyway is used per service. Migrations are forward-only. Hibernate runs with `ddl-auto=validate`, so the app fails fast if entities drift from migrations.

Gotcha to remember:

- If you point a service at the wrong database, Flyway will reject startup with `Migration checksum mismatch for migration version 1`, because it sees a different `V1` already applied by the other service. This bit us during the systemd setup; fix is to ensure `DB_URL_LOCAL` matches the right database name.

## 7. AWS deployment

### 7.1 EC2

- Name tag: `cloudstore-app`
- Instance ID: `i-0d4e9d02dfa7e9667`
- Type: `t3.micro`
- AMI: Amazon Linux 2023
- Region/AZ: `eu-north-1` / `eu-north-1a`
- Public DNS: `ec2-13-49-75-31.eu-north-1.compute.amazonaws.com`
- Public IP: `13.49.75.31` (no Elastic IP attached — will change on stop/start)
- SSH key: `cloudstore-ec2-key` (`.pem` lives on local Desktop, never in the repo)
- Java: Amazon Corretto 21, located at `/usr/bin/java`
- Working dir for JARs: `/home/ec2-user/`
- Both backend JARs live in the home folder.

### 7.2 RDS

- Identifier: `cloudstore-postgres`
- Engine: PostgreSQL 18.x
- Endpoint host: `cloudstore-postgres.cvck4ycychj2.eu-north-1.rds.amazonaws.com`
- Databases: `users_and_orders_db`, `product_db`
- Inbound: TCP 5432 from the EC2 instance's security group only (not public)
- Admin access: through EC2 → `psql`. Local laptop does not connect directly by design.

To list databases (the `\l` command may fail on PostgreSQL 18 with `d.daticulocale`); use:

```sql
SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY 1;
```

### 7.3 Security groups (testing posture)

EC2 inbound currently allows:

- TCP 22 (SSH)
- TCP 8080 (userOrderService)
- TCP 8082 (productService)

Source is `0.0.0.0/0` on purpose, so the course examiner can reach the API. This is **not** production-grade. Tighten or move behind a reverse proxy after the course.

## 8. systemd setup (2026-05-28)

Before this change the JARs were started with `nohup ... &` after each SSH session. They died as soon as the shell closed. Now both services are managed by `systemd`, so they:

- survive SSH disconnect,
- auto-restart on crash,
- start automatically on EC2 reboot.

### 8.1 Unit files

`/etc/systemd/system/cloudstore-product.service`:

```ini
[Unit]
Description=CloudStore Product Service
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user
EnvironmentFile=/etc/cloudstore-product.env
ExecStart=/usr/bin/java -jar /home/ec2-user/productService-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

`/etc/systemd/system/cloudstore-user-order.service`:

```ini
[Unit]
Description=CloudStore User Order Service
After=network-online.target cloudstore-product.service
Wants=network-online.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user
EnvironmentFile=/etc/cloudstore-user-order.env
ExecStart=/usr/bin/java -jar /home/ec2-user/userOrderService-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 8.2 Env files on the server

The unit files load secrets from two **root-owned, mode 600** env files. The files themselves are not in the repo; only their structure is documented here.

`/etc/cloudstore-product.env` (template, do not commit real values):

```
DB_URL_LOCAL=jdbc:postgresql://cloudstore-postgres.cvck4ycychj2.eu-north-1.rds.amazonaws.com:5432/product_db?sslmode=require
DB_USERNAME_LOCAL=<db_user>
DB_PASSWORD_LOCAL=<db_password>
JWT_SECRET_KEY=<shared_secret>
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

`/etc/cloudstore-user-order.env`:

```
DB_URL_LOCAL=jdbc:postgresql://cloudstore-postgres.cvck4ycychj2.eu-north-1.rds.amazonaws.com:5432/users_and_orders_db?sslmode=require
DB_USERNAME_LOCAL=<db_user>
DB_PASSWORD_LOCAL=<db_password>
JWT_SECRET_KEY=<same_shared_secret>
CORS_ALLOWED_ORIGINS=http://localhost:5173
AUTH_ENDPOINT_REGISTER=/api/users/register
AUTH_ENDPOINT_USER_FETCH=/api/users/**
PRODUCT_SERVICE_BASE_URL=http://127.0.0.1:8082
```

Format rules learned the hard way (see section 14):

- No `export` keyword in front of lines; `systemd` ignores those.
- No surrounding quotes around values unless the value really needs them.
- Each service has its **own** `DB_URL_LOCAL` value because the database name differs.

To safely peek at the keys without leaking the values:

```bash
sudo grep -v '^#' /etc/cloudstore-product.env | sed 's/=.*/=***/'
sudo grep -v '^#' /etc/cloudstore-user-order.env | sed 's/=.*/=***/'
```

### 8.3 Day-to-day systemd commands

```bash
sudo systemctl daemon-reload                              # after editing unit files
sudo systemctl enable  cloudstore-product.service cloudstore-user-order.service
sudo systemctl start   cloudstore-product.service
sudo systemctl start   cloudstore-user-order.service
sudo systemctl restart cloudstore-product.service
sudo systemctl stop    cloudstore-product.service
systemctl status       cloudstore-product.service
sudo journalctl -u cloudstore-product.service -n 200 --no-pager
sudo journalctl -u cloudstore-product.service -f
```

## 9. Frontend env config (2026-05-28)

Done to remove hardcoded URLs from the codebase.

- `frontend/src/api/endpoints.ts` now reads `VITE_USER_SERVICE_URL` and `VITE_PRODUCT_SERVICE_URL` at startup, strips trailing slashes, and throws a clear error if either var is missing.
- `frontend/.env.example` is committed and documents the variables, including local and EC2 example values.
- `frontend/.env.local` is git-ignored automatically by the existing `*.local` rule. It currently points at the EC2 DNS:

```
VITE_USER_SERVICE_URL=http://ec2-13-49-75-31.eu-north-1.compute.amazonaws.com:8080
VITE_PRODUCT_SERVICE_URL=http://ec2-13-49-75-31.eu-north-1.compute.amazonaws.com:8082
```

Switch back to local backend by changing both to `http://localhost:808x` and restarting `npm run dev`.

## 10. Restart / recovery playbook

If the app stops working and you have not touched it for a while, do these in order. This is the exact path taken on 2026-05-28 to bring it back.

### 10.1 Is the EC2 instance alive?

AWS Console → EC2 → instance `cloudstore-app` → check `Instance state: Running` and confirm the Public DNS still matches `ec2-13-49-75-31.eu-north-1.compute.amazonaws.com`. If you stopped/started the instance and there is no Elastic IP, the DNS will have changed — update `frontend/.env.local` accordingly.

### 10.2 Can your laptop reach the ports?

From PowerShell:

```powershell
Test-NetConnection -ComputerName ec2-13-49-75-31.eu-north-1.compute.amazonaws.com -Port 8080
Test-NetConnection -ComputerName ec2-13-49-75-31.eu-north-1.compute.amazonaws.com -Port 8082
```

Want `TcpTestSucceeded : True`.

### 10.3 SSH in if you need to inspect the box

```powershell
ssh -i "C:\Users\marce\Desktop\cloudstore-ec2-key.pem" ec2-user@ec2-13-49-75-31.eu-north-1.compute.amazonaws.com
```

### 10.4 systemd state

```bash
systemctl is-enabled cloudstore-product.service cloudstore-user-order.service
systemctl status     cloudstore-product.service
systemctl status     cloudstore-user-order.service
ss -tlnp | grep -E '8080|8082'
```

If a service is `failed`, look at the logs:

```bash
sudo journalctl -u cloudstore-product.service -n 200 --no-pager
```

### 10.5 Smoke test from the box itself

```bash
curl -i http://127.0.0.1:8082/api/products
curl -i -X POST http://127.0.0.1:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"nope@test.com","password":"nope"}'
```

Want `200` and `401` respectively.

### 10.6 Start the frontend

On the laptop:

```powershell
cd C:\Users\marce\Desktop\Projects\fakeStoreApp\frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Register → log in → see products → place an order → see "My Orders".

## 11. Smoke test layered approach

Pattern that helps localize failures. Cheap checks first.


| Layer | What it tests                      | Tool                                    |
| ----- | ---------------------------------- | --------------------------------------- |
| 1     | DNS resolves; box is alive         | `ping` (may be blocked, not conclusive) |
| 2     | Port is open and something listens | `Test-NetConnection`                    |
| 3     | Service responds at HTTP level     | `curl -i` to a public GET               |
| 4     | Service auth path works            | `curl -X POST .../login` expecting 401  |
| 5     | End-to-end happy path              | register → create order                 |


Read failures top-down: a Layer 3 failure is unambiguous, a Layer 4 failure points at the service config, a Layer 5 failure points at the cross-service or DB integration.

## 12. Done vs left

Authoritative checklist is in `userOrderService/kursinlämning.md`. Summary below:

### Done (G + parts of VG)

- Spring Boot backend with two services.
- Spring Security + JWT auth, refresh-token rotation, BCrypt password hashing.
- FakeStore API integration in productService.
- Postgres + Flyway, validated entities (`ddl-auto=validate`).
- Unit/integration tests in both services.
- GitHub Actions CI per service: `mvn verify`, Docker build, push to Docker Hub.
- Docker Compose for local dev (Postgres + both services).
- Frontend React + Vite SPA with full happy path.
- AWS RDS in use.
- AWS EC2 in use, public reachable.
- JWT shared between services for service-to-service calls.
- **VG infrastructure: two separate EC2 instances, one service each (DONE 2026-06-04).** product-service runs as Docker on the new EC2; user-order runs as a systemd JAR on the original EC2 and calls product over private DNS. Verified end-to-end. Detail in section 16.

### Left

- HTTPS termination (Let's Encrypt / Nginx).
- (Optional) Move user-order to Docker so both services deploy identically. See section 17.1.
- Hook GitHub Actions deploy step to real EC2 (Docker Hub → `docker pull` on the host). Workflow exists; needs repo secrets set. Env files live at `/opt/cloudstore/*.env` (matches the workflow), owned by `ec2-user` mode 600. See section 17.2.
- Optional polish: drop `application.properties` `spring.jpa.show-sql=true` in production, add proper `@Valid` Bean Validation on request DTOs, decide product DB usage or remove its table.
- Final submission: live URL documented for Learnpoint.

## 13. CI/CD

`.github/workflows/user-order-service-ci.yml` and `.github/workflows/product-service-ci.yml` are path-scoped per service. Each does:

1. `mvn verify` against an ephemeral Postgres 16 container with CI-only credentials.
2. Docker build, and on `push` to a branch, tag/push `:<sha>` to Docker Hub. On `main`, also `:latest`.
3. On `push` to `main`, SSH to the target EC2 host and `docker pull` + `docker run --env-file /opt/cloudstore/<service>.env`.

Required secrets in GitHub repo settings (not in code):

- `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`
- `USER_ORDER_EC2_HOST`, `USER_ORDER_EC2_USER`, `USER_ORDER_EC2_SSH_KEY`
- `PRODUCT_EC2_HOST`, `PRODUCT_EC2_USER`, `PRODUCT_EC2_SSH_KEY`

The deploy step currently expects host-side `.env` files at `/opt/cloudstore/`. The systemd setup uses `/etc/cloudstore-*.env`. If you wire the CI deploy to this same EC2, reconcile those two locations.

## 14. Lessons learned (gotchas worth remembering)

1. `**systemd` env files are not bash files.** No `export`, no surrounding quotes by default. The first attempt used `export KEY=value` and `systemd` silently ignored every line, which made the app crash with `Could not resolve placeholder 'JWT_SECRET_KEY'`.
2. **Each service needs its own `DB_URL_LOCAL`.** Sharing the variable across both services is fine, but the URL value must point to the right database. Pointing productService at `users_and_orders_db` produced `Migration checksum mismatch for migration version 1`.
3. **EC2 public DNS changes on stop/start without an Elastic IP.** If both ports time out after a break, suspect this first.
4. **ICMP (ping) is blocked by default.** A ping timeout is not a signal that the box is down; use `Test-NetConnection` on the actual TCP port.
5. `**nohup ... &` is not enough.** It survives the current SSH session but dies on reboot and won't auto-restart on crash. Use `systemd` for anything you want to keep.
6. **Frontend hardcoded URLs are a footgun.** Now centralized in `endpoints.ts` and fed by `VITE_`* env vars; it throws loudly when missing, instead of silently calling `undefined/api/...`.
7. **JWT must be identical on both services.** When you regenerate it, regenerate it in both env files at once, then restart both services.

## 15. Quick command cheat sheet

From the laptop:

```powershell
# Reach the backend
Test-NetConnection -ComputerName ec2-13-49-75-31.eu-north-1.compute.amazonaws.com -Port 8080
Test-NetConnection -ComputerName ec2-13-49-75-31.eu-north-1.compute.amazonaws.com -Port 8082

# Quick API check
curl.exe -i http://ec2-13-49-75-31.eu-north-1.compute.amazonaws.com:8082/api/products

# SSH in
ssh -i "C:\Users\marce\Desktop\cloudstore-ec2-key.pem" ec2-user@ec2-13-49-75-31.eu-north-1.compute.amazonaws.com

# Frontend
cd C:\Users\marce\Desktop\Projects\fakeStoreApp\frontend
npm install
npm run dev
```

On the EC2 box:

```bash
# Service state
systemctl status cloudstore-product.service
systemctl status cloudstore-user-order.service
ss -tlnp | grep -E '8080|8082'

# Restart after editing unit or env files
sudo systemctl daemon-reload
sudo systemctl restart cloudstore-product.service
sudo systemctl restart cloudstore-user-order.service

# Logs
sudo journalctl -u cloudstore-product.service -n 200 --no-pager
sudo journalctl -u cloudstore-user-order.service -f

# Safe env inspection (masks values)
sudo grep -v '^#' /etc/cloudstore-product.env    | sed 's/=.*/=***/'
sudo grep -v '^#' /etc/cloudstore-user-order.env | sed 's/=.*/=***/'

# Local smoke from the box itself
curl -i http://127.0.0.1:8082/api/products
curl -i -X POST http://127.0.0.1:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"nope@test.com","password":"nope"}'
```

## 16. Two-EC2 split — COMPLETE (checkpoint 2026-06-04)

This section is the working memory for the VG split. The split is **done and verified end-to-end**. Read this first when resuming, then jump to section 17 for what comes next.

### 16.1 Decisions locked in

- Keep the original EC2, launch ONE new EC2 for product-service.
- Service-to-service traffic goes over the **private DNS** inside the same VPC (not public internet).
- One Elastic IP per instance is allowed (only if it costs nothing meaningful; may be skipped).
- Both instances in the **same VPC**, same AZ (`eu-north-1a`).
- Packaging: **Docker** (pull image from Docker Hub, run with `docker run --restart unless-stopped`). Reason: the existing GitHub Actions deploy step already does `docker pull` + `docker run`, so Docker on the host is what makes the CI/CD deploy work end-to-end. `--restart unless-stopped` gives the same survive-reboot behavior systemd gave us.

### 16.2 The two instances

| Role | Name tag | Public DNS | Private hostname | Service | How it runs |
|------|----------|------------|------------------|---------|-------------|
| Original | `cloudstore-app` | `ec2-13-49-75-31.eu-north-1.compute.amazonaws.com` | `ip-172-31-20-136` | userOrderService :8080 ONLY (product unit stopped+disabled) | systemd JAR |
| NEW product | `cloudstore-product` | `ec2-16-171-175-179.eu-north-1.compute.amazonaws.com` (IP `16.171.175.179`) | `ip-172-31-46-104.eu-north-1.compute.internal` | productService :8082 ONLY | Docker container `cloudstore-product` |

Docker Hub user is `delucagit`. Product image: `delucagit/cloudstore-product-service:latest`.

Both instances share ONE security group with inbound TCP `8080` and `8082` open to `0.0.0.0/0` (course testing posture), plus SSH `22`. The shared SG is also why EC2-to-EC2 private calls on `8082` work.

### 16.3 What is DONE (product side)

- New EC2 launched (Amazon Linux 2023, t3.micro, same key `cloudstore-ec2-key`).
- Docker installed: `sudo dnf install -y docker`, `sudo systemctl enable --now docker`, `sudo usermod -aG docker ec2-user` (then re-login for the group to take effect).
- `docker login -u delucagit` succeeded (plain `docker login` with the prompt failed; using `-u` worked).
- Env file at `/opt/cloudstore/product.env`, owned by `ec2-user:ec2-user`, mode `600` (see 16.5 for why NOT root-owned).
- Container running and verified:

```bash
docker run -d \
  --name cloudstore-product \
  -p 8082:8082 \
  --restart unless-stopped \
  --env-file /opt/cloudstore/product.env \
  delucagit/cloudstore-product-service:latest
```

- Logs reach `Started ProductServiceApplication`, Flyway validates 1 migration against `product_db`, and `curl -i http://127.0.0.1:8082/api/products` returns `200` with the full FakeStore product list.

### 16.4 What was DONE (user-order side)

1. **Verified private reachability.** From the OLD EC2, `curl -i http://ip-172-31-46-104.eu-north-1.compute.internal:8082/api/products` returned `200`. Confirms VPC-internal service-to-service works.

2. **Repointed user-order.** On the OLD EC2, edited `/etc/cloudstore-user-order.env` and changed `PRODUCT_SERVICE_BASE_URL` from `http://127.0.0.1:8082` to `http://ip-172-31-46-104.eu-north-1.compute.internal:8082`, then `sudo systemctl restart cloudstore-user-order.service`.

   NOTE: user-order is still a **systemd JAR** on the old EC2, NOT Docker yet. We chose to repoint + restart the existing systemd service rather than migrate it to Docker in the same step, to keep the change small and testable. Moving user-order to Docker is still open (needed for the CI/CD deploy step to work against it). See section 17.

3. **Retired the product JAR on the OLD EC2.** `sudo systemctl stop cloudstore-product.service` + `sudo systemctl disable cloudstore-product.service`. Verified user-order is `active (running)` and product is inactive/disabled on the old box.

4. **End-to-end test passed.** From laptop + frontend: register → login → products → **create order** → my orders all worked. Order creation is the real proof, because that is when user-order calls product-service over the private DNS with the forwarded JWT.

### 16.5 Gotchas hit during the product split (so we don't repeat them)

- **Docker `--env-file` is literal `KEY=value`.** It does NOT strip quotes or process `export` like a shell. Three separate failures came from this:
  - Quotes around values: `DB_USERNAME_LOCAL='cloudstore'` made Postgres see the username literally as `'cloudstore'` (with quotes) → `password authentication failed for user "'cloudstore'"`.
  - A bad/empty URL value → `'url' must start with "jdbc"`.
  - Fix: plain values, no quotes, no `export`, no spaces around `=`. Verify with `grep -n "'" file ; grep -n '"' file` (should print nothing).
- **Wrong database name** → `Migration checksum mismatch for migration version 1`. The product env had `users_and_orders_db`; it MUST be `product_db`. Same gotcha as section 6/14. Do NOT run Flyway repair — just point at the right DB.
- **Env file ownership for Docker differs from systemd.** systemd reads `EnvironmentFile` as root, so root:root 600 was fine there. Docker reads `--env-file` as the invoking user (`ec2-user`), so the file must be `chown ec2-user:ec2-user` + `600`, otherwise `docker run` fails with `open /opt/cloudstore/product.env: permission denied`. Keeping it ec2-user-owned also matches how the CI deploy (SSH as ec2-user, no sudo) will read it.
- **`<placeholder>` in commands.** Pasting `<dockerhub-username>` literally makes bash try input redirection (`No such file or directory`). Always substitute real values; never leave angle brackets.
- **curl too soon.** Spring Boot takes ~10–15s to boot; an immediate `curl` gives `Connection reset by peer` / `Empty reply`. Wait, then check `docker ps` STATUS and `docker logs`.
- **A blocked client network looks like an AWS bug but isn't.** The frontend got `ERR_CONNECTION_TIMED_OUT` on `:8082` for BOTH EC2s, while `:8080` worked and EC2-to-EC2 `:8082` worked. Root cause was the laptop's Wi-Fi blocking outbound port `8082`; switching Wi-Fi fixed it instantly. How we proved it: `Test-NetConnection` from the laptop showed `8080 -> True` but `8082 -> timeout` to the same shared SG, so the SG couldn't be the cause. Lesson: when one port works and another times out on the same security group, suspect the client network, not AWS. (This is also an argument for the HTTPS/443 fix, since 443 is rarely blocked.)

## 17. What's next (resume here after the split)

The two-EC2 split is done. Remaining work toward final submission, roughly in priority order:

1. **(Optional consistency) Move user-order to Docker on the old EC2.** Right now product runs as Docker but user-order still runs as a systemd JAR. They work fine mixed, but the CI/CD deploy step (`docker pull` + `docker run`) can only auto-deploy a service that runs as Docker. Migrating user-order to Docker makes both deployable the same way. Mirror section 16.3: build/pull `delucagit/cloudstore-user-order-service:latest`, create `/opt/cloudstore/user-order.env` (ec2-user, 600), stop+disable `cloudstore-user-order.service`, then `docker run -p 8080:8080 --restart unless-stopped`.

2. **Wire GitHub Actions deploy to both EC2s.** Workflows already have a `deploy-ec2` job that SSHes and runs `docker pull`/`docker run` against `/opt/cloudstore/<service>.env`. Set the repo secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, and per service `*_EC2_HOST` / `*_EC2_USER` / `*_EC2_SSH_KEY`. Host paths already match (`/opt/cloudstore/`). For user-order this depends on step 1 being done.

3. **HTTPS (G requirement, still unchecked).** Put a reverse proxy (Nginx) + Let's Encrypt/Certbot in front so the app is reachable over `443`. Bonus: 443 is not blocked by restrictive client networks (see the gotcha in 16.5).

4. **Final submission.** Document the live public URL for Learnpoint and tick the remaining boxes in `userOrderService/kursinlämning.md`.

