package com.warmod.registry;

import com.warmod.Warmod;
import com.warmod.item.AmmoType;
import com.warmod.item.GunDefinition;
import com.warmod.item.GunRegistry;
import com.warmod.item.VehicleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Warmod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("warmod",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.warmod"))
                    .icon(() -> {
                        Item ak = ItemRegistry.gun("ak47");
                        return ak == null ? ItemStack.EMPTY : new ItemStack(ak);
                    })
                    .displayItems((params, output) -> {
                        for (GunDefinition def : GunRegistry.all()) {
                            Item gun = ItemRegistry.gun(def.id());
                            if (gun != null) {
                                output.accept(gun);
                            }
                        }
                        for (AmmoType type : AmmoType.values()) {
                            Item ammo = ItemRegistry.ammo(type);
                            if (ammo != null) {
                                output.accept(ammo);
                            }
                        }
                        for (VehicleType type : VehicleType.values()) {
                            Item key = ItemRegistry.vehicleKey(type);
                            if (key != null) {
                                output.accept(key);
                            }
                        }
                        output.accept(ItemRegistry.RADIO.get());
                        output.accept(BlockRegistry.AMMO_CRATE_ITEM.get());
                    })
                    .build());

    private CreativeTabRegistry() {}
}
