#!/usr/bin/env python3
"""Static validation of the generated resources and their cross-references with the Java code."""
import json
import os
import re
import sys

SRC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(SRC, "src/main/resources/assets/warmod")
DATA = os.path.join(SRC, "src/main/resources/data/warmod")
JAVA = os.path.join(SRC, "src/main/java/com/warmod")

problems = []
notes = []


def check(condition, message):
    if not condition:
        problems.append(message)


def load(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


# ------------------------------------------------------------------ weapons from Java
gun_java = open(os.path.join(JAVA, "item/GunRegistry.java"), encoding="utf-8").read()
gun_ids = re.findall(r'(?:auto|burst|semi|sniper|shotgun|pistol|mg|launcher|melee|throwable)\("([a-z0-9_]+)"', gun_java)
notes.append(f"weapons in Java: {len(gun_ids)}")
check(len(set(gun_ids)) == len(gun_ids), "duplicate weapon ids in GunRegistry")
check(len(gun_ids) >= 30, f"expected at least 30 weapons, found {len(gun_ids)}")

# sounds registered in Java
sound_registry = open(os.path.join(JAVA, "registry/SoundRegistry.java"), encoding="utf-8").read()
java_sounds = set(re.findall(r'reg\("([a-z0-9_]+)"\)', sound_registry))
sounds_json = load(os.path.join(ASSETS, "sounds.json"))
ogg_files = {f[:-4] for f in os.listdir(os.path.join(ASSETS, "sounds")) if f.endswith(".ogg")}
for sound in java_sounds:
    check(sound in sounds_json, f"sound '{sound}' missing from sounds.json")
    check(sound in ogg_files, f"sound '{sound}' missing OGG file")
for sound in ogg_files:
    check(sound in sounds_json, f"ogg '{sound}' has no sounds.json entry")
notes.append(f"sounds: {len(java_sounds)} registered, {len(ogg_files)} ogg files")

# sounds referenced by weapon definitions must exist
referenced = set(re.findall(r'"(gunshot_[a-z0-9_]+|reload_[a-z0-9_]+|melee_[a-z0-9_]+)"', gun_java))
for sound in referenced:
    check(sound in java_sounds, f"weapon sound '{sound}' is not registered in SoundRegistry")

# ------------------------------------------------------------------ geo / animations / textures
for gun_id in gun_ids:
    geo_path = os.path.join(ASSETS, "geo", gun_id + ".geo.json")
    anim_path = os.path.join(ASSETS, "animations", gun_id + ".animation.json")
    tex_path = os.path.join(ASSETS, "textures/geo", gun_id + ".png")
    icon_path = os.path.join(ASSETS, "textures/item", gun_id + ".png")
    model_path = os.path.join(ASSETS, "models/item", gun_id + ".json")
    for path in (geo_path, anim_path, tex_path, icon_path, model_path):
        check(os.path.exists(path), f"missing asset {path}")
    if os.path.exists(geo_path):
        geo = load(geo_path)
        geometry = geo.get("minecraft:geometry")
        check(isinstance(geometry, list) and geometry, f"{gun_id}: geometry list missing")
        bones = {bone["name"] for bone in geometry[0].get("bones", [])}
        for required in ("root", "body", "barrel", "magazine", "grip", "stock"):
            check(required in bones, f"{gun_id}: bone '{required}' missing")
        check(any(b.get("cubes") for b in geometry[0]["bones"]), f"{gun_id}: model has no cubes")
    if os.path.exists(anim_path):
        anims = load(anim_path).get("animations", {})
        for required in ("idle", "draw", "fire", "reload", "ads", "sprint"):
            check(required in anims, f"{gun_id}: animation '{required}' missing")

# ------------------------------------------------------------------ entities
for entity_id, texture in (("soldier", "soldier_alpha.png"),):
    geo = load(os.path.join(ASSETS, "geo", entity_id + ".geo.json"))
    bones = {b["name"] for b in geo["minecraft:geometry"][0]["bones"]}
    for required in ("root", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg", "weapon"):
        check(required in bones, f"soldier: bone '{required}' missing")
    anims = load(os.path.join(ASSETS, "animations", "soldier.animation.json"))["animations"]
    for required in ("idle", "run", "aim", "shoot", "reload", "death"):
        check(required in anims, f"soldier: animation '{required}' missing")
    for path in ("soldier_alpha.png", "soldier_bravo.png", "soldier_corpse.png"):
        check(os.path.exists(os.path.join(ASSETS, "textures/entity", path)), f"missing soldier texture {path}")

for vehicle in ("armored_car_1", "armored_car_2", "plane", "helicopter", "tank"):
    for path in (f"geo/{vehicle}.geo.json", f"animations/{vehicle}.animation.json",
                 f"textures/entity/{vehicle}.png"):
        check(os.path.exists(os.path.join(ASSETS, path)), f"missing vehicle asset {path}")
    anims = load(os.path.join(ASSETS, "animations", vehicle + ".animation.json"))["animations"]
    for required in ("idle", "move", "fire"):
        check(required in anims, f"{vehicle}: animation '{required}' missing")

# ------------------------------------------------------------------ scope textures
for path in os.listdir(os.path.join(ASSETS, "textures/gui/scope")):
    check(path.endswith(".png"), "unexpected scope file")

# gun registry scope map must match produced textures
scoped = re.findall(r'"([a-z0-9_]+)",\s*AmmoType[\s\S]{0,120}?ScopeType\.(?!NONE)', gun_java)
notes.append(f"scope overlays: {len(os.listdir(os.path.join(ASSETS, 'textures/gui/scope')))}")

# ------------------------------------------------------------------ lang
for lang in ("ru_ru.json", "en_us.json"):
    data = load(os.path.join(ASSETS, "lang", lang))
    for gun_id in gun_ids:
        check("item.warmod." + gun_id in data, f"{lang}: missing item.warmod.{gun_id}")
    for key in ("warmod.death.title", "warmod.death.respawn", "warmod.death.leave",
                "warmod.radio.title", "itemGroup.warmod", "key.warmod.reload",
                "warmod.firemode.auto", "warmod.order.follow", "warmod.faction.alpha"):
        check(key in data, f"{lang}: missing key {key}")
    # every msg.* key used in java must exist
    java_files = []
    for root, _, files in os.walk(JAVA):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))
    used = set()
    for file in java_files:
        content = open(file, encoding="utf-8").read()
        used |= set(re.findall(r'"(warmod\.[a-z0-9_.]+)"', content))
    for key in sorted(used):
        if key.endswith("."):
            continue
        if key.startswith("warmod.tooltip.") or key.startswith("warmod.msg.") or key.startswith("warmod.order.") \
                or key.startswith("warmod.radio.") or key.startswith("warmod.death.") or key.startswith("warmod.firemode.") \
                or key.startswith("warmod.faction.") or key.startswith("warmod.ammo."):
            check(key in data, f"{lang}: java uses missing lang key {key}")

# ------------------------------------------------------------------ data pack
dim_type = load(os.path.join(DATA, "dimension_type", "battlefield.json"))
dim = load(os.path.join(DATA, "dimension", "battlefield.json"))
biome = load(os.path.join(DATA, "worldgen", "biome", "ruined_city.json"))
check(dim["generator"]["type"] == "warmod:battlefield", "dimension generator type mismatch")
check(dim["generator"]["biome_source"]["biome"] == "warmod:ruined_city", "dimension biome mismatch")
check(dim_type["effects"] == "warmod:battlefield", "dimension_type effects mismatch")
check(dim_type["fixed_time"] == 18000, "fixed time must be night")
check("features" in biome and "carvers" in biome, "biome json incomplete")
for recipe in os.listdir(os.path.join(DATA, "recipes")):
    load(os.path.join(DATA, "recipes", recipe))

# every item model referenced by a recipe exists
print("\n".join(notes))
if problems:
    print("\nPROBLEMS:")
    for problem in problems:
        print(" -", problem)
    sys.exit(1)
print("OK: assets and cross references are consistent")
