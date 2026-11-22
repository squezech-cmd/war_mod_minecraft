package com.warmod.registry;

import com.warmod.Warmod;
import com.warmod.block.AmmoCrateBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Warmod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Warmod.MODID);

    public static final RegistryObject<Block> AMMO_CRATE = BLOCKS.register("ammo_crate", AmmoCrateBlock::new);
    public static final RegistryObject<Item> AMMO_CRATE_ITEM = ITEMS.register("ammo_crate",
            () -> new BlockItem(AMMO_CRATE.get(), new Item.Properties()));

    private BlockRegistry() {}
}
