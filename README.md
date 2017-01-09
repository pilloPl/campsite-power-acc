# campsite-power

Backend of a network of campsites that sells electricity on the pitches. Pedestals with metered CEE sockets talk to
the backend over three generations of a REST link; guests plug in with a camping card, a house card of the campsite
or a QR payment in an app. The backend keeps the supply sessions, turns them into priced consumption records
(tariffs with time, day and kWh restrictions, seasonal contracts with power packages), bills the network's guests
through the ERP, exchanges cards and records with partner card issuers, watches the pedestals and the power budget
of the campsite sections, and serves the operations console (reception, campsite hosts, service technicians, back
office, administrators).

## Modules

| Module | Content |
|---|---|
| `campsite-commons` | paging, dates, text helpers |
| `campsite-value-objects` | typed values (`PedestalCode`, `SocketCode`, `CardRfid`, `Money`, ...) |
| `campsite-transfer-objects` | DTOs of the service interfaces and the pedestal link |
| `campsite-api` | service interfaces used by the web layer |
| `campsite-db-migrations` | Flyway scripts (`db/migration`): tables, PL/pgSQL, views, materialized views, seed data |
| `campsite-core` | domain model, services, DAOs (JPA and hand-written SQL in `sql/sqls.xml`), mail templates |
| `campsite-endpoints` | console and pedestal link web services, security (login, CSRF, CORS), OpenAPI |
| `campsite-server` | Spring Boot application, Quartz jobs, configuration, demo data |

## Build and test

Java 21, Maven, Docker (the integration tests start PostgreSQL with Testcontainers):

```bash
mvn -q -B verify
```

## Running the backend with demo data

The profile `demo` fills the database with a demo network at start-up (`campsite-server/src/main/resources/db/demo`):
three campsites (Camp Istria Bay in Croatia, Garda Lakeside Camping in Italy, Ardeche River Camp in France) with map
outlines of the campsites, sections and pitches, 18 pedestals of the three link generations with 72 sockets, power
sections, tariffs and seasonal contracts, several hundred guests with stays, cards and house cards, 44 days of supplies
with meter readings every 30 minutes, consumption records with a review queue, ERP billing lines of the month before,
QR payments, an issuer partner, faults, incidents, events and audit rows. Every start replaces the demo rows (ids from
900000000 on) and moves them to the current date; `--campsite.demo.reseed=false` keeps an existing seed. The demo
also sets a few configuration values: `CheckoutSessionCloseDelayInMinutes` 0 (a check-out closes the supplies at the
next job run), `PedestalHeartbeatTimeoutInMinutes` 10080 (the demo pedestals send no heartbeats), the reviewers of
suspect records and temporary directories for the ERP files. Do not use the profile on a production database.

```bash
docker compose up -d                       # PostgreSQL 16, database campsite_power, user/password campsite
mvn -q -B -DskipTests package
java -jar campsite-server/target/campsite-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=demo
# or: mvn -pl campsite-server spring-boot:run -Dspring-boot.run.profiles=demo
```

The database and port can be changed with `CAMPSITE_DB_HOST`, `CAMPSITE_DB_PORT`, `CAMPSITE_DB_NAME`,
`CAMPSITE_DB_USER`, `CAMPSITE_DB_PASSWORD` and `CAMPSITE_HTTP_PORT` (default 8080).

### Demo users

The password of every demo user is `demo-<username>`.

| Username | Role | Scope | Console tabs |
|---|---|---|---|
| `reception` | RECEPTION | Camp Istria Bay, Croatian guests | 1 Dashboard, 2 Map, 5 Cards, 6 Guests & stays, 7 Supply sessions |
| `host` | CAMPSITE_MANAGER | Garda Lakeside Camping only | reception tabs of its campsite, tariffs, reports |
| `service` | SERVICE_TECHNICIAN | Croatian campsites | 2 Map, 4 Pedestals & sockets, 11 Power budget, 12 Monitoring & service |
| `backoffice` | BACK_OFFICE | Croatia | 8 Consumption records, 9 Tariffs, 10 QR payments, 13 Reports, 14 ERP billing, 15 Issuer exchange |
| `admin` | MARKET_ADMIN | Croatia | all tabs, 16 Administration |
| `admin.it`, `admin.fr` | MARKET_ADMIN | Italy, France | all tabs of their market |
| `qrpay-app` | QRPAY_APP_CLIENT | technical user of the payment app (HTTP basic on `/api/qrpay/**`) | none |

`trainee` is an invited reception user without a password (to try the user administration).

### Running in one container

The `Dockerfile` builds the console into the static resources of the server, so one container serves both (client-side
routes fall back to `index.html`). The JVM settings in the `Dockerfile` keep the process within 512 MB.

```bash
docker build -t campsite-power .
docker run -p 8080:8080 -e CAMPSITE_DB_HOST=host.docker.internal campsite-power
```

### Console login

The console is a single page application; the backend keeps the login in the HTTP session:

1. `GET /api/console/auth/currentUser` answers 401 without a login and sets the cookie `XSRF-TOKEN`.
2. `POST /api/console/auth/login` with `{"username": "admin", "password": "demo-admin"}` and the header
   `X-XSRF-TOKEN: <cookie value>` answers the user with roles and rights and sets the session cookie `JSESSIONID`.
3. Every further state-changing call sends the session cookie and the `X-XSRF-TOKEN` header;
   `POST /api/console/auth/logout` ends the session.

```bash
curl -c jar -b jar http://localhost:8080/api/console/auth/currentUser
curl -c jar -b jar -H "X-XSRF-TOKEN: $(awk '/XSRF-TOKEN/ {print $7}' jar)" -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"demo-admin"}' http://localhost:8080/api/console/auth/login
```

The Vite dev server of the console (`http://localhost:5173`) may call the backend with credentials; other origins are
configured with `campsite.console.cors.allowed-origins` (environment `CAMPSITE_CONSOLE_ORIGINS`, comma separated).

### API description

The OpenAPI description of all web services is at `/v3/api-docs` (the typed client of the console is generated from
it), the Swagger UI at `/swagger-ui.html`.
