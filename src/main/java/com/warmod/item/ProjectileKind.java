package com.warmod.item;

/** How a weapon delivers damage. */
public enum ProjectileKind {
    /** Hitscan raycast (all firearms + melee). */
    NONE,
    /** RPG style rocket, high explosive. */
    ROCKET,
    /** 40mm under-barrel grenade. */
    GRENADE_40MM,
    /** F-1 fragmentation grenade. */
    GRENADE_FRAG,
    /** Smoke grenade. */
    GRENADE_SMOKE,
    /** C4 satchel, detonated by its own item. */
    C4
}
