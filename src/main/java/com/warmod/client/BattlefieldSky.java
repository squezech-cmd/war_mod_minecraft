package com.warmod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ISkyRenderHandler;
import net.minecraftforge.client.IWeatherRenderHandler;
import net.minecraft.world.level.dimension.DimensionSpecialEffects;

import javax.annotation.Nullable;

/**
 * Dark, foggy sky of the battlefield: no sun, no stars, heavy dark fog (0x1a1a20).
 */
public class BattlefieldSky extends DimensionSpecialEffects {
    private static final Vec3 FOG = new Vec3(0.102D, 0.102D, 0.125D); // ~0x1a1a20
    private static final Vec3 CLOUD = new Vec3(0.07D, 0.07D, 0.09D);

    public BattlefieldSky() {
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(FOG.x * (0.6D + brightness * 0.2D), FOG.y * (0.6D + brightness * 0.2D),
                FOG.z * (0.6D + brightness * 0.2D));
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public Vec3 getCloudColor(Vec3 cloudColor, float partialTicks) {
        return CLOUD;
    }

    @Nullable
    @Override
    public ISkyRenderHandler getSkyRenderHandler() {
        return null;
    }

    @Nullable
    @Override
    public IWeatherRenderHandler getWeatherRenderHandler() {
        return null;
    }
}
