param(
    [ValidateSet("dev", "prod")]
    [string] $Flavor = "prod"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$AndroidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"

function Get-FlavorTitle([string] $Value) {
    return $Value.Substring(0, 1).ToUpperInvariant() + $Value.Substring(1)
}

if (-not (Get-Command java -ErrorAction SilentlyContinue) -and (Test-Path (Join-Path $AndroidStudioJbr "bin\java.exe"))) {
    $env:JAVA_HOME = $AndroidStudioJbr
    $env:PATH = "$AndroidStudioJbr\bin;$env:PATH"
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java was not found. Install Android Studio or a JDK, then run this script again."
}

$Gradle = Join-Path $ProjectRoot "gradlew.bat"
$FlavorTitle = Get-FlavorTitle $Flavor
$BuildTask = ":app:assemble${FlavorTitle}Debug"
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\$Flavor\debug\app-$Flavor-debug.apk"
$OutputDir = Join-Path $ProjectRoot "output\apks"
$OutputApk = Join-Path $OutputDir "lift-o-graph-$Flavor-debug.apk"

Write-Host "Building Lift-O-Graph $Flavor debug APK..."
& $Gradle -p $ProjectRoot $BuildTask

if (Test-Path $Apk) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    Copy-Item -LiteralPath $Apk -Destination $OutputApk -Force
    Write-Host "APK ready: $Apk"
    Write-Host "Copied shareable APK to: $OutputApk"
}
