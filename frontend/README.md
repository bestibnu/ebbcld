# Cloud City Frontend (V1)

Operational frontend for V1 backend APIs.

## Start
1. Install dependencies
```bash
npm install
```
2. Configure API base URL (optional if backend is on `http://localhost:8080`)
```bash
cp .env.example .env
```
By default, dev mode uses Vite proxy (`/api` -> `http://localhost:8080`) to avoid CORS issues.
3. Run
```bash
npm run dev
```

## Current Screen
- Project Overview Dashboard:
  - project + budget panel
  - cost delta + budget status
  - graph summary
  - graph health
  - pipeline check panel
  - discovery operations (create + execute + status polling)
  - terraform operations (plan + approve/reject + apply + status refresh)

## API Requirement
Backend should expose `/api/v1` endpoints documented in `docs/v1-frontend-api-contract.md`.
