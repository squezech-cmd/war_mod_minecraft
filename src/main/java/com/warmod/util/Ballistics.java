package com.warmod.util;

import com.warmod.item.GunDefinition;
import com.warmod.registry.SoundRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Server side hitscan ballistics: one ray, optional spread, block clipping, headshot detection.
 */
public final class Ballistics {
    private Ballistics() {}

    public record Shot(Vec3 from, Vec3 to, @Nullable Entity hitEntity, boolean headshot) {}

    /**
     * Fires a single ray and applies damage.
     *
     * @param spreadDegrees cone half-angle in degrees
     * @param damage        damage per ray (already multiplied by config)
     */
    public static Shot fire(ServerLevel level, LivingEntity shooter, Vec3 direction, GunDefinition def,
                           float spreadDegrees, float damage, DamageSource source, boolean tracer) {
        Vec3 from = shooter.getEyePosition();
        Vec3 dir = applySpread(direction, spreadDegrees, level.random::nextDouble);
        double range = def.range();
        Vec3 to = from.add(dir.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        Vec3 stop = to;
        if (blockHit.getType() != HitResult.Type.MISS) {
            stop = blockHit.getLocation();
        }

        Predicate<Entity> filter = e -> !e.isSpectator() && e.isPickable() && e != shooter
                && (!(e instanceof LivingEntity le) || le.isAlive());
        AABB box = new AABB(from, to).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level, shooter, from, to, box, filter);

        Entity hitEntity = null;
        boolean headshot = false;
        if (entityHit != null) {
            Vec3 loc = entityHit.getLocation();
            if (loc.distanceToSqr(from) <= stop.distanceToSqr(from)) {
                hitEntity = entityHit.getEntity();
                stop = loc;
                if (hitEntity instanceof LivingEntity living) {
                    headshot = loc.y > living.getEyeY() - 0.22D;
                    float finalDamage = headshot ? damage * 1.6F : damage;
                    hitEntity.invulnerableTime = 0;
                    hitEntity.hurt(source, finalDamage);
                    if (headshot) {
                        level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                                SoundRegistry.HEADSHOT.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                }
            }
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            BlockState state = level.getBlockState(blockHit.getBlockPos());
            if (!state.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        stop.x, stop.y, stop.z, 6, 0.1D, 0.1D, 0.1D, 0.05D);
                level.playSound(null, stop.x, stop.y, stop.z, SoundRegistry.BULLET_IMPACT.get(),
                        SoundSource.PLAYERS, 0.35F, 1.4F);
            }
        }

        if (tracer) {
            level.playSound(null, from.x, from.y, from.z, SoundRegistry.BULLET_IMPACT.get(),
                    SoundSource.PLAYERS, 0.0F, 1.8F);
        }
        return new Shot(from, stop, hitEntity, headshot);
    }

    /** Rotates the direction inside a cone of {@code degrees} half-angle. */
    public static Vec3 applySpread(Vec3 direction, float degrees, java.util.function.DoubleSupplier rnd) {
        if (degrees <= 0.0F) {
            return direction;
        }
        double radians = Math.toRadians(degrees) * Math.sqrt(rnd.getAsDouble());
        double theta = rnd.getAsDouble() * Math.PI * 2.0D;
        Vec3 dir = direction.normalize();
        Vec3 up = Math.abs(dir.y) > 0.99D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();
        return dir.add(right.scale(Math.cos(theta) * radians)).add(realUp.scale(Math.sin(theta) * radians)).normalize();
    }
}
