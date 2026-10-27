# CaelestiaTV

Android TV home screen in [Caelestia](https://github.com/caelestia-dots/shell) colours.
Built on the [CleanYourTV](https://github.com/Gathin23/cleanyourtv) launcher.

- 14 Caelestia colour schemes, dark and light, switched with the remote
- Big two-tone clock in the style of Caelestia's lock screen, with a random quote beside it
- Frosted dock, aurora backdrop, memory and storage rings
- Smart dock: the apps you use most move to the front
- App switcher and a clock screensaver
- No internet permission, no tracking, about 550 KB

![CaelestiaTV in Caelestia dark](screenshots/home-caelestia-dark.jpg)

## Screenshots

Home in a few of the schemes, with YouTube focused:

| | |
|---|---|
| ![Dracula dark](screenshots/home-dracula-dark.jpg) | ![Darkgreen](screenshots/home-darkgreen.jpg) |
| Dracula dark | Darkgreen |
| ![Caelestia light](screenshots/home-caelestia-light.jpg) | ![Colour scheme picker](screenshots/picker.jpg) |
| Caelestia light | The palette button opens the scheme list |

The screensaver takes the scheme too, with a new quote each time:

| | |
|---|---|
| ![](screenshots/screensaver-caelestia-dark.jpg) | ![](screenshots/screensaver-dracula-dark.jpg) |
| Caelestia dark | Dracula dark |
| ![](screenshots/screensaver-darkgreen.jpg) | ![](screenshots/screensaver-catppuccin-mocha.jpg) |
| Darkgreen | Catppuccin mocha |
| ![](screenshots/screensaver-gruvbox-dark.jpg) | ![](screenshots/screensaver-everforest-dark.jpg) |
| Gruvbox dark | Everforest dark |
| ![](screenshots/screensaver-rosepine.jpg) | ![](screenshots/screensaver-caelestia-light.jpg) |
| Rosé Pine | Caelestia light |

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

Change scheme: the palette button at the top right.
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

## AI declaration

CaelestiaTV is made with AI. Most of the code, the install scripts and this README were written by Claude in
Claude Code (Anthropic). I decided what to build and how it should look, tried every change on my own TV, and
went back and forth until it was right. Claude has been part of it since the first commit; the commits from this
one on also list Claude as co-author.

How I use it: one main chat on **Claude Opus 5.5**, which is the brain. It holds the conversation, makes the
plan, takes the design calls, writes the code that needs context and checks everything before it is used. To save
tokens, bigger jobs that are clearly defined go to subagents on cheaper models, and Opus reviews what comes back:

- **Claude Haiku** for reading: it mapped the CleanYourTV code before the first changes
- **Claude Sonnet** for building to an agreed plan: the colour schemes on every surface, the light schemes,
  the contrast test and the memory and storage rings
- **Opus 5.5** itself for the rest, mostly on medium effort: the review and fixes of Sonnet's work, the clock,
  quotes, palette button, layout and screensaver changes, and the install scripts

Before anything from the internet was cloned or installed, Claude checked it for security first. Testing is on a
real TV over adb: Claude installs the build, takes a screenshot, and I say what to change.

The launcher underneath is CleanYourTV, written by its own authors. The colour schemes, fonts and quotes come from
the projects named under Credits.
