package com.warmod.network;

import com.warmod.Warmod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "warmod-1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Warmod.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int id;

    private NetworkHandler() {}

    public static void register() {
        CHANNEL.messageBuilder(GunInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GunInputPacket::encode).decoder(GunInputPacket::decode).consumerMainThread(GunInputPacket::handle).add();
        CHANNEL.messageBuilder(GunActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GunActionPacket::encode).decoder(GunActionPacket::decode).consumerMainThread(GunActionPacket::handle).add();
        CHANNEL.messageBuilder(VehicleControlPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(VehicleControlPacket::encode).decoder(VehicleControlPacket::decode).consumerMainThread(VehicleControlPacket::handle).add();
        CHANNEL.messageBuilder(TeamActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TeamActionPacket::encode).decoder(TeamActionPacket::decode).consumerMainThread(TeamActionPacket::handle).add();
        CHANNEL.messageBuilder(RadioActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RadioActionPacket::encode).decoder(RadioActionPacket::decode).consumerMainThread(RadioActionPacket::handle).add();
        CHANNEL.messageBuilder(RespawnPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RespawnPacket::encode).decoder(RespawnPacket::decode).consumerMainThread(RespawnPacket::handle).add();

        CHANNEL.messageBuilder(GunStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GunStatePacket::encode).decoder(GunStatePacket::decode).consumerMainThread(GunStatePacket::handle).add();
        CHANNEL.messageBuilder(RecoilPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RecoilPacket::encode).decoder(RecoilPacket::decode).consumerMainThread(RecoilPacket::handle).add();
        CHANNEL.messageBuilder(TracerPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TracerPacket::encode).decoder(TracerPacket::decode).consumerMainThread(TracerPacket::handle).add();
        CHANNEL.messageBuilder(TeamSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TeamSyncPacket::encode).decoder(TeamSyncPacket::decode).consumerMainThread(TeamSyncPacket::handle).add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToTracking(Entity entity, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    public static void sendToAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }
}
