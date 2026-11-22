package com.warmod.network;

import com.warmod.item.GunHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: is the trigger held right now. */
public record ShootPacket(boolean trigger) {

    public static void encode(ShootPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.trigger);
    }

    public static ShootPacket decode(FriendlyByteBuf buf) {
        return new ShootPacket(buf.readBoolean());
    }

    public static void handle(ShootPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                GunHandler.setTrigger(player, msg.trigger);
            }
        });
        ctx.setPacketHandled(true);
    }
}
