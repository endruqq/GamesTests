package com.endruqq.imperium.block;

import com.endruqq.imperium.game.WarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The heart of a province, hidden in its tower. Destroy it to conquer the province. */
public class ProvinceCoreBlock extends Block {
    public ProvinceCoreBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            WarManager.playerHitsCore(sl, sp, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            WarManager.describeCore(sl, sp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
