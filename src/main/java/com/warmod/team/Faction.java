package com.warmod.team;

import net.minecraft.ChatFormatting;

public enum Faction {
    ALPHA("alpha", 0x3A6EA5, ChatFormatting.BLUE),
    BRAVO("bravo", 0xB04A3A, ChatFormatting.RED);

    private final String key;
    private final int color;
    private final ChatFormatting formatting;

    Faction(String key, int color, ChatFormatting formatting) {
        this.key = key;
        this.color = color;
        this.formatting = formatting;
    }

    public String key() {
        return this.key;
    }

    public int color() {
        return this.color;
    }

    public ChatFormatting formatting() {
        return this.formatting;
    }

    public String translationKey() {
        return "warmod.faction." + this.key;
    }

    public static Faction byKey(String key) {
        for (Faction faction : values()) {
            if (faction.key.equalsIgnoreCase(key)) {
                return faction;
            }
        }
        return ALPHA;
    }

    public static Faction byOrdinal(int ordinal) {
        Faction[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ALPHA;
    }
}
