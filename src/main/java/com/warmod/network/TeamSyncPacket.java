package com.warmod.network;

import com.warmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: player's team, score and battlefield counters. */
public record TeamSyncPacket(boolean inTeam, String teamName, int faction, int kills, int deaths,
                             int members, int bots, int personalKills, int personalDeaths) {

    public static void encode(TeamSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.inTeam);
        buf.writeUtf(msg.teamName, 64);
        buf.writeVarInt(msg.faction);
        buf.writeVarInt(msg.kills);
        buf.writeVarInt(msg.deaths);
        buf.writeVarInt(msg.members);
        buf.writeVarInt(msg.bots);
        buf.writeVarInt(msg.personalKills);
        buf.writeVarInt(msg.personalDeaths);
    }

    public static TeamSyncPacket decode(FriendlyByteBuf buf) {
        return new TeamSyncPacket(buf.readBoolean(), buf.readUtf(64), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(TeamSyncPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.onTeamSync(msg)));
        ctx.setPacketHandled(true);
    }
}
