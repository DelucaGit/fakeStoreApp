Inlämningsuppgift - Molnbaserad Javaapplikation

Projekt: CloudStore

Statusnyckel:
- [x] Klar
- [ ] Kvar

Projektbeskrivning:
I detta projekt ska ni bygga en forenklad e-handelsapplikation.
Applikationen ska integrera med FakeStore API for att hamta produkter och innehalla:
- anvandarregistrering
- inloggning
- visning av produkter
- skapande av bestallningar

Fokus i projektet ligger pa att utveckla, testa och driftsatta en molnbaserad applikation enligt moderna DevOps-principer.

## Krav for Godkant (G)

### Applikation
- [x] Backend ska byggas i Java med Spring Boot
- [ ] Applikationen ska ha bade frontend och backend
- [ ] Frontend kan byggas med Thymeleaf
- [x] Applikationen ska integrera med FakeStore API for produkter

### Autentisering
- [x] Applikationen ska innehalla inloggning med Spring Security
- [x] Anvandare ska kunna registrera konto och logga in

### Databas
- [ ] Applikationen ska anvanda en databas i molnet
- [ ] Databasen ska koras i AWS RDS

### Tester
- [x] Projektet ska innehalla automatiska tester for karnfunktionalitet
- [ ] Tester ska koras automatiskt i CI-pipelinen

### CI (Continuous Integration)
CI-pipelinen ska vid varje commit:
- [ ] bygga projektet
- [ ] kora tester
- [ ] skapa en Docker image och lagra den i Dockerhub

### Docker
Projektet ska innehalla:
- [ ] en Dockerfile for backendapplikationen
- [ ] en docker-compose konfiguration

docker-compose ska starta upp:
- [ ] backend
- [ ] en databas (exempelvis MySQL)

- [ ] Den lokala miljoen ska efterlikna produktionsmiljoen sa mycket som mojligt

### Deployment
Applikationen ska:
- [ ] deployas till AWS EC2
- [ ] vara tillganglig via internet

### HTTPS
- [ ] Applikationen ska vara tillganglig via HTTPS
- [ ] TLS-certifikat ska installeras med exempelvis Let's Encrypt/Certbot

## Val Godkant (VG)
Utöver allt i G ska du:

### Frontend
- [ ] Frontend ska byggas med ett frontendramverk (React, Vue eller Angular)

### Mikrotjanster
- [x] Applikationen ska delas upp i minst tva tjanster
- [x] Product service ansvarar for integrationen med FakeStore API
- [x] User/Order service hanterar anvandare, inloggning och bestallningar

### Servicekommunikation
- [x] Tjansterna ska kommunicera via HTTP
- [ ] Autentisering mellan tjanster ska ske med JWT

### Infrastruktur
- [ ] Tjansterna ska deployas pa separata EC2-instanser

### CI/CD
CI/CD-pipelinen ska:
- [ ] bygga applikationen
- [ ] kora tester
- [ ] skapa Docker images och deploya till Dockerhub
- [ ] deploya appen till AWS

Lamna in lank till repot pa Learnpoint och lank till er sida.

## Current Known State

### Backend architecture
- Two Spring Boot services exist in this monorepo:
  - `userOrderService` (users, auth, orders)
  - `productService` (product catalog via FakeStore API)

### Implemented API capabilities
- User registration and login are implemented.
- JWT-based authentication is implemented.
- Refresh token flow is implemented.
- Product listing and product-by-id are implemented.
- Order creation and "my orders" fetching are implemented.

### Service communication
- `userOrderService` calls `productService` over HTTP for product price lookup during order creation.

### Data and persistence
- Database-first style is configured with Flyway enabled in service configs.
- Core entities/repositories for users, refresh tokens, orders, and order items are present.

### Testing status
- Unit tests exist for key services/utilities (user service, order service, JWT utility).
- CI-based automatic test execution is not yet configured in repository workflows.

## What's Next (When You Return)

### 1) Frontend first (highest priority for G)
- Build a minimal frontend that supports:
  - register
  - login
  - list products
  - create order
- Connect frontend to `userOrderService` (`:8080`) and `productService` (`:8081`) endpoints.

### 2) Dockerize local environment
- Add one `Dockerfile` per backend service.
- Add `docker-compose.yml` that starts:
  - `userOrderService`
  - `productService`
  - database
- Make sure environment variables are passed from compose.

### 3) CI pipeline
- Add GitHub Actions workflow under `.github/workflows/`.
- On every push/PR: build + test both services.
- Then build Docker images and push to DockerHub.

### 4) Cloud deployment
- Deploy services to AWS EC2 (separate instances for VG target).
- Use cloud database (AWS RDS).
- Verify app is reachable from internet.

### 5) HTTPS
- Configure reverse proxy (for example Nginx).
- Install TLS certificates via Let's Encrypt + Certbot.
- Verify HTTPS works end-to-end.

### 6) Final submission checklist
- Confirm all unchecked boxes above are complete.
- Add live URL + repository URL for Learnpoint submission.
- Prepare short demo flow: register -> login -> browse products -> create order -> view orders.

### Quick restart command list
- Start backend services locally.
- Call health/basic endpoints.
- Run tests before any new feature work.
- Continue from section **1) Frontend first**.




