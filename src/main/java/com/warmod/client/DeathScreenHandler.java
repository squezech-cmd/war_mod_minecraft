package com.warmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Replaces the vanilla death screen with the war styled one. */
public final class DeathScreenHandler {
    private DeathScreenHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DeathScreen deathScreen && !(minecraft.screen instanceof DeathScreenWar)) {
            minecraft.setScreen(new DeathScreenWar(deathScreen.getDeathCause(), deathScreen.shouldShowDeathScreen()));
        }
    }
}
