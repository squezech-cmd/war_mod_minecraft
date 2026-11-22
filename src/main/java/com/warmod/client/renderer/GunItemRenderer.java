package com.warmod.client.renderer;

import com.warmod.item.GunItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** First person / GUI renderer of a weapon (GeckoLib BlockEntityWithoutLevelRenderer). */
public class GunItemRenderer extends GeoItemRenderer<GunItem> {

    public GunItemRenderer(GunItem item) {
        super(new GunGeoModel(item.getDefinition().id()));
    }

    @Override
    public boolean shouldRenderOffScreen(GunItem animatable) {
        return true;
    }
}
