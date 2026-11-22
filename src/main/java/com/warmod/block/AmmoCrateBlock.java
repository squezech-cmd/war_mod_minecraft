package com.warmod.block;

import com.warmod.item.GunData;
import com.warmod.item.GunItem;
import com.warmod.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ammunition crate: right click with a weapon to refill its magazine. */
public class AmmoCrateBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D);

    public AmmoCrateBlock() {
        super(Properties.of().mapColor(MapColor.METAL).strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof GunItem gun)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        GunData.setAmmo(stack, gun.getDefinition().magSize());
        level.playSound(null, pos, SoundRegistry.RELOAD_HEAVY.get(), SoundSource.BLOCKS, 1.0F, 1.1F);
        player.displayClientMessage(Component.translatable("warmod.msg.crate_refill",
                Component.translatable(gun.getDefinition().nameKey())), true);
        player.getCooldowns().addCooldown(stack.getItem(), 20);
        return InteractionResult.CONSUME;
    }
}
