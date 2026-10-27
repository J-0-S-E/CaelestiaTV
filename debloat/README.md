# Debloat lists

`common.txt` is safe on any Android TV and always runs.

For your TV's own bloat (vendor telemetry, ads, factory apps), take the list for your model from
[CleanYourTV's examples](https://github.com/Gathin23/cleanyourtv/tree/main/examples) and pass it to the script:

    ./install.sh <tv-ip> --list path/to/debloat.txt
    .\install.ps1 <tv-ip> -List path\to\debloat.txt

Read the list first and comment out (`#`) anything you use, such as the voice assistant or Chromecast.
The script never disables core Android packages, the app that owns Home, or anything that drives the HDMI and
tuner inputs, whatever the list says.
