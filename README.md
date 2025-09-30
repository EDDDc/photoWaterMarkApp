# Photo Watermark App

Spring Boot + Vue 3 application for batch applying watermarks to local images on Windows. This repository hosts the full-stack implementation outlined in [docs/PRD.md](docs/PRD.md).

## Project structure

- `backend/` – Spring Boot 3 service exposing `/api` endpoints.
- `frontend/` – Vue 3 + TypeScript SPA that consumes the backend.
- `docs/` – Product requirement document and supplementary design notes.

## Prerequisites

- JDK 17+
- Node.js 20+
- npm (bundled with Node.js)

## Development setup

### 1. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080` and exposes a health check at `/api/health`.

### 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Visit `http://localhost:5173` in a Chromium-based browser. Vite is configured to proxy `/api/*` calls to the local backend. The landing page displays backend connectivity status via the health check endpoint.

### 3. Run automated checks

```bash
cd backend
./mvnw test

cd ../frontend
npm run build
```

The frontend build step compiles TypeScript (via `vue-tsc`) and produces production assets.

## Next steps

- Implement API endpoints for font enumeration, template storage, and export jobs according to the PRD.
- Build the Vue UI for image import, preview canvas, and watermark configuration.
- Configure packaging (e.g., `jpackage`) to ship a Windows executable bundling the backend and frontend.
