package com.warmod.network;

import com.warmod.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: draw a bullet tracer from A to B. */
public record TracerPacket(double x1, double y1, double z1, double x2, double y2, double z2, int color) {

    public TracerPacket(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, int color) {
        this(from.x, from.y, from.z, to.x, to.y, to.z, color);
    }

    public static void encode(TracerPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.x1);
        buf.writeDouble(msg.y1);
        buf.writeDouble(msg.z1);
        buf.writeDouble(msg.x2);
        buf.writeDouble(msg.y2);
        buf.writeDouble(msg.z2);
        buf.writeInt(msg.color);
    }

    public static TracerPacket decode(FriendlyByteBuf buf) {
        return new TracerPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    public static void handle(TracerPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.onTracer(msg)));
        ctx.setPacketHandled(true);
    }
}
