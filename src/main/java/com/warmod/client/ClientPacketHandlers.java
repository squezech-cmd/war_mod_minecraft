package com.warmod.client;

import com.warmod.network.GunStatePacket;
import com.warmod.network.TeamSyncPacket;
import com.warmod.network.TracerPacket;
import com.warmod.util.ClientGunState;
import net.minecraft.client.Minecraft;

/** All server -> client packets land here (client only). */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void onGunState(GunStatePacket msg) {
        ClientGunState.apply(msg.gunId(), msg.ammo(), msg.magSize(), msg.fireMode(), msg.reloadEnd(),
                msg.aiming(), msg.fireCounter());
    }

    public static void onRecoil(float pitch, float yaw) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.setXRot(minecraft.player.getXRot() - pitch);
        minecraft.player.setYRot(minecraft.player.getYRot() + yaw);
        ClientGunState.recoilImpulse = Math.min(3.0F, ClientGunState.recoilImpulse + pitch * 0.5F);
    }

    public static void onTracer(TracerPacket msg) {
        TracerRenderer.add(msg);
    }

    public static void onTeamSync(TeamSyncPacket msg) {
        ClientTeamCache.apply(msg);
    }
}
