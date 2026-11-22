package com.warmod.entity;

import com.warmod.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.warmod.registry.EntityRegistry;

/** Light attack plane: pitch / yaw / roll, afterburner, simple altitude assistance. */
public class Plane extends VehicleBase {
    private boolean gearDown = true;

    public Plane(EntityType<? extends Plane> type, Level level) {
        super(type, level);
    }

    @Override
    public String getVehicleId() {
        return "plane";
    }

    @Override
    public float getMaxHealth() {
        return 120.0F;
    }

    @Override
    public int getSeatCount() {
        return 2;
    }

    @Override
    public String getEngineSoundName() {
        return "engine_plane";
    }

    public boolean isGearDown() {
        return this.gearDown;
    }

    @Override
    protected void tickVehicle() {
        Player driver = this.getDriver();
        float thrust = 0.35F + Math.max(0.0F, this.inForward) * 0.55F;
        if (this.inBoost) {
            thrust *= 1.8F;
        }

        // roll + yaw with A/D, pitch with the mouse
        this.setYRot(this.getYRot() + this.inStrafe * 1.6F);
        this.setXRot(Mth.clamp(this.getXRot() + this.inLookPitch * 0.35F, -70.0F, 70.0F));
        if (Math.abs(this.inLookPitch) < 0.01F) {
            this.setXRot(Mth.lerp(0.05F, this.getXRot(), 0.0F));
        }

        Vec3 direction = this.getViewVector(1.0F);
        Vec3 motion = direction.scale(thrust);
        // gravity takes over when flying too slow or without thrust
        double lift = Math.max(thrust, 0.25F);
        motion = motion.add(0.0D, -0.035D + lift * 0.03D, 0.0D);

        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
        if (this.horizontalCollision || this.verticalCollision) {
            if (this.onGround()) {
                this.gearDown = true;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.55D));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.3D));
                if (this.level() instanceof net.minecraft.server.level.ServerLevel) {
                    this.hurt(this.level().damageSources().flyIntoWall(), 12.0F);
                }
            }
        }
        this.enginePitch = 0.8F + thrust * 0.6F;
        this.gearDown = this.onGround();
        if (driver != null && this.tickCount % 5 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundRegistry.ENGINE_PLANE.get(), SoundSource.NEUTRAL, 3.0F, this.enginePitch);
        }
    }

    @Override
    protected int fireWeapon(Player driver) {
        Vec3 dir = driver.getViewVector(1.0F);
        Vec3 muzzle = this.position().add(dir.scale(2.4D));
        BulletEntity bullet = new BulletEntity(EntityRegistry.BULLET.get(), this.level());
        bullet.setOwner(driver);
        bullet.setDamage(6.0F);
        bullet.moveTo(muzzle.x, muzzle.y, muzzle.z, this.getYRot(), this.getXRot());
        bullet.shoot(dir.x, dir.y, dir.z, 3.5F, 0.6F);
        this.level().addFreshEntity(bullet);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.GUNSHOT_50CAL.get(), SoundSource.NEUTRAL, 1.8F, 1.0F);
        this.fireFlash = 4;
        return 5;
    }
}
