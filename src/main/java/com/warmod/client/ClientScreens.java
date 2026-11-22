package com.warmod.client;

import net.minecraft.client.Minecraft;

public final class ClientScreens {
    private ClientScreens() {}

    public static void openRadio() {
        Minecraft.getInstance().setScreen(new RadioScreen());
    }
}
