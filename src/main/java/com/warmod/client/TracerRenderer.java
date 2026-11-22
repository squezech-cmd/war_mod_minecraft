package com.warmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warmod.config.WarConfig;
import com.warmod.network.TracerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side bullet tracers: server packets describe the exact ray, the client draws a short
 * lived glowing beam (untextured quad, RenderType.lightning) and a muzzle flash.
 */
@OnlyIn(Dist.CLIENT)
public final class TracerRenderer {
    private static final List<Beam> BEAMS = new ArrayList<>();

    private TracerRenderer() {}

    private static final class Beam {
        private final Vec3 from;
        private final Vec3 to;
        private final int color;
        private int life = 6;

        private Beam(Vec3 from, Vec3 to, int color) {
            this.from = from;
            this.to = to;
            this.color = color;
        }
    }

    public static void add(TracerPacket packet) {
        if (!WarConfig.tracers() || BEAMS.size() > 64) {
            return;
        }
        BEAMS.add(new Beam(new Vec3(packet.x1(), packet.y1(), packet.z1()),
                new Vec3(packet.x2(), packet.y2(), packet.z2()), packet.color()));
        spawnMuzzleFlash(new Vec3(packet.x1(), packet.y1(), packet.z1()));
    }

    private static void spawnMuzzleFlash(Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        minecraft.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
        minecraft.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                position.x, position.y, position.z, 0.0D, 0.01D, 0.0D);
    }

    public static void tick() {
        Iterator<Beam> iterator = BEAMS.iterator();
        while (iterator.hasNext()) {
            if (--iterator.next().life <= 0) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BEAMS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        for (Beam beam : BEAMS) {
            float alpha = Math.min(1.0F, beam.life / 6.0F);
            drawBeam(matrix, consumer, beam.from, beam.to, 0.035F, beam.color, alpha);
        }
        buffers.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static void drawBeam(Matrix4f matrix, VertexConsumer consumer, Vec3 from, Vec3 to,
                                 float width, int color, float alpha) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 0.0001D) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 view = camera.subtract(from.add(to).scale(0.5D)).normalize();
        Vec3 side = direction.normalize().cross(view);
        if (side.lengthSqr() < 0.0001D) {
            side = new Vec3(0.0D, 1.0D, 0.0D);
        }
        side = side.normalize().scale(width);
        Vec3 up = direction.normalize().cross(side).normalize().scale(width * 0.6D);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 200.0F);

        Vec3 a1 = from.add(side).add(up);
        Vec3 a2 = from.subtract(side).subtract(up);
        Vec3 b1 = to.add(side).add(up);
        Vec3 b2 = to.subtract(side).subtract(up);

        vertex(consumer, matrix, a1, r, g, b, a);
        vertex(consumer, matrix, a2, r, g, b, a);
        vertex(consumer, matrix, b2, r, g, b, a);
        vertex(consumer, matrix, b1, r, g, b, a);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, int r, int g, int b, int a) {
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(r, g, b, a)
                .endVertex();
    }
}
