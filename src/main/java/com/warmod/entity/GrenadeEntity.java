package com.warmod.entity;

import com.warmod.item.ProjectileKind;
import com.warmod.registry.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Frag / smoke grenade and C4. Bounces, then detonates after its fuse. */
public class GrenadeEntity extends WarProjectile {
    private ProjectileKind kind = ProjectileKind.GRENADE_FRAG;
    private int fuse = 60;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
        this.life = 400;
        this.gravity = 0.045D;
    }

    public void configure(float power, ProjectileKind kind) {
        this.explosionPower = power;
        this.kind = kind;
        this.fuse = switch (kind) {
            case C4 -> 60;
            case GRENADE_SMOKE -> 40;
            default -> 60;
        };
    }

    @Override
    protected void onImpact(@Nullable Entity entity, @Nullable BlockHitResult blockHit) {
        if (blockHit != null) {
            Vec3 delta = this.getDeltaMovement();
            Vec3 normal = Vec3.atLowerCornerOf(blockHit.getDirection().getNormal());
            Vec3 bounced = delta.subtract(normal.scale(1.4D * delta.dot(normal))).scale(0.45D);
            this.setDeltaMovement(bounced);
            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundRegistry.GRENADE_BOUNCE.get(), SoundSource.PLAYERS, 0.7F, 1.2F);
            }
        } else if (entity != null) {
            this.damage(entity, 4.0F);
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.3D));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.kind == ProjectileKind.GRENADE_SMOKE) {
            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.getX(), this.getY() + 0.2D, this.getZ(), 3, 0.15D, 0.15D, 0.15D, 0.01D);
        }
        if (--this.fuse <= 0) {
            this.detonateNow();
        }
    }

    private void detonateNow() {
        if (this.kind == ProjectileKind.GRENADE_SMOKE) {
            for (int i = 0; i < 60; i++) {
                double angle = i / 60.0D * Math.PI * 2.0D;
                ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX() + Math.cos(angle) * 1.6D, this.getY() + 0.4D,
                        this.getZ() + Math.sin(angle) * 1.6D, 2, 0.4D, 0.4D, 0.4D, 0.02D);
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundRegistry.EXPLOSION_SMALL.get(), SoundSource.PLAYERS, 1.0F, 1.6F);
            this.discard();
            return;
        }
        this.detonate(this.explosionPower);
    }

    @Override
    protected void onExpire() {
        this.detonateNow();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.fuse = tag.getInt("Fuse");
        this.kind = ProjectileKind.values()[Math.max(0, Math.min(ProjectileKind.values().length - 1, tag.getInt("Kind")))];
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", this.fuse);
        tag.putInt("Kind", this.kind.ordinal());
    }
}
