# Build JARs on the host (no Docker Hub maven pull), then start Compose.
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root
$mvnw = Join-Path $root "mvnw.cmd"

Write-Host "==> Packaging school-management..." -ForegroundColor Cyan
& $mvnw -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "school-management package failed" }

Write-Host "==> Packaging assessment-service..." -ForegroundColor Cyan
Push-Location (Join-Path $root "assessment-service")
& $mvnw -q -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "assessment-service package failed" }
Pop-Location

Write-Host "==> Packaging assignment-service..." -ForegroundColor Cyan
Push-Location (Join-Path $root "assignment-service")
& $mvnw -q -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "assignment-service package failed" }
Pop-Location

Write-Host "==> Docker Compose up --build..." -ForegroundColor Cyan
docker compose up --build -d
if ($LASTEXITCODE -ne 0) { throw "docker compose failed" }

Write-Host "Done. App: http://localhost:8080" -ForegroundColor Green
