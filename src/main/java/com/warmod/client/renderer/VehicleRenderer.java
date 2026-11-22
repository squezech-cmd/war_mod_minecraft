package com.warmod.client.renderer;

import com.warmod.entity.VehicleBase;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VehicleRenderer extends GeoEntityRenderer<VehicleBase> {

    public VehicleRenderer(EntityRendererProvider.Context context) {
        super(context, new VehicleGeoModel());
        this.shadowRadius = 1.4F;
    }
}
