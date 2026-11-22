package com.warmod.entity;

import com.warmod.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** RPG / 40mm projectile. Explodes on impact. */
public class RocketEntity extends WarProjectile {
    private boolean grenade;

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
        this.life = 120;
        this.gravity = 0.0D;
    }

    public void setGrenade(boolean grenade) {
        this.grenade = grenade;
        this.gravity = grenade ? 0.02D : 0.0D;
    }

    @Override
    protected void onImpact(@Nullable Entity entity, @Nullable BlockHitResult blockHit) {
        if (this.level().isClientSide) {
            return;
        }
        if (entity != null) {
            this.damage(entity, 8.0F);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundRegistry.EXPLOSION_SMALL.get(), SoundSource.HOSTILE, 3.0F, 1.1F);
        this.detonate(this.grenade ? Math.max(1.5F, this.explosionPower) : this.explosionPower);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.grenade = tag.getBoolean("Grenade");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Grenade", this.grenade);
    }
}
