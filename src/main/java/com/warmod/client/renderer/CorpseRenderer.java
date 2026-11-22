package com.warmod.client.renderer;

import com.warmod.entity.CorpseEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CorpseRenderer extends GeoEntityRenderer<CorpseEntity> {

    public CorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new CorpseGeoModel());
        this.shadowRadius = 0.3F;
    }
}
