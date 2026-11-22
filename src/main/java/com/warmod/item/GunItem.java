package com.warmod.item;

import com.warmod.Warmod;
import com.warmod.client.renderer.GunGeoModel;
import com.warmod.client.renderer.GunItemRenderer;
import com.warmod.registry.EntityRegistry;
import com.warmod.registry.SoundRegistry;
import com.warmod.util.ClientGunState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * One class for every weapon in the mod: the per-weapon behaviour is fully described by its
 * {@link GunDefinition}. Rendering and animations are GeckoLib based, gameplay is server side.
 */
public class GunItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DRAW = RawAnimation.begin().thenPlay("draw");
    private static final RawAnimation FIRE = RawAnimation.begin().thenPlay("fire");
    private static final RawAnimation RELOAD = RawAnimation.begin().thenPlay("reload");
    private static final RawAnimation ADS = RawAnimation.begin().thenLoop("ads");

    private final GunDefinition definition;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GunItem(GunDefinition definition) {
        super(new Item.Properties().stacksTo(1));
        this.definition = definition;
        // enables server side animation triggering / sync for this singleton animatable
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public GunDefinition getDefinition() {
        return this.definition;
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "gun_controller", 2, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<GunItem> state) {
        if (!ClientGunState.gunId.equals(this.definition.id())) {
            return state.setAndContinue(IDLE);
        }
        if (ClientGunState.justFired()) {
            return state.setAndContinue(FIRE);
        }
        if (ClientGunState.reloading) {
            return state.setAndContinue(RELOAD);
        }
        if (ClientGunState.aiming) {
            return state.setAndContinue(ADS);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GunItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GunItemRenderer(GunItem.this);
                }
                return this.renderer;
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static GunGeoModel modelFor(GunItem item) {
        return new GunGeoModel(item.definition.id());
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GunDefinition def = this.definition;

        if (def.isThrowable()) {
            if (!level.isClientSide) {
                GunHandler.throwWeapon(level, player, def);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            } else {
                level.playSound(player, player.getX(), player.getY(), player.getZ(),
                        SoundRegistry.THROW.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            player.getCooldowns().addCooldown(this, Math.max(10, def.rpm() > 0 ? 1200 / def.rpm() : 20));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (def.isMelee()) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);
        if (!level.isClientSide) {
            GunHandler.setAiming(player, true);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof Player player) {
            GunHandler.setAiming(player, false);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // ------------------------------------------------------------------ display

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.definition.nameKey());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return this.definition.needsAmmo() && GunData.getAmmo(stack, this.definition) < this.definition.magSize();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!this.definition.needsAmmo()) {
            return 13;
        }
        return Math.round(13.0F * GunData.getAmmo(stack, this.definition) / Math.max(1, this.definition.magSize()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xC8A03C;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level,
                                 net.minecraft.core.BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        GunDefinition def = this.definition;
        tooltip.add(Component.translatable("warmod.tooltip.category." + def.category().key())
                .withStyle(ChatFormatting.DARK_GRAY));
        if (def.needsAmmo()) {
            tooltip.add(Component.translatable("warmod.tooltip.ammo", def.ammo().key(), def.magSize())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("warmod.tooltip.damage", String.format(java.util.Locale.ROOT, "%.1f", def.damage()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("warmod.tooltip.firemode", def.defaultMode().name())
                    .withStyle(ChatFormatting.GRAY));
        } else if (def.isMelee()) {
            tooltip.add(Component.translatable("warmod.tooltip.damage", String.format(java.util.Locale.ROOT, "%.1f", def.damage()))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("warmod.tooltip.hint." + def.category().key())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Spawns the projectile belonging to a launcher/throwable. */
    public static void spawnProjectile(Level level, LivingEntity shooter, GunDefinition def, float speed) {
        if (level.isClientSide) {
            return;
        }
        net.minecraft.world.phys.Vec3 dir = shooter.getViewVector(1.0F);
        net.minecraft.world.phys.Vec3 spawn = shooter.getEyePosition().add(dir.scale(1.1D));
        switch (def.projectile()) {
            case ROCKET, GRENADE_40MM -> {
                var rocket = new com.warmod.entity.RocketEntity(EntityRegistry.ROCKET.get(), level);
                rocket.setOwner(shooter);
                rocket.moveTo(spawn.x, spawn.y, spawn.z, shooter.getYRot(), shooter.getXRot());
                rocket.shoot(dir.x, dir.y, dir.z, speed, 1.0F);
                rocket.setExplosionPower(def.explosionPower());
                rocket.setGrenade(def.projectile() == ProjectileKind.GRENADE_40MM);
                level.addFreshEntity(rocket);
            }
            case GRENADE_FRAG, GRENADE_SMOKE, C4 -> {
                var grenade = new com.warmod.entity.GrenadeEntity(EntityRegistry.GRENADE.get(), level);
                grenade.setOwner(shooter);
                grenade.moveTo(spawn.x, spawn.y, spawn.z, shooter.getYRot(), shooter.getXRot());
                grenade.shoot(dir.x, dir.y + 0.12D, dir.z, speed, 0.6F);
                grenade.configure(def.explosionPower(), def.projectile());
                level.addFreshEntity(grenade);
            }
            default -> Warmod.LOGGER.warn("Weapon {} has no projectile", def.id());
        }
    }
}
