package com.warmod.entity;

import com.warmod.registry.EntityRegistry;
import com.warmod.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Tank: separate hull and turret rotation, mouse aimed cannon, heavy armored. */
public class Tank extends VehicleBase {

    public Tank(EntityType<? extends Tank> type, Level level) {
        super(type, level);
    }

    @Override
    public String getVehicleId() {
        return "tank";
    }

    @Override
    public float getMaxHealth() {
        return 420.0F;
    }

    @Override
    public int getSeatCount() {
        return 2;
    }

    @Override
    public String getEngineSoundName() {
        return "engine_tank";
    }

    @Override
    protected void tickVehicle() {
        this.driveGround(0.035D, 0.30D, 2.4F);

        Player driver = this.getDriver();
        if (driver != null) {
            float target = this.getYRot() - driver.getYRot();
            this.setTurretYaw(this.getTurretYaw() + Mth.wrapDegrees(target - this.getTurretYaw()) * 0.12F);
        }
    }

    @Override
    protected int fireWeapon(Player driver) {
        Vec3 dir = driver.getViewVector(1.0F);
        Vec3 muzzle = this.getEyePosition().add(dir.scale(3.2D)).add(0.0D, 0.1D, 0.0D);
        ShellEntity shell = new ShellEntity(EntityRegistry.SHELL.get(), this.level());
        shell.setOwner(driver);
        shell.setExplosionPower(4.5F);
        shell.moveTo(muzzle.x, muzzle.y, muzzle.z, this.getYRot(), this.getXRot());
        shell.shoot(dir.x, dir.y, dir.z, 3.6F, 0.2F);
        this.level().addFreshEntity(shell);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.TANK_CANNON.get(), SoundSource.HOSTILE, 4.0F, 0.9F);
        this.fireFlash = 8;
        // recoil
        this.setDeltaMovement(this.getDeltaMovement().add(dir.scale(-0.25D)));
        return 80;
    }
}
