package com.warmod.item;

public enum ScopeType {
    /** No magnification: ironsights / hip fire. */
    NONE(1.0F),
    /** Red dot / holo. */
    RED_DOT(1.15F),
    /** ACOG style 4x. */
    ACOG(1.45F),
    /** Assault-rifle optics. */
    RIFLE(1.25F),
    /** High power sniper scope. */
    SNIPER(2.6F);

    private final float zoom;

    ScopeType(float zoom) {
        this.zoom = zoom;
    }

    public float zoom() {
        return this.zoom;
    }
}
