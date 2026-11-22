#!/usr/bin/env python3
"""Generates the JSON resources: sounds.json, item/block models, blockstates, lang files,
dimension data and crafting recipes, plus the remaining placeholder icons."""
import json
import os
import random

from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources")
ASSETS = os.path.join(BASE, "assets", "warmod")
DATA = os.path.join(BASE, "data", "warmod")

for sub in ("models/item", "models/block", "blockstates", "textures/item", "textures/block", "lang"):
    os.makedirs(os.path.join(ASSETS, sub), exist_ok=True)
for sub in ("dimension", "dimension_type", "worldgen/biome", "recipes", "tags/blocks/mineable"):
    os.makedirs(os.path.join(DATA, sub), exist_ok=True)

WEAPONS = [
    # id, ru, en, category, ammo
    ("ak47", "АК-47", "AK-47", "assault", "762"),
    ("ak74", "АК-74", "AK-74", "assault", "556"),
    ("m4a1", "M4A1", "M4A1", "assault", "556"),
    ("m16a4", "M16A4", "M16A4", "assault", "556"),
    ("scar_l", "SCAR-L", "SCAR-L", "assault", "762"),
    ("aug", "Steyr AUG", "Steyr AUG", "assault", "556"),
    ("g36c", "G36C", "G36C", "assault", "556"),
    ("mp5", "MP5", "MP5", "smg", "9mm"),
    ("mp7", "MP7", "MP7", "smg", "9mm"),
    ("ump45", "UMP-45", "UMP-45", "smg", "9mm"),
    ("p90", "P90", "P90", "smg", "9mm"),
    ("awp", "AWP", "AWP", "sniper", "762"),
    ("m24", "M24", "M24", "sniper", "762"),
    ("barrett_m82", "Barrett M82", "Barrett M82", "sniper", "50"),
    ("svd", "СВД", "SVD", "sniper", "762"),
    ("remington_870", "Remington 870", "Remington 870", "shotgun", "12g"),
    ("saiga_12", "Сайга-12", "Saiga-12", "shotgun", "12g"),
    ("spas_12", "SPAS-12", "SPAS-12", "shotgun", "12g"),
    ("glock_17", "Glock 17", "Glock 17", "pistol", "9mm"),
    ("desert_eagle", "Desert Eagle", "Desert Eagle", "pistol", "50"),
    ("m1911", "M1911", "M1911", "pistol", "9mm"),
    ("usp", "USP", "USP", "pistol", "9mm"),
    ("m249", "M249", "M249", "mg", "556"),
    ("pkm", "ПКМ", "PKM", "mg", "762"),
    ("minigun", "Миниган", "Minigun", "mg", "762"),
    ("rpg7", "РПГ-7", "RPG-7", "launcher", "rocket"),
    ("m320", "M320", "M320", "launcher", "40mm"),
    ("knife", "Нож", "Knife", "melee", ""),
    ("machete", "Мачете", "Machete", "melee", ""),
    ("grenade_f1", "Граната Ф-1", "F-1 Grenade", "throwable", ""),
    ("smoke_grenade", "Дымовая граната", "Smoke Grenade", "throwable", ""),
    ("c4", "C4", "C4", "throwable", ""),
]

AMMO = [("556", "5.56×45 мм", "5.56x45mm"), ("762", "7.62×39 мм", "7.62x39mm"),
        ("9mm", "9×19 мм", "9x19mm"), ("50", ".50 BMG", ".50 BMG"),
        ("12g", "12 калибр", "12 gauge"), ("40mm", "40 мм", "40mm"),
        ("rocket", "Ракета ПГ-7", "PG-7 Rocket"), ("shell", "Снаряд 125 мм", "125mm Shell")]

VEHICLES = [("armored_car_1", "Бронеавтомобиль «Тип-1»", "Armored Car Type-1"),
            ("armored_car_2", "Бронеавтомобиль «Тип-2»", "Armored Car Type-2"),
            ("plane", "Штурмовик", "Attack Plane"),
            ("helicopter", "Вертолёт", "Helicopter"),
            ("tank", "Танк", "Tank")]

SOUND_CATEGORIES = {
    "music_battlefield": "music",
    "ambient_far_gunfire": "ambient", "ambient_jet_pass": "ambient", "ambient_siren": "ambient",
    "ambient_explosion": "ambient", "ambient_drone": "ambient",
    "engine_car": "neutral", "engine_plane": "neutral", "engine_heli": "neutral", "engine_tank": "neutral",
    "ui_click": "master", "deploy": "player",
}


def write(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)


def gen_sounds_json():
    sounds = {}
    folder = os.path.join(ASSETS, "sounds")
    for file in sorted(os.listdir(folder)):
        if not file.endswith(".ogg"):
            continue
        name = file[:-4]
        category = SOUND_CATEGORIES.get(name, "player")
        entry = {"name": "warmod:" + name, "stream": name == "music_battlefield"}
        sounds[name] = {"category": category, "sounds": [entry]}
    write(os.path.join(ASSETS, "sounds.json"), sounds)


def make_icon(draw_fn, color=(70, 74, 80)):
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw_fn(draw, image, color)
    return image


def ammo_icon(label, color):
    def painter(draw, image, _):
        draw.rounded_rectangle([9, 6, 23, 26], radius=2, fill=color + (255,), outline=(24, 24, 26, 255))
        draw.rectangle([12, 3, 20, 8], fill=(196, 160, 74, 255))
        draw.line([16, 9, 16, 24], fill=(30, 30, 32, 255))
        draw.text((10, 13), label, fill=(240, 236, 210, 255))
    return painter


def key_icon(color):
    def painter(draw, image, _):
        draw.rounded_rectangle([6, 12, 20, 22], radius=3, fill=color + (255,), outline=(20, 20, 22, 255))
        draw.rectangle([20, 15, 26, 19], fill=(180, 180, 186, 255))
        draw.ellipse([9, 15, 13, 19], fill=(30, 30, 32, 255))
        draw.rectangle([8, 24, 24, 26], fill=(60, 60, 64, 255))
    return painter


def radio_icon(draw, image, _):
    draw.rounded_rectangle([6, 8, 26, 26], radius=2, fill=(56, 66, 52, 255), outline=(20, 20, 22, 255))
    draw.rectangle([9, 11, 17, 17], fill=(26, 28, 26, 255))
    draw.rectangle([19, 11, 23, 17], fill=(120, 160, 120, 255))
    draw.line([16, 4, 16, 9], fill=(180, 180, 186, 255), width=2)
    draw.ellipse([20, 19, 24, 23], fill=(160, 60, 50, 255))


def crate_texture():
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, 31, 31], fill=(78, 84, 62, 255))
    draw.rectangle([0, 0, 31, 4], fill=(98, 104, 78, 255))
    draw.rectangle([0, 27, 31, 31], fill=(52, 56, 42, 255))
    draw.rectangle([2, 8, 29, 24], outline=(36, 38, 30, 255), width=2)
    draw.line([3, 9, 28, 23], fill=(140, 120, 60, 255), width=1)
    draw.line([3, 23, 28, 9], fill=(140, 120, 60, 255), width=1)
    draw.text((11, 13), "AMMO", fill=(226, 214, 170, 255))
    return image


# Hand/first-person placement of the GeckoLib weapon models. Vanilla still applies these transforms for
# "builtin/entity" models, so the guns are scaled down and pulled into the lower right corner of the screen.
GUN_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 0], "translation": [0.0, 3.0, 1.0], "scale": [0.45, 0.45, 0.45]},
    "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [0.0, 3.0, 1.0], "scale": [0.45, 0.45, 0.45]},
    "firstperson_righthand": {"rotation": [0, -90, 0], "translation": [1.0, 2.0, 1.0], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, 90, 0], "translation": [1.0, 2.0, 1.0], "scale": [0.5, 0.5, 0.5]},
    "ground": {"rotation": [0, 0, 0], "translation": [0.0, 2.0, 0.0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [0, 0, 0], "translation": [0.0, 0.0, 0.0], "scale": [0.62, 0.62, 0.62]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0.0, 0.0, 0.0], "scale": [0.5, 0.5, 0.5]},
}


def gen_models():
    for gun_id, _, _, _, _ in WEAPONS:
        write(os.path.join(ASSETS, "models", "item", gun_id + ".json"),
              {"parent": "builtin/entity",
               "textures": {"particle": "warmod:item/" + gun_id},
               "display": GUN_DISPLAY})
    for key, _, _ in AMMO:
        write(os.path.join(ASSETS, "models", "item", "ammo_" + key + ".json"),
              {"parent": "item/generated", "textures": {"layer0": "warmod:item/ammo_" + key}})
        make_icon(ammo_icon(key.upper(), (86, 92, 70))).save(os.path.join(ASSETS, "textures", "item", "ammo_" + key + ".png"))
    for vid, _, _ in VEHICLES:
        write(os.path.join(ASSETS, "models", "item", vid + "_key.json"),
              {"parent": "item/generated", "textures": {"layer0": "warmod:item/" + vid + "_key"}})
        make_icon(key_icon((96, 104, 74))).save(os.path.join(ASSETS, "textures", "item", vid + "_key.png"))
    write(os.path.join(ASSETS, "models", "item", "commander_radio.json"),
          {"parent": "item/generated", "textures": {"layer0": "warmod:item/commander_radio"}})
    make_icon(radio_icon).save(os.path.join(ASSETS, "textures", "item", "commander_radio.png"))

    write(os.path.join(ASSETS, "models", "block", "ammo_crate.json"),
          {"parent": "block/cube_all", "textures": {"all": "warmod:block/ammo_crate"}})
    write(os.path.join(ASSETS, "models", "item", "ammo_crate.json"),
          {"parent": "warmod:block/ammo_crate"})
    write(os.path.join(ASSETS, "blockstates", "ammo_crate.json"),
          {"variants": {"": {"model": "warmod:block/ammo_crate"}}})
    crate_texture().save(os.path.join(ASSETS, "textures", "block", "ammo_crate.png"))


def gen_lang():
    def build(lang):
        ru = lang == "ru"
        data = {}

        def add(key, ru_text, en_text):
            data[key] = ru_text if ru else en_text

        add("itemGroup.warmod", "War Mod — Война", "War Mod - Warfare")
        for gun_id, ru_name, en_name, category, ammo in WEAPONS:
            add("item.warmod." + gun_id, ru_name, en_name)
            add("warmod.tooltip.category." + category,
                {"assault": "Штурмовая винтовка", "smg": "Пистолет-пулемёт", "sniper": "Снайперская винтовка",
                 "shotgun": "Дробовик", "pistol": "Пистолет", "mg": "Пулемёт", "launcher": "Гранатомёт",
                 "melee": "Ближний бой", "throwable": "Метательное"}[category],
                {"assault": "Assault rifle", "smg": "Submachine gun", "sniper": "Sniper rifle",
                 "shotgun": "Shotgun", "pistol": "Pistol", "mg": "Machine gun", "launcher": "Launcher",
                 "melee": "Melee", "throwable": "Throwable"}[category])
            add("warmod.tooltip.hint." + category,
                {"assault": "ЛКМ — огонь, ПКМ — прицел, R — перезарядка, B — режим огня",
                 "smg": "ЛКМ — огонь, ПКМ — прицел, R — перезарядка",
                 "sniper": "ПКМ — оптика, ЛКМ — выстрел",
                 "shotgun": "ЛКМ — выстрел, R — перезарядка",
                 "pistol": "ЛКМ — огонь, R — перезарядка",
                 "mg": "ЛКМ — длинная очередь",
                 "launcher": "ЛКМ — пуск",
                 "melee": "ЛКМ — удар",
                 "throwable": "ПКМ — бросок"}[category],
                {"assault": "LMB fire, RMB aim, R reload, B fire mode", "smg": "LMB fire, RMB aim, R reload",
                 "sniper": "RMB scope, LMB shoot", "shotgun": "LMB shoot, R reload",
                 "pistol": "LMB fire, R reload", "mg": "LMB sustained fire", "launcher": "LMB launch",
                 "melee": "LMB strike", "throwable": "RMB throw"}[category])
        for key, ru_name, en_name in AMMO:
            add("item.warmod.ammo_" + key, ru_name, en_name)
        for vid, ru_name, en_name in VEHICLES:
            add("item.warmod." + vid + "_key", "Ключ: " + ru_name, "Key: " + en_name)
        add("item.warmod.commander_radio", "Рация командира", "Commander Radio")
        add("block.warmod.ammo_crate", "Ящик боеприпасов", "Ammo Crate")

        add("warmod.tooltip.ammo", "Калибр: %s · магазин %s", "Caliber: %s - magazine %s")
        add("warmod.tooltip.damage", "Урон: %s", "Damage: %s")
        add("warmod.tooltip.firemode", "Режим: %s", "Mode: %s")
        add("warmod.tooltip.vehicle_key", "ПКМ по блоку — развернуть технику", "RMB on a block to deploy")
        add("warmod.tooltip.radio", "ПКМ — управление отрядом", "RMB to manage your squad")
        for vid, ru_name, en_name in VEHICLES:
            add("warmod.tooltip.vehicle." + vid, ru_name, en_name)
        add("warmod.ammo.caliber", "Калибр: %s", "Caliber: %s")

        add("warmod.firemode.auto", "АВТО", "AUTO")
        add("warmod.firemode.semi", "ОДИНОЧНЫЙ", "SEMI")
        add("warmod.firemode.burst", "ОЧЕРЕДЬ", "BURST")

        add("warmod.faction.alpha", "Альфа", "Alpha")
        add("warmod.faction.bravo", "Браво", "Bravo")

        add("warmod.order.follow", "Следовать за мной", "Follow me")
        add("warmod.order.hold", "Держать позицию", "Hold position")
        add("warmod.order.attack", "Атаковать точку", "Attack position")
        add("warmod.order.retreat", "Отступать", "Retreat")
        add("warmod.order.patrol", "Патруль", "Patrol")

        messages = {
            "team_exists": ("Команда «%s» уже существует", "Team '%s' already exists"),
            "already_in_team": ("Вы уже в команде", "You are already in a team"),
            "team_created": ("Команда «%s» создана, фракция %s", "Team '%s' created, faction %s"),
            "target_in_team": ("Игрок уже в команде", "Player is already in a team"),
            "invited": ("Приглашение отправлено: %s", "Invitation sent to %s"),
            "invite_received": ("Вас пригласили в «%s» (от %s). /warmod team accept %s", "%s invited you to '%s'. /warmod team accept %s"),
            "no_invite": ("Нет приглашения в эту команду", "No invitation for that team"),
            "joined": ("Вы вступили в команду «%s»", "Joined team '%s'"),
            "no_team": ("Вы не в команде. /warmod team create <имя> <alpha|bravo>", "No team. /warmod team create <name> <alpha|bravo>"),
            "left_team": ("Вы покинули команду", "You left the team"),
            "bot_limit": ("Достигнут лимит ботов: %s", "Bot limit reached: %s"),
            "bots_spawned": ("Развёрнуто бойцов: %s", "Soldiers deployed: %s"),
            "no_dimension": ("Измерение battlefield недоступно", "Battlefield dimension unavailable"),
            "joined_battlefield": ("Переброска на поле боя", "Deployed to the battlefield"),
            "left_battlefield": ("Вы покинули поле боя", "You left the battlefield"),
            "order_given": ("Приказ «%s» отдан %s бойцам", "Order '%s' given to %s soldiers"),
            "bots_equipped": ("%s бойцов перевооружены: %s", "%s soldiers re-equipped: %s"),
            "bots_dismissed": ("Отряд расформирован (%s)", "Squad dismissed (%s)"),
            "player_not_found": ("Игрок не найден: %s", "Player not found: %s"),
            "unknown_vehicle": ("Неизвестная техника: %s", "Unknown vehicle: %s"),
            "vehicle_spawned": ("Развёрнуто: %s", "Deployed: %s"),
            "unknown_order": ("Неизвестный приказ: %s", "Unknown order: %s"),
            "team_info": ("«%s» [%s] убийств %s / смертей %s, участников %s",
                          "'%s' [%s] kills %s / deaths %s, members %s"),
            "crate_refill": ("Магазин пополнен: %s", "Magazine refilled: %s"),
        }
        for key, (ru_text, en_text) in messages.items():
            add("warmod.msg." + key, ru_text, en_text)

        add("warmod.death.title", "ПОГИБ.", "K.I.A.")
        add("warmod.death.kills", "УБИЙСТВ", "KILLS")
        add("warmod.death.deaths", "ВОЗРОЖДЕНИЙ", "RESPAWNS")
        add("warmod.death.respawn", "Вернуться в бой", "Return to battle")
        add("warmod.death.respawn_countdown", "Вернуться в бой (%s)", "Return to battle (%s)")
        add("warmod.death.leave", "Покинуть поле боя", "Leave the battlefield")

        add("warmod.radio.title", "РАЦИЯ КОМАНДИРА", "COMMANDER RADIO")
        add("warmod.radio.spawn", "Заспавнить бойцов: %s", "Spawn soldiers: %s")
        add("warmod.radio.equip", "Сменить оружие: %s", "Change weapon: %s")
        add("warmod.radio.dismiss", "Расформировать отряд", "Dismiss squad")
        add("warmod.radio.status", "«%s» · ботов %s · убийств %s", "'%s' - bots %s - kills %s")

        add("key.categories.warmod", "War Mod", "War Mod")
        add("key.warmod.reload", "Перезарядка", "Reload")
        add("key.warmod.firemode", "Режим огня", "Fire mode")
        add("key.warmod.vehicle_camera", "Камера техники", "Vehicle camera")
        add("key.warmod.vehicle_exit", "Выйти из техники", "Exit vehicle")
        return data

    write(os.path.join(ASSETS, "lang", "ru_ru.json"), build("ru"))
    write(os.path.join(ASSETS, "lang", "en_us.json"), build("en"))


def gen_data():
    write(os.path.join(DATA, "dimension_type", "battlefield.json"), {
        "ultrawarm": False,
        "natural": False,
        "piglin_safe": False,
        "respawn_anchor_works": False,
        "bed_works": False,
        "has_raids": False,
        "has_skylight": True,
        "has_ceiling": False,
        "coordinate_scale": 1.0,
        "ambient_light": 0.08,
        "monster_spawn_block_light_limit": 0,
        "monster_spawn_light_level": 0,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "logical_height": 256,
        "min_y": 0,
        "height": 256,
        "effects": "warmod:battlefield",
        "fixed_time": 18000,
    })

    write(os.path.join(DATA, "dimension", "battlefield.json"), {
        "type": "warmod:battlefield",
        "generator": {
            "type": "warmod:battlefield",
            "biome_source": {"type": "minecraft:fixed", "biome": "warmod:ruined_city"},
            "settings": "minecraft:overworld",
        },
    })

    write(os.path.join(DATA, "worldgen", "biome", "ruined_city.json"), {
        "temperature": 2.0,
        "downfall": 0.0,
        "has_precipitation": False,
        "effects": {
            "sky_color": 1710632,
            "fog_color": 1710632,
            "water_color": 4159204,
            "water_fog_color": 329011,
            "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000,
                           "block_search_extent": 8, "offset": 2.0},
        },
        "spawners": {
            "monster": [], "creature": [], "ambient": [], "axolotls": [],
            "underground_water_creature": [], "water_creature": [],
            "water_ambient": [], "misc": [],
        },
        "spawn_costs": {},
        "carvers": {},
        "features": [],
    })

    write(os.path.join(DATA, "recipes", "ammo_crate.json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": ["III", "ICI", "III"],
        "key": {"I": {"item": "minecraft:iron_ingot"}, "C": {"item": "minecraft:chest"}},
        "result": {"item": "warmod:ammo_crate"},
    })
    write(os.path.join(DATA, "recipes", "commander_radio.json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": ["IRI", "RGR", "IRI"],
        "key": {"I": {"item": "minecraft:iron_ingot"}, "R": {"item": "minecraft:redstone"},
                "G": {"item": "minecraft:gold_ingot"}},
        "result": {"item": "warmod:commander_radio"},
    })
    write(os.path.join(DATA, "recipes", "ammo_556.json"), {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:iron_nugget"}, {"item": "minecraft:iron_nugget"},
                        {"item": "minecraft:gunpowder"}],
        "result": {"item": "warmod:ammo_556", "count": 8},
    })
    write(os.path.join(DATA, "recipes", "ammo_762.json"), {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": "minecraft:iron_nugget"}, {"item": "minecraft:iron_nugget"},
                        {"item": "minecraft:gunpowder"}],
        "result": {"item": "warmod:ammo_762", "count": 6},
    })
    write(os.path.join(DATA, "tags", "blocks", "mineable", "pickaxe.json"),
          {"replace": False, "values": ["warmod:ammo_crate"]})


def main():
    random.seed(7)
    gen_sounds_json()
    gen_models()
    gen_lang()
    gen_data()
    print("resources done")


if __name__ == "__main__":
    main()
