package com.warmod.network;

import com.warmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: apply recoil (camera kick). Values are computed on the server from the weapon. */
public record RecoilPacket(float pitch, float yaw) {

    public static void encode(RecoilPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.pitch);
        buf.writeFloat(msg.yaw);
    }

    public static RecoilPacket decode(FriendlyByteBuf buf) {
        return new RecoilPacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(RecoilPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.onRecoil(msg.pitch(), msg.yaw())));
        ctx.setPacketHandled(true);
    }
}
