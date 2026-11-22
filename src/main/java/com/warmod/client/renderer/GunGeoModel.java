package com.warmod.client.renderer;

import com.warmod.Warmod;
import com.warmod.item.GunItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Per weapon GeckoLib model / texture / animation set. */
public class GunGeoModel extends GeoModel<GunItem> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animations;

    public GunGeoModel(String gunId) {
        this.model = new ResourceLocation(Warmod.MODID, "geo/" + gunId + ".geo.json");
        this.texture = new ResourceLocation(Warmod.MODID, "textures/geo/" + gunId + ".png");
        this.animations = new ResourceLocation(Warmod.MODID, "animations/" + gunId + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(GunItem animatable) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(GunItem animatable) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(GunItem animatable) {
        return this.animations;
    }
}
