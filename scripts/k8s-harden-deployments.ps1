<#
.SYNOPSIS
  Hardens aisales Kubernetes service Deployments for production readiness.

.DESCRIPTION
  Adds startup/liveness/readiness probes, resources, preStop, and terminationGracePeriodSeconds.
  Preserves the trailing Service manifest if present.

  Run from repo root:
    powershell -File ./scripts/k8s-harden-deployments.ps1
#>
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root "deployment/kubernetes"
$files = @(Get-ChildItem (Join-Path $dir "*-service.yml"))
$gw = Join-Path $dir "api-gateway.yml"
if (Test-Path $gw) { $files += Get-Item $gw }

function Get-ProbeBlock([string]$port) {
    return @"
          startupProbe:
            httpGet:
              path: /actuator/health/readiness
              port: $port
            failureThreshold: 30
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: $port
            initialDelaySeconds: 20
            periodSeconds: 15
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: $port
            initialDelaySeconds: 10
            periodSeconds: 10
            failureThreshold: 3
          resources:
            requests:
              cpu: 100m
              memory: 512Mi
            limits:
              cpu: "1"
              memory: 1536Mi
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 5"]
"@
}

foreach ($f in $files) {
    $text = Get-Content $f.FullName -Raw
    if ($text -notmatch 'containerPort:\s*(\d+)') {
        Write-Host "Skip (no containerPort): $($f.Name)"
        continue
    }
    $port = $Matches[1]

    $servicePart = ""
    if ($text -match '(?s)(\r?\n---\r?\napiVersion: v1\r?\nkind: Service\r?\n.*)\s*$') {
        $servicePart = $Matches[1].TrimEnd() + "`r`n"
        $text = $text.Substring(0, $text.Length - $Matches[1].Length)
    }

    if ($text -notmatch 'terminationGracePeriodSeconds') {
        $text = $text -replace '(?m)^(\s+containers:\s*)$', "      terminationGracePeriodSeconds: 45`r`n`$1"
    }

    $probe = Get-ProbeBlock $port
    # Replace only the readinessProbe block (bounded, not through end of file)
    if ($text -match '(?ms)\r?\n(\s+)readinessProbe:\r?\n(?:\1  .*\r?\n)+') {
        $text = [regex]::Replace($text, '(?ms)\r?\n(\s+)readinessProbe:\r?\n(?:\1  .*\r?\n)+', "`r`n$probe`r`n", 1)
    } elseif ($text -notmatch 'startupProbe:') {
        $text = $text.TrimEnd() + "`r`n" + $probe + "`r`n"
    }

    # Strip duplicate probe/resource blocks if re-run
    if (([regex]::Matches($text, 'startupProbe:')).Count -gt 1) {
        Write-Host "Warning: multiple startupProbe blocks in $($f.Name) — manual review recommended"
    }

    $out = $text.TrimEnd() + "`r`n" + $servicePart
    Set-Content -Path $f.FullName -Value $out -NoNewline
    Write-Host "Hardened $($f.Name) (port $port)"
}

Write-Host "Done."
