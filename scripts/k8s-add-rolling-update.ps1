<#
.SYNOPSIS
  Adds RollingUpdate strategy to aisales Deployments (idempotent).
#>
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root "deployment/kubernetes"
$files = Get-ChildItem (Join-Path $dir "*.yml") | Where-Object {
    $_.Name -notin @("namespace.yml", "configmap.yml", "secrets.yml", "ingress.yml", "redis.yml")
}

$strategy = @"
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
"@

foreach ($f in $files) {
    $text = Get-Content $f.FullName -Raw
    if ($text -notmatch 'kind:\s*Deployment') { continue }
    if ($text -match '(?m)^\s+strategy:\s*$') {
        Write-Host "Skip (strategy present): $($f.Name)"
        continue
    }
    if ($text -match '(?m)^(\s+replicas:\s*\d+\s*)$') {
        $text = [regex]::Replace($text, '(?m)^(\s+replicas:\s*\d+\s*)$', "`$1`r`n$strategy", 1)
        Set-Content -Path $f.FullName -Value $text -NoNewline
        Write-Host "Updated $($f.Name)"
    } else {
        Write-Host "Skip (no replicas): $($f.Name)"
    }
}
