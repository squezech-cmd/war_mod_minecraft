package com.warmod.item;

public enum FireMode {
    AUTO,
    SEMI,
    BURST;

    public static FireMode byOrdinal(int ordinal) {
        FireMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SEMI;
    }

    public String translationKey() {
        return "warmod.firemode." + this.name().toLowerCase(java.util.Locale.ROOT);
    }
}
