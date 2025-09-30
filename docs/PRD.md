# 项目 PRD：本地图片批量加水印应用（Windows）

版本：v0.1（草案）  
技术栈：Spring Boot + Vue 3（Vite）  
目标系统：Windows 10/11（离线可用）

## 1. 背景与目标
- 面向本地图片批量加水印的轻量工具，保障导入、预览、导出流程顺畅，满足课程作业提交要求。
- 必备：文本水印（透明度、位置/拖拽、实时预览）、批量处理、输出 JPEG/PNG、模板保存与自动加载。
- 建议升级：图片水印、字体/颜色选择、描边/阴影、旋转、JPEG 质量、导出尺寸调整、更多格式（BMP/TIFF）。

## 2. 范围
- 在同一台 Windows 电脑本地运行（无外网依赖），前端浏览器访问本地后端。
- 仅处理静态位图图片；不涉及视频/动图；不做水印去除。
- 单用户，无登录与权限系统。

不做清单（v1）：
- 瓦片平铺水印、渐变文本、复杂滤镜、多语言、云同步、团队协作、跨平台安装包（仅 Windows）。

## 3. 用户与场景
- 学生/助教：批量给作业图片加“署名/水印”；需要快速、可视化预览与批量导出。
- 自媒体运营：给图片统一加 Logo 或账号标识。

关键场景：
- 导入多张图片或整个文件夹 → 调整水印 → 预览多图效果 → 选择输出目录与命名规则 → 一键导出。

## 4. 术语
- 预设位置：九宫格定位（四角、四边中点、中心）。
- 模板：水印全量设置的命名快照，可复用。

## 5. 功能需求（Must/Should）
### 5.1 文件处理
- 导入图片（Must）
  - 支持拖拽单张/多张、文件选择器导入。
  - 支持批量导入与文件夹导入（webkitdirectory）。
  - 在界面显示缩略图+文件名，可切换预览不同图片。
- 支持格式（Must/Should）
  - 输入（Must）：JPEG、PNG；PNG 需支持透明通道。
  - 输入（Should）：BMP、TIFF（建议支持）。
  - 输出（Must）：JPEG 或 PNG 可选。
- 导出（Must/Should）
  - 选择输出文件夹；默认禁止导出到原文件夹（防覆盖）。
  - 命名规则：保留原名 / 自定义前缀 / 自定义后缀。
  - JPEG 质量（0-100）（Should）。
  - 导出尺寸调整：按宽/高/百分比缩放（Should）。

### 5.2 水印类型
- 文本水印（Must）
  - 任意文本输入；透明度 0-100%。
  - 字体族/字号/粗体/斜体（Should）。
  - 颜色拾取（Should）。
  - 描边/阴影（Should）。
- 图片水印（Should）
  - 选择本地图片（建议 PNG 带透明通道）。
  - 支持按比例/自由缩放；透明度 0-100%。

### 5.3 布局与样式
- 实时预览（Must）：所有调整在主预览区即时呈现；可点击列表切换预览对象。
- 位置（Must）：
  - 预设九宫格一键定位。
  - 预览区内拖拽到任意位置；显示对齐/安全边距参考线（Should）。
- 旋转（Should）：滑块或数值输入，任意角度。

### 5.4 配置管理
- 模板（Must）：保存/加载/删除模板；启动时自动加载“上次设置”或默认模板。

## 6. 非功能需求
- 性能：
  - 典型机器（i5/8GB）对 24MP JPG 50 张，批量导出 < 120s；单张预览调整 < 50ms 响应。
  - 并行处理（CPU 核心数）+ 有序限流，避免内存暴涨。
- 兼容：Windows 10/11；推荐 Chrome/Edge 最新版。
- 离线：不依赖网络。图片处理全在本地完成。
- 可靠性：导出失败有错误列表与可重试；崩溃重启可恢复“上次设置”。
- 可安装：提供 Windows 可执行文件（release），内置运行环境；双击运行并自动打开浏览器。
- 记录：日志写入本地文件；导出报告（成功/失败清单）。
- 隐私：不上传图片；仅在本地硬盘读写。

## 7. 交互与界面（草图描述）
- 顶部：项目标题、主题切换、版本。
- 左侧：已导入图片列表（缩略图 + 文件名 + 选中状态）。
- 中央：预览画布（支持缩放/拖拽、对齐线）。
- 右侧：水印面板
  - 类型：文本/图片；文本内容；字体、字号、粗斜体、颜色；透明度；描边/阴影；旋转；缩放。
  - 位置：九宫格按钮；X/Y 偏移（相对/像素）；锁定比例、对齐到边缘。
- 底部导出条：
  - 输出格式（JPEG/PNG）、JPEG 质量、尺寸调整。
  - 输出目录选择（文本框 + 校验 + 打开资源管理器按钮）。
  - 命名规则（保留/前缀/后缀）。
  - 开始导出/停止；进度与剩余时间估计。
- 模板管理对话框：保存为模板、重命名、设为默认、删除。

## 8. 系统设计（Spring Boot + Vue3）
- 前端（Vue 3 + Vite）：
  - 采用 Canvas 实时渲染预览（WYSIWYG）；目录导入基于 `input[webkitdirectory]`；
  - 大图仅在导出时以流式方式上传到本地后端，避免重复传输；
  - 本地缓存最近一次设置（localStorage）；
  - 进度轮询/事件源显示导出进度。
- 后端（Spring Boot）：
  - 提供系统字体枚举；接收多文件与水印配置；服务器端 Java2D 生成成品图；
  - 输出到指定目录（校验不可为源目录）；并发处理 + 限流；
  - 持久化：`%APPDATA%/PhotoWatermark/{config.json, templates/*.json, logs/}`。
  - 可选依赖：
    - 图片处理：Java2D + ImageIO；缩放优选 Thumbnailator；
    - 格式扩展：TwelveMonkeys ImageIO（BMP/TIFF）。
- 打包发布：
  - 前端打包产物内嵌到后端 `resources/static`；
  - 使用 JDK `jpackage` 生成 Windows x64 可执行（自带运行时）；
  - 启动后自动打开浏览器 `http://localhost:8080`；
  - GitHub Release 提供安装包/绿色版 zip。

架构图（文字）：
- Vue3 SPA（浏览器） ⇄ REST API（localhost:8080，Spring Boot） → 文件系统（读取上传临时文件、写入导出目录）

## 9. API 设计（草案）
- GET /api/health → { status }
- GET /api/fonts → [ "Microsoft YaHei", "SimSun", ... ]
- POST /api/export (multipart/form-data)
  - form：`config`（JSON 字符串，含水印与导出配置）、`files[]`（多图片）
  - 返回：{ jobId }
- GET /api/export/{jobId}/status → { total, done, failed, progress:0-1, currentFile, eta }
- POST /api/export/{jobId}/cancel → { success }
- GET /api/templates → 列表
- POST /api/templates → 创建/更新
- DELETE /api/templates/{id} → 删除
- GET /api/settings/last → 读取上次设置
- POST /api/settings/last → 保存上次设置

主要模型（简化）：
- WatermarkConfig
  - type: "text" | "image"
  - text: { content, fontFamily, fontSize, bold, italic, color, opacity, stroke?, shadow? }
  - image: { name, mime, data(base64 或后端缓存键), scale, opacity }
  - layout: { preset?: "top-left"|...|"center", x:0-1, y:0-1, rotationDeg }
- ExportConfig
  - outputDir, format:"jpeg"|"png", jpegQuality:0-100, resize?: { mode:"w"|"h"|"pct", value:number }
  - naming: { keepOriginal:boolean, prefix?:string, suffix?:string }
- Template { id, name, watermarkConfig, exportConfig, updatedAt }

## 10. 处理与渲染策略
- 预览：前端 Canvas 先按容器缩放原图，再按配置绘制文本/图片水印（含透明度、描边/阴影、旋转）。
- 导出：后端读取上传的原图，基于同一套参数进行高质量渲染：
  - 文本：Java2D `Graphics2D`，抗锯齿、AlphaComposite、AffineTransform 旋转；
  - 图片水印：按比例缩放后叠加；
  - PNG 透明度保留；JPEG 受质量参数控制；
  - 统一坐标系（相对宽高 0..1）确保预览与导出一致。
- EXIF 方向：导出前归一化处理。

## 11. 边界与错误处理
- 非法输出目录（为空/不存在/无权限/等于源目录）→ 阻断导出并提示。
- 重名冲突：按命名规则生成；若仍冲突，自动追加 `-1`, `-2`。
- 不支持格式/损坏图片 → 忽略并在结果中列出错误。
- 极端大图内存保护：启用分块/下采样策略或提示内存不足。
- 非拉丁字符文件名与路径：完整保留（UTF-8）。

## 12. 验收标准（对应需求）
- 能导入单张/多张/文件夹；缩略图与文件名展示正确。
- 文本水印透明度可调；九宫格定位与拖拽准确；预览所见即所得。
- 导出 JPEG/PNG 可选；默认不允许选为源目录；命名规则生效。
- 模板保存/加载/删除可用；应用启动自动加载上次设置。
- 可选功能（若实现）：字体/颜色、描边/阴影、旋转、图片水印、JPEG 质量、尺寸调整、BMP/TIFF 输入。

## 13. 里程碑计划
- M1（核心功能，1 周）：
  - 基础 UI + 预览、文本水印（透明度/位置/拖拽）、导入/列表、导出 JPEG/PNG、输出目录校验、命名规则、模板（保存/自动加载）。
- M2（增强，1 周）：
  - 字体/颜色、描边/阴影、旋转、导出进度、日志；Windows 打包（jpackage）与 Release。
- M3（可选，1 周）：
  - 图片水印、JPEG 质量、尺寸调整、BMP/TIFF 支持。

## 14. 发布与提交要求
- GitHub 私/公开仓库，按 Conventional Commits 编写提交信息。
- Release 提供：
  - Windows 安装包（含运行时）与绿色版；
  - 使用说明 README；
  - 可直接运行并完成样例批量导出验证。
- 提交一个 PDF，含 GitHub 仓库地址与版本说明。

## 15. 风险与权衡
- 浏览器目录选择的限制：优先使用 Chromium `showDirectoryPicker`/`webkitdirectory`；非 Chromium 下提供手输路径方案（后端校验并打开资源管理器）。
- 字体一致性：预览与导出可能存在字体差异；后端导出以系统字体枚举为准，并在前端提示“未安装字体时可能替换”。
- TIFF 支持需额外解码库（TwelveMonkeys）；若发布尺寸受限，可作为可选模块。

## 16. 运行环境与依赖
- JDK 17、Spring Boot 3.x；
- Vue 3 + Vite；UI 组件库（Element Plus 或 Ant Design Vue 二选一）；
- 图片处理：Java2D、Thumbnailator（可选）、TwelveMonkeys（可选）。

