package com.warmod.client.renderer;

import com.warmod.Warmod;
import com.warmod.entity.CorpseEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CorpseGeoModel extends GeoModel<CorpseEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(Warmod.MODID, "geo/soldier.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(Warmod.MODID, "animations/soldier.animation.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Warmod.MODID, "textures/entity/soldier_corpse.png");

    @Override
    public ResourceLocation getModelResource(CorpseEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CorpseEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CorpseEntity animatable) {
        return ANIMATIONS;
    }
}
