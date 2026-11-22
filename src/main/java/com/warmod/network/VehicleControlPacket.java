package com.warmod.network;

import com.warmod.entity.VehicleBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: driver input of the vehicle the player is riding. */
public record VehicleControlPacket(int entityId, float forward, float strafe, float lookYaw, float lookPitch,
                                   boolean brake, boolean boost) {

    public static void encode(VehicleControlPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.forward);
        buf.writeFloat(msg.strafe);
        buf.writeFloat(msg.lookYaw);
        buf.writeFloat(msg.lookPitch);
        buf.writeBoolean(msg.brake);
        buf.writeBoolean(msg.boost);
    }

    public static VehicleControlPacket decode(FriendlyByteBuf buf) {
        return new VehicleControlPacket(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(VehicleControlPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            Entity entity = player.level().getEntity(msg.entityId);
            if (entity instanceof VehicleBase vehicle && vehicle.getControllingPassenger() == player) {
                vehicle.setInput(msg.forward, msg.strafe, msg.lookYaw, msg.lookPitch, msg.brake, msg.boost);
            }
        });
        ctx.setPacketHandled(true);
    }
}
