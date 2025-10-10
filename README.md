# Photo Watermark App

Photo Watermark App 是一款面向 Windows 平台的本地图片批量加水印工具。后端基于 Spring Boot，前端使用 Vue 3，桌面客户端通过 Electron 与 jpackage 打包。

## 环境要求

- JDK 17 及以上（需包含 jpackage）
- Node.js 20 及以上
- npm（随 Node.js 安装）

## 快速开始

1. 克隆仓库并启动后端：
   ```bash
   git clone <repo-url>
   cd photoWatermarkApp/backend
   ./mvnw spring-boot:run
   ```
   后端默认监听 `http://localhost:8080`。

2. 在新终端窗口启动前端：
   ```bash
   cd ../frontend
   npm install
   npm run dev
   ```
   浏览器访问 `http://localhost:5173`，Vite 会将 `/api/*` 请求代理到本地后端。

3. （可选）启动桌面壳：
   ```bash
   cd ../backend
   ./mvnw clean package -DskipTests

   cd ../desktop
   npm install
   npm run start
   ```
   Electron 会在启动时自动拉起 Spring Boot，并在退出时尝试终止后端进程。

## 构建与打包

- 执行自动化检查：
  ```bash
  cd backend
  ./mvnw test

  cd ../frontend
  npm run build
  ```

- 生成 Windows 安装包（需提前将 WiX Toolset 解压到 `tools/wix314`）：
  ```powershell
  $env:WIX = "$(Resolve-Path tools/wix314)"
  $env:PATH = "$env:WIX;" + $env:PATH
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package-windows.ps1 -AppVersion '1.0.5'
  ```
  安装包会同时保存到 `build/windows/PhotoWatermarkApp-<版本>.exe` 与仓库根目录 `PhotoWatermarkApp-<版本>.exe`。

## 安装步骤

1. 执行上述打包脚本（或获取现成的 `PhotoWatermarkApp-<版本>.exe`）。
2. 双击安装程序，按照向导选择安装目录与是否创建快捷方式。
3. 安装完成后，可通过开始菜单或桌面快捷方式启动 Photo Watermark App。

## 使用指南

- 首次启动会自动打开浏览器访问 `http://127.0.0.1:8080`，也可以手动在浏览器访问该地址。
- 关闭应用时，请通过托盘图标或应用窗口的“退出”按钮彻底结束，以确保后台 Spring Boot 进程同步关闭。
- 需要调整端口或数据目录时，编辑安装目录下的 `PhotoWatermarkApp.cfg` 并重新启动应用。

## 常见问题

- **第二次启动提示 “Failed to launch JVM”**：首次运行后若仅关闭浏览器或前端窗口，后台 `PhotoWatermarkApp.exe` 仍在运行。再次双击会因为运行时被占用而失败。请通过托盘图标退出，或执行 `taskkill /IM PhotoWatermarkApp.exe /F` 清理进程后再启动。
- **端口被占用**：在安装目录的 `PhotoWatermarkApp.cfg` 中追加 `-Dserver.port=<新的端口>`，保存后重新启动。
- **自定义数据目录**：在 `PhotoWatermarkApp.cfg` 中追加 `-Dapp.storage.base-dir=<路径>`，目标路径需对当前用户可写。若配置无效，应用会回退到 `%APPDATA%/PhotoWatermark`（或 `${user.home}/.photo-watermark`）。

## 仓库结构

- `backend/`：Spring Boot 服务，提供模板、导出与存储 API。
- `frontend/`：Vue 3 单页应用，负责模板编辑与批量导出界面。
- `desktop/`：Electron 桌面壳，封装后端并提供桌面窗口入口。
- `scripts/`：构建与打包脚本（含 Windows 打包脚本）。
- `docs/`：需求说明、发布步骤及其他文档。

更多历史记录与设计细节可参考 `docs/` 目录。
