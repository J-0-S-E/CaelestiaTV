#!/usr/bin/env python3
"""Fetch Caelestia's colour schemes into schemes/ under short names.

Only the colour values are taken (caelestia-dots/cli, GPL-3.0), no code. dracula-dark.txt is our own darker
variant of Caelestia's Dracula and is kept in the repo as it is.
Run: python make_schemes.py
"""
import sys
import urllib.request
from pathlib import Path

BASE = "https://raw.githubusercontent.com/caelestia-dots/cli/main/src/caelestia/data/schemes/"

# path in caelestia-dots/cli -> file name on the TV
SCHEMES = {
    "caelestia/default/dark.txt": "caelestia-dark.txt",
    "caelestia/default/light.txt": "caelestia-light.txt",
    "catppuccin/mocha/dark.txt": "catppuccin-mocha.txt",
    "catppuccin/latte/light.txt": "catppuccin-latte.txt",
    "gruvbox/medium/dark.txt": "gruvbox-dark.txt",
    "gruvbox/medium/light.txt": "gruvbox-light.txt",
    "rosepine/main/dark.txt": "rosepine.txt",
    "rosepine/dawn/light.txt": "rosepine-dawn.txt",
    "everforest/medium/dark.txt": "everforest-dark.txt",
    "everforest/medium/light.txt": "everforest-light.txt",
    "tokyonight/medium/dark.txt": "tokyonight.txt",
    "nord/medium/dark.txt": "nord.txt",
    "darkgreen/medium/dark.txt": "darkgreen.txt",
}

OUT = Path(__file__).resolve().parent / "schemes"


def main():
    OUT.mkdir(exist_ok=True)
    failed = 0
    for path, name in SCHEMES.items():
        try:
            with urllib.request.urlopen(BASE + path, timeout=30) as r:
                data = r.read()
            if b"primary " not in data:
                raise ValueError("no primary line")
            (OUT / name).write_bytes(data)
            print("ok   ", name)
        except Exception as e:  # network or format
            failed += 1
            print("FAIL ", name, "-", e, file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
