package com.warmod.item;

public enum GunCategory {
    ASSAULT("assault"),
    SMG("smg"),
    SNIPER("sniper"),
    SHOTGUN("shotgun"),
    PISTOL("pistol"),
    MG("mg"),
    LAUNCHER("launcher"),
    MELEE("melee"),
    THROWABLE("throwable");

    private final String key;

    GunCategory(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public boolean isFirearm() {
        return this != MELEE && this != THROWABLE;
    }
}
