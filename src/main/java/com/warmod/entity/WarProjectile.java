package com.warmod.entity;

import com.warmod.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Base class for all simple server simulated war projectiles (rockets, grenades, shells, bullets).
 * Deliberately built on plain {@link Entity} so the physics stays fully under mod control.
 */
public abstract class WarProjectile extends Entity {
    protected int life = 200;
    protected float explosionPower = 3.0F;
    protected double gravity = 0.03D;
    @Nullable
    protected UUID ownerId;

    protected WarProjectile(EntityType<? extends WarProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    public void setOwner(@Nullable Entity owner) {
        this.ownerId = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public Entity getOwner() {
        if (this.ownerId == null || this.level().isClientSide) {
            return null;
        }
        return this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? serverLevel.getEntity(this.ownerId) : null;
    }

    public void setExplosionPower(float power) {
        this.explosionPower = power;
    }

    public void shoot(double x, double y, double z, float speed, float inaccuracy) {
        Vec3 direction = new Vec3(x, y, z).normalize().scale(speed);
        this.setDeltaMovement(direction);
        this.hasImpulse = true;
        float yaw = (float) (net.minecraft.util.Mth.atan2(direction.x, direction.z) * (180.0F / (float) Math.PI));
        float pitch = (float) (net.minecraft.util.Mth.atan2(direction.y, direction.horizontalDistance()) * (180.0F / (float) Math.PI));
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = this.position();
        Vec3 delta = this.getDeltaMovement();
        Vec3 to = from.add(delta);

        BlockHitResult blockHit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            this.setPos(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
            this.onImpact(null, blockHit);
            return;
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), this, from, to,
                new AABB(from, to).inflate(0.35D),
                entity -> !entity.isSpectator() && entity.isPickable() && entity != this.getOwner());
        if (entityHit != null) {
            this.setPos(entityHit.getLocation().x, entityHit.getLocation().y, entityHit.getLocation().z);
            this.onImpact(entityHit.getEntity(), null);
            return;
        }

        this.setPos(to.x, to.y, to.z);
        if (this.gravity != 0.0D && !this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -this.gravity, 0.0D));
        }
        if (!this.level().isClientSide && --this.life <= 0) {
            this.onExpire();
        }
    }

    protected abstract void onImpact(@Nullable Entity entity, @Nullable BlockHitResult blockHit);

    protected void onExpire() {
        this.discard();
    }

    protected void detonate(float power) {
        if (this.level().isClientSide) {
            return;
        }
        this.level().explode(this.getOwner(), this.getX(), this.getY(), this.getZ(), power,
                Level.ExplosionInteraction.TNT);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.EXPLOSION_LARGE.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        this.discard();
    }

    protected void damage(Entity entity, float amount) {
        if (this.getOwner() instanceof LivingEntity owner) {
            entity.hurt(this.level().damageSources().mobProjectile(this, owner), amount);
        } else {
            entity.hurt(this.level().damageSources().explosion(this, null), amount);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.explosionPower = tag.getFloat("Power");
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
        this.setDeltaMovement(tag.getDouble("VX"), tag.getDouble("VY"), tag.getDouble("VZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putFloat("Power", this.explosionPower);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        Vec3 delta = this.getDeltaMovement();
        tag.putDouble("VX", delta.x);
        tag.putDouble("VY", delta.y);
        tag.putDouble("VZ", delta.z);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
