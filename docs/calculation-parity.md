# Frontend calculation parity

`frontend/src/rules.ts` contains presentation-only calculations for training previews and ability requirement indicators. The backend still validates every write and the director-only unique-ability review.

`calculation-fixtures.json` is the small canonical smoke fixture used when changing either implementation. The repository currently has no frontend test runner, so `pnpm typecheck` and the backend test suite are the automated gates; the fixture is also used during manual browser validation.
