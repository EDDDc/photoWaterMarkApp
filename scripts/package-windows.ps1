param(
    [string]$AppVersion = "1.0.0",
    [string]$OutputDir = "../build/windows"
)

$ErrorActionPreference = 'Stop'

function Invoke-Step {
    param(
        [string]$Title,
        [scriptblock]$Action
    )
    Write-Host "[package] $Title" -ForegroundColor Cyan
    & $Action
}

$repoRoot = Resolve-Path "$PSScriptRoot/.."
$frontend = Join-Path $repoRoot 'frontend'
$backend = Join-Path $repoRoot 'backend'
$staticDir = Join-Path $backend 'src/main/resources/static'
$buildOutput = Resolve-Path (Join-Path $repoRoot $OutputDir)

Invoke-Step "Install and build frontend" {
    Push-Location $frontend
    npm install --no-fund --no-audit | Out-Host
    npm run build | Out-Host
    Pop-Location
}

Invoke-Step "Sync frontend dist into backend static resources" {
    if (Test-Path $staticDir) {
        Remove-Item $staticDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $staticDir | Out-Null
    Get-ChildItem (Join-Path $frontend 'dist') | Copy-Item -Destination $staticDir -Recurse -Force
}

Invoke-Step "Build backend" {
    Push-Location $backend
    ./mvnw clean package -DskipTests | Out-Host
    Pop-Location
}

$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackage) {
    throw "未找到 jpackage，请确认正在使用包含 jpackage 的 JDK 17+。"
}

Invoke-Step "Create Windows installer via jpackage" {
    $targetDir = Join-Path $backend 'target'
    $mainJar = 'backend-0.0.1-SNAPSHOT.jar'
    $mainJarPath = Join-Path $targetDir $mainJar
    if (-not (Test-Path $mainJarPath)) {
        throw "未找到 $mainJar，请确认 Maven 构建成功。"
    }

    if (-not (Test-Path $buildOutput)) {
        New-Item -ItemType Directory -Path $buildOutput | Out-Null
    }

    $args = @(
        '--type', 'exe',
        '--name', 'PhotoWatermarkApp',
        '--app-version', $AppVersion,
        '--input', $targetDir,
        '--main-jar', $mainJar,
        '--main-class', 'com.photowatermarkapp.BackendApplication',
        '--dest', (Resolve-Path $buildOutput),
        '--win-shortcut',
        '--win-dir-chooser'
    )

    jpackage @args | Out-Host
}

Write-Host "打包完成，输出目录：" (Resolve-Path $buildOutput) -ForegroundColor Green
