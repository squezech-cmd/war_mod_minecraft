package com.warmod.entity;

import com.warmod.config.WarConfig;
import com.warmod.item.GunDefinition;
import com.warmod.item.GunRegistry;
import com.warmod.network.NetworkHandler;
import com.warmod.network.TracerPacket;
import com.warmod.registry.EntityRegistry;
import com.warmod.registry.SoundRegistry;
import com.warmod.team.Faction;
import com.warmod.team.TeamManager;
import com.warmod.util.Ballistics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * AI soldier with GeckoLib animations. Server side AI only: targeting, line of sight shooting
 * with realistic spread, reloading, cover seeking and squad orders.
 */
public class SoldierEntity extends PathfinderMob implements GeoEntity {
    public enum Order {
        FOLLOW,
        HOLD,
        ATTACK,
        RETREAT,
        PATROL;

        public String translationKey() {
            return "warmod.order." + this.name().toLowerCase(Locale.ROOT);
        }

        public static Order byOrdinal(int ordinal) {
            Order[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FOLLOW;
        }
    }

    public enum Anim {
        IDLE,
        RUN,
        AIM,
        SHOOT,
        RELOAD,
        DEATH
    }

    private static final EntityDataAccessor<String> DATA_TEAM =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_WEAPON =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_ANIM =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ORDER =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FACTION =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> DATA_ORDER_POS =
            SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BLOCK_POS);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation ANIM_AIM = RawAnimation.begin().thenLoop("aim");
    private static final RawAnimation ANIM_SHOOT = RawAnimation.begin().thenPlay("shoot");
    private static final RawAnimation ANIM_RELOAD = RawAnimation.begin().thenPlay("reload");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("death");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int ammo = 30;
    private int shootCooldown;
    private int reloadTimer;
    private int animTimer;
    private int thinkCooldown;
    private int coverCooldown;

    public SoldierEntity(EntityType<? extends SoldierEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GunGoal(this));
        this.goalSelector.addGoal(2, new MoveGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                this::isEnemyOf));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TEAM, "");
        this.entityData.define(DATA_WEAPON, "ak47");
        this.entityData.define(DATA_ANIM, Anim.IDLE.ordinal());
        this.entityData.define(DATA_ORDER, Order.FOLLOW.ordinal());
        this.entityData.define(DATA_FACTION, Faction.ALPHA.ordinal());
        this.entityData.define(DATA_ORDER_POS, BlockPos.ZERO);
    }

    // ------------------------------------------------------------------ data

    public String getTeamName() {
        return this.entityData.get(DATA_TEAM);
    }

    public void setTeamName(String name) {
        this.entityData.set(DATA_TEAM, name == null ? "" : name);
    }

    public Faction getFaction() {
        return Faction.byOrdinal(this.entityData.get(DATA_FACTION));
    }

    public void setFaction(Faction faction) {
        this.entityData.set(DATA_FACTION, faction.ordinal());
    }

    public Order getOrder() {
        return Order.byOrdinal(this.entityData.get(DATA_ORDER));
    }

    public BlockPos getOrderPos() {
        return this.entityData.get(DATA_ORDER_POS);
    }

    public void setOrder(Order order, BlockPos pos) {
        this.entityData.set(DATA_ORDER, order.ordinal());
        if (pos != null) {
            this.entityData.set(DATA_ORDER_POS, pos);
        }
        this.thinkCooldown = 0;
    }

    public Anim getAnim() {
        return Anim.values()[Mth.clamp(this.entityData.get(DATA_ANIM), 0, Anim.values().length - 1)];
    }

    public void setAnim(Anim anim) {
        this.entityData.set(DATA_ANIM, anim.ordinal());
    }

    public String getWeaponId() {
        return this.entityData.get(DATA_WEAPON);
    }

    @Nullable
    public GunDefinition getGun() {
        return GunRegistry.byId(this.getWeaponId());
    }

    public void equip(String gunId) {
        GunDefinition def = GunRegistry.byId(gunId);
        if (def == null) {
            return;
        }
        this.entityData.set(DATA_WEAPON, gunId);
        this.ammo = def.magSize();
        this.reloadTimer = 0;
    }

    public boolean isEnemyOf(LivingEntity other) {
        if (other == this || !other.isAlive() || other instanceof SoldierEntity soldier && soldier == this) {
            return false;
        }
        if (other instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return TeamManager.areEnemies(this, other);
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.animTimer > 0) {
                this.animTimer--;
            }
            return;
        }
        if (this.shootCooldown > 0) {
            this.shootCooldown--;
        }
        if (this.animTimer > 0) {
            this.animTimer--;
            if (this.animTimer == 0 && this.getAnim() != Anim.DEATH) {
                this.setAnim(Anim.IDLE);
            }
        }
        if (this.reloadTimer > 0) {
            this.reloadTimer--;
            this.setAnim(Anim.RELOAD);
            if (this.reloadTimer == 0) {
                GunDefinition def = this.getGun();
                this.ammo = def == null ? 30 : def.magSize();
            }
        }
        if (this.thinkCooldown > 0) {
            this.thinkCooldown--;
        }
        if (this.coverCooldown > 0) {
            this.coverCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || !TeamManager.areEnemies(this, target))) {
            this.setTarget(null);
        }
    }

    /** Called by the gun goal: fire a single round at the target with weapon spread + bot accuracy. */
    public void tryShoot(LivingEntity target) {
        GunDefinition def = this.getGun();
        if (def == null || this.level().isClientSide()) {
            return;
        }
        if (this.reloadTimer > 0) {
            return;
        }
        double distance = this.distanceTo(target);
        if (distance > def.range() || !this.hasLineOfSight(target)) {
            return;
        }
        if (this.shootCooldown > 0) {
            return;
        }
        if (this.ammo <= 0) {
            this.reloadTimer = Math.max(20, def.reloadTicks());
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundRegistry.get(def.reloadSound()), SoundSource.HOSTILE, 0.8F, 1.0F);
            return;
        }
        this.ammo--;
        this.shootCooldown = Math.max(6, (int) (def.fireInterval() * 2.2D));
        float accuracy = (float) WarConfig.botAccuracy();
        float spread = def.spread() * (1.7F - accuracy) * (float) (1.0D + distance / Math.max(1.0F, def.range()));
        this.setAnim(Anim.AIM);
        this.animTimer = 6;

        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 direction = target.getEyePosition().subtract(this.getEyePosition()).normalize();
            for (int pellet = 0; pellet < Math.max(1, def.pellets()); pellet++) {
                Ballistics.Shot shot = Ballistics.fire(serverLevel, this, direction, def, spread,
                        def.damage() * (float) WarConfig.damageMul() * 0.75F,
                        serverLevel.damageSources().mobAttack(this), true);
                NetworkHandler.sendToTracking(this, new TracerPacket(shot.from(), shot.to(), 0xFF9A3C));
            }
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundRegistry.get(def.fireSound()), SoundSource.HOSTILE, 1.1F, 1.0F);
            this.setAnim(Anim.SHOOT);
            this.animTimer = 4;
        }
    }

    /** Simple cover search: pick a nearby solid block and walk to the opposite side. */
    public void seekCover(LivingEntity threat) {
        if (this.coverCooldown > 0 || this.level().isClientSide()) {
            return;
        }
        this.coverCooldown = 60;
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dx = -6; dx <= 6; dx += 2) {
            for (int dz = -6; dz <= 6; dz += 2) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!this.level().isEmptyBlock(pos) || !this.level().isEmptyBlock(pos.above())) {
                        continue;
                    }
                    if (this.level().isEmptyBlock(pos.west()) && this.level().isEmptyBlock(pos.east())
                            && this.level().isEmptyBlock(pos.north()) && this.level().isEmptyBlock(pos.south())) {
                        continue;
                    }
                    double score = pos.distSqr(threat.blockPosition());
                    if (score < bestScore) {
                        bestScore = score;
                        best = pos;
                    }
                }
            }
        }
        if (best != null) {
            this.getNavigation().moveTo(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D, 1.25D);
        }
    }

    @Override
    public void die(DamageSource source) {
        this.setAnim(Anim.DEATH);
        if (!this.level().isClientSide) {
            TeamManager.onSoldierKilled(this);
            CorpseEntity corpse = new CorpseEntity(EntityRegistry.CORPSE.get(), this.level());
            corpse.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            corpse.setVariant(this.getWeaponId());
            this.level().addFreshEntity(corpse);
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Team", this.getTeamName());
        tag.putString("Weapon", this.getWeaponId());
        tag.putInt("Faction", this.entityData.get(DATA_FACTION));
        tag.putInt("Order", this.entityData.get(DATA_ORDER));
        tag.putInt("OrderX", this.getOrderPos().getX());
        tag.putInt("OrderY", this.getOrderPos().getY());
        tag.putInt("OrderZ", this.getOrderPos().getZ());
        tag.putInt("Ammo", this.ammo);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setTeamName(tag.getString("Team"));
        this.equip(tag.contains("Weapon") ? tag.getString("Weapon") : "ak47");
        this.entityData.set(DATA_FACTION, tag.getInt("Faction"));
        this.setOrder(Order.byOrdinal(tag.getInt("Order")),
                new BlockPos(tag.getInt("OrderX"), tag.getInt("OrderY"), tag.getInt("OrderZ")));
        this.ammo = tag.contains("Ammo") ? tag.getInt("Ammo") : this.ammo;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag tag) {
        if (this.getWeaponId() == null || GunRegistry.byId(this.getWeaponId()) == null) {
            List<GunDefinition> pool = GunRegistry.firearmPool();
            this.equip(pool.get(this.random.nextInt(pool.size())).id());
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "soldier", 4, state -> {
            Anim anim = this.getAnim();
            if (this.isDeadOrDying()) {
                return state.setAndContinue(ANIM_DEATH);
            }
            return switch (anim) {
                case RUN -> state.setAndContinue(ANIM_RUN);
                case AIM -> state.setAndContinue(ANIM_AIM);
                case SHOOT -> state.setAndContinue(ANIM_SHOOT);
                case RELOAD -> state.setAndContinue(ANIM_RELOAD);
                case DEATH -> state.setAndContinue(ANIM_DEATH);
                default -> state.setAndContinue(ANIM_IDLE);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ------------------------------------------------------------------ goals

    /** Aims and shoots at the current target. */
    private static final class GunGoal extends Goal {
        private final SoldierEntity soldier;

        private GunGoal(SoldierEntity soldier) {
            this.soldier = soldier;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.soldier.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = this.soldier.getTarget();
            if (target == null) {
                return;
            }
            this.soldier.getLookControl().setLookAt(target, 40.0F, 40.0F);
            if (this.soldier.reloadTimer > 0) {
                this.soldier.seekCover(target);
                return;
            }
            if (this.soldier.distanceTo(target) > Math.max(3.0F, this.weaponRange() * 0.85F)) {
                this.soldier.getNavigation().moveTo(target, 1.1D);
            } else if (this.soldier.distanceTo(target) < 3.0F) {
                Vec3 away = this.soldier.position().subtract(target.position()).normalize().scale(2.0D);
                this.soldier.getNavigation().moveTo(
                        this.soldier.getX() + away.x, this.soldier.getY(), this.soldier.getZ() + away.z, 1.0D);
            }
            this.soldier.tryShoot(target);
        }

        private float weaponRange() {
            GunDefinition def = this.soldier.getGun();
            return def == null ? 30.0F : def.range();
        }

        @Override
        public void stop() {
            this.soldier.setAnim(Anim.IDLE);
        }
    }

    /** Squad order handling: follow / hold / attack a position / retreat / patrol. */
    private static final class MoveGoal extends Goal {
        private final SoldierEntity soldier;
        private int pathCooldown;

        private MoveGoal(SoldierEntity soldier) {
            this.soldier = soldier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            if (this.pathCooldown > 0) {
                this.pathCooldown--;
                return;
            }
            this.pathCooldown = 20 + this.soldier.random.nextInt(20);
            Order order = this.soldier.getOrder();
            LivingEntity target = this.soldier.getTarget();
            switch (order) {
                case HOLD -> {
                    if (this.soldier.blockPosition().distSqr(this.soldier.getOrderPos()) > 16) {
                        this.moveTo(this.soldier.getOrderPos(), 1.0D);
                    } else {
                        this.soldier.getNavigation().stop();
                    }
                }
                case ATTACK -> this.moveTo(this.soldier.getOrderPos(), 1.1D);
                case RETREAT -> {
                    if (target != null) {
                        Vec3 away = this.soldier.position().subtract(target.position()).normalize().scale(12.0D);
                        this.soldier.getNavigation().moveTo(
                                this.soldier.getX() + away.x, this.soldier.getY(),
                                this.soldier.getZ() + away.z, 1.2D);
                    }
                }
                case PATROL -> {
                    if (this.soldier.getNavigation().isDone()) {
                        BlockPos center = this.soldier.getOrderPos();
                        double x = center.getX() + (this.soldier.random.nextDouble() - 0.5D) * 16.0D;
                        double z = center.getZ() + (this.soldier.random.nextDouble() - 0.5D) * 16.0D;
                        this.soldier.getNavigation().moveTo(x, center.getY(), z, 0.8D);
                    }
                }
                default -> {
                    // FOLLOW: stay close to the nearest player of the same team
                    Player leader = this.nearestTeammate();
                    if (leader != null && this.soldier.distanceToSqr(leader) > 25.0D) {
                        this.soldier.getNavigation().moveTo(leader, 1.15D);
                    } else if (this.soldier.getNavigation().isDone()) {
                        this.soldier.getNavigation().stop();
                    }
                }
            }
            this.soldier.setAnim(this.soldier.getNavigation().isInProgress() ? Anim.RUN : Anim.IDLE);
        }

        private void moveTo(BlockPos pos, double speed) {
            if (this.soldier.blockPosition().distSqr(pos) > 9) {
                this.soldier.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, speed);
            }
        }

        @Nullable
        private Player nearestTeammate() {
            Player best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Player player : this.soldier.level().getEntitiesOfClass(Player.class,
                    this.soldier.getBoundingBox().inflate(32.0D))) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                if (!this.soldier.getTeamName().equalsIgnoreCase(TeamManager.teamNameOf(player))) {
                    continue;
                }
                double distance = this.soldier.distanceToSqr(player);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = player;
                }
            }
            return best;
        }
    }
}
