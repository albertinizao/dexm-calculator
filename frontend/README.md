# Personajes frontend

```powershell
pnpm install --ignore-scripts
pnpm run dev
```

The Vite proxy targets `http://localhost:8084`; set `VITE_API_PROXY_TARGET`
and `VITE_DEV_PORT` when running the backend on another port.

The development server listens on all interfaces and is available at:

- `http://localhost:5177/`
- `http://192.168.1.201:5177/`
- `http://192-168-1-201.sslip.io:5177/`

The Vite server is only needed for development. In production the frontend is
built by `backend/mvnw package` and served by the Spring Boot JAR; do not start
a second frontend process.

The production build uses the frontend source as input but does not require a
running Vite server. API, OAuth and login requests are then resolved by the
same backend origin, so the Vite development proxy is not involved.
