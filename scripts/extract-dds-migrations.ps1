# Extract Flyway SQL blocks from DDS.html into database/migrations-monolith/
param(
    [string]$SourceHtml = ".cursor/knowledge/Documents/DDS.html",
    [string]$OutputDir = "backend/database/migrations-monolith"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$htmlPath = Join-Path $root $SourceHtml
$outPath = Join-Path $root $OutputDir

if (-not (Test-Path $htmlPath)) {
    throw "DDS.html not found: $htmlPath"
}

New-Item -ItemType Directory -Force -Path $outPath | Out-Null

$content = Get-Content -Path $htmlPath -Raw -Encoding UTF8

# Match <h3>V###__name.sql</h3> followed by first <pre>...</pre> in sql-block
$pattern = '(?s)<h3>(V\d{3}__[^<]+\.sql)</h3>.*?<pre>(.*?)</pre>'
$matches = [regex]::Matches($content, $pattern)

if ($matches.Count -eq 0) {
    throw "No migration blocks found in DDS.html"
}

$extracted = @()
foreach ($m in $matches) {
    $filename = $m.Groups[1].Value.Trim()
    $sql = $m.Groups[2].Value.Trim()
    $target = Join-Path $outPath $filename
    [System.IO.File]::WriteAllText($target, $sql + "`n", [System.Text.UTF8Encoding]::new($false))
    $extracted += $filename
    Write-Host "Extracted: $filename ($($sql.Length) chars)"
}

Write-Host ""
Write-Host "Extracted $($extracted.Count) migration(s) to $outPath"
Write-Host "Files: $($extracted -join ', ')"
