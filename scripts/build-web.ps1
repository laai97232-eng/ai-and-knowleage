# 构建前端并把产物同步到后端静态资源目录，最终由 Spring Boot 一起托管。
# 用法：pwsh -File scripts/build-web.ps1
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'frontend'
$target = Join-Path $root 'backend\src\main\resources\static'

Write-Host "==> 构建前端 ($frontend)"
Push-Location $frontend
try {
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw "前端构建失败，退出码 $LASTEXITCODE" }
} finally {
    Pop-Location
}

$dist = Join-Path $frontend 'dist'
if (-not (Test-Path (Join-Path $dist 'index.html'))) { throw "没有找到构建产物：$dist" }

Write-Host "==> 同步到 $target"
if (Test-Path $target) { Remove-Item -Recurse -Force $target }
New-Item -ItemType Directory -Path $target -Force | Out-Null
Copy-Item -Path (Join-Path $dist '*') -Destination $target -Recurse -Force

$size = (Get-ChildItem -Recurse $target -File | Measure-Object Length -Sum).Sum / 1MB
Write-Host ("==> 完成，静态资源 {0:N2} MB" -f $size)
