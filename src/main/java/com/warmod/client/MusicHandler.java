package com.warmod.client;

import com.warmod.config.WarConfig;
import com.warmod.registry.SoundRegistry;
import com.warmod.world.DimensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Looping dark battlefield music track (restarted every track length). */
public final class MusicHandler {
    private static final int TRACK_TICKS = 20 * 40;
    private static int ticks = TRACK_TICKS;

    private MusicHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (!minecraft.level.dimension().equals(DimensionRegistration.BATTLEFIELD)) {
            ticks = TRACK_TICKS;
            return;
        }
        if (++ticks < TRACK_TICKS) {
            return;
        }
        ticks = 0;
        double volume = WarConfig.musicVolume();
        if (volume <= 0.01D) {
            return;
        }
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                SoundRegistry.MUSIC_BATTLEFIELD.get().getLocation(), SoundSource.MUSIC,
                (float) volume, 1.0F, RandomSource.create(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true));
    }
}
