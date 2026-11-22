package com.warmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** One cartridge/rocket/shell. Consumed round by round when reloading. */
public class AmmoItem extends Item {
    private final AmmoType ammoType;

    public AmmoItem(AmmoType ammoType, Properties properties) {
        super(properties);
        this.ammoType = ammoType;
    }

    public AmmoType getAmmoType() {
        return this.ammoType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("warmod.ammo.caliber", this.ammoType.key())
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
