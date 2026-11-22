package com.warmod.world;

import com.warmod.Warmod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import com.mojang.serialization.Codec;

public final class DimensionRegistration {
    public static final ResourceKey<Level> BATTLEFIELD =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Warmod.MODID, "battlefield"));
    public static final ResourceKey<DimensionType> BATTLEFIELD_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Warmod.MODID, "battlefield"));
    public static final ResourceLocation BATTLEFIELD_EFFECTS = new ResourceLocation(Warmod.MODID, "battlefield");

    /** Team spawns are placed on the road grid of the ruined city (see BattlefieldChunkGenerator). */
    public static final BlockPos ALPHA_SPAWN = new BlockPos(2, 72, 2);
    public static final BlockPos BRAVO_SPAWN = new BlockPos(-30, 72, -30);

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Warmod.MODID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> BATTLEFIELD_GENERATOR =
            CHUNK_GENERATORS.register("battlefield", () -> BattlefieldChunkGenerator.CODEC);

    private DimensionRegistration() {}

    public static void register(IEventBus bus) {
        CHUNK_GENERATORS.register(bus);
    }

    public static boolean isBattlefield(Level level) {
        return level.dimension().equals(BATTLEFIELD);
    }
}
