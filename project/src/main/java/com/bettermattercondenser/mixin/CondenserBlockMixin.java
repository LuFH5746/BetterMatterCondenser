package com.bettermattercondenser.mixin;

import appeng.block.misc.CondenserBlock;
import appeng.blockentity.misc.CondenserBlockEntity;
import com.bettermattercondenser.mixin.CondenserBlockEntityMixinAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CondenserBlock.class, remap = false)
public abstract class CondenserBlockMixin {

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void bmc$onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof CondenserBlockEntity condenser) {
            var accessor = (CondenserBlockEntityMixinAccessor) condenser;
            accessor.bmc$updateRedstoneState(level.hasNeighborSignal(pos));
        }
    }
}
