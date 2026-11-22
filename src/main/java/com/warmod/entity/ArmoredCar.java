package com.warmod.entity;

import com.warmod.registry.EntityRegistry;
import com.warmod.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** ArmoredCar_1 (light, fast) and ArmoredCar_2 (heavy, more armor, slower). */
public class ArmoredCar extends VehicleBase {

    public ArmoredCar(EntityType<? extends ArmoredCar> type, Level level) {
        super(type, level);
    }

    public boolean isHeavy() {
        return this.getType() == EntityRegistry.ARMORED_CAR_2.get();
    }

    @Override
    public String getVehicleId() {
        return this.isHeavy() ? "armored_car_2" : "armored_car_1";
    }

    @Override
    public float getMaxHealth() {
        return this.isHeavy() ? 260.0F : 180.0F;
    }

    @Override
    public int getSeatCount() {
        return 3;
    }

    @Override
    public String getEngineSoundName() {
        return "engine_car";
    }

    @Override
    protected void tickVehicle() {
        double acceleration = this.isHeavy() ? 0.045D : 0.07D;
        double maxSpeed = this.isHeavy() ? 0.42D : 0.58D;
        float turn = this.isHeavy() ? 3.2F : 4.4F;
        this.driveGround(acceleration, maxSpeed, turn);
    }

    @Override
    protected int fireWeapon(Player driver) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return 10;
        }
        Vec3 dir = driver.getViewVector(1.0F);
        Vec3 muzzle = this.getEyePosition().add(dir.scale(2.0D)).add(0.0D, -0.3D, 0.0D);
        BulletEntity bullet = new BulletEntity(EntityRegistry.BULLET.get(), this.level());
        bullet.setOwner(driver);
        bullet.setDamage(4.0F);
        bullet.moveTo(muzzle.x, muzzle.y, muzzle.z, this.getYRot(), this.getXRot());
        bullet.shoot(dir.x, dir.y, dir.z, 3.2F, 0.5F);
        this.level().addFreshEntity(bullet);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.GUNSHOT_MG.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        this.fireFlash = 4;
        return 6;
    }
}
