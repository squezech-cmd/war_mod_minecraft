package com.warmod.client;

import com.warmod.client.renderer.CorpseRenderer;
import com.warmod.client.renderer.SoldierRenderer;
import com.warmod.client.renderer.VehicleRenderer;
import com.warmod.client.renderer.WarProjectileRenderer;
import com.warmod.registry.EntityRegistry;
import com.warmod.world.DimensionRegistration;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/** Client only initialisation: renderers, keys, sky effects, HUD and input handlers. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::onRegisterRenderers);
        modBus.addListener(ClientSetup::onRegisterKeys);
        modBus.addListener(ClientSetup::onRegisterDimensionEffects);

        MinecraftForge.EVENT_BUS.register(ClientGunInputs.class);
        MinecraftForge.EVENT_BUS.register(TracerRenderer.class);
        MinecraftForge.EVENT_BUS.register(WarHud.class);
        MinecraftForge.EVENT_BUS.register(MusicHandler.class);
        MinecraftForge.EVENT_BUS.register(DeathScreenHandler.class);
        MinecraftForge.EVENT_BUS.register(ClientSkyAndFov.class);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.SOLDIER.get(), SoldierRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CORPSE.get(), CorpseRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARMORED_CAR_1.get(), VehicleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ARMORED_CAR_2.get(), VehicleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.PLANE.get(), VehicleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.HELICOPTER.get(), VehicleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.TANK.get(), VehicleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ROCKET.get(),
                context -> new WarProjectileRenderer(context, 0.16F, 0xFFAA33));
        event.registerEntityRenderer(EntityRegistry.SHELL.get(),
                context -> new WarProjectileRenderer(context, 0.22F, 0xFFE066));
        event.registerEntityRenderer(EntityRegistry.GRENADE.get(),
                context -> new WarProjectileRenderer(context, 0.12F, 0x7CBF5A));
        event.registerEntityRenderer(EntityRegistry.BULLET.get(),
                context -> new WarProjectileRenderer(context, 0.06F, 0xFFD27A));
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.RELOAD);
        event.register(KeyBindings.FIRE_MODE);
        event.register(KeyBindings.VEHICLE_CAMERA);
        event.register(KeyBindings.VEHICLE_EXIT);
    }

    private static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(DimensionRegistration.BATTLEFIELD_EFFECTS, new BattlefieldSky());
    }
}
