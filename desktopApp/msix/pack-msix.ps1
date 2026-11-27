# Builds installer/KoMD-<version>.msix for the Microsoft Store from the jpackage app image.
# Run after `gradlew :desktopApp:createDistributable` on Windows (needs makeappx.exe from the Windows SDK).
# Identity values come from Partner Center > the app > Product identity:
#   MSIX_IDENTITY_NAME       Package/Identity/Name           e.g. 12345GiuseppeSorce.KoMD
#   MSIX_PUBLISHER           Package/Identity/Publisher      e.g. CN=1A2B3C4D-....
#   MSIX_PUBLISHER_DISPLAY_NAME  Package/Properties/PublisherDisplayName
$ErrorActionPreference = 'Stop'

$repo   = Resolve-Path "$PSScriptRoot\..\.."
$appDir = Join-Path $repo 'desktopApp\build\compose\binaries\main\app\KoMD'
if (-not (Test-Path (Join-Path $appDir 'KoMD.exe'))) { throw "App image not found in ${appDir}: run createDistributable first." }

foreach ($name in 'MSIX_IDENTITY_NAME', 'MSIX_PUBLISHER', 'MSIX_PUBLISHER_DISPLAY_NAME') {
    if (-not [Environment]::GetEnvironmentVariable($name)) { throw "$name is not set (see the header of this script)." }
}

# packageVersion "1.0.0" -> MSIX "1.0.0.0" (the Store requires the 4th number to be 0).
$gradle = Get-Content (Join-Path $repo 'desktopApp\build.gradle.kts') -Raw
if ($gradle -notmatch 'packageVersion\s*=\s*"(\d+)\.(\d+)\.(\d+)"') { throw 'packageVersion not found in desktopApp/build.gradle.kts' }
$version = "$($Matches[1]).$($Matches[2]).$($Matches[3]).0"
$short   = "$($Matches[1]).$($Matches[2]).$($Matches[3])"

$stage = Join-Path $repo 'desktopApp\build\msix\root'
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item "$appDir\*" $stage -Recurse
Copy-Item (Join-Path $PSScriptRoot 'Assets') $stage -Recurse

$manifest = Get-Content (Join-Path $PSScriptRoot 'AppxManifest.template.xml') -Raw
$manifest = $manifest.Replace('{{IDENTITY_NAME}}', $env:MSIX_IDENTITY_NAME).
                      Replace('{{PUBLISHER}}', [Security.SecurityElement]::Escape($env:MSIX_PUBLISHER)).
                      Replace('{{PUBLISHER_DISPLAY_NAME}}', [Security.SecurityElement]::Escape($env:MSIX_PUBLISHER_DISPLAY_NAME)).
                      Replace('{{VERSION}}', $version)
Set-Content -Path (Join-Path $stage 'AppxManifest.xml') -Value $manifest -Encoding UTF8

$makeappx = Get-ChildItem "${env:ProgramFiles(x86)}\Windows Kits\10\bin\*\x64\makeappx.exe" |
    Sort-Object { [version]$_.Directory.Parent.Name } | Select-Object -Last 1
if (-not $makeappx) { throw 'makeappx.exe not found: install the Windows 10/11 SDK.' }

$outDir = Join-Path $repo 'installer'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$out = Join-Path $outDir "KoMD-$short.msix"
& $makeappx.FullName pack /o /d $stage /p $out
if ($LASTEXITCODE -ne 0) { throw "makeappx failed ($LASTEXITCODE)" }
Write-Host "MSIX: $out"
