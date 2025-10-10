param(
    [string]$AppVersion = '1.0.0',
    [string]$OutputDir = '../build/windows'
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
$buildOutput = Join-Path $repoRoot $OutputDir

Invoke-Step 'Install and build frontend' {
    Push-Location $frontend
    npm install --no-fund --no-audit | Out-Host
    npm run build | Out-Host
    Pop-Location
}

Invoke-Step 'Sync frontend dist into backend static resources' {
    if (Test-Path $staticDir) {
        Remove-Item $staticDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $staticDir | Out-Null
    Get-ChildItem (Join-Path $frontend 'dist') | Copy-Item -Destination $staticDir -Recurse -Force
}

Invoke-Step 'Build backend' {
    Push-Location $backend
    ./mvnw clean package -DskipTests | Out-Host
    Pop-Location
}

$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackage) {
    throw 'jpackage not found. Please use JDK 17+ with jpackage available.'
}

Invoke-Step 'Create Windows installer via jpackage' {
    $targetDir = Join-Path $backend 'target'
    $mainJar = 'backend-0.0.1-SNAPSHOT.jar'
    $mainJarPath = Join-Path $targetDir $mainJar
    if (-not (Test-Path $mainJarPath)) {
        throw "Could not find $mainJar. Make sure Maven build succeeded."
    }

    if (-not (Test-Path $buildOutput)) {
        New-Item -ItemType Directory -Path $buildOutput | Out-Null
    }

    $resolvedOutput = (Resolve-Path $buildOutput).Path

    $args = @(
        '--type', 'exe',
        '--name', 'PhotoWatermarkApp',
        '--app-version', $AppVersion,
        '--input', $targetDir,
        '--main-jar', $mainJar,
        '--dest', $resolvedOutput,
        '--win-shortcut',
        '--win-dir-chooser'
    )

    jpackage @args | Out-Host

    $script:latestInstaller = Join-Path $resolvedOutput "PhotoWatermarkApp-$AppVersion.exe"
}

Invoke-Step 'Copy installer to repository root' {
    if (-not (Test-Path $script:latestInstaller)) {
        throw "Installer not found: $script:latestInstaller"
    }

    $destination = Join-Path $repoRoot (Split-Path $script:latestInstaller -Leaf)
    Copy-Item $script:latestInstaller -Destination $destination -Force
    Write-Host "Installer copied to" $destination -ForegroundColor Green
}

Write-Host "Package complete. Output directory:" (Resolve-Path $buildOutput) -ForegroundColor Green
