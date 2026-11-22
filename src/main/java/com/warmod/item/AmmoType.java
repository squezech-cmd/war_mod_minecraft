package com.warmod.item;

/**
 * Calibers. Every non-melee/non-throwable weapon consumes ammo items of exactly one caliber.
 */
public enum AmmoType {
    AMMO_556("556"),
    AMMO_762("762"),
    AMMO_9MM("9mm"),
    AMMO_50("50"),
    AMMO_12G("12g"),
    AMMO_40MM("40mm"),
    AMMO_ROCKET("rocket"),
    AMMO_SHELL("shell"),
    NONE("none");

    private final String key;

    AmmoType(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public static AmmoType byOrdinal(int ordinal) {
        AmmoType[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
