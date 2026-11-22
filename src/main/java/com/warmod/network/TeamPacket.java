package com.warmod.network;

import com.warmod.team.TeamCommands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: team management without typing commands. */
public record TeamPacket(Action action, String arg, String arg2) {

    public enum Action {
        CREATE,
        INVITE,
        ACCEPT,
        LEAVE,
        INFO
    }

    public static void encode(TeamPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.arg == null ? "" : msg.arg, 64);
        buf.writeUtf(msg.arg2 == null ? "" : msg.arg2, 64);
    }

    public static TeamPacket decode(FriendlyByteBuf buf) {
        return new TeamPacket(buf.readEnum(Action.class), buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(TeamPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                TeamCommands.handlePacket(player, msg.action, msg.arg, msg.arg2);
            }
        });
        ctx.setPacketHandled(true);
    }
}
