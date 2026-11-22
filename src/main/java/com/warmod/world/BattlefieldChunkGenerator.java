package com.warmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Ruined city generator: vanilla noise terrain, then a procedurally destroyed street grid on top
 * (sand streets, hollow concrete/brick building shells with holes, rubble).
 */
public class BattlefieldChunkGenerator extends NoiseBasedChunkGenerator {
    public static final Codec<BattlefieldChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(gen -> gen.biomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(BattlefieldChunkGenerator::settings))
            .apply(instance, BattlefieldChunkGenerator::new));

    private static final int CELL = 32;
    private static final int ROAD = 5;
    private static final int WALL_INSET = 7;

    private final Holder<NoiseGeneratorSettings> noiseSettings;

    public BattlefieldChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.noiseSettings = settings;
    }

    private Holder<NoiseGeneratorSettings> settings() {
        return this.noiseSettings;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
        super.buildSurface(region, structures, random, chunk);
        this.decorateRuins(region, random, chunk);
    }

    private void decorateRuins(WorldGenRegion region, RandomState random, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.getMinBlockX() + localX;
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                int height = this.getBaseHeight(worldX, worldZ, Heightmap.Types.WORLD_SURFACE_WG, region, random);
                if (height <= region.getMinBuildHeight() + 2) {
                    continue;
                }

                // desert floor
                chunk.setBlockState(new BlockPos(worldX, height, worldZ), sand, false);
                chunk.setBlockState(new BlockPos(worldX, height - 1, worldZ), sand, false);
                chunk.setBlockState(new BlockPos(worldX, height - 2, worldZ), sandstone, false);

                int cellX = Math.floorDiv(worldX, CELL);
                int cellZ = Math.floorDiv(worldZ, CELL);
                int inX = Math.floorMod(worldX, CELL);
                int inZ = Math.floorMod(worldZ, CELL);
                boolean road = inX < ROAD || inZ < ROAD;

                RandomSource cellRandom = RandomSource.create(cellX * 341873128712L ^ cellZ * 132897987541L ^ 0x5EED);
                if (road) {
                    if (cellRandom.nextInt(14) == 0) {
                        chunk.setBlockState(new BlockPos(worldX, height, worldZ), gravel, false);
                    }
                    continue;
                }

                boolean wall = inX == WALL_INSET || inZ == WALL_INSET
                        || inX == CELL - 1 - WALL_INSET || inZ == CELL - 1 - WALL_INSET;
                int floors = 3 + cellRandom.nextInt(6);
                if (wall) {
                    BlockState material = switch (cellRandom.nextInt(4)) {
                        case 0 -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                        case 1 -> Blocks.STONE_BRICKS.defaultBlockState();
                        case 2 -> Blocks.GRAY_CONCRETE.defaultBlockState();
                        default -> Blocks.SANDSTONE.defaultBlockState();
                    };
                    int wallHeight = floors * 3;
                    for (int y = 1; y <= wallHeight; y++) {
                        // battle damage: random holes in the walls
                        if (cellRandom.nextFloat() < 0.14F) {
                            continue;
                        }
                        chunk.setBlockState(new BlockPos(worldX, height + y, worldZ), material, false);
                    }
                    if (cellRandom.nextFloat() < 0.35F) {
                        chunk.setBlockState(new BlockPos(worldX, height + wallHeight + 1, worldZ),
                                Blocks.STONE_BRICK_SLAB.defaultBlockState(), false);
                    }
                } else if (cellRandom.nextFloat() < 0.06F) {
                    // rubble inside the building
                    chunk.setBlockState(new BlockPos(worldX, height + 1, worldZ),
                            Blocks.COBBLESTONE.defaultBlockState(), false);
                }
            }
        }
    }
}
