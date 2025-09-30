# Photo Watermark App

Spring Boot + Vue 3 application for applying watermarks to local images on Windows. The detailed requirements are documented in [docs/PRD.md](docs/PRD.md).

## Project structure

- `backend/` – Spring Boot 3 REST API (templates, settings, export jobs, watermark rendering).
- `frontend/` – Vue 3 + TypeScript SPA (图片预览、模板管理、批量导出面板)。
- `docs/` – 产品需求文档与补充设计说明。

## Frontend overview

- `components/ImageWorkspace.vue`：导入图片、拖拽、缩略图列表与 Canvas 预览。
- `components/TemplateFormPanel.vue`：水印模板表单及“默认设置”保存/清除按钮。
- `components/TemplateListPanel.vue`：模板列表，支持加载/删除，与表单共享状态。
- `composables/useImageStore.ts`：管理已导入图片的响应式状态与对象 URL 生命周期。
- `composables/useExportJobs.ts`：封装导出任务提交、轮询、取消与历史记录。

`App.vue` 将上述模块与后端 `/api/templates`、`/api/settings/last`、`/api/export` 等接口贯通。

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

服务监听 `http://localhost:8080`。

### 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`（Chromium 内核浏览器）。Vite 会将 `/api/*` 代理到本地后端。页面提供：健康检测、模板管理、字体枚举、图片工作区及导出面板。

### 3. Run automated checks

```bash
cd backend
./mvnw test

cd ../frontend
npm run build
```

前端构建命令会执行 TypeScript 校验与生产构建。完成后可删除 `frontend/dist/`（已列入 `.gitignore`）。

### 4. 使用批量导出功能

1. 在左侧图片工作区拖拽或选择图片；
2. 在右侧调整水印与导出配置；
3. 打开“批量导出”面板，点击“开始导出”；
4. 面板会显示任务进度、当前处理文件，可在执行中“取消当前任务”。

若未指定输出目录，导出结果默认写入 `%APPDATA%/PhotoWatermark/exports/<timestamp>`。

## API overview

| Method | Path | Description |
| ------ | ----- | ----------- |
| GET | `/api/health` | 健康检查，返回服务状态与时间戳。 |
| GET | `/api/fonts` | 列出系统可用字体（按名称排序）。 |
| GET | `/api/templates` | 获取已保存的水印模板列表。 |
| GET | `/api/templates/{id}` | 读取单个模板详情。 |
| POST | `/api/templates` | 新建或更新模板（根据 `id` 判断）。 |
| DELETE | `/api/templates/{id}` | 删除指定模板。 |
| GET | `/api/settings/last` | 读取最近一次保存的全局设置（无内容返回 204）。 |
| POST | `/api/settings/last` | 保存最近一次使用的设置。 |
| DELETE | `/api/settings/last` | 清除最近一次使用的设置。 |
| POST | `/api/export` | multipart 上传图片与配置，创建导出任务。 |
| GET | `/api/export/{jobId}/status` | 查询指定任务的进度与结果。 |
| POST | `/api/export/{jobId}/cancel` | 取消正在执行的导出任务。 |

请求/响应主要采用 JSON（导出上传使用 multipart），前端示例定义见 `frontend/src/services/api.ts` 与 `frontend/src/composables/useExportJobs.ts`。

## Data storage

- 默认：`%APPDATA%/PhotoWatermark`
- 其他平台（开发测试）：`${user.home}/.photo-watermark`

模板保存为 `templates/{templateId}.json`，最近一次设置保存为 `settings/last.json`。可在 `backend/src/main/resources/application.properties` 配置 `app.storage.base-dir` 自定义目录。

## Next steps

- 前端：在 Canvas 预览中叠加文本/图片水印、支持拖拽定位与导出预览对齐。
- 后端：拓展图片水印、导出失败重试、更多格式处理。
- 发布：使用 `jpackage` 生成携带运行时的 Windows 包，并在 GitHub Release 提供下载。
