package com.warmod.client;

import com.warmod.entity.VehicleBase;
import com.warmod.item.GunItem;
import com.warmod.network.GunInputPacket;
import com.warmod.network.NetworkHandler;
import com.warmod.network.VehicleControlPacket;
import com.warmod.util.ClientGunState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client -> server input pump: trigger, reload, fire mode, vehicle controls.
 * No gameplay decision is taken here, the server validates everything.
 */
public final class ClientGunInputs {
    private static boolean lastTrigger;
    private static int refresh;

    private ClientGunInputs() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        TracerRenderer.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        ClientGunState.tickClient(1.0F, minecraft.level.getGameTime());

        if (minecraft.screen != null) {
            if (lastTrigger) {
                lastTrigger = false;
                NetworkHandler.sendToServer(new GunInputPacket(false));
            }
            return;
        }

        while (KeyBindings.RELOAD.consumeClick()) {
            NetworkHandler.sendToServer(new com.warmod.network.GunActionPacket(
                    com.warmod.network.GunActionPacket.Action.RELOAD));
        }
        while (KeyBindings.FIRE_MODE.consumeClick()) {
            NetworkHandler.sendToServer(new com.warmod.network.GunActionPacket(
                    com.warmod.network.GunActionPacket.Action.CYCLE_FIRE_MODE));
        }
        while (KeyBindings.VEHICLE_EXIT.consumeClick()) {
            if (minecraft.player.getVehicle() instanceof VehicleBase) {
                minecraft.player.stopRiding();
            }
        }

        boolean holdingGun = minecraft.player.getMainHandItem().getItem() instanceof GunItem;
        boolean riding = minecraft.player.getVehicle() instanceof VehicleBase;
        boolean trigger = minecraft.options.keyAttack.isDown() && (holdingGun || riding);

        if (trigger != lastTrigger || (trigger && ++refresh % 8 == 0)) {
            NetworkHandler.sendToServer(new GunInputPacket(trigger));
            lastTrigger = trigger;
        }

        if (minecraft.player.getVehicle() instanceof VehicleBase vehicle) {
            float forward = 0.0F;
            float strafe = 0.0F;
            if (minecraft.options.keyUp.isDown()) {
                forward += 1.0F;
            }
            if (minecraft.options.keyDown.isDown()) {
                forward -= 1.0F;
            }
            if (minecraft.options.keyLeft.isDown()) {
                strafe += 1.0F;
            }
            if (minecraft.options.keyRight.isDown()) {
                strafe -= 1.0F;
            }
            NetworkHandler.sendToServer(new VehicleControlPacket(vehicle.getId(), forward, strafe,
                    minecraft.player.getYRot(), minecraft.player.getXRot(),
                    minecraft.options.keyShift.isDown(), minecraft.options.keySprint.isDown()));
        }
    }
}
