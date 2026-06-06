param(
    [ValidateSet("dev", "prod")]
    [string] $Flavor = "dev"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$LocalProperties = Join-Path $ProjectRoot "local.properties"
$ActivityName = "com.liftograph.app.MainActivity"

function Get-FlavorTitle([string] $Value) {
    return $Value.Substring(0, 1).ToUpperInvariant() + $Value.Substring(1)
}

function Read-AndroidSdkPath {
    if (Test-Path $LocalProperties) {
        $line = Get-Content $LocalProperties | Where-Object { $_ -match "^sdk\.dir=" } | Select-Object -First 1
        if ($line) {
            return ($line -replace "^sdk\.dir=", "" -replace "\\\\", "\" -replace "\\:", ":")
        }
    }

    if ($env:ANDROID_HOME) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { return $env:ANDROID_SDK_ROOT }
    return Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

function Assert-NativeSuccess([string] $Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Action failed with exit code $LASTEXITCODE."
    }
}

function Assert-AndroidDeviceConnected {
    $devices = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" }
    if (-not $devices) {
        throw "No connected Android device was found. Plug in the phone, unlock it, confirm USB debugging, then run this again."
    }
}

$AndroidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
if (-not (Get-Command java -ErrorAction SilentlyContinue) -and (Test-Path (Join-Path $AndroidStudioJbr "bin\java.exe"))) {
    $env:JAVA_HOME = $AndroidStudioJbr
    $env:PATH = "$AndroidStudioJbr\bin;$env:PATH"
}

$SdkPath = Read-AndroidSdkPath
$Adb = Join-Path $SdkPath "platform-tools\adb.exe"
$Gradle = Join-Path $ProjectRoot "gradlew.bat"
$FlavorTitle = Get-FlavorTitle $Flavor
$BuildTask = ":app:assemble${FlavorTitle}Debug"
$PackageName = if ($Flavor -eq "dev") { "com.liftograph.app.dev" } else { "com.liftograph.app" }
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\$Flavor\debug\app-$Flavor-debug.apk"

if (-not (Test-Path $Adb)) {
    throw "ADB was not found at $Adb. Open Android Studio SDK Manager and install Android SDK Platform-Tools."
}

Write-Host "Checking connected Android devices..."
& $Adb devices -l
Assert-NativeSuccess "ADB device check"
Assert-AndroidDeviceConnected

Write-Host "Building Lift-O-Graph $Flavor debug APK..."
& $Gradle -p $ProjectRoot $BuildTask
Assert-NativeSuccess "Gradle build"

Write-Host "Installing $Apk..."
& $Adb install -r $Apk
Assert-NativeSuccess "APK install"

Write-Host "Launching $PackageName..."
& $Adb shell am start -n "$PackageName/$ActivityName"
Assert-NativeSuccess "App launch"

Write-Host "Lift-O-Graph $Flavor launched on the connected device."
