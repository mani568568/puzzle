@echo off
setlocal EnableExtensions

set "ART_DIR=%~dp0src\main\res\drawable-nodpi"

if not exist "%ART_DIR%" (
    echo.
    echo ERROR: drawable-nodpi folder was not found:
    echo %ART_DIR%
    exit /b 1
)

echo.
echo ============================================================
echo SnapFrame level artwork normalizer
echo ============================================================
echo Folder: %ART_DIR%
echo.

rem Use PowerShell inline rather than a .ps1 file. This is not blocked by the
rem local PowerShell script execution policy and is substantially safer than
rem CMD parsing for case-only renames and zero-padded numeric names.

powershell.exe -NoLogo -NoProfile -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$dir = $env:ART_DIR;" ^
  "$supported = @('.jpeg','.jpg','.png','.webp');" ^
  "$all = @(Get-ChildItem -LiteralPath $dir -File);" ^
  "$images = @($all | Where-Object { $supported -contains $_.Extension.ToLowerInvariant() });" ^
  "Write-Host ('Files in folder: ' + $all.Count);" ^
  "Write-Host ('Supported image files: ' + $images.Count);" ^
  "$renamed = 0; $matched = 0; $ignored = New-Object System.Collections.Generic.List[string];" ^
  "foreach ($f in $images) {" ^
  "  if ($f.BaseName -match '^(?i:level)[ _-]?0*([0-9]{1,3})$') {" ^
  "    $id = [int]$Matches[1];" ^
  "    if ($id -ge 1 -and $id -le 999) {" ^
  "      $matched++;" ^
  "      $ext = $f.Extension.ToLowerInvariant();" ^
  "      $targetName = ('level_{0:D3}{1}' -f $id, $ext);" ^
  "      $target = Join-Path $dir $targetName;" ^
  "      if ($f.Name -cne $targetName) {" ^
  "        if ((Test-Path -LiteralPath $target) -and ($f.FullName -ne $target)) {" ^
  "          Write-Host ('DUPLICATE: ' + $f.Name + ' conflicts with ' + $targetName) -ForegroundColor Yellow;" ^
  "        } else {" ^
  "          $tmpName = ('__snapframe_tmp_{0}_{1}{2}' -f $id, [guid]::NewGuid().ToString('N'), $ext);" ^
  "          $tmp = Join-Path $dir $tmpName;" ^
  "          Move-Item -LiteralPath $f.FullName -Destination $tmp;" ^
  "          Move-Item -LiteralPath $tmp -Destination $target;" ^
  "          $renamed++;" ^
  "        }" ^
  "      }" ^
  "    } else { [void]$ignored.Add($f.Name) }" ^
  "  } else { [void]$ignored.Add($f.Name) }" ^
  "}" ^
  "$after = @(Get-ChildItem -LiteralPath $dir -File | Where-Object { $supported -contains $_.Extension.ToLowerInvariant() });" ^
  "$ids = New-Object 'System.Collections.Generic.HashSet[int]';" ^
  "foreach ($f in $after) { if ($f.BaseName -match '^level_([0-9]{3})$') { $id=[int]$Matches[1]; if ($id -ge 1 -and $id -le 999) { [void]$ids.Add($id) } } }" ^
  "$missing = @(1..999 | Where-Object { -not $ids.Contains($_) });" ^
  "Write-Host '';" ^
  "Write-Host ('Matched level artwork before normalization: ' + $matched);" ^
  "Write-Host ('Renamed: ' + $renamed);" ^
  "Write-Host ('Valid level resources now: ' + $ids.Count);" ^
  "Write-Host ('Missing level resources: ' + $missing.Count);" ^
  "if ($missing.Count -gt 0) {" ^
  "  $preview = @($missing | Select-Object -First 25 | ForEach-Object { 'level_{0:D3}' -f $_ });" ^
  "  Write-Host ('First missing IDs: ' + ($preview -join ', ')) -ForegroundColor Yellow;" ^
  "  if ($missing.Count -gt 25) { Write-Host ('... plus ' + ($missing.Count - 25) + ' more.') -ForegroundColor Yellow }" ^
  "}" ^
  "if ($ignored.Count -gt 0) {" ^
  "  Write-Host '';" ^
  "  Write-Host ('Image files that did not match Level_NNN / level_NNN: ' + $ignored.Count) -ForegroundColor Yellow;" ^
  "  $ignored | Select-Object -First 15 | ForEach-Object { Write-Host ('  ' + $_) };" ^
  "  if ($ignored.Count -gt 15) { Write-Host ('  ... plus ' + ($ignored.Count - 15) + ' more.') }" ^
  "}" ^
  "Write-Host '';" ^
  "if ($ids.Count -eq 999) { Write-Host 'SUCCESS: level_001 through level_999 are available.' -ForegroundColor Green } else { Write-Host 'Normalization completed. Missing files are reported as a warning, not a script failure.' -ForegroundColor Yellow }"

if errorlevel 1 (
    echo.
    echo ERROR: Artwork normalization encountered an unexpected error.
    exit /b 1
)

echo.
echo Done.
exit /b 0
