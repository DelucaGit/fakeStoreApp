Inlämningsuppgift - Molnbaserad Javaapplikation

Projekt: CloudStore

Statusnyckel:
- [ ] Klar
- [x] Kvar

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
- [x] Applikationen ska ha bade frontend och backend
- [ ] Frontend kan byggas med Thymeleaf (ej anvant; separat React-frontend i `frontend/`)
- [x] Applikationen ska integrera med FakeStore API for produkter

### Autentisering
- [x] Applikationen ska innehalla inloggning med Spring Security
- [x] Anvandare ska kunna registrera konto och logga in

### Databas
- [ ] Applikationen ska anvanda en databas i molnet
- [ ] Databasen ska koras i AWS RDS

### Tester
- [x] Projektet ska innehalla automatiska tester for karnfunktionalitet
- [x] Tester ska koras automatiskt i CI-pipelinen (GitHub Actions per mikrotjanst)

### CI (Continuous Integration)
CI-pipelinen ska vid varje commit:
- [x] bygga projektet (`mvn verify` nar respektive tjansts sokvagar andrats)
- [x] kora tester
- [x] skapa en Docker image och lagra den i Dockerhub (vid `push`, nar Docker Hub-secrets ar satta)

### Docker
Projektet ska innehalla:
- [x] en Dockerfile for backendapplikationen (en per tjanst: `userOrderService/Dockerfile`, `productService/Dockerfile`)
- [x] en docker-compose konfiguration (`fakeStoreApp/docker-compose.yml`)

docker-compose ska starta upp:
- [x] backend (bada tjanster)
- [x] en databas (PostgreSQL i Compose; kursmall namner MySQL som exempel)

- [x] Den lokala miljoen ska efterlikna produktionsmiljoen sa mycket som mojligt (samma typer av miljovariabler via `.env`)

### Deployment
Applikationen ska:
- [ ] deployas till AWS EC2 (workflow finns; verifiera nar instanser ar klara)
- [ ] vara tillganglig via internet

### HTTPS
- [ ] Applikationen ska vara tillganglig via HTTPS
- [ ] TLS-certifikat ska installeras med exempelvis Let's Encrypt/Certbot

## Val Godkant (VG)
Utöver allt i G ska du:

### Frontend
- [x] Frontend ska byggas med ett frontendramverk (React, Vue eller Angular) (`frontend/`: React + Vite)

### Mikrotjanster
- [x] Applikationen ska delas upp i minst tva tjanster
- [x] Product service ansvarar for integrationen med FakeStore API
- [x] User/Order service hanterar anvandare, inloggning och bestallningar

### Servicekommunikation
- [x] Tjansterna ska kommunicera via HTTP
- [x] Autentisering mellan tjanster ska ske med JWT (`ProductServiceClient` skickar `Authorization: Bearer ...`; `productService` validerar access-JWT med samma hemlighet som user-order)

### Infrastruktur
- [ ] Tjansterna ska deployas pa separata EC2-instanser

### CI/CD
CI/CD-pipelinen ska:
- [x] bygga applikationen
- [x] kora tester
- [x] skapa Docker images och deploya till Dockerhub
- [ ] deploya appen till AWS (deploy-steg i workflow; kraver EC2 + secrets + miljo pa server)

Lamna in lank till repot pa Learnpoint och lank till er sida.

## Current Known State

### Repo layout (monorepo)
- `userOrderService` — port **8080** (anvandare, JWT-inloggning, ordrar)
- `productService` — port **8082** (produkter via FakeStore API)
- `frontend` — React (Vite), anropar user service pa 8080 och product service pa 8082

### Backend architecture
- Tva Spring Boot-tjanster med Flyway och PostgreSQL (konfigurerat via miljovariabler, t.ex. `DB_URL_LOCAL`).

### Implemented API capabilities
- Registrering, inloggning, JWT och refresh-token-flode i user/order-tjansten.
- Produktlista och produkt enligt id i product-tjansten.
- Skapa order och hamta "mina ordrar" i user/order-tjansten.

### Service communication
- `userOrderService` anropar `productService` over HTTP (`GET .../api/products/{id}`) for prislookup vid orderskapande med **vidarebefordrad anvandar-access-JWT** i `Authorization`; `productService` validerar token (Spring Security + `JwtUtil`).

### Data and persistence
- Database-first med Flyway; entiteter/repositories for anvandare, refresh tokens, ordrar, orderrader.

### DevOps
- **GitHub Actions:** `.github/workflows/user-order-service-ci.yml` och `product-service-ci.yml` — kors vid `push` och `pull_request` nar filer under respektive tjanst (eller workflow-filen) andrats.
- **Docker:** multi-stage `Dockerfile` i varje backend-mapp; bygge och push till Docker Hub efter lyckade tester (pa `push`).
- **Saknas annu:** RDS, EC2 i produktion, HTTPS. **Compose:** `fakeStoreApp/docker-compose.yml` (Postgres + bada backends; `JWT_SECRET_KEY` till bada tjanster for JWT-validering i product).

### Testing status
- Enhetstester och `@SpringBootTest` dar det behovs; CI kor `mvn verify` med PostgreSQL som tjanst i workflow.

## What's Next (prioriterat)

### 1) Molndata och deployment (G + VG-infrastruktur)
- Satt upp AWS RDS och peka miljovariabler mot molndatabasen.
- Driftsatt pa tva EC2 (eller motsvarande), sakra att GitHub-secrets for SSH/host matchar; verifiera `docker pull` + `docker run` med env-filer pa server.
- Gor appen narbar fran internet och dokumentera URL for inlamning.

### 2) HTTPS
- Nginx (eller liknande) som reverse proxy, Let's Encrypt/Certbot for TLS.

### 3) JWT mellan tjanster (VG)
- [x] `productService` validerar access-JWT; `ProductServiceClient` skickar `Authorization` vid prislookup. CORS/OPTIONS ar hanterat i `productService` for frontend mot `8082`.

### 4) docker-compose (G)
- [x] `fakeStoreApp/docker-compose.yml`: Postgres + `user-order-service` + `product-service`; hemligheter i `.env` (se `.env.example`).

### 5) Final inlamning
- Bocka av alla kravrutor ovan nar de ar sanna.
- Learnpoint: repo-URL + live-sida.
- Demo: registrera -> logga in -> produkter -> skapa order -> mina ordrar.

### Quick restart command list
- Docker (fran `fakeStoreApp/`): kopiera `.env.example` till `.env`, fyll i varden; `docker compose up --build`.
- Starta `userOrderService` och `productService` lokalt utan Docker (med PostgreSQL och miljovariabler).
- Starta frontend: `npm run dev` i `frontend/`.
- Kor `./mvnw verify` i respektive tjanstmapp innan storre andringar.
