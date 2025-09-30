# Photo Watermark App Frontend

This project is the Vue 3 + TypeScript single-page application for the Photo Watermark desktop app. It consumes the Spring Boot backend via the `/api` endpoints and provides the desktop UI shell.

## Scripts

```bash
npm install
npm run dev
npm run build
npm run preview
```

During development, the Vite dev server proxies API requests to `http://localhost:8080`. Start the backend with `../backend/mvnw spring-boot:run` before loading the SPA.
