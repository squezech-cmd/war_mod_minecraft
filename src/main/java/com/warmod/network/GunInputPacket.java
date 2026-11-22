package com.warmod.network;

import com.warmod.item.GunHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: is the trigger held right now. */
public record GunInputPacket(boolean trigger) {

    public static void encode(GunInputPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.trigger);
    }

    public static GunInputPacket decode(FriendlyByteBuf buf) {
        return new GunInputPacket(buf.readBoolean());
    }

    public static void handle(GunInputPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
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
