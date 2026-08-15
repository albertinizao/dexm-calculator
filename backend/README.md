# Deus ex Machina Personajes

Backend Spring Boot 4.1 / Java 25 for the character manager.

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

During development, run the Vue dev server separately for hot reload:

```powershell
cd frontend
pnpm install
pnpm run dev
```

For production, Maven builds the frontend and packages it into the Spring Boot
JAR. Start only the backend process:

```powershell
cd backend
.\mvnw.cmd package
java -jar target/personajes-0.1.0-SNAPSHOT.jar
```

After changing the packaged frontend or OAuth origin, stop every previous Java
process before starting the new JAR. A stale process can still expose the old
5177 callback. To force the integrated local origin from PowerShell:

```powershell
$env:APP_SECURITY_FRONTEND_URL = "http://192-168-1-201.sslip.io:8084/"
java -jar target/personajes-0.1.0-SNAPSHOT.jar
```

The Maven build performs these steps automatically (using the same `pnpm`
workflow as local frontend development):

1. Runs `pnpm install --frozen-lockfile` in `frontend/`.
2. Runs `pnpm run build`.
3. Copies `frontend/dist` into the backend classpath before creating the JAR.

`pnpm` must be installed and available on `PATH` (the same prerequisite as
`pnpm run dev`). You can select another executable with
`-Dpnpm.executable=...` if necessary.

The build requires network access when dependencies are not already cached. To
compile the backend without rebuilding the frontend (for example, during an
offline backend-only check), use:

```powershell
.\mvnw.cmd '-Dfrontend.skip=true' '-DskipTests' compile
```

The packaged application serves the SPA and API from the same origin. Vue
history routes under `/characters/**` are forwarded to `index.html`, while
`/api/**` remains protected and is never handled by the SPA fallback.
Static files under `/assets/**` are public; all API endpoints still require the
same authentication and authorization as in development.

The default connection is `jdbc:mariadb://localhost:3306/dexm_personajes` with
`dexm` credentials. Override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and
`SERVER_PORT` for another local environment. Tests use H2 with the `test`
profile.

## Google login (local)

Create a Google OAuth web client and register
`http://localhost:5177/login/oauth2/code/google` as an authorized redirect URI
for development. For production, register
`https://<public-origin>/login/oauth2/code/google` and set
`APP_SECURITY_FRONTEND_URL=https://<public-origin>/` (the trailing slash is
required).
Before starting the backend, set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.
The administrator defaults to `albertinizao@gmail.com`; override it with
`APP_SECURITY_ADMIN_EMAILS` (comma-separated). The
`APP_SECURITY_FRONTEND_URL` setting controls the frontend origin used for the
OAuth callback; the production default is
`http://192-168-1-201.sslip.io:8084/`. When using Vite locally, set it to the
Vite origin, for example `http://192-168-1-201.sslip.io:5177/`.

## Docker production

From the repository root:

```powershell
docker build -t dexm-personajes .
docker run --rm -p 8080:8080 `
  -e SPRING_DATASOURCE_URL="jdbc:mariadb://db.example:3306/dexm_personajes" `
  -e SPRING_DATASOURCE_USERNAME="dexm" `
  -e SPRING_DATASOURCE_PASSWORD="change-me" `
  -e GOOGLE_CLIENT_ID="<google-client-id>" `
  -e GOOGLE_CLIENT_SECRET="<google-client-secret>" `
  -e APP_SECURITY_FRONTEND_URL="https://app.example.com/" `
  dexm-personajes
```

The image activates the `prod` profile, listens on port `8080`, and requires
all six variables shown above. Set `APP_SECURITY_ADMIN_EMAILS` when overriding
the default administrator. Firestore is accessed through Application Default
Credentials on Cloud Run. Register `https://app.example.com/login/oauth2/code/google` with
Google. The same container serves the Vue SPA (including `/characters/**`
history routes) and the `/api/**` endpoints.

## Catalog seeding

Catalog seeding is disabled during normal application startup to avoid reading
the complete Firestore catalogs whenever Cloud Run creates a new instance. Run
the application as a controlled maintenance operation with
`APP_CATALOG_SEED_ENABLED=true` only when catalog data must be synchronized.

## Character aggregate backfill

The migration is intentionally not executed during normal startup. Run the application once with `--app.maintenance.backfill-character-aggregates=true` against the target Firestore project, verify the reported counts, then restart without the flag. The new aggregate format is written by normal operations while legacy collections remain readable as a temporary fallback.
