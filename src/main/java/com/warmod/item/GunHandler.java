package com.warmod.item;

import com.warmod.config.WarConfig;
import com.warmod.network.GunStatePacket;
import com.warmod.network.NetworkHandler;
import com.warmod.network.RecoilPacket;
import com.warmod.network.TracerPacket;
import com.warmod.registry.SoundRegistry;
import com.warmod.util.Ballistics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server authoritative weapon logic: trigger state machine, fire modes, reload timers, recoil,
 * projectile spawning and tracers. The client only reports "trigger held" and plays visuals.
 */
public final class GunHandler {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final UUID ADS_MODIFIER_ID = UUID.fromString("6f3a0d1e-0c6f-4f7a-9a10-4a1c1b0c7e21");

    private GunHandler() {}

    public static final class State {
        boolean trigger;
        boolean prevTrigger;
        boolean aiming;
        int burstLeft;
        int fireCounter;
        long nextShot;
    }

    public static State state(LivingEntity entity) {
        return STATES.computeIfAbsent(entity.getUUID(), key -> new State());
    }

    // ------------------------------------------------------------------ input

    public static void setTrigger(ServerPlayer player, boolean trigger) {
        state(player).trigger = trigger;
    }

    public static void setAiming(net.minecraft.world.entity.player.Player player, boolean aiming) {
        State st = state(player);
        if (st.aiming == aiming) {
            return;
        }
        st.aiming = aiming;
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ADS_MODIFIER_ID);
            if (aiming) {
                speed.addTransientModifier(new AttributeModifier(ADS_MODIFIER_ID, "warmod_ads_slow", -0.35D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        if (player instanceof ServerPlayer serverPlayer) {
            syncState(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        state(event.getEntity()).trigger = false;
        state(event.getEntity()).burstLeft = 0;
        if (event.getEntity() instanceof ServerPlayer player) {
            syncState(player);
        }
    }

    // ------------------------------------------------------------------ per tick

    public static void tick(ServerPlayer player) {
        State st = state(player);
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof GunItem gun) || !player.isAlive() || player.isSpectator()) {
            if (st.aiming) {
                setAiming(player, false);
            }
            st.trigger = false;
            st.prevTrigger = false;
            st.burstLeft = 0;
            return;
        }

        GunDefinition def = gun.getDefinition();
        Level level = player.level();
        long now = level.getGameTime();

        if (def.needsAmmo()) {
            long reloadEnd = GunData.getReloadEnd(stack);
            if (reloadEnd > 0L) {
                if (now >= reloadEnd) {
                    finishReload(player, stack, def);
                }
                st.prevTrigger = st.trigger;
                return;
            }
        }

        FireMode mode = GunData.getMode(stack, def);
        boolean shouldShoot = false;

        if (st.burstLeft > 0) {
            if (now >= st.nextShot) {
                shouldShoot = true;
                st.burstLeft--;
            }
        } else if (st.trigger && now >= st.nextShot) {
            if (mode == FireMode.AUTO) {
                shouldShoot = true;
            } else if (!st.prevTrigger) {
                shouldShoot = true;
                if (mode == FireMode.BURST) {
                    st.burstLeft = 2;
                }
            }
        }

        if (shouldShoot) {
            shoot(player, stack, gun);
            st.nextShot = now + def.fireInterval();
        }
        st.prevTrigger = st.trigger;
    }

    // ------------------------------------------------------------------ shooting

    private static void shoot(ServerPlayer player, ItemStack stack, GunItem gun) {
        GunDefinition def = gun.getDefinition();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        State st = state(player);

        if (def.isMelee()) {
            meleeSwing(player, def);
            st.fireCounter++;
            syncState(player);
            return;
        }

        if (def.projectile() != ProjectileKind.NONE) {
            float speed = def.projectile() == ProjectileKind.ROCKET ? 2.2F : 1.0F;
            GunItem.spawnProjectile(level, player, def, speed);
            playFireSound(level, player, def);
            applyRecoil(player, def, st);
            st.fireCounter++;
            syncState(player);
            return;
        }

        int ammo = GunData.getAmmo(stack, def);
        if (ammo <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.DRY_FIRE.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
            st.trigger = false;
            return;
        }
        GunData.setAmmo(stack, ammo - 1);

        float spread = computeSpread(player, def, st.aiming);
        float damage = def.damage() * (float) WarConfig.damageMul();
        Vec3 direction = player.getViewVector(1.0F);

        for (int pellet = 0; pellet < Math.max(1, def.pellets()); pellet++) {
            Ballistics.Shot shot = Ballistics.fire(level, player, direction, def, spread, damage,
                    level.damageSources().playerAttack(player), true);
            NetworkHandler.sendToTracking(player, new TracerPacket(shot.from(), shot.to(), 0xFFD27A));
        }

        playFireSound(level, player, def);
        applyRecoil(player, def, st);
        muzzleFlash(level, player);
        st.fireCounter++;
        syncState(player);

        if (GunData.getAmmo(stack, def) == 0 && countAmmo(player, def.ammo()) > 0) {
            // automatic reload once the magazine runs dry
            reload(player);
        }
    }

    public static float computeSpread(ServerPlayer player, GunDefinition def, boolean aiming) {
        float spread = def.spread() * (float) WarConfig.spreadMul();
        if (aiming) {
            spread *= 0.35F;
        }
        if (player.isSprinting()) {
            spread *= 1.6F;
        }
        if (!player.onGround()) {
            spread *= 1.5F;
        }
        if (player.isCrouching()) {
            spread *= 0.8F;
        }
        return spread;
    }

    private static void playFireSound(ServerLevel level, ServerPlayer player, GunDefinition def) {
        float pitch = 0.95F + level.random.nextFloat() * 0.1F;
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
                SoundRegistry.get(def.fireSound()), SoundSource.PLAYERS, 1.6F, pitch);
    }

    private static void muzzleFlash(ServerLevel level, ServerPlayer player) {
        Vec3 dir = player.getViewVector(1.0F);
        Vec3 pos = player.getEyePosition().add(dir.scale(1.0D)).add(0.0D, -0.12D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 2, 0.02D, 0.02D, 0.02D, 0.01D);
    }

    private static void applyRecoil(ServerPlayer player, GunDefinition def, State st) {
        float pitch = def.recoilPitch() * (float) WarConfig.recoilMul();
        float yaw = (player.level().random.nextFloat() - 0.5F) * 2.0F * def.recoilYaw() * (float) WarConfig.recoilMul();
        if (st.aiming) {
            pitch *= 0.6F;
            yaw *= 0.6F;
        }
        NetworkHandler.sendToPlayer(player, new RecoilPacket(pitch, yaw));
    }

    private static void meleeSwing(ServerPlayer player, GunDefinition def) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(def.range()));
        AABB box = new AABB(eye, end).inflate(0.45D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, box,
                entity -> entity != player && entity instanceof LivingEntity && entity.isPickable());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundRegistry.MELEE_SWING.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        if (hit != null) {
            hit.getEntity().invulnerableTime = 0;
            hit.getEntity().hurt(level.damageSources().playerAttack(player),
                    def.damage() * (float) WarConfig.damageMul());
            level.playSound(null, hit.getEntity().getX(), hit.getEntity().getY(), hit.getEntity().getZ(),
                    SoundRegistry.MELEE_HIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ reload

    public static void reload(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        GunDefinition def = gun.getDefinition();
        if (!def.needsAmmo()) {
            return;
        }
        long now = player.level().getGameTime();
        if (GunData.getReloadEnd(stack) > 0L) {
            return;
        }
        if (GunData.getAmmo(stack, def) >= def.magSize()) {
            return;
        }
        if (countAmmo(player, def.ammo()) <= 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.DRY_FIRE.get(), SoundSource.PLAYERS, 0.6F, 1.2F);
            return;
        }
        GunData.setReloadEnd(stack, now + def.reloadTicks());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.get(def.reloadSound()), SoundSource.PLAYERS, 1.0F, 1.0F);
        syncState(player);
    }

    private static void finishReload(ServerPlayer player, ItemStack stack, GunDefinition def) {
        int missing = def.magSize() - GunData.getAmmo(stack, def);
        int taken = consumeAmmo(player, def.ammo(), missing);
        GunData.setAmmo(stack, GunData.getAmmo(stack, def) + taken);
        GunData.setReloadEnd(stack, 0L);
        if (taken > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.get(def.reloadSound()), SoundSource.PLAYERS, 0.6F, 1.35F);
        }
        syncState(player);
    }

    public static void cycleFireMode(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        GunDefinition def = gun.getDefinition();
        if (def.modes().size() <= 1) {
            return;
        }
        FireMode mode = GunData.nextMode(stack, def);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundRegistry.UI_CLICK.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(mode.translationKey()), true);
        syncState(player);
    }

    // ------------------------------------------------------------------ helpers

    public static int countAmmo(ServerPlayer player, AmmoType type) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof AmmoItem ammo && ammo.getAmmoType() == type) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int consumeAmmo(ServerPlayer player, AmmoType type, int wanted) {
        int taken = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && taken < wanted; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof AmmoItem ammo && ammo.getAmmoType() == type) {
                int take = Math.min(wanted - taken, stack.getCount());
                stack.shrink(take);
                taken += take;
            }
        }
        return taken;
    }

    public static void syncState(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            NetworkHandler.sendToPlayer(player, new GunStatePacket("", 0, 1, 0, 0L, false, 0));
            return;
        }
        GunDefinition def = gun.getDefinition();
        State st = state(player);
        NetworkHandler.sendToPlayer(player, new GunStatePacket(def.id(), GunData.getAmmo(stack, def),
                def.magSize(), GunData.getMode(stack, def).ordinal(), GunData.getReloadEnd(stack),
                st.aiming, st.fireCounter));
    }

    public static void throwWeapon(Level level, LivingEntity thrower, GunDefinition def) {
        GunItem.spawnProjectile(level, thrower, def, 1.15F);
    }
}
