#!/bin/sh
# Put the TV back the way install.sh found it, from backup/<tv>.txt: the apps it disabled, the animation and
# background-app settings, and the old home screen. CaelestiaTV stays installed; uninstall it in the TV's settings.
#
# Usage: ./restore.sh <tv-ip>[:port]
set -e
[ -n "$1" ] || { echo "usage: $0 <tv-ip>[:port]"; exit 1; }
here=$(cd "$(dirname "$0")" && pwd)
tv=$1
case $tv in *:*) ;; *) tv=$tv:5555 ;; esac
backup=$here/backup/$(echo "$tv" | tr -c 'A-Za-z0-9.\n' '_').txt
[ -f "$backup" ] || { echo "no backup for $tv ($backup)"; exit 1; }
# </dev/null: adb would otherwise read the backup lines meant for the loop below.
sh_tv() { adb -s "$tv" shell "$1" </dev/null | tr -d '\r'; }

adb connect "$tv"
[ "$(adb -s "$tv" get-state 2>/dev/null)" = device ] || { echo "no adb connection to $tv"; exit 1; }

while read -r kind a b c; do
    case $kind in
        disabled) sh_tv "pm enable --user 0 $a" >/dev/null; echo "enabled $a" ;;
        setting) if [ -z "$c" ] || [ "$c" = null ]; then sh_tv "settings delete $a $b"; else sh_tv "settings put $a $b $c"; fi >/dev/null ;;
        device_config) if [ -z "$c" ] || [ "$c" = null ]; then sh_tv "device_config delete $a $b"; else sh_tv "device_config put $a $b $c"; fi >/dev/null ;;
        home) sh_tv "cmd package set-home-activity --user 0 $a" >/dev/null; echo "home is $a again" ;;
    esac
done < "$backup"
sh_tv 'input keyevent KEYCODE_HOME' >/dev/null
echo "Restored. Turn Network debugging off on the TV when you are finished."
