# Photo Watermark App

Windows 平台的本地图片批量加水印工具，基于 Spring Boot + Vue 3 技术栈。详细需求与交互说明请参见 [docs/PRD.md](docs/PRD.md)。

## 项目结构

- `backend/`：Spring Boot 3 REST API，负责模板管理、默认设置、导出任务及水印渲染。
- `frontend/`：Vue 3 + TypeScript 单页应用，提供图片预览、模板编辑、批量导出面板等界面能力。
- `docs/`：产品需求文档与补充设计说明。

## 前端模块概览

- `components/ImageWorkspace.vue`：导入图片、拖拽排序、缩略图列表与 Canvas 预览。
- `components/TemplateFormPanel.vue`：水印模板表单、默认设置保存/清除按钮。
- `components/TemplateListPanel.vue`：模板列表（加载、删除、选择），与表单共享状态。
- `composables/useImageStore.ts`：已导入图片的响应式状态与对象 URL 生命周期管理。
- `composables/useExportJobs.ts`：导出任务的提交、轮询、取消与历史记录封装。

`App.vue` 负责将上述模块与后端 `/api/templates`、`/api/settings/last`、`/api/export` 等接口打通。

## 环境要求

- JDK 17 及以上
- Node.js 20 及以上
- npm（随 Node.js 安装）

## 开发运行

### 1. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

服务监听 `http://localhost:8080`。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

使用 Chromium 内核浏览器访问 `http://localhost:5173`。Vite 会把 `/api/*` 请求代理到本地后端。页面包含健康检测、模板管理、字体枚举、图片工作区与导出面板。

### 3. 执行自动化检查

```bash
cd backend
./mvnw test

cd ../frontend
npm run build
```

前端构建命令会完成 TypeScript 校验与生产构建，结束后可删除 `frontend/dist/`（已在 `.gitignore` 中忽略）。

### 4. 体验批量导出

1. 在左侧图片工作区拖拽或选择图片；
2. 在右侧表单调整水印与导出配置；
3. 打开“批量导出”面板，点击“开始导出”；
4. 面板会展示任务进度、当前处理文件，可在执行中“取消当前任务”。

若未设置输出目录，导出结果默认写入仓库根目录的 `tar-photos/exports/<timestamp>`。可在 `backend/src/main/resources/application.properties` 调整 `app.storage.base-dir`。

**目录安全**：无论是否自定义目录，系统都会在目标目录下生成带时间戳的子目录，以避免覆盖原始图片。

## 支持的格式

- 输入：JPEG、PNG（含透明），并通过 TwelveMonkeys ImageIO 扩展支持 BMP、TIFF 等常见格式。
- 输出：PNG、JPEG（可配置压缩质量）。

## 接口概览

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/health` | 健康检查，返回服务状态与时间戳 |
| GET | `/api/fonts` | 列出系统可用字体（按名称排序） |
| GET | `/api/templates` | 获取已保存的水印模板列表 |
| GET | `/api/templates/{id}` | 读取单个模板详情 |
| POST | `/api/templates` | 新建或更新模板（依据 `id` 判断） |
| DELETE | `/api/templates/{id}` | 删除指定模板 |
| GET | `/api/settings/last` | 读取最近一次保存的全局设置（无内容返回 204） |
| POST | `/api/settings/last` | 保存最近一次使用的设置 |
| DELETE | `/api/settings/last` | 清除最近一次使用的设置 |
| POST | `/api/export` | 以 multipart 上传图片与配置，创建导出任务 |
| GET | `/api/export/{jobId}/status` | 查询指定任务的进度与结果 |
| POST | `/api/export/{jobId}/cancel` | 取消正在执行的导出任务 |

接口请求/响应主要采用 JSON（导出上传使用 multipart）。前端调用示例见 `frontend/src/services/api.ts` 和 `frontend/src/composables/useExportJobs.ts`。

## 数据存储

- 默认（仓库配置）：项目根目录下的 `tar-photos`
- 自定义：修改 `backend/src/main/resources/application.properties` 中的 `app.storage.base-dir`；若删除该配置会回退至 `%APPDATA%/PhotoWatermark`（或 `${user.home}/.photo-watermark`）

模板保存为 `templates/{templateId}.json`，最近一次设置保存为 `settings/last.json`。同样可借助 `app.storage.base-dir` 自定义存储目录。

## 后续规划

- 前端：在 Canvas 预览中叠加文本/图片水印，支持拖拽定位与导出预览对齐。
- 后端：扩展图片水印、导出失败重试、支持更多图片格式处理。
- 发布：使用 `jpackage` 生成自带运行时的 Windows 安装包，并在 GitHub Release 提供下载。

## 打包与发布

- Windows 可执行安装包：执行 `scripts\package-windows.ps1`（需 JDK 17+ 且包含 jpackage）。脚本会构建前后端并在 `build/windows` 目录生成安装文件。
- GitHub 地址 PDF：仓库 `docs/github-link.pdf` 已包含仓库链接，便于在作业中提交。
- 可根据需要将其他使用说明导出为 PDF（例如通过浏览器打印 README）。
