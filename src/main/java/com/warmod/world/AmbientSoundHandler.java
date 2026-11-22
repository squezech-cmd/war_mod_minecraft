package com.warmod.world;

import com.warmod.config.WarConfig;
import com.warmod.registry.SoundRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Server side ambient atmosphere: every 10-30 seconds a random war sound is played to every
 * player inside the battlefield dimension.
 */
public final class AmbientSoundHandler {
    private static final List<String> AMBIENT = List.of(
            "ambient_far_gunfire", "ambient_jet_pass", "ambient_siren", "ambient_explosion", "ambient_drone");

    private static int ticksUntilNext = 20 * 12;

    private AmbientSoundHandler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        if (--ticksUntilNext > 0) {
            return;
        }
        int min = Math.max(3, WarConfig.ambientMinSeconds());
        int max = Math.max(min + 1, WarConfig.ambientMaxSeconds());
        ticksUntilNext = (min + server.overworld().random.nextInt(max - min)) * 20;
        if (!WarConfig.ambientSounds()) {
            return;
        }
        ServerLevel level = server.getLevel(DimensionRegistration.BATTLEFIELD);
        if (level == null) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer anchor = players.get(level.random.nextInt(players.size()));
        String name = AMBIENT.get(level.random.nextInt(AMBIENT.size()));
        SoundEvent sound = SoundRegistry.get(name);
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double distance = 30.0D + level.random.nextDouble() * 60.0D;
        double x = anchor.getX() + Math.cos(angle) * distance;
        double z = anchor.getZ() + Math.sin(angle) * distance;
        for (ServerPlayer player : players) {
            player.playNotifySound(sound, SoundSource.AMBIENT, 1.0F, 0.85F + level.random.nextFloat() * 0.3F);
        }
        level.playSound(null, x, anchor.getY(), z, sound, SoundSource.AMBIENT, 2.0F, 1.0F);
    }
}
