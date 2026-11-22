package com.warmod.item;

import com.warmod.config.WarConfig;
import com.warmod.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Melee weapons (knife, machete) use the vanilla left click attack, firearms must not punch.
 */
public final class MeleeHandler {
    private MeleeHandler() {}

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        GunDefinition def = gun.getDefinition();
        if (!def.isMelee() || player.level().isClientSide()) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity target) {
            target.invulnerableTime = 0;
            target.hurt(player.level().damageSources().playerAttack(player),
                    def.damage() * (float) WarConfig.damageMul());
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundRegistry.MELEE_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof GunItem gun && !gun.getDefinition().isMelee()) {
            event.setCanceled(true);
        }
    }
}
