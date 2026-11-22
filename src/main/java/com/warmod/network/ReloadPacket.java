package com.warmod.network;

import com.warmod.item.GunHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: reload or cycle fire mode. */
public record ReloadPacket(Action action) {

    public enum Action {
        RELOAD,
        CYCLE_FIRE_MODE
    }

    public static void encode(ReloadPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
    }

    public static ReloadPacket decode(FriendlyByteBuf buf) {
        return new ReloadPacket(buf.readEnum(Action.class));
    }

    public static void handle(ReloadPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            switch (msg.action) {
                case RELOAD -> GunHandler.reload(player);
                case CYCLE_FIRE_MODE -> GunHandler.cycleFireMode(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
