# Windows Release 指南

本文档说明如何构建并验证 Photo Watermark App 的 Windows 安装包，以及如何在 GitHub Release 中发布。

## 1. 前置准备

1. 安装 JDK 17 及以上版本，并确保 `jpackage` 命令可用。
2. 安装 Node.js 20（含 npm），用于构建前端资源。
3. 安装或下载 WiX Toolset 3.14：
   - 建议在仓库根目录的 `tools/wix314` 下解压可执行二进制包，并把该目录加入当前 PowerShell 会话的 `PATH`；
   - 例如：
     ```powershell
     $env:WIX = "$(Resolve-Path tools/wix314)"
     $env:PATH = "$env:WIX;" + $env:PATH
     ```
4. （首次运行）在仓库根目录执行 `git pull`，保证代码为最新状态。

## 2. 构建安装包

```powershell
# 以 1.0.0 为例，可根据需要调整版本号
$env:WIX = "$(Resolve-Path tools/wix314)"
$env:PATH = "$env:WIX;" + $env:PATH

powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package-windows.ps1 -AppVersion '1.0.0'
```

脚本流程：

1. 安装并构建前端，产物输出到 `frontend/dist/`；
2. 将前端静态文件同步到 `backend/src/main/resources/static/`；
3. 构建 Spring Boot 后端 Jar；
4. 使用 `jpackage` + WiX 生成 Windows 安装包，默认输出至 `../build/windows/PhotoWatermarkApp-<版本号>.exe`。

> **注意**：`backend/src/main/resources/static/` 为临时构建目录，如无需保留，可在打包完成后删除。

## 3. 本地验收

1. 运行生成的 `PhotoWatermarkApp-<版本>.exe`，按照向导完成安装；
2. 从开始菜单或安装目录启动 `PhotoWatermarkApp`；
3. 在终端执行健康检查：
   ```powershell
   curl http://localhost:8080/api/health
   ```
   若返回 JSON（包含时间戳、状态），说明后端已成功启动；
4. 在浏览器访问 `http://localhost:8080`，确认前端页面可正常加载与操作；
5. 如需退出，关闭应用窗口或在任务栏图标上选择退出，Spring Boot 进程会自动终止。
6. 若需禁用自动打开浏览器，可在安装目录的 `PhotoWatermarkApp.cfg` 中添加 `-Dapp.desktop.auto-open=false` 后重启。

## 4. 发布到 GitHub Release

1. （可选）将安装包重命名为带版本号的文件，例如 `PhotoWatermarkApp-1.0.0-win-x64.exe`；
2. 创建标签并推送：
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. 在 GitHub 仓库页面创建新的 Release：
   - 选择上述标签；
   - 填写标题与更新说明（可参考 CHANGELOG 或本次提交记录）；
   - 上传安装包 exe 到 Release 资产（Assets）；
4. 发布 Release，确认下载链接可用。

## 5. 常见问题

- **WiX 未找到**：请确认 `candle.exe`、`light.exe` 所在目录已加入 `PATH`，或参考上方命令临时设置环境变量；
- **端口被占用**：若启动后提示 8080 被占用，可在重新运行前停止占用进程，或在安装目录下的 `PhotoWatermarkApp.cfg` 中添加 `-Dserver.port=<新端口>` 后重启；
- **静态资源未更新**：确保在运行 `scripts/package-windows.ps1` 前 `frontend` 构建成功，如遇缓存可加 `npm run build -- --force`。

执行以上步骤后，即可将稳定的 Windows 安装包上传到 GitHub 供助教下载、安装与验收。
