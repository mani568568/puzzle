# Run this from the app module (or double-click in PowerShell) after overlaying an upgrade.
# It removes files from older MPB versions that are intentionally absent from the current single-activity architecture.

$moduleRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$staleFiles = @(
    "src\\main\\java\\com\\hb\\puzz\\GameActivity.kt",
    "src\\main\\res\\layout\\activity_main.xml"
)

foreach ($relativePath in $staleFiles) {
    $fullPath = Join-Path $moduleRoot $relativePath
    if (Test-Path $fullPath) {
        Remove-Item $fullPath -Force
        Write-Host "Removed stale file: $relativePath"
    }
}

Write-Host "Stale-file cleanup complete."
