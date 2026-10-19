package com.endruqq.imperium.block;

import com.endruqq.imperium.game.Treasury;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** National treasury: right-click with materials to deposit them, sneak + right-click to open it. */
public class TreasuryBlock extends Block {
    public TreasuryBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown() || player.getItemInHand(hand).isEmpty()) {
                if (hand == InteractionHand.MAIN_HAND) Treasury.open(sp, pos);
            } else {
                Treasury.deposit(sp, hand, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
