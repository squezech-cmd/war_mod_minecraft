package com.warmod.world;

import com.warmod.config.WarConfig;
import com.warmod.team.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Server ticker that periodically sends reinforcement waves of bots to the battlefield. */
public final class BotWaveSpawner {
    private static int ticks;

    private BotWaveSpawner() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        if (!WarConfig.botWaves()) {
            ticks = 0;
            return;
        }
        if (++ticks >= Math.max(15, WarConfig.botWaveInterval())) {
            ticks = 0;
            TeamManager.spawnWave(server);
        }
    }
}
