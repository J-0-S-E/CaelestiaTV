# Install CaelestiaTV on an Android TV over adb: the launcher and theme, a debloat and lighter memory settings.
# Everything it changes is written to backup\<tv>.txt first, and restore.ps1 puts it back.
#
# Usage: .\install.ps1 <tv-ip>[:port] [-Apk path\to\CaelestiaTV.apk] [-List model-debloat.txt] [-NoDebloat] [-NoTweaks]
# Without an APK it uses CaelestiaTV.apk next to this script, or builds one from launcher\ (needs JDK 17 and the Android SDK).
param(
    [Parameter(Mandatory = $true, Position = 0)][string]$Tv,
    [string]$Apk = (Join-Path $PSScriptRoot 'CaelestiaTV.apk'),
    [string]$List,
    [switch]$NoDebloat,
    [switch]$NoTweaks
)
$ErrorActionPreference = 'Stop'
$pkg = 'com.gathin.tvhome'
$dir = "/sdcard/Android/data/$pkg/files"
if ($Tv -notmatch ':') { $Tv = "${Tv}:5555" }
function Sh([string]$cmd) { (adb -s $Tv shell $cmd) -join "`n" }

if (-not (Test-Path $Apk)) {
    Write-Host 'No APK found, building one from launcher\'
    Push-Location (Join-Path $PSScriptRoot 'launcher')
    .\gradlew.bat assembleRelease -q
    Pop-Location
    if ($LASTEXITCODE -ne 0) { throw 'build failed' }
    $Apk = Join-Path $PSScriptRoot 'launcher\app\build\outputs\apk\release\app-release.apk'
}

adb connect $Tv | Out-Host
if ((adb -s $Tv get-state 2>$null) -ne 'device') { throw "no adb connection to $Tv (allow the prompt on the TV, then run again)" }

# Backup: the first run records the TV as it was; a later run only adds what it changes on top.
$backupDir = Join-Path $PSScriptRoot 'backup'
New-Item -ItemType Directory -Force $backupDir | Out-Null
$backup = Join-Path $backupDir (($Tv -replace '[^A-Za-z0-9.]', '_') + '.txt')
$record = @(if (Test-Path $backup) { Get-Content $backup })
function Remember([string]$line) {
    $key = ($line -split ' ')[0..2] -join ' '
    if (-not ($script:record | Where-Object { $_.StartsWith($key + ' ') -or $_ -eq $key })) {
        $script:record += $line
        Set-Content -Path $backup -Value $script:record
    }
}
$oldHome = (Sh 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1').Trim()
if ($oldHome -match '/' -and $oldHome -notlike "$pkg/*") { Remember "home $oldHome" }

adb -s $Tv install -r $Apk
if ($LASTEXITCODE -ne 0) { throw 'install failed' }

# Theme: start the launcher once so Android makes its files folder, then put the theme and the schemes there.
Sh "am start -W -n $pkg/.MainActivity >/dev/null; mkdir -p $dir/schemes" | Out-Null
adb -s $Tv push (Join-Path $PSScriptRoot 'theme\config.json') "$dir/config.json" | Out-Null
Get-ChildItem (Join-Path $PSScriptRoot 'theme\schemes') -Filter *.txt | ForEach-Object {
    adb -s $Tv push $_.FullName "$dir/schemes/$($_.Name)" | Out-Null
}
adb -s $Tv push (Join-Path $PSScriptRoot 'theme\quotes.json') "$dir/quotes.json" | Out-Null
Sh "printf '%s' '$(Get-Date -Format yyyyMMddHHmmss)' > $dir/theme_revision; cmd package set-home-activity --user 0 $pkg/.MainActivity" | Out-Null

if (-not $NoTweaks) {
    # Snappier menus, and Android keeps 6 apps in the background instead of 32, so memory and zram stay free.
    foreach ($s in 'window_animation_scale', 'transition_animation_scale', 'animator_duration_scale') {
        Remember "setting global $s $((Sh "settings get global $s").Trim())"
        Sh "settings put global $s 0.5" | Out-Null
    }
    if ([int](Sh 'getprop ro.build.version.sdk') -ge 29) {
        Remember "device_config activity_manager max_cached_processes $((Sh 'device_config get activity_manager max_cached_processes').Trim())"
        Sh 'device_config put activity_manager max_cached_processes 6' | Out-Null
    }
    # The screensaver after 15 minutes without a key; Android TV starts it at screen_off_timeout.
    Remember "setting system screen_off_timeout $((Sh 'settings get system screen_off_timeout').Trim())"
    Sh 'settings put system screen_off_timeout 900000' | Out-Null
    Write-Host 'Tweaks: animations 0.5x, at most 6 background apps, screensaver after 15 min'
}

if (-not $NoDebloat) {
    $lists = @(Join-Path $PSScriptRoot 'debloat\common.txt') + @($List | Where-Object { $_ })
    $wanted = foreach ($l in $lists) {
        Get-Content $l | ForEach-Object { ($_ -replace '#.*$', '').Trim() } | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$' }
    }
    $wanted = @($wanted | Select-Object -Unique)
    # Never these: Android itself, the setup wizard (may own the saved Wi-Fi), apps that answer Home or open
    # Settings, and anything under the HDMI and tuner inputs (TV_INPUT_HARDWARE).
    $core = '^(android|com\.android\.(systemui|shell|phone|settings|tv\.settings|providers\..*)|com\.(google\.)?android\.(permissioncontroller|packageinstaller|ext\.services|ext\.shared|modulemetadata)|com\.google\.android\.overlay\.modules.*|com\.google\.android\.tungsten\.setupwraith)$'
    $enabled = (Sh 'pm list packages -e --user 0') -split "`n" | ForEach-Object { $_.Trim() -replace '^package:', '' }
    $protected = (Sh 'cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME; cmd package query-activities --brief -a android.settings.SETTINGS') -split "`n" |
        ForEach-Object { ($_.Trim() -split '/')[0] }
    $count = 0
    foreach ($p in $wanted) {
        if ($enabled -notcontains $p) { continue }
        if ($p -match $core -or $protected -contains $p) { Write-Host "  kept $p (the TV needs it)"; continue }
        if (Sh "dumpsys package $p | grep -q TV_INPUT_HARDWARE && echo hw") { Write-Host "  kept $p (drives an input)"; continue }
        Remember "disabled $p"
        Sh "pm disable-user --user 0 $p" | Out-Null
        $count++
    }
    Write-Host "Debloat: $count apps disabled"
}

Sh "am force-stop $pkg; am start -a android.intent.action.MAIN -c android.intent.category.HOME -n $pkg/.MainActivity" | Out-Null
Write-Host "Done. Backup of the old state: $backup"
Write-Host 'Turn Network debugging off on the TV when you are finished.'
