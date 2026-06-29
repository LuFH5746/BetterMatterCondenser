package com.bettermattercondenser.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.menu.me.common.MEStorageMenu;
import com.bettermattercondenser.CondenserLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MEStorageMenu.class, remap = false)
public abstract class MEStorageMenuMixin {

    @Shadow
    protected abstract ItemStack getCarried();

    @Shadow
    protected abstract void setCarried(ItemStack stack);

    @Shadow
    public abstract Player getPlayer();

    @Shadow
    public abstract IGridNode getGridNode();

    @Inject(method = "putCarriedItemIntoNetwork", at = @At("HEAD"), cancellable = true)
    private void bmc$interceptClear(boolean all, CallbackInfo ci) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;

        Player player = getPlayer();
        if (player == null || player.level().isClientSide()) return;

        IGridNode gridNode = getGridNode();
        if (gridNode == null) return;

        IGrid grid = gridNode.getGrid();
        if (grid == null) return;

        int insertCount = all ? carried.getCount() : 1;
        ItemStack toInsert = carried.copy();
        toInsert.setCount(insertCount);

        int accepted = CondenserLogic.acceptTrashItems((ServerLevel) player.level(), toInsert, grid);
        if (accepted > 0) {
            carried.shrink(accepted);
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            if (carried.isEmpty()) {
                ci.cancel();
            }
        }
    }
}
