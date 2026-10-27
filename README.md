# CaelestiaTV

Android TV home screen in [Caelestia](https://github.com/caelestia-dots/shell) colours.
Built on the [CleanYourTV](https://github.com/Gathin23/cleanyourtv) launcher.

- 13 Caelestia colour schemes, dark and light, switched with the remote
- Frosted dock, aurora backdrop, memory and storage rings
- Smart dock: the apps you use most move to the front
- App switcher and a clock screensaver
- No internet permission, no tracking, about 550 KB

## Install

You need `adb` on your computer and Network debugging on the TV (Android 8 or newer).
Get `CaelestiaTV.apk` from Releases and put it next to the script, then:

    ./install.sh <tv-ip>          # Linux, macOS
    .\install.ps1 <tv-ip>         # Windows

It installs the launcher and theme, disables Google's extras that no TV needs ([debloat](debloat/)), sets
animations to 0.5x and lets Android keep 6 apps in the background instead of 32. That last one is most of the
speed on a TV with 2 GB of RAM. Add your TV's own bloat list with `--list` (see [debloat](debloat/)), or skip
parts with `--no-debloat` / `--no-tweaks`.

Nothing is uninstalled. The old state goes to `backup/` first, and `./restore.sh <tv-ip>` puts it back.
Without the APK the script builds one from `launcher/` (JDK 17 and the Android SDK).

Change scheme: Settings button → Launcher settings → Colour scheme.
Change the layout or the dock apps in `theme/config.json` and run the script again.

## Extras

Two intents you can bind to a key, for example with [Key Mapper](https://github.com/keymapperorg/KeyMapper):

- `com.gathin.tvhome.RECENTS` opens the app switcher
- `com.gathin.tvhome.SCREENSAVER` starts the clock

Pressing Home while you are already on Home also starts the clock.

## Credits

Launcher: CleanYourTV (MIT). Colour schemes: Caelestia and the themes they come from (Dracula, Catppuccin,
Gruvbox, Rosé Pine, Tokyo Night, Nord, Everforest). Fonts: Outfit (OFL), and Google Sans Flex (OFL) for the
clock, and the quotes list.

Shoutout to [caelestia-sddm](https://github.com/ItsABigIgloo/caelestia-sddm) by ItsABigIgloo: its lock screen
is the inspiration for the clock, the quotes and the look of CaelestiaTV.
