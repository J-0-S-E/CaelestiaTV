# Put the TV back the way install.ps1 found it, from backup\<tv>.txt: the apps it disabled, the animation and
# background-app settings, and the old home screen. CaelestiaTV stays installed; uninstall it in the TV's settings.
#
# Usage: .\restore.ps1 <tv-ip>[:port]
param([Parameter(Mandatory = $true, Position = 0)][string]$Tv)
$ErrorActionPreference = 'Stop'
if ($Tv -notmatch ':') { $Tv = "${Tv}:5555" }
$backup = Join-Path $PSScriptRoot ('backup\' + ($Tv -replace '[^A-Za-z0-9.]', '_') + '.txt')
if (-not (Test-Path $backup)) { throw "no backup for $Tv ($backup)" }
function Sh([string]$cmd) { (adb -s $Tv shell $cmd) -join "`n" }

adb connect $Tv | Out-Host
if ((adb -s $Tv get-state 2>$null) -ne 'device') { throw "no adb connection to $Tv" }

foreach ($line in Get-Content $backup) {
    $f = $line -split ' '
    switch ($f[0]) {
        'disabled' { Sh "pm enable --user 0 $($f[1])" | Out-Null; Write-Host "enabled $($f[1])" }
        'setting' {
            if ($f[3] -eq 'null' -or -not $f[3]) { Sh "settings delete $($f[1]) $($f[2])" | Out-Null }
            else { Sh "settings put $($f[1]) $($f[2]) $($f[3])" | Out-Null }
        }
        'device_config' {
            if ($f[3] -eq 'null' -or -not $f[3]) { Sh "device_config delete $($f[1]) $($f[2])" | Out-Null }
            else { Sh "device_config put $($f[1]) $($f[2]) $($f[3])" | Out-Null }
        }
        'home' { Sh "cmd package set-home-activity --user 0 $($f[1])" | Out-Null; Write-Host "home is $($f[1]) again" }
    }
}
Sh 'input keyevent KEYCODE_HOME' | Out-Null
Write-Host 'Restored. Turn Network debugging off on the TV when you are finished.'
