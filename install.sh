#!/bin/sh
# Install CaelestiaTV on an Android TV over adb: the launcher and theme, a debloat and lighter memory settings.
# Everything it changes is written to backup/<tv>.txt first, and restore.sh puts it back.
#
# Usage: ./install.sh <tv-ip>[:port] [--apk file] [--list model-debloat.txt] [--no-debloat] [--no-tweaks]
# Without an APK it uses CaelestiaTV.apk next to this script, or builds one from launcher/ (needs JDK 17 and the Android SDK).
set -e
[ -n "$1" ] || { echo "usage: $0 <tv-ip>[:port] [--apk file] [--list file] [--no-debloat] [--no-tweaks]"; exit 1; }
here=$(cd "$(dirname "$0")" && pwd)
tv=$1; shift
case $tv in *:*) ;; *) tv=$tv:5555 ;; esac
apk=$here/CaelestiaTV.apk; list=; debloat=1; tweaks=1
while [ $# -gt 0 ]; do
    case $1 in
        --apk) apk=$2; shift ;;
        --list) list=$2; shift ;;
        --no-debloat) debloat= ;;
        --no-tweaks) tweaks= ;;
        *) echo "unknown option $1"; exit 1 ;;
    esac
    shift
done
pkg=com.gathin.tvhome
dir=/sdcard/Android/data/$pkg/files
sh_tv() { adb -s "$tv" shell "$1" </dev/null | tr -d '\r'; }

if [ ! -f "$apk" ]; then
    echo "No APK found, building one from launcher/"
    (cd "$here/launcher" && ./gradlew assembleRelease -q)
    apk=$here/launcher/app/build/outputs/apk/release/app-release.apk
fi

adb connect "$tv"
[ "$(adb -s "$tv" get-state 2>/dev/null)" = device ] || { echo "no adb connection to $tv (allow the prompt on the TV, then run again)"; exit 1; }

# Backup: the first run records the TV as it was; a later run only adds what it changes on top.
mkdir -p "$here/backup"
backup=$here/backup/$(echo "$tv" | tr -c 'A-Za-z0-9.\n' '_').txt
touch "$backup"
remember() {
    key=$(echo "$1" | cut -d' ' -f1-3)
    grep -q "^$key\( \|\$\)" "$backup" || echo "$1" >> "$backup"
}
old_home=$(sh_tv 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1')
case $old_home in */*) case $old_home in $pkg/*) ;; *) remember "home $old_home" ;; esac ;; esac

adb -s "$tv" install -r "$apk"

# Theme: start the launcher once so Android makes its files folder, then put the theme and the schemes there.
sh_tv "am start -W -n $pkg/.MainActivity >/dev/null; mkdir -p $dir/schemes" >/dev/null
adb -s "$tv" push "$here/theme/config.json" "$dir/config.json" >/dev/null
for f in "$here"/theme/schemes/*.txt; do
    adb -s "$tv" push "$f" "$dir/schemes/$(basename "$f")" >/dev/null
done
adb -s "$tv" push "$here/theme/quotes.json" "$dir/quotes.json" >/dev/null
sh_tv "printf '%s' '$(date +%Y%m%d%H%M%S)' > $dir/theme_revision; cmd package set-home-activity --user 0 $pkg/.MainActivity" >/dev/null

if [ -n "$tweaks" ]; then
    # Snappier menus, and Android keeps 6 apps in the background instead of 32, so memory and zram stay free.
    for s in window_animation_scale transition_animation_scale animator_duration_scale; do
        remember "setting global $s $(sh_tv "settings get global $s")"
        sh_tv "settings put global $s 0.5" >/dev/null
    done
    if [ "$(sh_tv 'getprop ro.build.version.sdk')" -ge 29 ]; then
        remember "device_config activity_manager max_cached_processes $(sh_tv 'device_config get activity_manager max_cached_processes')"
        sh_tv 'device_config put activity_manager max_cached_processes 6' >/dev/null
    fi
    # The screensaver after 15 minutes without a key; Android TV starts it at screen_off_timeout.
    remember "setting system screen_off_timeout $(sh_tv 'settings get system screen_off_timeout')"
    sh_tv 'settings put system screen_off_timeout 900000' >/dev/null
    echo "Tweaks: animations 0.5x, at most 6 background apps, screensaver after 15 min"
fi

if [ -n "$debloat" ]; then
    # Never these: Android itself, the setup wizard (may own the saved Wi-Fi), apps that answer Home or open
    # Settings, and anything under the HDMI and tuner inputs (TV_INPUT_HARDWARE).
    core='^(android|com\.android\.(systemui|shell|phone|settings|tv\.settings|providers\..*)|com\.(google\.)?android\.(permissioncontroller|packageinstaller|ext\.services|ext\.shared|modulemetadata)|com\.google\.android\.overlay\.modules.*|com\.google\.android\.tungsten\.setupwraith)$'
    enabled=$(sh_tv 'pm list packages -e --user 0' | sed 's/^package://')
    protected=$(sh_tv 'cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME; cmd package query-activities --brief -a android.settings.SETTINGS' | cut -d/ -f1)
    count=0
    for p in $(cat "$here/debloat/common.txt" ${list:+"$list"} | sed 's/#.*//' | tr -d ' \t\r' | grep -E '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$' | sort -u); do
        echo "$enabled" | grep -qx "$p" || continue
        if echo "$p" | grep -qE "$core" || echo "$protected" | grep -qx "$p"; then echo "  kept $p (the TV needs it)"; continue; fi
        if [ -n "$(sh_tv "dumpsys package $p | grep -q TV_INPUT_HARDWARE && echo hw")" ]; then echo "  kept $p (drives an input)"; continue; fi
        remember "disabled $p"
        sh_tv "pm disable-user --user 0 $p" >/dev/null
        count=$((count + 1))
    done
    echo "Debloat: $count apps disabled"
fi

sh_tv "am force-stop $pkg; am start -a android.intent.action.MAIN -c android.intent.category.HOME -n $pkg/.MainActivity" >/dev/null
echo "Done. Backup of the old state: $backup"
echo "Turn Network debugging off on the TV when you are finished."
