package com.warmod.client.renderer;

import com.warmod.Warmod;
import com.warmod.entity.VehicleBase;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

public class VehicleGeoModel extends GeoModel<VehicleBase> {
    private final Map<String, ResourceLocation> models = new HashMap<>();
    private final Map<String, ResourceLocation> textures = new HashMap<>();
    private final Map<String, ResourceLocation> animations = new HashMap<>();

    private ResourceLocation cached(String id, Map<String, ResourceLocation> cache, String folder, String suffix) {
        return cache.computeIfAbsent(id, key -> new ResourceLocation(Warmod.MODID, folder + key + suffix));
    }

    @Override
    public ResourceLocation getModelResource(VehicleBase animatable) {
        return this.cached(animatable.getVehicleId(), this.models, "geo/", ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VehicleBase animatable) {
        return this.cached(animatable.getVehicleId(), this.textures, "textures/entity/", ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(VehicleBase animatable) {
        return this.cached(animatable.getVehicleId(), this.animations, "animations/", ".animation.json");
    }
}
