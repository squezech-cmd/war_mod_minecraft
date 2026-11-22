package com.warmod;

import com.mojang.logging.LogUtils;
import com.warmod.client.ClientSetup;
import com.warmod.config.WarConfig;
import com.warmod.item.GunHandler;
import com.warmod.item.MeleeHandler;
import com.warmod.network.NetworkHandler;
import com.warmod.registry.BlockRegistry;
import com.warmod.registry.CreativeTabRegistry;
import com.warmod.registry.EntityRegistry;
import com.warmod.registry.ItemRegistry;
import com.warmod.registry.SoundRegistry;
import com.warmod.team.TeamCommands;
import com.warmod.world.AmbientSoundHandler;
import com.warmod.world.BotWaveSpawner;
import com.warmod.world.DimensionRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Warmod.MODID)
public class Warmod {
    public static final String MODID = "warmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Warmod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, WarConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WarConfig.CLIENT_SPEC);

        SoundRegistry.SOUNDS.register(modBus);
        BlockRegistry.BLOCKS.register(modBus);
        BlockRegistry.ITEMS.register(modBus);
        ItemRegistry.register(modBus);
        EntityRegistry.ENTITIES.register(modBus);
        CreativeTabRegistry.TABS.register(modBus);
        DimensionRegistration.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(EntityRegistry::onAttributes);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(modBus));

        MinecraftForge.EVENT_BUS.register(GunHandler.class);
        MinecraftForge.EVENT_BUS.register(MeleeHandler.class);
        MinecraftForge.EVENT_BUS.register(WarmodEvents.class);
        MinecraftForge.EVENT_BUS.register(AmbientSoundHandler.class);
        MinecraftForge.EVENT_BUS.register(BotWaveSpawner.class);
        MinecraftForge.EVENT_BUS.register(TeamCommands.class);

        LOGGER.info("War Mod loaded: {} weapons registered", com.warmod.item.GunRegistry.all().size());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
