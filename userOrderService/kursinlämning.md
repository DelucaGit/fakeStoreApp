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
- [x] Applikationen ska anvanda en databas i molnet (bada tjanster kor pa EC2 mot RDS; verifierat med Postman: skapa anvandare, hamta produkter m.m.)
- [x] Databasen ska koras i AWS RDS (instans `cloudstore-postgres`, region `eu-north-1`, bada databaserna `users_and_orders_db` och `product_db`)

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
- [x] deployas till AWS EC2 (en instans, Amazon Linux 2023; bada Spring Boot-JAR mot RDS; manuell start via `java -jar` efter SSH — se AWS-avsnittet nedan)
- [x] vara tillganglig via internet (Postman mot publikt EC2-IP pa 8080/8082; EC2-SG oppen `0.0.0.0/0` for lararkorrection — medvetet testlage, inte produktionssakerhet)

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
- [x] Tjansterna ska deployas pa separata EC2-instanser (KLART 2026-06-04: `userOrderService` kor pa `cloudstore-app`-EC2, `productService` kor pa ny `cloudstore-product`-EC2; bada kor nu som Docker-containrar; user-order anropar product via privat DNS i samma VPC; verifierat end-to-end med orderskapande)

### CI/CD
CI/CD-pipelinen ska:
- [x] bygga applikationen
- [x] kora tester
- [x] skapa Docker images och deploya till Dockerhub
- [x] deploya appen till AWS (KLART 2026-06-07: deploy-steg SSH:ar till respektive EC2 och kor `docker pull` + `docker run`; GitHub-secrets satta; verifierat for user-order vid push till `main` — `deploy-ec2`-jobbet lyckades och ny container kordes pa EC2. Product-workflow ar identiskt uppsatt och EC2:n ar Docker-redo)

Lamna in lank till repot pa Learnpoint och lank till er sida.

## AWS-driftsattning — handoff for nasta AI / aterupptag

**Var vi ar (2026-05):** Molnlaget ar **uppe och API-testat** (Postman: anvandarregistrering, produkter). **Aterstar:** processoverlevnad efter SSH (`systemd`/`tmux`/`nohup`), ev. tva EC2 for VG, HTTPS, ev. CI-deploy till AWS, dokumentera publik URL for inlamning.

### Stegdefinitioner (denna konversation)

| Steg | Innehall | Status |
|------|-----------|--------|
| 1 | EC2 security group: SSH 22, 8080, 8082 | Klart — inbound **`0.0.0.0/0`** avsiktligt sa **larare** kan na API fran internet (testkurs; inte rekommenderat i riktig produktion) |
| 2 | Java 21 pa EC2 (Amazon Corretto 21 verifierad) | Klart |
| 3 | Bygg JAR lokalt (`mvnw package`), `scp` fran **Windows PowerShell** till `ec2-user@...:/home/ec2-user/` | Klart (`productService-0.0.1-SNAPSHOT.jar`, `userOrderService-0.0.1-SNAPSHOT.jar` i `~`) |
| 4 | Pa EC2: `export` / miljovariabler; **olika** `DB_URL_LOCAL` per tjanst; **product** (8082) forst, sedan **user-order** (8080) | Klart |
| 5 | Roktest (Postman / HTTP mot publikt EC2-IP) | Klart |
| 6 | Process lever vid SSH-avbrott (`systemd`, `screen`, `tmux`, eller `nohup`) | **Ej gjort** — tjanster stoppas om SSH-sessionen avslutas utan bakgrundskorning |
| 7 | VG: **tva** separata EC2 (en tjanst per instans) | **Planerat** — just nu en instans kor bada JAR |

### RDS (PostgreSQL; motorversion i AWS kan vara nyare an 16 — t.ex. 18.x — appen anvander `sslmode=require`)

- **Instansidentifierare:** `cloudstore-postgres`
- **Region:** `eu-north-1`
- **Endpoint (host i JDBC/psql):** `cloudstore-postgres.cvck4ycychj2.eu-north-1.rds.amazonaws.com`
- **Databaser:** `users_and_orders_db` (skapad vid RDS-wizard), `product_db` (skapad med `CREATE DATABASE` fran EC2 via `psql`)
- **Lokal laptop ansluter inte till RDS** (design); admin gar via SSH till EC2.
- **`psql \l`:** kan ge katalogfel (`d.daticulocale`); anvand `SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY 1;` for att lista databaser.

**JDBC-exempel (samma host, olika databasnamn — laggs i `DB_URL_LOCAL` per process):**

- Product: `jdbc:postgresql://cloudstore-postgres.cvck4ycychj2.eu-north-1.rds.amazonaws.com:5432/product_db?sslmode=require`
- User-order: `jdbc:postgresql://cloudstore-postgres.cvck4ycychj2.eu-north-1.rds.amazonaws.com:5432/users_and_orders_db?sslmode=require`

Ovriga variabler enligt repo: `DB_USERNAME_LOCAL`, `DB_PASSWORD_LOCAL`, samma `JWT_SECRET_KEY` pa **bada** tjanster, `AUTH_ENDPOINT_REGISTER`, `AUTH_ENDPOINT_USER_FETCH`, `PRODUCT_SERVICE_BASE_URL` (t.ex. `http://127.0.0.1:8082` om bada JAR kor pa samma EC2), `CORS_ALLOWED_ORIGINS`. Se `.env.example` / `README.md`.

### EC2

- SSH med `.pem` pa **Desktop** (Windows) — korrekt `ssh -i "C:\Users\...\Desktop\....pem" ec2-user@<publik IPv4>`.
- RDS security group ska tillata **5432 fran EC2-instansens security group** (inte publika klienter).

### Sakerhet (nuvarande testbeslut)

- **EC2:** Inbound **`0.0.0.0/0`** pa relevanta portar — **medvetet** for lararaccess; byt till snavare regler eller Session Manager efter kursen om mojligt.
- **RDS:** Endast **5432** mot EC2-instansens security group (inte oppen mot hela internet). Masterlosenord / JWT **ej** roterade i detta testskede (acceptabelt for kursdemo; rotera vid riktig exponering).
- **Hemligheter:** lag aldrig riktiga losenord eller JWT i repo eller chatloggar.

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
- **AWS (drift idag):** RDS `cloudstore-postgres` + **en** EC2 med tva manuellt startade JAR mot RDS; **Postman** verifierar floden. **Ej annu:** `systemd`/bakgrundskorning, **HTTPS**, **tva EC2** (VG), automatisk **workflow-deploy** till denna miljo (JAR-korning ar manuell efter SSH). **Compose** for lokal utveckling: `fakeStoreApp/docker-compose.yml`.

### Testing status
- Enhetstester och `@SpringBootTest` dar det behovs; CI kor `mvn verify` med PostgreSQL som tjanst i workflow.

## What's Next (prioriterat)

### 1) Molndata och deployment (G + VG-infrastruktur)
- **Klart i grova drag:** RDS + en EC2 + manuella JAR + Postman-test; G-krav for molndata och EC2-deploy ar bockade under **Databas** och **Deployment** ovan.
- **Nasta konkreta steg:** `systemd` (eller motsv.) sa tjansterna **overlever SSH-avbrott** och omstart; dokumentera **publik bas-URL** (EC2-IP eller Elastic IP/DNS) for Learnpoint.
- **VG:** dela upp i **tva EC2** (en tjanst per instans); uppdatera `PRODUCT_SERVICE_BASE_URL` till intern/publik adress mellan instanser.
- **Valfritt:** koppla befintlig GitHub Actions-deploy till miljon (secrets mot EC2), eller `docker pull` + `docker run` pa server i stallet for manuell JAR.

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
