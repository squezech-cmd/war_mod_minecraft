package com.warmod.network;

import com.warmod.world.DimensionRegistration;
import com.warmod.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: death screen buttons. */
public record RespawnPacket(Action action) {

    public enum Action {
        RESPAWN_AT_TEAM_SPAWN,
        LEAVE_BATTLEFIELD
    }

    public static void encode(RespawnPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
    }

    public static RespawnPacket decode(FriendlyByteBuf buf) {
        return new RespawnPacket(buf.readEnum(Action.class));
    }

    public static void handle(RespawnPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (msg.action == Action.LEAVE_BATTLEFIELD) {
                ServerLevel overworld = player.server.overworld();
                BlockPos spawn = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D,
                        player.getYRot(), player.getXRot());
                TeamManager.message(player, "warmod.msg.left_battlefield");
            } else if (player.level().dimension().equals(DimensionRegistration.BATTLEFIELD)) {
                BlockPos spawn = TeamManager.spawnFor(player);
                ((ServerLevel) player.level()).getChunk(spawn);
                player.teleportTo((ServerLevel) player.level(), spawn.getX() + 0.5D, spawn.getY() + 1.2D,
                        spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
            }
        });
        ctx.setPacketHandled(true);
    }
}
