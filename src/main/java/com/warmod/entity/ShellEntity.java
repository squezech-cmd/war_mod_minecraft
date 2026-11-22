package com.warmod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Tank cannon shell: fast, heavy explosion, visible smoke trail. */
public class ShellEntity extends WarProjectile {
    public ShellEntity(EntityType<? extends ShellEntity> type, Level level) {
        super(type, level);
        this.life = 200;
        this.gravity = 0.02D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 1,
                    0.05D, 0.05D, 0.05D, 0.005D);
        }
    }

    @Override
    protected void onImpact(@Nullable Entity entity, @Nullable BlockHitResult blockHit) {
        if (this.level().isClientSide) {
            return;
        }
        if (entity != null) {
            this.damage(entity, 20.0F);
        }
        this.detonate(Math.max(2.0F, this.explosionPower));
    }
}
