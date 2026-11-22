#!/usr/bin/env python3
"""Procedural asset generator for War Mod (placeholder art, all of it is meant to be replaced).

Generates:
  * GeckoLib .geo.json models        -> assets/warmod/geo/
  * GeckoLib .animation.json files   -> assets/warmod/animations/
  * model atlases + icons (PNG)      -> assets/warmod/textures/geo/, textures/item/
  * scope overlays                   -> assets/warmod/textures/gui/scope/
"""
import json
import os
import math
import random

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources", "assets", "warmod")

for sub in ("geo", "animations", "textures/geo", "textures/item", "textures/entity", "textures/block",
            "textures/gui/scope", "textures/particle", "models/item", "models/block", "blockstates", "lang"):
    os.makedirs(os.path.join(ROOT, sub), exist_ok=True)

from PIL import Image, ImageDraw  # noqa: E402

# --------------------------------------------------------------------------- weapon table
# id: (category, scale, palette)
# scale multiplies the base length so a pistol is small and a sniper is long.
WEAPONS = {
    # assault rifles
    "ak47": ("rifle", 1.05, "wood"), "ak74": ("rifle", 1.02, "wood"), "m4a1": ("rifle", 1.0, "black"),
    "m16a4": ("rifle", 1.06, "black"), "scar_l": ("rifle", 1.08, "tan"), "aug": ("rifle", 1.0, "olive"),
    "g36c": ("rifle", 1.0, "black"),
    # smg
    "mp5": ("smg", 0.8, "black"), "mp7": ("smg", 0.74, "black"), "ump45": ("smg", 0.82, "black"),
    "p90": ("smg", 0.78, "olive"),
    # snipers
    "awp": ("sniper", 1.25, "olive"), "m24": ("sniper", 1.22, "wood"),
    "barrett_m82": ("sniper", 1.4, "tan"), "svd": ("sniper", 1.2, "wood"),
    # shotguns
    "remington_870": ("shotgun", 1.1, "wood"), "saiga_12": ("shotgun", 1.05, "black"),
    "spas_12": ("shotgun", 1.08, "black"),
    # pistols
    "glock_17": ("pistol", 0.5, "black"), "desert_eagle": ("pistol", 0.56, "tan"),
    "m1911": ("pistol", 0.52, "black"), "usp": ("pistol", 0.5, "black"),
    # mg
    "m249": ("mg", 1.2, "olive"), "pkm": ("mg", 1.22, "wood"), "minigun": ("mg", 1.15, "black"),
    # launchers
    "rpg7": ("launcher", 1.3, "olive"), "m320": ("launcher", 0.6, "black"),
    # melee
    "knife": ("melee", 0.4, "steel"), "machete": ("melee", 0.52, "steel"),
    # throwables
    "grenade_f1": ("throwable", 0.35, "olive"), "smoke_grenade": ("throwable", 0.36, "gray"),
    "c4": ("throwable", 0.4, "tan"),
}

PALETTES = {
    "black": ((44, 44, 48), (70, 70, 76), (24, 24, 28)),
    "tan": ((126, 106, 78), (156, 134, 100), (92, 76, 56)),
    "olive": ((74, 82, 58), (98, 108, 76), (52, 58, 40)),
    "wood": ((92, 60, 34), (120, 82, 48), (70, 44, 24)),
    "steel": ((150, 152, 158), (190, 192, 200), (104, 106, 112)),
    "gray": ((96, 98, 102), (128, 130, 134), (72, 74, 78)),
}

SCOPES = {
    "awp": "SNIPER", "m24": "SNIPER", "barrett_m82": "SNIPER", "svd": "ACOG",
    "m16a4": "ACOG", "scar_l": "ACOG", "m249": "ACOG", "pkm": "ACOG",
    "mp5": "RED_DOT", "mp7": "RED_DOT", "ump45": "RED_DOT", "p90": "RED_DOT",
    "saiga_12": "RED_DOT", "desert_eagle": "RED_DOT", "usp": "RED_DOT",
    "ak74": "RIFLE", "m4a1": "RIFLE", "aug": "RIFLE", "g36c": "RIFLE",
}


class Packer:
    """Tiny shelf packer for box-UV atlases."""

    def __init__(self, width, height):
        self.width = width
        self.height = height
        self.x = 1
        self.y = 1
        self.row_height = 0
        self.placed = []

    def add(self, w, h):
        if self.x + w + 1 > self.width:
            self.x = 1
            self.y += self.row_height + 1
            self.row_height = 0
        if self.y + h + 1 > self.height:
            return None
        pos = (self.x, self.y)
        self.x += w + 1
        self.row_height = max(self.row_height, h)
        self.placed.append((pos[0], pos[1], w, h))
        return pos


def box_uv_size(w, h, d):
    return (2 * d + 2 * w, d + h)


def build_gun_model(gun_id, category, scale, palette_name):
    """Builds a geckolib geo model + matching atlas texture."""
    base, light, dark = PALETTES[palette_name]

    if category == "pistol":
        body = (2.0, 3.0, 7.0 * scale * 1.4)
        barrel = (1.0, 1.0, 4.0 * scale)
        stock = (1.6, 2.2, 1.6)
        mag = (1.3, 3.4, 1.6)
        grip = (1.4, 3.0, 1.8)
        has_scope_default = False
    elif category == "melee":
        body = (0.8, 1.6, 6.0 * scale * 3)
        barrel = (0.4, 0.6, 5.0 * scale * 2)
        stock = (1.2, 1.4, 3.0)
        mag = (0.1, 0.1, 0.1)
        grip = (1.4, 1.4, 2.4)
        has_scope_default = False
    elif category == "throwable":
        body = (2.4, 3.2, 2.4)
        barrel = (0.1, 0.1, 0.1)
        stock = (0.1, 0.1, 0.1)
        mag = (0.1, 0.1, 0.1)
        grip = (0.8, 1.4, 0.8)
        has_scope_default = False
    elif category == "launcher":
        body = (2.6, 2.6, 9.0 * scale * 1.4)
        barrel = (1.8, 1.8, 9.0 * scale)
        stock = (1.4, 2.0, 4.0)
        mag = (0.1, 0.1, 0.1)
        grip = (1.4, 2.6, 1.8)
        has_scope_default = False
    elif category == "mg":
        body = (2.6, 3.4, 11.0 * scale * 1.2)
        barrel = (1.2, 1.2, 9.0 * scale)
        stock = (1.8, 2.6, 5.0)
        mag = (2.4, 5.0, 3.0)
        grip = (1.4, 3.0, 1.8)
        has_scope_default = True
    elif category == "sniper":
        body = (2.2, 3.0, 11.0 * scale * 1.3)
        barrel = (1.0, 1.0, 11.0 * scale)
        stock = (1.8, 2.6, 6.0)
        mag = (1.4, 2.6, 1.8)
        grip = (1.4, 3.0, 1.8)
        has_scope_default = True
    elif category == "shotgun":
        body = (2.4, 3.2, 9.0 * scale * 1.3)
        barrel = (1.6, 1.6, 9.0 * scale)
        stock = (1.8, 2.8, 5.0)
        mag = (0.1, 0.1, 0.1)
        grip = (1.4, 3.0, 1.8)
        has_scope_default = False
    else:  # rifle / smg
        body = (2.2, 3.2, 9.0 * scale * 1.25)
        barrel = (1.1, 1.1, 8.0 * scale)
        stock = (1.8, 2.6, 4.6)
        mag = (1.5, 4.0, 2.0)
        grip = (1.4, 3.0, 1.8)
        has_scope_default = category == "rifle"

    scope_type = SCOPES.get(gun_id)
    has_scope = scope_type is not None or (has_scope_default and category in ("rifle", "sniper", "mg"))

    cubes = []  # (bone, origin, size, color)
    body_z_back = body[2] / 2.0
    body_z_front = -body[2] / 2.0

    cubes.append(("body", [-body[0] / 2, 0.0, body_z_front], list(body), base))
    cubes.append(("barrel", [-barrel[0] / 2, body[1] * 0.45, body_z_front - barrel[2] + 1.0],
                  list(barrel), dark))
    cubes.append(("stock", [-stock[0] / 2, body[1] * 0.1, body_z_back - stock[2] + 1.0],
                  list(stock), base if category not in ("melee",) else dark))
    if mag[0] > 0.5:
        cubes.append(("magazine", [-mag[0] / 2, -mag[1] * 0.85, -0.5], list(mag), dark))
    if grip[0] > 0.5:
        cubes.append(("grip", [-grip[0] / 2, -grip[1] * 0.8, body_z_back - grip[2] * 1.6],
                      list(grip), base))
    if has_scope:
        cubes.append(("scope", [-0.8, body[1] + 0.2, -body[2] * 0.25], [1.6, 1.6, 3.6], dark))

    atlas_size = 128
    packer = Packer(atlas_size, atlas_size)
    uv_map = {}
    for index, (bone, origin, size, color) in enumerate(cubes):
        w, h, d = (max(1, int(round(v))) for v in size)
        pos = packer.add(*box_uv_size(w, h, d))
        if pos is None:
            pos = (1, 1)
        uv_map[index] = pos

    bones = [{"name": "root", "pivot": [0, 0, 0]},
             {"name": "body", "parent": "root", "pivot": [0, 0, 0], "cubes": []},
             {"name": "barrel", "parent": "body", "pivot": [0, 1.4, body_z_front], "cubes": []},
             {"name": "magazine", "parent": "body", "pivot": [0, -1.2, 0], "cubes": []},
             {"name": "grip", "parent": "body", "pivot": [0, -1.4, body_z_back - 2], "cubes": []},
             {"name": "stock", "parent": "body", "pivot": [0, 0.4, body_z_back], "cubes": []},
             {"name": "scope", "parent": "body", "pivot": [0, body[1] + 1.0, 0], "cubes": []},
             {"name": "muzzle_flash", "parent": "barrel", "pivot": [0, 0.6, body_z_front - barrel[2]],
              "cubes": []}]
    bone_index = {b["name"]: b for b in bones}
    for index, (bone, origin, size, color) in enumerate(cubes):
        u, v = uv_map[index]
        bone_index[bone]["cubes"].append({
            "origin": [round(o, 2) for o in origin],
            "size": [round(s, 2) for s in size],
            "uv": [u, v],
        })

    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.warmod." + gun_id,
                "texture_width": atlas_size,
                "texture_height": atlas_size,
                "visible_bounds_width": 4,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 0.5, 0],
            },
            "bones": bones,
        }],
    }

    # atlas texture
    image = Image.new("RGBA", (atlas_size, atlas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for index, (bone, origin, size, color) in enumerate(cubes):
        w, h, d = (max(1, int(round(v))) for v in size)
        u, v = uv_map[index]
        bw, bh = box_uv_size(w, h, d)
        draw.rectangle([u, v, u + bw - 1, v + bh - 1], fill=color + (255,))
        draw.rectangle([u, v, u + bw - 1, v + 1], fill=light + (255,))          # top highlight
        draw.rectangle([u, v + bh - 2, u + bw - 1, v + bh - 1], fill=dark + (255,))
        draw.rectangle([u, v, u + 1, v + bh - 1], outline=dark + (255,))
        # a few scratches so that the placeholder does not look totally flat
        for _ in range(max(1, bw // 12)):
            sx = u + 2 + random.randint(0, max(0, bw - 5))
            sy = v + 2 + random.randint(0, max(0, bh - 4))
            draw.point((sx, sy), fill=(20, 20, 22, 255))

    return geo, image


def animations_for(category):
    """Bedrock 1.8.0 animations: idle, draw, fire, reload, ads, sprint."""
    if category == "throwable":
        fire = {"loop": False, "animation_length": 0.35, "bones": {
            "body": {"position": {"0": [0, 0, 0], "0.15": [0, 1.5, -1.5], "0.35": [0, 0, 0]},
                     "rotation": {"0": [0, 0, 0], "0.15": [-35, 0, 0], "0.35": [0, 0, 0]}}}}
        reload = {"loop": False, "animation_length": 0.8, "bones": {
            "root": {"rotation": {"0": [0, 0, 0], "0.4": [0, 25, 0], "0.8": [0, 0, 0]}}}}
    elif category == "melee":
        fire = {"loop": False, "animation_length": 0.35, "bones": {
            "root": {"rotation": {"0": [0, 0, 0], "0.12": [-55, 25, 35], "0.35": [0, 0, 0]}}}}
        reload = {"loop": False, "animation_length": 0.5, "bones": {
            "root": {"rotation": {"0": [0, 0, 0], "0.25": [0, 40, 0], "0.5": [0, 0, 0]}}}}
    else:
        fire = {"loop": False, "animation_length": 0.28, "bones": {
            "root": {"position": {"0": [0, 0, 0], "0.06": [0, 0.35, 1.1], "0.28": [0, 0, 0]},
                     "rotation": {"0": [0, 0, 0], "0.06": [-6, 0, 0], "0.28": [0, 0, 0]}},
            "barrel": {"rotation": {"0": [0, 0, 0], "0.05": [-4, 0, 0], "0.28": [0, 0, 0]}},
            "magazine": {"position": {"0": [0, 0, 0], "0.05": [0, 0.15, 0], "0.28": [0, 0, 0]}}}}
        reload = {"loop": False, "animation_length": 1.7, "bones": {
            "root": {"rotation": {"0": [0, 0, 0], "0.3": [-22, 0, 32], "1.3": [-22, 0, 32], "1.7": [0, 0, 0]},
                     "position": {"0": [0, 0, 0], "0.3": [0, -0.8, 0], "1.3": [0, -0.8, 0], "1.7": [0, 0, 0]}},
            "magazine": {"position": {"0": [0, 0, 0], "0.5": [0, -5, 0], "0.8": [0, -5.5, 0],
                                      "1.05": [0, -5.5, 0], "1.3": [0, 0, 0]}}}}

    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": {"loop": True, "animation_length": 4.0, "bones": {
                "root": {"rotation": {"0": [0, 0, 0], "2.0": [1.5, 1.2, 1.0], "4.0": [0, 0, 0]},
                         "position": {"0": [0, 0, 0], "2.0": [0.08, 0.12, 0], "4.0": [0, 0, 0]}}}},
            "draw": {"loop": False, "animation_length": 0.55, "bones": {
                "root": {"rotation": {"0": [-45, 20, 25], "0.55": [0, 0, 0]},
                         "position": {"0": [0, -3, 1], "0.55": [0, 0, 0]}}}},
            "fire": fire,
            "reload": reload,
            "ads": {"loop": True, "animation_length": 2.0, "bones": {
                "root": {"position": {"0": [0, 0, 0], "1.0": [0, 0.55, 0.9], "2.0": [0, 0, 0]},
                         "rotation": {"0": [0, 0, 0], "1.0": [-3, 0, 0], "2.0": [0, 0, 0]}}}},
            "sprint": {"loop": True, "animation_length": 1.0, "bones": {
                "root": {"rotation": {"0": [-10, 0, 18], "0.5": [-14, 0, 24], "1.0": [-10, 0, 18]}}}},
        },
    }


def icon_for(gun_id, category, scale, palette_name):
    """32x32 inventory icon: side silhouette of the weapon."""
    base, light, dark = PALETTES[palette_name]
    size = 32
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    random.seed(hash(gun_id) & 0xFFFF)

    if category == "melee":
        length = int(18 * (1.0 + scale))
        draw.rectangle([3, 15, 3 + length, 19], fill=light + (255,))
        draw.polygon([(3 + length, 15), (3 + length + 5, 17), (3 + length, 19)], fill=light + (255,))
        draw.rectangle([2, 14, 6, 20], fill=dark + (255,))
    elif category == "throwable":
        draw.ellipse([10, 10, 22, 24], fill=base + (255,), outline=dark + (255,))
        draw.rectangle([14, 6, 18, 11], fill=dark + (255,))
        draw.line([16, 6, 20, 2], fill=light + (255,))
    elif category == "pistol":
        draw.rectangle([8, 12, 24, 17], fill=base + (255,))
        draw.rectangle([10, 17, 15, 25], fill=dark + (255,))
        draw.rectangle([24, 13, 27, 15], fill=light + (255,))
    else:
        length = int(20 * scale)
        body_top = 13
        draw.rectangle([2, body_top, 2 + length, body_top + 5], fill=base + (255,))
        draw.rectangle([2 + length, body_top + 1, 2 + length + 8, body_top + 3], fill=light + (255,))
        # stock
        draw.polygon([(2, body_top + 5), (2, body_top + 9), (6, body_top + 9), (7, body_top + 5)],
                     fill=dark + (255,))
        # magazine
        draw.polygon([(2 + length - 6, body_top + 5), (2 + length - 2, body_top + 5),
                      (2 + length - 3, body_top + 11), (2 + length - 7, body_top + 11)], fill=dark + (255,))
        # grip
        draw.rectangle([4, body_top + 5, 7, body_top + 10], fill=dark + (255,))
        if SCOPES.get(gun_id) or category in ("sniper", "mg"):
            draw.rectangle([2 + length // 2 - 3, body_top - 3, 2 + length // 2 + 4, body_top],
                           fill=light + (255,))
        # highlight
        draw.line([3, body_top + 1, 1 + length, body_top + 1], fill=light + (255,))

    return image


def scope_overlay(gun_id, scope_type):
    """256x256 ADS overlay: vignette + reticle, transparent centre."""
    size = 256
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    margin = 18 if scope_type == "SNIPER" else 40
    # soft dark corners
    for i in range(margin):
        alpha = int(235 * (1.0 - i / margin))
        draw.rectangle([i, i, size - 1 - i, size - 1 - i], outline=(0, 0, 0, alpha))
    cx, cy = size // 2, size // 2
    draw.line([cx, cy - 70, cx, cy - 8], fill=(20, 20, 20, 220), width=2)
    draw.line([cx, cy + 8, cx, cy + 70], fill=(20, 20, 20, 220), width=2)
    draw.line([cx - 70, cy, cx - 8, cy], fill=(20, 20, 20, 220), width=2)
    draw.line([cx + 8, cy, cx + 70, cy], fill=(20, 20, 20, 220), width=2)
    draw.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(200, 30, 30, 230))
    if scope_type == "SNIPER":
        for step in range(-60, 61, 20):
            if step == 0:
                continue
            draw.line([cx - 6, cy + step, cx + 6, cy + step], fill=(20, 20, 20, 200), width=1)
            draw.line([cx + step, cy - 6, cx + step, cy + 6], fill=(20, 20, 20, 200), width=1)
        draw.ellipse([cx - 100, cy - 100, cx + 100, cy + 100], outline=(15, 15, 15, 240), width=6)
    return image


def main():
    random.seed(20240101)
    for gun_id, (category, scale, palette) in WEAPONS.items():
        geo, atlas = build_gun_model(gun_id, category, scale, palette)
        with open(os.path.join(ROOT, "geo", gun_id + ".geo.json"), "w") as handle:
            json.dump(geo, handle, indent=1)
        with open(os.path.join(ROOT, "animations", gun_id + ".animation.json"), "w") as handle:
            json.dump(animations_for(category), handle, indent=1)
        atlas.save(os.path.join(ROOT, "textures", "geo", gun_id + ".png"))
        icon_for(gun_id, category, scale, palette).save(
            os.path.join(ROOT, "textures", "item", gun_id + ".png"))
        if gun_id in SCOPES:
            scope_overlay(gun_id, SCOPES[gun_id]).save(
                os.path.join(ROOT, "textures", "gui", "scope", gun_id + ".png"))
    print("guns done:", len(WEAPONS))


if __name__ == "__main__":
    main()
