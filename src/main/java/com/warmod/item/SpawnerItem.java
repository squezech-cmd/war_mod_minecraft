package com.warmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Vehicle spawner key. Right click a block to deploy the vehicle. */
public class SpawnerItem extends Item {
    private final VehicleType vehicleType;

    public SpawnerItem(VehicleType vehicleType) {
        super(new Item.Properties().stacksTo(1));
        this.vehicleType = vehicleType;
    }

    public VehicleType getVehicleType() {
        return this.vehicleType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Entity vehicle = this.vehicleType.create(level);
        vehicle.moveTo(pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D,
                player == null ? 0.0F : player.getYRot(), 0.0F);
        level.addFreshEntity(vehicle);
        if (player != null && !player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(this, 200);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("warmod.tooltip.vehicle_key").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("warmod.tooltip.vehicle." + this.vehicleType.id())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
