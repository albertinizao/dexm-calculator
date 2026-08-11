# Deus ex Machina Personajes

Backend Spring Boot 4.1 / Java 25 for the character manager.

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The default connection is `jdbc:mariadb://localhost:3306/dexm_personajes` with
`dexm` credentials. Override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and
`SERVER_PORT` for another local environment. Tests use H2 with the `test`
profile.

## Google login (local)

Create a Google OAuth web client and register
`http://localhost:5177/login/oauth2/code/google` as an authorized redirect URI.
Before starting the backend, set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.
The administrator defaults to `albertinizao@gmail.com`; override it with
`APP_SECURITY_ADMIN_EMAILS` (comma-separated). The
`APP_SECURITY_FRONTEND_URL` setting controls the frontend origin used for the
OAuth callback; the default is `http://localhost:5177/` and production must
provide the public frontend origin with a trailing slash.
