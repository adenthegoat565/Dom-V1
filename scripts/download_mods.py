#!/usr/bin/env python3
"""
Downloads the mods listed in installer/payload/modpack.json (checking SHA-512), for example to
load them next to Dom Client in the in-game test:

    python3 scripts/download_mods.py --dest /tmp/mods --exclude fabric-api
"""
import argparse
import hashlib
import json
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dest", required=True)
    parser.add_argument("--exclude", default="", help="comma separated mod ids to skip")
    args = parser.parse_args()

    skip = {s.strip() for s in args.exclude.split(",") if s.strip()}
    dest = pathlib.Path(args.dest)
    dest.mkdir(parents=True, exist_ok=True)
    modpack = json.loads((ROOT / "installer" / "payload" / "modpack.json").read_text())
    for mod in modpack["mods"]:
        if mod["id"] in skip:
            continue
        target = dest / mod["fileName"]
        if target.exists() and hashlib.sha512(target.read_bytes()).hexdigest() == mod["sha512"]:
            print(f"up to date  {mod['fileName']}")
            continue
        request = urllib.request.Request(mod["url"], headers={"User-Agent": "Dom-V1-build/1.0"})
        with urllib.request.urlopen(request, timeout=120) as response:
            data = response.read()
        if hashlib.sha512(data).hexdigest() != mod["sha512"]:
            raise SystemExit(f"checksum mismatch for {mod['fileName']}")
        target.write_bytes(data)
        print(f"downloaded  {mod['fileName']}")


if __name__ == "__main__":
    main()
