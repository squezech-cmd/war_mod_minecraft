#!/usr/bin/env python3
"""Placeholder GeckoLib models + textures for the soldier, the corpse and the five vehicles."""
import json
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources", "assets", "warmod")


class Packer:
    def __init__(self, width, height):
        self.width, self.height = width, height
        self.x, self.y, self.row = 1, 1, 0

    def add(self, w, h):
        if self.x + w + 1 > self.width:
            self.x, self.y, self.row = 1, self.y + self.row + 1, 0
        if self.y + h + 1 > self.height:
            return None
        pos = (self.x, self.y)
        self.x += w + 1
        self.row = max(self.row, h)
        return pos


def box_uv_size(w, h, d):
    return (2 * d + 2 * w, d + h)


def build_model(identifier, boxes, atlas_size=128):
    """boxes: list of dicts {bone, origin, size, color, pivot}"""
    packer = Packer(atlas_size, atlas_size)
    bones = {}
    for entry in boxes:
        name = entry["bone"]
        if name not in bones:
            bones[name] = {
                "name": name,
                "parent": entry.get("parent", "root"),
                "pivot": entry.get("pivot", [0, 0, 0]),
                "cubes": [],
            }
    for entry in boxes:
        w, h, d = (max(1, int(round(v))) for v in entry["size"])
        pos = packer.add(*box_uv_size(w, h, d)) or (1, 1)
        entry["uv"] = pos
        bones[entry["bone"]]["cubes"].append({
            "origin": [round(float(v), 2) for v in entry["origin"]],
            "size": [round(float(v), 2) for v in entry["size"]],
            "uv": list(pos),
        })
    if "root" not in bones:
        bones["root"] = {"name": "root", "pivot": [0, 0, 0], "cubes": []}
    ordered = [bones["root"]] + [b for name, b in bones.items() if name != "root"]
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.warmod." + identifier,
                "texture_width": atlas_size,
                "texture_height": atlas_size,
                "visible_bounds_width": 4,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": ordered,
        }],
    }
    image = Image.new("RGBA", (atlas_size, atlas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for entry in boxes:
        w, h, d = (max(1, int(round(v))) for v in entry["size"])
        u, v = entry["uv"]
        bw, bh = box_uv_size(w, h, d)
        base = entry["color"]
        light = tuple(min(255, c + 26) for c in base)
        dark = tuple(max(0, c - 26) for c in base)
        draw.rectangle([u, v, u + bw - 1, v + bh - 1], fill=base + (255,))
        draw.rectangle([u, v, u + bw - 1, v + 1], fill=light + (255,))
        draw.rectangle([u, v + bh - 2, u + bw - 1, v + bh - 1], fill=dark + (255,))
        for _ in range(max(1, (bw * bh) // 90)):
            sx = u + random.randint(1, max(1, bw - 2))
            sy = v + random.randint(1, max(1, bh - 2))
            draw.point((sx, sy), fill=dark + (255,))
    return geo, image


# --------------------------------------------------------------------------- soldier
def soldier_boxes(main, dark, skin, boots):
    return [
        {"bone": "body", "origin": [-3, 12, -1.5], "size": [6, 10, 3], "color": main, "pivot": [0, 12, 0]},
        {"bone": "body", "origin": [-3.4, 15, -2.1], "size": [6.8, 6, 4.2], "color": dark, "parent": "body",
         "pivot": [0, 15, 0]},
        {"bone": "head", "origin": [-3, 22, -3], "size": [6, 6, 6], "color": skin, "parent": "body",
         "pivot": [0, 22, 0]},
        {"bone": "head", "origin": [-3.3, 25.8, -3.3], "size": [6.6, 2.6, 6.6], "color": dark, "parent": "body",
         "pivot": [0, 26, 0]},
        {"bone": "right_arm", "origin": [2.5, 11.5, -1.2], "size": [2.6, 10, 2.4], "color": main, "parent": "body",
         "pivot": [4, 21, 0]},
        {"bone": "left_arm", "origin": [-5.1, 11.5, -1.2], "size": [2.6, 10, 2.4], "color": main, "parent": "body",
         "pivot": [-4, 21, 0]},
        {"bone": "right_leg", "origin": [1.0, 1, -1.2], "size": [2.4, 11, 2.4], "color": dark, "parent": "body",
         "pivot": [2, 12, 0]},
        {"bone": "left_leg", "origin": [-3.4, 1, -1.2], "size": [2.4, 11, 2.4], "color": dark, "parent": "body",
         "pivot": [-2, 12, 0]},
        {"bone": "right_leg", "origin": [0.9, 0.4, -1.4], "size": [2.6, 1.6, 3.0], "color": boots,
         "parent": "body", "pivot": [2, 1, 0]},
        {"bone": "left_leg", "origin": [-3.5, 0.4, -1.4], "size": [2.6, 1.6, 3.0], "color": boots,
         "parent": "body", "pivot": [-2, 1, 0]},
        {"bone": "weapon", "origin": [2.2, 15.5, -9.5], "size": [1.4, 2.4, 11], "color": (48, 48, 52),
         "parent": "right_arm", "pivot": [4, 19, -4]},
    ]


def soldier_animations():
    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": {"loop": True, "animation_length": 3.0, "bones": {
                "body": {"rotation": {"0": [0, 0, 0], "1.5": [2, 0, 0], "3.0": [0, 0, 0]}},
                "head": {"rotation": {"0": [0, -8, 0], "1.5": [0, 8, 0], "3.0": [0, -8, 0]}},
                "right_arm": {"rotation": {"0": [-6, 0, 4], "1.5": [-2, 0, 6], "3.0": [-6, 0, 4]}},
                "left_arm": {"rotation": {"0": [-6, 0, -4], "1.5": [-2, 0, -6], "3.0": [-6, 0, -4]}},
            }},
            "run": {"loop": True, "animation_length": 0.8, "bones": {
                "right_leg": {"rotation": {"0": [-35, 0, 0], "0.4": [35, 0, 0], "0.8": [-35, 0, 0]}},
                "left_leg": {"rotation": {"0": [35, 0, 0], "0.4": [-35, 0, 0], "0.8": [35, 0, 0]}},
                "right_arm": {"rotation": {"0": [30, 0, 5], "0.4": [-30, 0, 5], "0.8": [30, 0, 5]}},
                "left_arm": {"rotation": {"0": [-30, 0, -5], "0.4": [30, 0, -5], "0.8": [-30, 0, -5]}},
                "body": {"rotation": {"0": [8, 0, 0], "0.4": [10, 0, 0], "0.8": [8, 0, 0]},
                         "position": {"0": [0, 0.4, 0], "0.4": [0, -0.2, 0], "0.8": [0, 0.4, 0]}},
            }},
            "aim": {"loop": True, "animation_length": 1.6, "bones": {
                "right_arm": {"rotation": {"0": [-82, 6, 0], "0.8": [-80, 6, 0], "1.6": [-82, 6, 0]}},
                "left_arm": {"rotation": {"0": [-72, -18, 0], "0.8": [-70, -18, 0], "1.6": [-72, -18, 0]}},
                "body": {"rotation": {"0": [0, 8, 0], "1.6": [0, 8, 0]}},
            }},
            "shoot": {"loop": False, "animation_length": 0.28, "bones": {
                "right_arm": {"rotation": {"0": [-82, 6, 0], "0.06": [-88, 6, 0], "0.28": [-82, 6, 0]}},
                "body": {"rotation": {"0": [0, 8, 0], "0.06": [-4, 8, 0], "0.28": [0, 8, 0]}},
            }},
            "reload": {"loop": False, "animation_length": 1.6, "bones": {
                "left_arm": {"rotation": {"0": [-72, -18, 0], "0.4": [-40, -40, 15], "1.0": [-20, -30, 20],
                                          "1.6": [-72, -18, 0]}},
                "body": {"rotation": {"0": [0, 8, 0], "0.5": [6, 12, 0], "1.6": [0, 8, 0]}},
                "head": {"rotation": {"0": [0, 0, 0], "0.6": [10, 0, 0], "1.6": [0, 0, 0]}},
            }},
            "death": {"loop": False, "animation_length": 1.8, "bones": {
                "root": {"position": {"0": [0, 0, 0], "0.9": [0, -5.5, 0], "1.8": [0, -6.2, 0]},
                         "rotation": {"0": [0, 0, 0], "1.0": [40, 0, 8], "1.8": [78, 0, 12]}},
                "right_leg": {"rotation": {"0": [0, 0, 0], "0.9": [-85, 0, 0], "1.8": [-100, 0, 0]}},
                "left_leg": {"rotation": {"0": [0, 0, 0], "0.9": [-60, 0, 0], "1.8": [-95, 0, 0]}},
                "right_arm": {"rotation": {"0": [0, 0, 0], "1.0": [25, 0, 30], "1.8": [40, 0, 45]}},
                "left_arm": {"rotation": {"0": [0, 0, 0], "1.0": [20, 0, -30], "1.8": [35, 0, -45]}},
                "head": {"rotation": {"0": [0, 0, 0], "1.2": [15, 0, 0], "1.8": [30, 0, 0]}},
            }},
        },
    }


# --------------------------------------------------------------------------- vehicles
def vehicle_boxes(palette, kind):
    base, dark, light, track = palette
    boxes = []
    if kind in ("armored_car_1", "armored_car_2"):
        long_body = 68 if kind == "armored_car_2" else 62
        boxes += [
            {"bone": "body", "origin": [-20, 8, -long_body / 2], "size": [40, 12, long_body], "color": base},
            {"bone": "body", "origin": [-17, 20, -long_body / 2 + 8], "size": [34, 8, long_body * 0.55],
             "color": dark},
            {"bone": "body", "origin": [-16, 22, -8], "size": [32, 6, 10], "color": (40, 52, 62)},
            {"bone": "turret", "origin": [-6, 28, -2], "size": [12, 6, 12], "color": dark, "pivot": [0, 28, 0]},
            {"bone": "gun", "origin": [-1.5, 30, -18], "size": [3, 3, 16], "color": light,
             "parent": "turret", "pivot": [0, 30, -2]},
        ]
        for index, (x, z) in enumerate(((-22, -20), (16, -20), (-22, 12), (16, 12))):
            boxes.append({"bone": "wheel_" + str(index), "origin": [x, 0, z], "size": [6, 10, 14],
                          "color": track, "pivot": [x + 3, 5, z + 7]})
    elif kind == "tank":
        boxes += [
            {"bone": "body", "origin": [-22, 8, -34], "size": [44, 14, 68], "color": base},
            {"bone": "body", "origin": [-26, 0, -36], "size": [10, 16, 72], "color": track},
            {"bone": "body", "origin": [16, 0, -36], "size": [10, 16, 72], "color": track},
            {"bone": "turret", "origin": [-15, 22, -12], "size": [30, 10, 24], "color": dark, "pivot": [0, 22, 0]},
            {"bone": "barrel", "origin": [-2.5, 25, -52], "size": [5, 5, 42], "color": light,
             "parent": "turret", "pivot": [0, 27, -12]},
        ]
    elif kind == "plane":
        boxes += [
            {"bone": "body", "origin": [-6, 10, -44], "size": [12, 12, 88], "color": base},
            {"bone": "body", "origin": [-52, 12, -12], "size": [104, 3, 24], "color": base},
            {"bone": "body", "origin": [-18, 14, 30], "size": [36, 3, 12], "color": base},
            {"bone": "body", "origin": [-2, 20, 34], "size": [4, 14, 10], "color": dark},
            {"bone": "body", "origin": [-5, 20, -30], "size": [10, 8, 18], "color": (40, 60, 70)},
            {"bone": "propeller", "origin": [-14, 14, -50], "size": [28, 2, 2], "color": light,
             "pivot": [0, 15, -46]},
            {"bone": "propeller", "origin": [-2, 2, -50], "size": [2, 26, 2], "color": light,
             "pivot": [0, 15, -46]},
            {"bone": "body", "origin": [-10, 0, -28], "size": [3, 10, 3], "color": track},
            {"bone": "body", "origin": [7, 0, -28], "size": [3, 10, 3], "color": track},
        ]
    else:  # helicopter
        boxes += [
            {"bone": "body", "origin": [-10, 10, -26], "size": [20, 16, 30], "color": base},
            {"bone": "body", "origin": [-9, 12, -34], "size": [18, 12, 10], "color": (40, 60, 70)},
            {"bone": "body", "origin": [-3, 18, 2], "size": [6, 6, 52], "color": base},
            {"bone": "body", "origin": [-1, 20, 48], "size": [2, 12, 8], "color": dark},
            {"bone": "rotor", "origin": [-64, 30, -2], "size": [128, 2, 5], "color": light,
             "pivot": [0, 31, 0]},
            {"bone": "rotor", "origin": [-3, 30, -64], "size": [6, 2, 128], "color": light,
             "pivot": [0, 31, 0]},
            {"bone": "tail_rotor", "origin": [-1, 20, 50], "size": [2, 22, 3], "color": light,
             "pivot": [0, 31, 51]},
            {"bone": "body", "origin": [-13, 0, -20], "size": [3, 10, 40], "color": track},
            {"bone": "body", "origin": [10, 0, -20], "size": [3, 10, 40], "color": track},
        ]
    return boxes


def vehicle_animations(kind):
    if kind == "plane":
        move = {"loop": True, "animation_length": 0.4, "bones": {
            "propeller": {"rotation": {"0": [0, 0, 0], "0.4": [0, 0, 360]}}}}
    elif kind == "helicopter":
        move = {"loop": True, "animation_length": 0.3, "bones": {
            "rotor": {"rotation": {"0": [0, 360, 0], "0.3": [0, 0, 0]}},
            "tail_rotor": {"rotation": {"0": [0, 0, 0], "0.3": [360, 0, 0]}}}}
    else:
        move = {"loop": True, "animation_length": 0.6, "bones": {
            "body": {"position": {"0": [0, 0, 0], "0.3": [0, 0.25, 0], "0.6": [0, 0, 0]}},
            "wheel_0": {"rotation": {"0": [0, 0, 0], "0.6": [0, 0, 360]}},
            "wheel_1": {"rotation": {"0": [0, 0, 0], "0.6": [0, 0, 360]}},
            "wheel_2": {"rotation": {"0": [0, 0, 0], "0.6": [0, 0, 360]}},
            "wheel_3": {"rotation": {"0": [0, 0, 0], "0.6": [0, 0, 360]}}}}

    fire_bone = "barrel" if kind == "tank" else ("gun" if kind.startswith("armored") else "body")
    return {
        "format_version": "1.8.0",
        "animations": {
            "idle": {"loop": True, "animation_length": 3.0, "bones": {
                "body": {"position": {"0": [0, 0, 0], "1.5": [0, 0.15, 0], "3.0": [0, 0, 0]}}}},
            "move": move,
            "fire": {"loop": False, "animation_length": 0.4, "bones": {
                fire_bone: {"position": {"0": [0, 0, 0], "0.08": [0, 0, 3.5], "0.4": [0, 0, 0]}},
                "body": {"position": {"0": [0, 0, 0], "0.08": [0, 0, 1.2], "0.4": [0, 0, 0]}}}},
        },
    }


def main():
    random.seed(424242)
    alpha = soldier_boxes((74, 92, 66), (58, 72, 52), (196, 160, 122), (44, 42, 40))
    bravo = soldier_boxes((122, 78, 58), (96, 60, 44), (206, 168, 128), (44, 42, 40))
    corpse = soldier_boxes((70, 66, 60), (52, 50, 46), (150, 130, 108), (38, 36, 34))

    for name, boxes in (("soldier_alpha", alpha), ("soldier_bravo", bravo), ("soldier_corpse", corpse)):
        geo, image = build_model("soldier", boxes, 128)
        image.save(os.path.join(ROOT, "textures", "entity", name + ".png"))
    with open(os.path.join(ROOT, "geo", "soldier.geo.json"), "w") as handle:
        json.dump(build_model("soldier", alpha, 128)[0], handle, indent=1)
    with open(os.path.join(ROOT, "animations", "soldier.animation.json"), "w") as handle:
        json.dump(soldier_animations(), handle, indent=1)

    palettes = {
        "armored_car_1": ((78, 88, 62), (56, 64, 46), (110, 120, 92), (34, 34, 36)),
        "armored_car_2": ((110, 100, 78), (84, 76, 58), (140, 130, 104), (34, 34, 36)),
        "plane": ((86, 96, 104), (62, 70, 78), (120, 132, 142), (40, 40, 42)),
        "helicopter": ((70, 78, 60), (52, 58, 44), (100, 110, 84), (36, 36, 38)),
        "tank": ((92, 100, 70), (66, 72, 50), (122, 130, 96), (40, 40, 42)),
    }
    for kind, palette in palettes.items():
        boxes = vehicle_boxes(palette, kind)
        geo, image = build_model(kind, boxes, 256)
        with open(os.path.join(ROOT, "geo", kind + ".geo.json"), "w") as handle:
            json.dump(geo, handle, indent=1)
        with open(os.path.join(ROOT, "animations", kind + ".animation.json"), "w") as handle:
            json.dump(vehicle_animations(kind), handle, indent=1)
        image.save(os.path.join(ROOT, "textures", "entity", kind + ".png"))
    print("entities done")


if __name__ == "__main__":
    main()
