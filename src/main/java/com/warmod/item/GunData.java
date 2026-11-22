package com.warmod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** NBT layout of a weapon stack. Authority is always the server. */
public final class GunData {
    public static final String TAG_AMMO = "warmod:Ammo";
    public static final String TAG_MODE = "warmod:FireMode";
    public static final String TAG_RELOAD_END = "warmod:ReloadEnd";
    public static final String TAG_OWNER = "warmod:Owner";

    private GunData() {}

    public static int getAmmo(ItemStack stack, GunDefinition def) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_AMMO)) {
            return def.magSize();
        }
        return Math.max(0, Math.min(def.magSize(), tag.getInt(TAG_AMMO)));
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt(TAG_AMMO, Math.max(0, ammo));
    }

    public static FireMode getMode(ItemStack stack, GunDefinition def) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_MODE)) {
            return def.defaultMode();
        }
        FireMode mode = FireMode.byOrdinal(tag.getInt(TAG_MODE));
        return def.hasMode(mode) ? mode : def.defaultMode();
    }

    public static void setMode(ItemStack stack, FireMode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.ordinal());
    }

    /** Cycles through the available fire modes of a weapon. */
    public static FireMode nextMode(ItemStack stack, GunDefinition def) {
        FireMode current = getMode(stack, def);
        int index = def.modes().indexOf(current);
        FireMode next = def.modes().get((index + 1) % def.modes().size());
        setMode(stack, next);
        return next;
    }

    public static long getReloadEnd(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(TAG_RELOAD_END);
    }

    public static void setReloadEnd(ItemStack stack, long absoluteTick) {
        stack.getOrCreateTag().putLong(TAG_RELOAD_END, absoluteTick);
    }

    public static boolean isReloading(ItemStack stack, long now) {
        long end = getReloadEnd(stack);
        return end > 0L && now < end;
    }

    public static String getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_OWNER);
    }

    public static void setOwner(ItemStack stack, String owner) {
        stack.getOrCreateTag().putString(TAG_OWNER, owner);
    }
}
