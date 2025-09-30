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

The service listens on `http://localhost:8080`.

### 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Visit `http://localhost:5173` in a Chromium-based browser. Vite proxies `/api/*` calls to the local backend. 登录页面包含：

- 后端健康检测（/api/health）。
- 模板管理面板，可保存/编辑/删除水印模板并实时读取后端数据。
- 字体枚举结果，可用于提示前端展示哪些字体已经在系统中可用。

### 3. Run automated checks

```bash
cd backend
./mvnw test

cd ../frontend
npm run build
```

The frontend build step compiles TypeScript (via `vue-tsc`) and produces production assets. 运行完成后可删除 `frontend/dist/`（已列入 `.gitignore`）。

## API overview

| Method | Path                   | Description                       |
| ------ | ---------------------- | --------------------------------- |
| GET    | `/api/health`          | 健康检查，返回服务状态与时间戳。
| GET    | `/api/fonts`           | 列出操作系统可用字体（按名称排序）。
| GET    | `/api/templates`       | 获取已保存的水印模板列表。
| GET    | `/api/templates/{id}`  | 读取单个模板详情。                |
| POST   | `/api/templates`       | 新建或更新模板（根据 `id` 判断）。|
| DELETE | `/api/templates/{id}`  | 删除指定模板。                    |

请求/响应采用 JSON，前端示例接口定义位于 `frontend/src/services/api.ts`。

## Data storage

默认存储目录遵循：

- Windows：`%APPDATA%/PhotoWatermark`
- 其他系统（开发测试用）：`${user.home}/.photo-watermark`

模板以 JSON 文件形式保存在 `templates/` 子目录。例如：

```
%APPDATA%/PhotoWatermark/templates/{templateId}.json
```

若需要自定义目录，可在 `backend/src/main/resources/application.properties` 中设置：

```
app.storage.base-dir=E:/PhotoWatermarkData
```

## Next steps

- 扩展后端：实现水印渲染、图片导入批处理、导出队列与进度 API。
- 扩展前端：接入图片列表、预览画布、拖拽定位和导出流程 UI。
- 打包与发布：使用 `jpackage` 生成自带运行时的 Windows 安装包/绿色版，并在 GitHub Release 提供下载。