package com.warmod.network;

import com.warmod.entity.SoldierEntity;
import com.warmod.team.TeamManager;
import com.warmod.team.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: commander radio actions (spawn bots, orders, equipment). */
public record OrderPacket(Action action, int value) {

    public enum Action {
        SPAWN_BOTS,
        ORDER,
        EQUIP,
        DISMISS
    }

    public static void encode(OrderPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.value);
    }

    public static OrderPacket decode(FriendlyByteBuf buf) {
        return new OrderPacket(buf.readEnum(Action.class), buf.readVarInt());
    }

    public static void handle(OrderPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            Team team = TeamManager.getTeam(player);
            if (team == null) {
                TeamManager.message(player, "warmod.msg.no_team");
                return;
            }
            switch (msg.action) {
                case SPAWN_BOTS -> TeamManager.spawnBots(player, team, Math.max(1, Math.min(10, msg.value)));
                case ORDER -> TeamManager.issueOrder(player, team, SoldierEntity.Order.byOrdinal(msg.value));
                case EQUIP -> TeamManager.equipBots(player, team, msg.value);
                case DISMISS -> TeamManager.dismissBots(player, team);
            }
        });
        ctx.setPacketHandled(true);
    }
}
