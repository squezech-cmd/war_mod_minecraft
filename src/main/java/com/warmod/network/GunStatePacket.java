package com.warmod.network;

import com.warmod.Warmod;
import com.warmod.util.ClientGunState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: authoritative weapon state for the HUD, ADS overlay and animations. */
public record GunStatePacket(String gunId, int ammo, int magSize, int fireMode, long reloadEnd,
                             boolean aiming, int fireCounter) {

    public static void encode(GunStatePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.gunId, 64);
        buf.writeVarInt(msg.ammo);
        buf.writeVarInt(msg.magSize);
        buf.writeVarInt(msg.fireMode);
        buf.writeLong(msg.reloadEnd);
        buf.writeBoolean(msg.aiming);
        buf.writeVarInt(msg.fireCounter);
    }

    public static GunStatePacket decode(FriendlyByteBuf buf) {
        return new GunStatePacket(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readLong(), buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(GunStatePacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                ClientGunState.apply(msg.gunId, msg.ammo, msg.magSize, msg.fireMode, msg.reloadEnd,
                        msg.aiming, msg.fireCounter);
            } catch (Throwable t) {
                Warmod.LOGGER.error("Failed to apply gun state", t);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
