#!/bin/sh
# Install CaelestiaTV on an Android TV over adb and make it the home screen.
# Usage: ./install.sh <tv-ip>[:port] [path/to/CaelestiaTV.apk]
# Without an APK it uses CaelestiaTV.apk next to this script, or builds one from launcher/ (needs JDK 17 and the Android SDK).
set -e
[ -n "$1" ] || { echo "usage: $0 <tv-ip>[:port] [apk]"; exit 1; }
here=$(cd "$(dirname "$0")" && pwd)
tv=$1
case $tv in *:*) ;; *) tv=$tv:5555 ;; esac
apk=${2:-$here/CaelestiaTV.apk}
pkg=com.gathin.tvhome
dir=/sdcard/Android/data/$pkg/files

if [ ! -f "$apk" ]; then
    echo "No APK found, building one from launcher/"
    (cd "$here/launcher" && ./gradlew assembleRelease -q)
    apk=$here/launcher/app/build/outputs/apk/release/app-release.apk
fi

adb connect "$tv"
adb -s "$tv" install -r "$apk"

# Start it once so Android makes its files folder, then put the theme and the schemes there.
adb -s "$tv" shell "am start -W -n $pkg/.MainActivity >/dev/null; mkdir -p $dir/schemes"
adb -s "$tv" push "$here/theme/config.json" "$dir/config.json" >/dev/null
for f in "$here"/theme/schemes/*.txt; do
    adb -s "$tv" push "$f" "$dir/schemes/$(basename "$f")" >/dev/null
done
adb -s "$tv" shell "printf '%s' '$(date +%Y%m%d%H%M%S)' > $dir/theme_revision; cmd package set-home-activity --user 0 $pkg/.MainActivity; am force-stop $pkg; am start -a android.intent.action.MAIN -c android.intent.category.HOME -n $pkg/.MainActivity" >/dev/null
echo "Done. Press Home on the remote."
