package com.warmod.client;

import com.warmod.config.WarConfig;
import com.warmod.item.GunDefinition;
import com.warmod.item.GunItem;
import com.warmod.util.ClientGunState;
import com.warmod.world.DimensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Dark battlefield fog, ADS field of view zoom and recoil camera shake. */
public final class ClientSkyAndFov {
    private ClientSkyAndFov() {}

    private static boolean inBattlefield() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.level.dimension().equals(DimensionRegistration.BATTLEFIELD);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!WarConfig.darkSky() || !inBattlefield()) {
            return;
        }
        event.setRed(0.102F);
        event.setGreen(0.102F);
        event.setBlue(0.125F);
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!WarConfig.darkSky() || !inBattlefield()) {
            return;
        }
        event.setNearPlaneDistance(6.0F);
        event.setFarPlaneDistance(110.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (!(minecraft.player.getMainHandItem().getItem() instanceof GunItem gun)) {
            return;
        }
        GunDefinition def = gun.getDefinition();
        float zoom = def.scope().zoom();
        float progress = ClientGunState.adsProgress;
        if (progress <= 0.01F) {
            return;
        }
        float factor = 1.0F - progress * (1.0F - 1.0F / zoom) * (float) WarConfig.adsZoom();
        event.setFOV(event.getFOV() * Math.max(0.25F, factor));
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!WarConfig.cameraShake()) {
            return;
        }
        float impulse = ClientGunState.recoilImpulse;
        if (impulse <= 0.01F) {
            return;
        }
        event.setPitch(event.getPitch() - impulse * 0.35F);
        event.setRoll(event.getRoll() + (float) Math.sin(System.currentTimeMillis() * 0.02D) * impulse * 0.5F);
    }
}
