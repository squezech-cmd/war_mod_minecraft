package com.warmod.item;

import java.util.List;

/**
 * Immutable parameter block of a single weapon. Everything gameplay related (damage, spread,
 * recoil, reload time, fire modes, range, magazine size) lives here, never in the renderer.
 */
public record GunDefinition(
        String id,
        GunCategory category,
        AmmoType ammo,
        int magSize,
        float damage,
        float spread,
        float recoilPitch,
        float recoilYaw,
        int reloadTicks,
        List<FireMode> modes,
        int rpm,
        float range,
        ScopeType scope,
        int pellets,
        String fireSound,
        String reloadSound,
        ProjectileKind projectile,
        float explosionPower
) {
    /** Ticks between two shots at the given rate of fire. */
    public int fireInterval() {
        return Math.max(1, (int) Math.round(1200.0D / Math.max(1, this.rpm)));
    }

    public boolean isMelee() {
        return this.category == GunCategory.MELEE;
    }

    public boolean isThrowable() {
        return this.category == GunCategory.THROWABLE;
    }

    public boolean needsAmmo() {
        return this.ammo != AmmoType.NONE && this.category.isFirearm();
    }

    public FireMode defaultMode() {
        return this.modes.isEmpty() ? FireMode.SEMI : this.modes.get(0);
    }

    public boolean hasMode(FireMode mode) {
        return this.modes.contains(mode);
    }

    public String nameKey() {
        return "item.warmod." + this.id;
    }

    /** Sprite of the ADS overlay: textures/gui/scope/&lt;id&gt;.png */
    public String scopeTexture() {
        return "textures/gui/scope/" + this.id + ".png";
    }
}
