package com.warmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Vehicle machine gun bullet: small, fast, no explosion. */
public class BulletEntity extends WarProjectile {
    private float damage = 4.0F;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.life = 60;
        this.gravity = 0.0D;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 1 == 0) {
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onImpact(@Nullable Entity entity, @Nullable BlockHitResult blockHit) {
        if (this.level().isClientSide) {
            return;
        }
        if (entity != null) {
            this.damage(entity, this.damage);
        }
        this.discard();
    }
}
