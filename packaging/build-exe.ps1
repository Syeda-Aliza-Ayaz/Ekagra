# Builds dist\Ekagra\Ekagra.exe: a double-clickable app with its own Java runtime inside,
# so the lab PC doesn't need Java installed.
#
#   powershell -ExecutionPolicy Bypass -File packaging\build-exe.ps1
#
# Requires JDK 21 (for jpackage) and either Maven on PATH or a jar already built in target\.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" | Select-Object -First 1
    $env:JAVA_HOME = $jdk.FullName
}
$jpackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"

# 1. build the runnable jar (skipped if Maven isn't available and the jar already exists)
$mvn = $null
$onPath = Get-Command mvn -ErrorAction SilentlyContinue
if ($onPath) { $mvn = $onPath.Source }
$local = Join-Path $env:USERPROFILE "tools\apache-maven-3.9.9\bin\mvn.cmd"
if (-not $mvn -and (Test-Path $local)) { $mvn = $local }
if ($mvn) { & $mvn -q -B package -DskipTests }
$jar = "target\ekagra-1.0.0.jar"
if (-not (Test-Path $jar)) { throw "Jar not found: build it first (mvn package)" }

# 2. icon
if (-not (Test-Path "packaging\ekagra.ico")) { & (Join-Path $PSScriptRoot "make-icon.ps1") }

# 3. jpackage needs a folder that contains only the jar
$pkgDir = "target\package"
if (Test-Path $pkgDir) { Remove-Item $pkgDir -Recurse -Force }
New-Item -ItemType Directory $pkgDir | Out-Null
Copy-Item $jar $pkgDir

if (Test-Path "dist\Ekagra") { Remove-Item "dist\Ekagra" -Recurse -Force }
& $jpackage --type app-image --name Ekagra --app-version 1.0.0 --vendor "CT-263 OOAD Group" `
    --description "Student Productivity and Digital Wellbeing Monitoring System" `
    --input $pkgDir --main-jar ekagra-1.0.0.jar --main-class com.ekagra.app.Launcher `
    --icon packaging\ekagra.ico --java-options "-Dfile.encoding=UTF-8" --dest dist

Write-Output "Built dist\Ekagra\Ekagra.exe"
