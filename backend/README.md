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
