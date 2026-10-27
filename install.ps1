# Install CaelestiaTV on an Android TV over adb and make it the home screen.
# Usage: .\install.ps1 <tv-ip>[:port] [path\to\CaelestiaTV.apk]
# Without an APK it uses CaelestiaTV.apk next to this script, or builds one from launcher/ (needs JDK 17 and the Android SDK).
param(
    [Parameter(Mandatory = $true, Position = 0)][string]$Tv,
    [Parameter(Position = 1)][string]$Apk = (Join-Path $PSScriptRoot 'CaelestiaTV.apk')
)
$ErrorActionPreference = 'Stop'
$pkg = 'com.gathin.tvhome'
$dir = "/sdcard/Android/data/$pkg/files"
if ($Tv -notmatch ':') { $Tv = "${Tv}:5555" }

if (-not (Test-Path $Apk)) {
    Write-Host 'No APK found, building one from launcher/'
    Push-Location (Join-Path $PSScriptRoot 'launcher')
    .\gradlew.bat assembleRelease -q
    Pop-Location
    if ($LASTEXITCODE -ne 0) { throw 'build failed' }
    $Apk = Join-Path $PSScriptRoot 'launcher\app\build\outputs\apk\release\app-release.apk'
}

adb connect $Tv | Out-Host
adb -s $Tv install -r $Apk
if ($LASTEXITCODE -ne 0) { throw 'install failed' }

# Start it once so Android makes its files folder, then put the theme and the schemes there.
adb -s $Tv shell "am start -W -n $pkg/.MainActivity >/dev/null; mkdir -p $dir/schemes"
adb -s $Tv push (Join-Path $PSScriptRoot 'theme\config.json') "$dir/config.json" | Out-Null
Get-ChildItem (Join-Path $PSScriptRoot 'theme\schemes') -Filter *.txt | ForEach-Object {
    adb -s $Tv push $_.FullName "$dir/schemes/$($_.Name)" | Out-Null
}
$rev = Get-Date -Format 'yyyyMMddHHmmss'
adb -s $Tv shell "printf '%s' '$rev' > $dir/theme_revision; cmd package set-home-activity --user 0 $pkg/.MainActivity; am force-stop $pkg; am start -a android.intent.action.MAIN -c android.intent.category.HOME -n $pkg/.MainActivity" | Out-Null
Write-Host 'Done. Press Home on the remote.'
