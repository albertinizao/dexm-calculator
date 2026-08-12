# dexm-calculator.github.io
Deus ex Machina sheet calculator

## Development and production

Development uses two processes so Vite can provide hot reload:

```powershell
cd backend; .\mvnw.cmd spring-boot:run
cd frontend; pnpm install; pnpm run dev
```

For production, build and run a single Spring Boot process. Maven invokes the
same `pnpm install --frozen-lockfile` and `pnpm run build` workflow used by the
frontend, then embeds `frontend/dist` in the JAR:

```powershell
cd backend
.\mvnw.cmd package
java -jar target/personajes-0.1.0-SNAPSHOT.jar
```

Set `APP_SECURITY_FRONTEND_URL` to the public backend origin with a trailing
slash and register its `/login/oauth2/code/google` callback in Google OAuth.

The production JAR serves both the Vue SPA and the REST API from the same
origin. Direct navigation or refresh on `/characters/**` is handled by the SPA
history fallback; requests under `/api/**` are not rewritten to HTML.

### Docker (production)

Build from the repository root and run against an externally managed MariaDB:

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

The production profile requires `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `GOOGLE_CLIENT_ID`,
`GOOGLE_CLIENT_SECRET`, and `APP_SECURITY_FRONTEND_URL` (the latter must end in
`/`). `APP_SECURITY_ADMIN_EMAILS` is optional and accepts comma-separated
addresses. Register `https://app.example.com/login/oauth2/code/google` as the
Google OAuth callback. The container serves the SPA, history routes, and API
from the same origin; Flyway validates/applies migrations at startup.

## Excel to JSON
Excel to JSON: https://products.aspose.app/cells/conversion/excel-to-json#google_vignette
