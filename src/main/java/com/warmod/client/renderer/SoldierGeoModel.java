package com.warmod.client.renderer;

import com.warmod.Warmod;
import com.warmod.entity.SoldierEntity;
import com.warmod.team.Faction;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SoldierGeoModel extends GeoModel<SoldierEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(Warmod.MODID, "geo/soldier.geo.json");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(Warmod.MODID, "animations/soldier.animation.json");
    private static final ResourceLocation ALPHA = new ResourceLocation(Warmod.MODID, "textures/entity/soldier_alpha.png");
    private static final ResourceLocation BRAVO = new ResourceLocation(Warmod.MODID, "textures/entity/soldier_bravo.png");

    @Override
    public ResourceLocation getModelResource(SoldierEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SoldierEntity animatable) {
        return animatable.getFaction() == Faction.BRAVO ? BRAVO : ALPHA;
    }

    @Override
    public ResourceLocation getAnimationResource(SoldierEntity animatable) {
        return ANIMATIONS;
    }
}
