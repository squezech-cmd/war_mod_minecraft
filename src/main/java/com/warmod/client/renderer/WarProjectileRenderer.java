package com.warmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.warmod.entity.WarProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Glowing billboard drawn for rockets / shells / grenades (no texture required). */
public class WarProjectileRenderer extends EntityRenderer<WarProjectile> {
    private static final ResourceLocation EMPTY = new ResourceLocation("minecraft", "textures/misc/white.png");

    private final float size;
    private final int color;

    public WarProjectileRenderer(EntityRendererProvider.Context context, float size, int color) {
        super(context);
        this.size = size;
        this.color = color;
    }

    @Override
    public void render(WarProjectile entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        int r = (this.color >> 16) & 0xFF;
        int g = (this.color >> 8) & 0xFF;
        int b = this.color & 0xFF;

        consumer.vertex(matrix, -this.size, -this.size, 0.0F).color(r, g, b, 210).endVertex();
        consumer.vertex(matrix, this.size, -this.size, 0.0F).color(r, g, b, 210).endVertex();
        consumer.vertex(matrix, this.size, this.size, 0.0F).color(255, 255, 255, 235).endVertex();
        consumer.vertex(matrix, -this.size, this.size, 0.0F).color(255, 255, 255, 235).endVertex();
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WarProjectile entity) {
        return EMPTY;
    }
}
