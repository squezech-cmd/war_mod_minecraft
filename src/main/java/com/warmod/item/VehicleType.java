package com.warmod.item;

import com.warmod.entity.ArmoredCar;
import com.warmod.entity.Helicopter;
import com.warmod.entity.Plane;
import com.warmod.entity.Tank;
import com.warmod.registry.EntityRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Locale;

/** The five spawnable vehicles. */
public enum VehicleType {
    ARMORED_CAR_1("armored_car_1"),
    ARMORED_CAR_2("armored_car_2"),
    PLANE("plane"),
    HELICOPTER("helicopter"),
    TANK("tank");

    private final String id;

    VehicleType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public String nameKey() {
        return "item.warmod." + this.id + "_key";
    }

    public Entity create(Level level) {
        return switch (this) {
            case ARMORED_CAR_1 -> new ArmoredCar(EntityRegistry.ARMORED_CAR_1.get(), level);
            case ARMORED_CAR_2 -> new ArmoredCar(EntityRegistry.ARMORED_CAR_2.get(), level);
            case PLANE -> new Plane(EntityRegistry.PLANE.get(), level);
            case HELICOPTER -> new Helicopter(EntityRegistry.HELICOPTER.get(), level);
            case TANK -> new Tank(EntityRegistry.TANK.get(), level);
        };
    }

    public static VehicleType byId(String id) {
        for (VehicleType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public String key() {
        return this.id.toLowerCase(Locale.ROOT);
    }
}
