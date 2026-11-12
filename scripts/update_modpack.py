#!/usr/bin/env python3
"""
Resolves the mods that the Dom Client installer downloads and writes
installer/payload/modpack.json, plus the Fabric Loader launcher profile
(installer/payload/fabric-profile.json).

The installer never ships third-party jars inside the .exe. It downloads each
mod straight from Modrinth's CDN when you click Install and checks the SHA-512
pinned here, the same way a Modrinth modpack works. Re-run this script to bump
versions:

    python3 scripts/update_modpack.py
"""
import json
import pathlib
import sys
import urllib.parse
import urllib.request

GAME_VERSION = "26.2"
LOADER = "fabric"
LOADER_VERSION = "0.20.1"
USER_AGENT = "Dom-V1-modpack-builder/1.0 (github.com/adenthegoat565/Dom-V1)"

# required: always installed, the box is locked
# default:  ticked when the installer opens
# beta_ok:  allow Modrinth "beta" builds when they are newer than the latest release
MODS = [
    {"slug": "fabric-api", "name": "Fabric API",
     "desc": "Library that almost every Fabric mod needs.", "required": True},
    {"slug": "simple-voice-chat", "name": "Simple Voice Chat",
     "desc": "Proximity voice chat. Press V in game to set it up.",
     "required": True, "beta_ok": True},
    {"slug": "sodium", "name": "Sodium",
     "desc": "Much higher FPS and smoother chunk loading.", "default": True},
    {"slug": "lithium", "name": "Lithium",
     "desc": "Faster game logic (mobs, redstone, ticking).", "default": True},
    {"slug": "ferrite-core", "name": "FerriteCore",
     "desc": "Uses a lot less RAM.", "default": True},
    {"slug": "entityculling", "name": "Entity Culling",
     "desc": "Skips drawing mobs and chests you can't see.", "default": True},
    {"slug": "modmenu", "name": "Mod Menu",
     "desc": "Adds a Mods button to see and configure your mods.", "default": True},
    {"slug": "iris", "name": "Iris Shaders",
     "desc": "Lets you use shader packs (needs Sodium).", "default": False},
]

ROOT = pathlib.Path(__file__).resolve().parent.parent
PAYLOAD = ROOT / "installer" / "payload"


def get_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)


def pick_version(project, beta_ok):
    q = urllib.parse.urlencode({
        "loaders": json.dumps([LOADER]),
        "game_versions": json.dumps([GAME_VERSION]),
    })
    versions = get_json(f"https://api.modrinth.com/v2/project/{project}/version?{q}")
    allowed = ("release", "beta") if beta_ok else ("release",)
    for v in versions:  # Modrinth returns newest first
        if v["version_type"] in allowed:
            return v
    if versions:
        return versions[0]
    sys.exit(f"No {LOADER} build of '{project}' for Minecraft {GAME_VERSION} on Modrinth")


def primary_file(version):
    files = version["files"]
    return next((f for f in files if f.get("primary")), files[0])


def main():
    entries = {}
    id_to_slug = {}
    pending = [dict(m) for m in MODS]
    while pending:
        mod = pending.pop(0)
        slug = mod["slug"]
        if slug in entries:
            continue
        project = get_json(f"https://api.modrinth.com/v2/project/{slug}")
        version = pick_version(slug, mod.get("beta_ok", False))
        f = primary_file(version)
        id_to_slug[project["id"]] = slug
        entries[slug] = {
            "id": slug,
            "name": mod.get("name", project["title"]),
            "description": mod.get("desc", project["description"]),
            "version": version["version_number"],
            "required": bool(mod.get("required", False)),
            "default": bool(mod.get("required", False) or mod.get("default", False)),
            "hidden": bool(mod.get("hidden", False)),
            "fileName": f["filename"],
            "url": f["url"],
            "sha512": f["hashes"]["sha512"],
            "sha1": f["hashes"]["sha1"],
            "size": f["size"],
            "_deps": [d["project_id"] for d in version["dependencies"]
                      if d["dependency_type"] == "required" and d.get("project_id")],
        }
        print(f"  {slug:<22} {version['version_number']:<28} {version['version_type']}")
        # Pull in required dependencies (for example Mod Menu -> Text Placeholder API).
        for dep_id in entries[slug]["_deps"]:
            if dep_id in id_to_slug:
                continue
            dep = get_json(f"https://api.modrinth.com/v2/project/{dep_id}")
            id_to_slug[dep_id] = dep["slug"]
            if dep["slug"] not in entries:
                pending.append({"slug": dep["slug"], "name": dep["title"],
                                "desc": "Needed by another mod in this list.",
                                "hidden": True})

    for e in entries.values():
        e["requires"] = sorted({id_to_slug[d] for d in e.pop("_deps") if d in id_to_slug})

    PAYLOAD.mkdir(parents=True, exist_ok=True)
    modpack = {
        "gameVersion": GAME_VERSION,
        "loaderVersion": LOADER_VERSION,
        "mods": list(entries.values()),
    }
    (PAYLOAD / "modpack.json").write_text(json.dumps(modpack, indent=2) + "\n")

    profile = get_json(f"https://meta.fabricmc.net/v2/versions/loader/"
                       f"{GAME_VERSION}/{LOADER_VERSION}/profile/json")
    (PAYLOAD / "fabric-profile.json").write_text(json.dumps(profile, indent=2) + "\n")
    print(f"Wrote {PAYLOAD / 'modpack.json'} and fabric-profile.json ({profile['id']})")


if __name__ == "__main__":
    main()
