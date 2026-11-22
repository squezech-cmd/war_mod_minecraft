package com.warmod.entity;

import com.warmod.config.WarConfig;
import com.warmod.item.GunHandler;
import com.warmod.registry.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * Base class of all five war vehicles. Server authoritative physics, fuel, health, seats and
 * weapons; the client only sends its input and plays the engine sound.
 */
public abstract class VehicleBase extends Entity implements GeoEntity {
    protected static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> DATA_FUEL =
            SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> DATA_SEATS =
            SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> DATA_TURRET_YAW =
            SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Boolean> DATA_DAMAGED =
            SynchedEntityData.defineId(VehicleBase.class, EntityDataSerializers.BOOLEAN);

    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation MOVE = RawAnimation.begin().thenLoop("move");
    protected static final RawAnimation FIRE = RawAnimation.begin().thenPlay("fire");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected float inForward;
    protected float inStrafe;
    protected float inLookYaw;
    protected float inLookPitch;
    protected boolean inBrake;
    protected boolean inBoost;
    protected int weaponCooldown;
    protected int fireFlash;
    protected float enginePitch = 1.0F;

    protected VehicleBase(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.setHealth(this.getMaxHealth());
        this.setFuel(1.0F);
        this.setSeats(this.getSeatCount());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_HEALTH, this.getMaxHealth());
        this.entityData.define(DATA_FUEL, 1.0F);
        this.entityData.define(DATA_SEATS, 2);
        this.entityData.define(DATA_TURRET_YAW, 0.0F);
        this.entityData.define(DATA_DAMAGED, false);
    }

    // ------------------------------------------------------------------ abstract API

    /** Asset id used for the GeckoLib model / texture: assets/warmod/geo/&lt;id&gt;.geo.json */
    public abstract String getVehicleId();

    public abstract float getMaxHealth();

    public abstract int getSeatCount();

    public abstract String getEngineSoundName();

    /** Called every server tick: applies the vehicle specific physics. */
    protected abstract void tickVehicle();

    /** Called on the server when the driver pulls the trigger. Returns the cooldown in ticks. */
    protected int fireWeapon(Player driver) {
        return 0;
    }

    // ------------------------------------------------------------------ data

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH, Mth.clamp(health, 0.0F, this.getMaxHealth()));
        this.entityData.set(DATA_DAMAGED, health < this.getMaxHealth() * 0.5F);
    }

    public float getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    public void setFuel(float fuel) {
        this.entityData.set(DATA_FUEL, Mth.clamp(fuel, 0.0F, 1.0F));
    }

    public int getSeats() {
        return this.entityData.get(DATA_SEATS);
    }

    protected void setSeats(int seats) {
        this.entityData.set(DATA_SEATS, seats);
    }

    public float getTurretYaw() {
        return this.entityData.get(DATA_TURRET_YAW);
    }

    public void setTurretYaw(float yaw) {
        this.entityData.set(DATA_TURRET_YAW, yaw);
    }

    public boolean isDamaged() {
        return this.entityData.get(DATA_DAMAGED);
    }

    // ------------------------------------------------------------------ input

    public void setInput(float forward, float strafe, float lookYaw, float lookPitch, boolean brake, boolean boost) {
        this.inForward = Mth.clamp(forward, -1.0F, 1.0F);
        this.inStrafe = Mth.clamp(strafe, -1.0F, 1.0F);
        this.inLookYaw = lookYaw;
        this.inLookPitch = Mth.clamp(lookPitch, -90.0F, 90.0F);
        this.inBrake = brake;
        this.inBoost = boost;
    }

    public void clearInput() {
        this.inForward = 0.0F;
        this.inStrafe = 0.0F;
        this.inBrake = false;
        this.inBoost = false;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.fireFlash > 0) {
            this.fireFlash--;
        }
        if (this.level().isClientSide) {
            return;
        }
        if (WarConfig.vehiclesNeedFuel()) {
            float burn = Math.abs(this.inForward) * 0.0008F + 0.0001F;
            if (this.getFuel() > 0.0F) {
                this.setFuel(this.getFuel() - burn);
            } else {
                this.clearInput();
            }
        }
        this.tickVehicle();
        this.handleWeapon();
        if (this.getHealth() <= 0.0F) {
            this.destroyVehicle();
        }
        if (this.weaponCooldown > 0) {
            this.weaponCooldown--;
        }
    }

    protected void handleWeapon() {
        if (this.weaponCooldown > 0) {
            return;
        }
        Player driver = this.getDriver();
        if (driver == null || !GunHandler.isTriggerHeld(driver)) {
            return;
        }
        this.weaponCooldown = this.fireWeapon(driver);
    }

    /** Shared car / tank physics. */
    protected void driveGround(double acceleration, double maxSpeed, float turnSpeed) {
        Vec3 delta = this.getDeltaMovement();
        float yawRad = (float) Math.toRadians(this.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        double currentSpeed = delta.dot(forward);

        double target = this.inForward * (this.inForward >= 0.0F ? maxSpeed : maxSpeed * 0.45D);
        if (this.inBoost) {
            target *= 1.35D;
        }
        double newSpeed = Mth.lerp(acceleration, currentSpeed, target);
        if (this.inBrake) {
            newSpeed *= 0.82D;
        }
        if (this.horizontalCollision) {
            newSpeed *= 0.4D;
        }

        float speedFactor = (float) Math.min(1.0D, Math.abs(newSpeed) / 0.25D);
        float steer = this.inStrafe * turnSpeed * speedFactor;
        this.setYRot(this.getYRot() + steer);

        float newYawRad = (float) Math.toRadians(this.getYRot());
        Vec3 newForward = new Vec3(-Math.sin(newYawRad), 0.0D, Math.cos(newYawRad));
        double vertical = this.onGround() ? Math.max(-0.08D, delta.y - 0.06D) : delta.y - 0.05D;
        this.setDeltaMovement(newForward.x * newSpeed, vertical, newForward.z * newSpeed);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.94D));

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
        }
        this.enginePitch = 0.7F + Math.min(1.2F, Math.abs((float) newSpeed) * 0.6F);
    }

    // ------------------------------------------------------------------ damage / destruction

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        this.setHealth(this.getHealth() - amount);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.VEHICLE_HIT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(),
                    4, 0.4D, 0.2D, 0.4D, 0.01D);
        }
        if (this.getHealth() <= 0.0F) {
            this.destroyVehicle();
        } else if (this.getHealth() < this.getMaxHealth() * 0.35F && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.2D, this.getZ(),
                    3, 0.3D, 0.2D, 0.3D, 0.02D);
        }
        return true;
    }

    protected void destroyVehicle() {
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        this.ejectPassengers();
        this.level().explode(this, this.getX(), this.getY() + 0.5D, this.getZ(), 2.4F,
                Level.ExplosionInteraction.NONE);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.VEHICLE_DESTROY.get(), SoundSource.HOSTILE, 3.0F, 0.8F);
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            if (this.getPassengers().size() >= this.getSeats()) {
                return InteractionResult.FAIL;
            }
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    // ------------------------------------------------------------------ passengers

    @Nullable
    public Player getDriver() {
        Entity first = this.getFirstPassenger();
        return first instanceof Player player ? player : null;
    }

    @Override
    @Nullable
    public net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return this.getDriver();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.getSeats();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    public Vec3 seatOffset(int index) {
        return switch (index) {
            case 0 -> new Vec3(0.0D, 1.15D, 0.2D);
            case 1 -> new Vec3(1.1D, 1.1D, -0.4D);
            default -> new Vec3(-1.1D, 1.1D, -0.4D);
        };
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        int index = Math.max(0, this.getPassengers().indexOf(passenger));
        Vec3 offset = this.seatOffset(index);
        Vec3 rotated = offset.yRot((float) -Math.toRadians(this.getYRot()));
        move.accept(passenger, this.getX() + rotated.x, this.getY() + rotated.y, this.getZ() + rotated.z);
    }

    @Override
    public void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player player && this.getPassengers().isEmpty()) {
            this.clearInput();
        }
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setHealth(tag.getFloat("Health"));
        this.setFuel(tag.contains("Fuel") ? tag.getFloat("Fuel") : 1.0F);
        this.setTurretYaw(tag.getFloat("TurretYaw"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", this.getHealth());
        tag.putFloat("Fuel", this.getFuel());
        tag.putFloat("TurretYaw", this.getTurretYaw());
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "vehicle", 5, state -> {
            if (this.fireFlash > 0) {
                return state.setAndContinue(FIRE);
            }
            double speed = this.getDeltaMovement().horizontalDistance();
            return state.setAndContinue(speed > 0.05D ? MOVE : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
