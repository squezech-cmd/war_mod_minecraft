package com.warmod.registry;

import com.warmod.Warmod;
import com.warmod.entity.ArmoredCar;
import com.warmod.entity.BulletEntity;
import com.warmod.entity.CorpseEntity;
import com.warmod.entity.GrenadeEntity;
import com.warmod.entity.Helicopter;
import com.warmod.entity.Plane;
import com.warmod.entity.RocketEntity;
import com.warmod.entity.ShellEntity;
import com.warmod.entity.SoldierEntity;
import com.warmod.entity.Tank;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Warmod.MODID);

    public static final RegistryObject<EntityType<SoldierEntity>> SOLDIER = ENTITIES.register("soldier",
            () -> EntityType.Builder.of(SoldierEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.9F).clientTrackingRange(10).build("warmod:soldier"));

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE = ENTITIES.register("corpse",
            () -> EntityType.Builder.of(CorpseEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.5F).clientTrackingRange(8).build("warmod:corpse"));

    public static final RegistryObject<EntityType<RocketEntity>> ROCKET = ENTITIES.register("rocket",
            () -> EntityType.Builder.of(RocketEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build("warmod:rocket"));

    public static final RegistryObject<EntityType<GrenadeEntity>> GRENADE = ENTITIES.register("grenade",
            () -> EntityType.Builder.of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1).build("warmod:grenade"));

    public static final RegistryObject<EntityType<ShellEntity>> SHELL = ENTITIES.register("shell",
            () -> EntityType.Builder.of(ShellEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(10).updateInterval(1).build("warmod:shell"));

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> EntityType.Builder.of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.15F, 0.15F).clientTrackingRange(8).updateInterval(1).build("warmod:bullet"));

    public static final RegistryObject<EntityType<ArmoredCar>> ARMORED_CAR_1 = ENTITIES.register("armored_car_1",
            () -> EntityType.Builder.of(ArmoredCar::new, MobCategory.MISC)
                    .sized(2.6F, 1.9F).clientTrackingRange(10).updateInterval(2).build("warmod:armored_car_1"));

    public static final RegistryObject<EntityType<ArmoredCar>> ARMORED_CAR_2 = ENTITIES.register("armored_car_2",
            () -> EntityType.Builder.of(ArmoredCar::new, MobCategory.MISC)
                    .sized(2.9F, 2.1F).clientTrackingRange(10).updateInterval(2).build("warmod:armored_car_2"));

    public static final RegistryObject<EntityType<Plane>> PLANE = ENTITIES.register("plane",
            () -> EntityType.Builder.of(Plane::new, MobCategory.MISC)
                    .sized(3.0F, 1.2F).clientTrackingRange(12).updateInterval(2).build("warmod:plane"));

    public static final RegistryObject<EntityType<Helicopter>> HELICOPTER = ENTITIES.register("helicopter",
            () -> EntityType.Builder.of(Helicopter::new, MobCategory.MISC)
                    .sized(3.2F, 1.8F).clientTrackingRange(12).updateInterval(2).build("warmod:helicopter"));

    public static final RegistryObject<EntityType<Tank>> TANK = ENTITIES.register("tank",
            () -> EntityType.Builder.of(Tank::new, MobCategory.MISC)
                    .sized(3.2F, 2.2F).clientTrackingRange(12).updateInterval(2).build("warmod:tank"));

    private EntityRegistry() {}

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SOLDIER.get(), SoldierEntity.createAttributes().build());
    }
}
