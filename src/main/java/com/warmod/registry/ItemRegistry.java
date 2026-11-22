package com.warmod.registry;

import com.warmod.Warmod;
import com.warmod.item.AmmoItem;
import com.warmod.item.AmmoType;
import com.warmod.item.GunDefinition;
import com.warmod.item.GunItem;
import com.warmod.item.GunRegistry;
import com.warmod.item.RadioItem;
import com.warmod.item.SpawnerItem;
import com.warmod.item.VehicleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Warmod.MODID);

    public static final Map<String, RegistryObject<Item>> GUNS = new LinkedHashMap<>();
    public static final Map<AmmoType, RegistryObject<Item>> AMMO = new EnumMap<>(AmmoType.class);
    public static final Map<VehicleType, RegistryObject<Item>> VEHICLE_KEYS = new EnumMap<>(VehicleType.class);

    static {
        for (GunDefinition def : GunRegistry.all()) {
            GUNS.put(def.id(), ITEMS.register(def.id(),
                    () -> new GunItem(def)));
        }
        for (AmmoType type : AmmoType.values()) {
            if (type == AmmoType.NONE) {
                continue;
            }
            AMMO.put(type, ITEMS.register("ammo_" + type.key(),
                    () -> new AmmoItem(type, new Item.Properties().stacksTo(64))));
        }
        for (VehicleType type : VehicleType.values()) {
            VEHICLE_KEYS.put(type, ITEMS.register(type.id() + "_key",
                    () -> new SpawnerItem(type)));
        }
    }

    public static final RegistryObject<Item> RADIO = ITEMS.register("commander_radio",
            () -> new RadioItem());

    private ItemRegistry() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static Item gun(String id) {
        RegistryObject<Item> obj = GUNS.get(id);
        return obj == null ? null : obj.get();
    }

    public static Item ammo(AmmoType type) {
        RegistryObject<Item> obj = AMMO.get(type);
        return obj == null ? null : obj.get();
    }

    public static Item vehicleKey(VehicleType type) {
        RegistryObject<Item> obj = VEHICLE_KEYS.get(type);
        return obj == null ? null : obj.get();
    }

    public static Rarity gunRarity(GunDefinition def) {
        return switch (def.category()) {
            case LAUNCHER, MG, SNIPER -> Rarity.RARE;
            case MELEE, THROWABLE -> Rarity.COMMON;
            default -> Rarity.UNCOMMON;
        };
    }
}
