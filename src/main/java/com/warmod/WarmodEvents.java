package com.warmod;

import com.warmod.team.TeamManager;
import com.warmod.world.DimensionRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Server side glue: team bookkeeping, battlefield spawns, death statistics. */
public final class WarmodEvents {
    private WarmodEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeamManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TeamManager.sync(player);
        if (event.getTo().equals(DimensionRegistration.BATTLEFIELD)) {
            BlockPos spawn = TeamManager.spawnFor(player);
            player.teleportTo((ServerLevel) player.level(), spawn.getX() + 0.5D, spawn.getY() + 1.0D,
                    spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TeamManager.sync(player);
        if (player.level().dimension().equals(DimensionRegistration.BATTLEFIELD)) {
            BlockPos spawn = TeamManager.spawnFor(player);
            player.teleportTo((ServerLevel) player.level(), spawn.getX() + 0.5D, spawn.getY() + 1.0D,
                    spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            Entity killer = victim.getKillCredit();
            TeamManager.onPlayerKilled(victim, killer);
        }
    }
}
