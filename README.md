# Photo Watermark App

Photo Watermark App is a Windows desktop utility that batches watermark operations on images.  
The backend is built with Spring Boot, the SPA frontend uses Vue 3, and the desktop shell packages everything with Electron and `jpackage`.

## Requirements

- JDK 17 or later (with `jpackage`)
- Node.js 20 or later
- npm (bundled with Node.js)

## Quick Start

1. Clone the repository and start the backend:
   ```bash
   git clone <repo-url>
   cd photoWatermarkApp/backend
   ./mvnw spring-boot:run
   ```
   The API listens on `http://localhost:8080`.

2. In a new terminal, run the frontend:
   ```bash
   cd ../frontend
   npm install
   npm run dev
   ```
   Browse to `http://localhost:5173`. Vite proxies `/api/*` requests to the backend.

3. Optional – launch the desktop shell (requires a packaged backend jar):
   ```bash
   cd ../backend
   ./mvnw clean package -DskipTests

   cd ../desktop
   npm install
   npm run start
   ```
   Electron boots the backend automatically and stops it when the shell exits.

## Build & Package

- Run automated checks:
  ```bash
  cd backend
  ./mvnw test

  cd ../frontend
  npm run build
  ```

- Create a Windows installer (WiX Toolset is expected in `tools/wix314`):
  ```powershell
  $env:WIX = "$(Resolve-Path tools/wix314)"
  $env:PATH = "$env:WIX;" + $env:PATH
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package-windows.ps1 -AppVersion '1.0.5'
  ```
  The installer is written to `build/windows/PhotoWatermarkApp-<version>.exe`.

## Troubleshooting

- **Failed to launch JVM on the second open**  
  If you close the browser window only, the background `PhotoWatermarkApp.exe` stays alive. A second launch finds the JVM already locked and aborts. Quit from the tray icon or run `taskkill /IM PhotoWatermarkApp.exe /F` before starting again.

- **Port already in use**  
  Add `-Dserver.port=<new-port>` to `PhotoWatermarkApp.cfg` in the installation directory, then restart.

- **Custom storage directory**  
  Add `-Dapp.storage.base-dir=<path>` to `PhotoWatermarkApp.cfg`. Ensure the directory is writable for the current user.

## Repository Layout

- `backend/` – Spring Boot service exposing template, export, and storage APIs.
- `frontend/` – Vue 3 single-page app for template management and export workflows.
- `desktop/` – Electron wrapper that runs the backend alongside a desktop window.
- `scripts/` – Build and packaging scripts.
- `docs/` – Additional documentation and release notes.

Refer to the documents under `docs/` for further details.
