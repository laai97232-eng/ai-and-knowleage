# 一条命令完成“构建前端 + 提交推送”，推送后 Render 会自动重新部署。
# 用法：powershell -ExecutionPolicy Bypass -File scripts\deploy.ps1 -Message "更新说明"
param(
    [string]$Message = "更新部署"
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot 'build-web.ps1')
if ($LASTEXITCODE -ne 0) { throw "前端构建失败" }

Push-Location $root
try {
    git add -A
    if (git diff --cached --quiet) {
        Write-Host '==> 没有需要提交的改动'
    } else {
        git commit -m $Message
        git push
        Write-Host '==> 已推送，Render 会自动重新部署'
    }
} finally {
    Pop-Location
}
