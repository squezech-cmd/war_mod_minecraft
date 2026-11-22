package com.warmod.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The full weapon catalogue. 32 weapons, each one gets its own GeckoLib model, animation set,
 * icon, sounds and ballistics.
 */
public final class GunRegistry {
    private static final List<GunDefinition> ALL_LIST = new ArrayList<>();
    private static final Map<String, GunDefinition> BY_ID = new LinkedHashMap<>();

    static {
        // ------------------------------------------------------------------ assault rifles
        auto("ak47", AmmoType.AMMO_762, 30, 8.0F, 1.6F, 1.15F, 0.40F, 55, 600, 90, ScopeType.NONE, "gunshot_rifle", "reload_rifle");
        auto("ak74", AmmoType.AMMO_556, 30, 7.0F, 1.4F, 0.95F, 0.32F, 52, 650, 95, ScopeType.RIFLE, "gunshot_rifle", "reload_rifle");
        burst("m4a1", AmmoType.AMMO_556, 30, 7.0F, 1.0F, 0.70F, 0.25F, 50, 750, 100, ScopeType.RIFLE, "gunshot_rifle", "reload_rifle");
        semi("m16a4", AmmoType.AMMO_556, 30, 7.5F, 1.1F, 0.80F, 0.25F, 52, 700, 110, ScopeType.ACOG, "gunshot_rifle", "reload_rifle");
        auto("scar_l", AmmoType.AMMO_762, 20, 8.5F, 1.2F, 1.00F, 0.30F, 55, 600, 100, ScopeType.ACOG, "gunshot_rifle", "reload_rifle");
        auto("aug", AmmoType.AMMO_556, 30, 7.2F, 1.2F, 0.80F, 0.30F, 54, 680, 95, ScopeType.RIFLE, "gunshot_rifle", "reload_rifle");
        auto("g36c", AmmoType.AMMO_556, 30, 7.0F, 1.3F, 0.85F, 0.30F, 52, 700, 95, ScopeType.RIFLE, "gunshot_rifle", "reload_rifle");

        // ------------------------------------------------------------------ submachine guns
        auto("mp5", AmmoType.AMMO_9MM, 30, 5.5F, 1.6F, 0.55F, 0.35F, 45, 800, 60, ScopeType.RED_DOT, "gunshot_smg", "reload_pistol");
        auto("mp7", AmmoType.AMMO_9MM, 40, 5.0F, 1.7F, 0.50F, 0.40F, 44, 900, 55, ScopeType.RED_DOT, "gunshot_smg", "reload_pistol");
        auto("ump45", AmmoType.AMMO_9MM, 25, 6.5F, 1.5F, 0.70F, 0.35F, 46, 620, 60, ScopeType.RED_DOT, "gunshot_smg", "reload_pistol");
        auto("p90", AmmoType.AMMO_9MM, 50, 4.8F, 1.9F, 0.45F, 0.40F, 48, 900, 55, ScopeType.RED_DOT, "gunshot_smg", "reload_pistol");

        // ------------------------------------------------------------------ sniper rifles
        sniper("awp", AmmoType.AMMO_762, 10, 26.0F, 0.05F, 3.5F, 0.9F, 70, 45, 250, ScopeType.SNIPER, "gunshot_sniper", "reload_rifle");
        sniper("m24", AmmoType.AMMO_762, 5, 24.0F, 0.05F, 3.2F, 0.8F, 65, 50, 240, ScopeType.SNIPER, "gunshot_sniper", "reload_rifle");
        sniper("barrett_m82", AmmoType.AMMO_50, 10, 38.0F, 0.08F, 4.5F, 1.2F, 80, 40, 300, ScopeType.SNIPER, "gunshot_50cal", "reload_heavy");
        sniper("svd", AmmoType.AMMO_762, 10, 18.0F, 0.15F, 2.2F, 0.6F, 62, 90, 200, ScopeType.ACOG, "gunshot_sniper", "reload_rifle");

        // ------------------------------------------------------------------ shotguns (8 pellets)
        shotgun("remington_870", 6, 2.6F, 6.0F, 3.0F, 1.4F, 70, 65, 28, ScopeType.NONE, false);
        shotgun("saiga_12", 8, 2.2F, 5.0F, 2.4F, 1.2F, 68, 180, 26, ScopeType.RED_DOT, true);
        shotgun("spas_12", 7, 2.4F, 5.5F, 2.6F, 1.3F, 72, 120, 27, ScopeType.NONE, true);

        // ------------------------------------------------------------------ pistols
        pistol("glock_17", AmmoType.AMMO_9MM, 17, 5.5F, 1.4F, 0.55F, 0.30F, 35, 400, 55, ScopeType.NONE, "gunshot_pistol");
        pistol("desert_eagle", AmmoType.AMMO_50, 7, 12.0F, 1.8F, 1.80F, 0.60F, 42, 200, 70, ScopeType.RED_DOT, "gunshot_50cal");
        pistol("m1911", AmmoType.AMMO_9MM, 7, 8.0F, 1.5F, 1.20F, 0.40F, 38, 300, 60, ScopeType.NONE, "gunshot_pistol");
        pistol("usp", AmmoType.AMMO_9MM, 12, 6.0F, 1.5F, 0.80F, 0.35F, 36, 350, 58, ScopeType.RED_DOT, "gunshot_pistol");

        // ------------------------------------------------------------------ machine guns
        mg("m249", AmmoType.AMMO_556, 100, 7.0F, 2.2F, 0.90F, 0.60F, 160, 750, 110, ScopeType.ACOG, "gunshot_mg");
        mg("pkm", AmmoType.AMMO_762, 100, 9.0F, 2.6F, 1.60F, 0.70F, 170, 650, 120, ScopeType.ACOG, "gunshot_mg");
        mg("minigun", AmmoType.AMMO_762, 200, 5.5F, 4.5F, 1.00F, 1.20F, 200, 1200, 80, ScopeType.NONE, "gunshot_mg");

        // ------------------------------------------------------------------ launchers
        launcher("rpg7", AmmoType.AMMO_ROCKET, 1, 0.0F, 0.5F, 4.0F, 1.0F, 90, 30, 200, ProjectileKind.ROCKET, 4.0F, "gunshot_launcher", "reload_heavy");
        launcher("m320", AmmoType.AMMO_40MM, 1, 0.0F, 0.8F, 2.5F, 0.8F, 60, 40, 120, ProjectileKind.GRENADE_40MM, 2.5F, "gunshot_launcher", "reload_heavy");

        // ------------------------------------------------------------------ melee
        melee("knife", 8.0F, 3.0F, 10, "melee_swing");
        melee("machete", 11.0F, 3.2F, 15, "melee_swing");

        // ------------------------------------------------------------------ throwables
        throwable("grenade_f1", ProjectileKind.GRENADE_FRAG, 3.5F, 55, "gunshot_launcher");
        throwable("smoke_grenade", ProjectileKind.GRENADE_SMOKE, 0.0F, 40, "gunshot_launcher");
        throwable("c4", ProjectileKind.C4, 5.0F, 80, "gunshot_launcher");
    }

    private GunRegistry() {}

    private static void put(GunDefinition def) {
        if (BY_ID.containsKey(def.id())) {
            throw new IllegalStateException("Duplicate weapon id: " + def.id());
        }
        ALL_LIST.add(def);
        BY_ID.put(def.id(), def);
    }

    private static void auto(String id, AmmoType ammo, int mag, float dmg, float spread, float rp, float ry,
                             int reload, int rpm, float range, ScopeType scope, String fire, String reloadSnd) {
        put(new GunDefinition(id, GunCategory.ASSAULT, ammo, mag, dmg, spread, rp, ry, reload,
                List.of(FireMode.AUTO, FireMode.SEMI, FireMode.BURST), rpm, range, scope, 1, fire, reloadSnd,
                ProjectileKind.NONE, 0.0F));
    }

    private static void burst(String id, AmmoType ammo, int mag, float dmg, float spread, float rp, float ry,
                              int reload, int rpm, float range, ScopeType scope, String fire, String reloadSnd) {
        put(new GunDefinition(id, GunCategory.ASSAULT, ammo, mag, dmg, spread, rp, ry, reload,
                List.of(FireMode.AUTO, FireMode.SEMI, FireMode.BURST), rpm, range, scope, 1, fire, reloadSnd,
                ProjectileKind.NONE, 0.0F));
    }

    private static void semi(String id, AmmoType ammo, int mag, float dmg, float spread, float rp, float ry,
                             int reload, int rpm, float range, ScopeType scope, String fire, String reloadSnd) {
        put(new GunDefinition(id, GunCategory.ASSAULT, ammo, mag, dmg, spread, rp, ry, reload,
                List.of(FireMode.SEMI, FireMode.BURST), rpm, range, scope, 1, fire, reloadSnd,
                ProjectileKind.NONE, 0.0F));
    }

    private static void sniper(String id, AmmoType ammo, int mag, float dmg, float spread, float rp, float ry,
                               int reload, int rpm, float range, ScopeType scope, String fire, String reloadSnd) {
        put(new GunDefinition(id, GunCategory.SNIPER, ammo, mag, dmg, spread, rp, ry, reload,
                List.of(FireMode.SEMI), rpm, range, scope, 1, fire, reloadSnd, ProjectileKind.NONE, 0.0F));
    }

    private static void shotgun(String id, int mag, float pelletDmg, float spread, float rp, float ry,
                                int reload, int rpm, float range, ScopeType scope, boolean auto) {
        put(new GunDefinition(id, GunCategory.SHOTGUN, AmmoType.AMMO_12G, mag, pelletDmg, spread, rp, ry, reload,
                auto ? List.of(FireMode.AUTO, FireMode.SEMI) : List.of(FireMode.SEMI),
                rpm, range, scope, 8, "gunshot_shotgun", "reload_rifle", ProjectileKind.NONE, 0.0F));
    }

    private static void pistol(String id, AmmoType ammo, int mag, float dmg, float spread, float rp, float ry,
                               int reload, int rpm, float range, ScopeType scope, String fire) {
        put(new GunDefinition(id, GunCategory.PISTOL, ammo, mag, dmg, spread, rp, ry, reload,
                List.of(FireMode.SEMI), rpm, range, scope, 1, fire, "reload_pistol", ProjectileKind.NONE, 0.0F));
    }

    private static void mg(String id, AmmoType ammo, int mag, float dmg, float spread, float rp, float ry,
                           int reload, int rpm, float range, ScopeType scope, String fire) {
        put(new GunDefinition(id, GunCategory.MG, ammo, mag, dmg, spread, rp, ry, reload,
                List.of(FireMode.AUTO), rpm, range, scope, 1, fire, "reload_heavy", ProjectileKind.NONE, 0.0F));
    }

    private static void launcher(String id, AmmoType ammo, int mag, float dmg, float spread, float rp, float ry,
                                 int reload, int rpm, float range, ProjectileKind kind, float power,
                                 String fire, String reloadSnd) {
        put(new GunDefinition(id, GunCategory.LAUNCHER, ammo, mag, dmg, spread, rp, ry, reload,
                List.of(FireMode.SEMI), rpm, range, ScopeType.NONE, 1, fire, reloadSnd, kind, power));
    }

    private static void melee(String id, float dmg, float range, int rpm, String sound) {
        put(new GunDefinition(id, GunCategory.MELEE, AmmoType.NONE, 1, dmg, 0.0F, 0.4F, 0.2F, 0,
                List.of(FireMode.SEMI), rpm, range, ScopeType.NONE, 1, sound, sound, ProjectileKind.NONE, 0.0F));
    }

    private static void throwable(String id, ProjectileKind kind, float power, int rpm, String sound) {
        put(new GunDefinition(id, GunCategory.THROWABLE, AmmoType.NONE, 1, 0.0F, 0.0F, 0.2F, 0.1F, 0,
                List.of(FireMode.SEMI), rpm, 24.0F, ScopeType.NONE, 1, sound, sound, kind, power));
    }

    public static List<GunDefinition> all() {
        return Collections.unmodifiableList(ALL_LIST);
    }

    public static GunDefinition byId(String id) {
        return BY_ID.get(id);
    }

    public static List<GunDefinition> byCategory(GunCategory category) {
        List<GunDefinition> out = new ArrayList<>();
        for (GunDefinition def : ALL_LIST) {
            if (def.category() == category) {
                out.add(def);
            }
        }
        return out;
    }

    /** Weapons an AI soldier can be equipped with. */
    public static List<GunDefinition> firearmPool() {
        List<GunDefinition> out = new ArrayList<>();
        for (GunDefinition def : ALL_LIST) {
            if (def.category().isFirearm()) {
                out.add(def);
            }
        }
        return out;
    }
}
