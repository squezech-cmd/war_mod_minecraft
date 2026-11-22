package com.warmod.registry;

import com.warmod.Warmod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every sound is an OGG Vorbis file in assets/warmod/sounds/ described by sounds.json.
 * All audio shipped with the repository is a procedural placeholder - see README.
 */
public final class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Warmod.MODID);

    private static final Map<String, RegistryObject<SoundEvent>> BY_NAME = new LinkedHashMap<>();

    /** Register a simple variable-range sound event. */
    public static RegistryObject<SoundEvent> reg(String name) {
        RegistryObject<SoundEvent> obj = SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Warmod.MODID, name)));
        BY_NAME.put(name, obj);
        return obj;
    }

    public static RegistryObject<SoundEvent> event(String name) {
        return BY_NAME.get(name);
    }

    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> obj = BY_NAME.get(name);
        if (obj == null || !obj.isPresent()) {
            Warmod.LOGGER.warn("Unknown sound '{}', falling back to generic explosion", name);
            return SoundEvents.GENERIC_EXPLODE;
        }
        return obj.get();
    }

    public static Map<String, RegistryObject<SoundEvent>> all() {
        return BY_NAME;
    }

    // ---- weapons
    public static final RegistryObject<SoundEvent> GUNSHOT_PISTOL = reg("gunshot_pistol");
    public static final RegistryObject<SoundEvent> GUNSHOT_SMG = reg("gunshot_smg");
    public static final RegistryObject<SoundEvent> GUNSHOT_RIFLE = reg("gunshot_rifle");
    public static final RegistryObject<SoundEvent> GUNSHOT_SNIPER = reg("gunshot_sniper");
    public static final RegistryObject<SoundEvent> GUNSHOT_SHOTGUN = reg("gunshot_shotgun");
    public static final RegistryObject<SoundEvent> GUNSHOT_MG = reg("gunshot_mg");
    public static final RegistryObject<SoundEvent> GUNSHOT_LAUNCHER = reg("gunshot_launcher");
    public static final RegistryObject<SoundEvent> GUNSHOT_50CAL = reg("gunshot_50cal");
    public static final RegistryObject<SoundEvent> RELOAD_RIFLE = reg("reload_rifle");
    public static final RegistryObject<SoundEvent> RELOAD_PISTOL = reg("reload_pistol");
    public static final RegistryObject<SoundEvent> RELOAD_HEAVY = reg("reload_heavy");
    public static final RegistryObject<SoundEvent> DRY_FIRE = reg("dry_fire");
    public static final RegistryObject<SoundEvent> MELEE_SWING = reg("melee_swing");
    public static final RegistryObject<SoundEvent> MELEE_HIT = reg("melee_hit");
    public static final RegistryObject<SoundEvent> BULLET_IMPACT = reg("bullet_impact");
    public static final RegistryObject<SoundEvent> HEADSHOT = reg("headshot");
    public static final RegistryObject<SoundEvent> THROW = reg("throw");

    // ---- explosives
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL = reg("explosion_small");
    public static final RegistryObject<SoundEvent> EXPLOSION_LARGE = reg("explosion_large");
    public static final RegistryObject<SoundEvent> GRENADE_BOUNCE = reg("grenade_bounce");

    // ---- ambience
    public static final RegistryObject<SoundEvent> AMBIENT_FAR_GUNFIRE = reg("ambient_far_gunfire");
    public static final RegistryObject<SoundEvent> AMBIENT_JET_PASS = reg("ambient_jet_pass");
    public static final RegistryObject<SoundEvent> AMBIENT_SIREN = reg("ambient_siren");
    public static final RegistryObject<SoundEvent> AMBIENT_EXPLOSION = reg("ambient_explosion");
    public static final RegistryObject<SoundEvent> AMBIENT_DRONE = reg("ambient_drone");
    public static final RegistryObject<SoundEvent> MUSIC_BATTLEFIELD = reg("music_battlefield");

    // ---- vehicles
    public static final RegistryObject<SoundEvent> ENGINE_CAR = reg("engine_car");
    public static final RegistryObject<SoundEvent> ENGINE_PLANE = reg("engine_plane");
    public static final RegistryObject<SoundEvent> ENGINE_HELI = reg("engine_heli");
    public static final RegistryObject<SoundEvent> ENGINE_TANK = reg("engine_tank");
    public static final RegistryObject<SoundEvent> TANK_CANNON = reg("tank_cannon");
    public static final RegistryObject<SoundEvent> VEHICLE_HIT = reg("vehicle_hit");
    public static final RegistryObject<SoundEvent> VEHICLE_DESTROY = reg("vehicle_destroy");

    // ---- ui
    public static final RegistryObject<SoundEvent> UI_CLICK = reg("ui_click");
    public static final RegistryObject<SoundEvent> DEPLOY = reg("deploy");
}
