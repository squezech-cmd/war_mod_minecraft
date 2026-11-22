package com.warmod.entity;

import com.warmod.registry.EntityRegistry;
import com.warmod.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Attack helicopter: collective pitch, vertical take off, yaw with A/D, door guns. */
public class Helicopter extends VehicleBase {
    private double rotors = 0.0D;

    public Helicopter(EntityType<? extends Helicopter> type, Level level) {
        super(type, level);
    }

    @Override
    public String getVehicleId() {
        return "helicopter";
    }

    @Override
    public float getMaxHealth() {
        return 200.0F;
    }

    @Override
    public int getSeatCount() {
        return 3;
    }

    @Override
    public String getEngineSoundName() {
        return "engine_heli";
    }

    public double getRotorAngle() {
        return this.rotors;
    }

    @Override
    protected void tickVehicle() {
        this.rotors += 0.9D;
        Player driver = this.getDriver();

        // collective: W climbs, S descends, space boost
        double collective = Math.max(0.0F, this.inForward);
        double vertical = collective * 0.22D - 0.07D;
        if (this.inBoost) {
            vertical += 0.12D;
        }
        if (this.inBrake) {
            vertical -= 0.12D;
        }
        vertical = Mth.clamp(vertical, -0.35D, 0.45D);

        // yaw
        this.setYRot(this.getYRot() + this.inStrafe * 2.6F);
        // slight pitch attitude in the flight direction
        float pitchTarget = this.inForward > 0.1F ? 8.0F : (this.inForward < -0.1F ? -8.0F : 0.0F);
        this.setXRot(Mth.lerp(0.08F, this.getXRot(), pitchTarget));

        float yawRad = (float) Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        double horizontalSpeed = Math.abs(this.inForward) * 0.28D;
        Vec3 motion = forward.scale(horizontalSpeed).add(0.0D, vertical, 0.0D);
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
        if (this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
        }
        this.enginePitch = 1.0F + (float) vertical;
        if (driver != null && this.tickCount % 12 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundRegistry.ENGINE_HELI.get(), SoundSource.NEUTRAL, 3.0F, this.enginePitch);
        }
    }

    @Override
    protected int fireWeapon(Player driver) {
        Vec3 dir = driver.getViewVector(1.0F);
        Vec3 muzzle = this.position().add(dir.scale(2.0D)).add(0.0D, 0.4D, 0.0D);
        BulletEntity bullet = new BulletEntity(EntityRegistry.BULLET.get(), this.level());
        bullet.setOwner(driver);
        bullet.setDamage(5.0F);
        bullet.moveTo(muzzle.x, muzzle.y, muzzle.z, this.getYRot(), this.getXRot());
        bullet.shoot(dir.x, dir.y, dir.z, 3.0F, 0.8F);
        this.level().addFreshEntity(bullet);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.GUNSHOT_MG.get(), SoundSource.NEUTRAL, 1.5F, 1.1F);
        this.fireFlash = 4;
        return 5;
    }
}
