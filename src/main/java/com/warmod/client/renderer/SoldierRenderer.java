package com.warmod.client.renderer;

import com.warmod.entity.SoldierEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SoldierRenderer extends GeoEntityRenderer<SoldierEntity> {

    public SoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new SoldierGeoModel());
        this.shadowRadius = 0.45F;
    }
}
