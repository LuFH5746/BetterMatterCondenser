package com.bettermolecularassembler.mixin;

import appeng.api.networking.IGridNode;
import appeng.menu.me.common.MEStorageMenu;
import com.bettermolecularassembler.block.BetterMABlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(value = MEStorageMenu.class, remap = false)
public abstract class MEStorageMenuMixin {

    @Shadow
    protected abstract ItemStack getCarried();

    @Shadow
    protected abstract void setCarried(ItemStack stack);

    @Shadow
    public abstract IGridNode getGridNode();

    @Inject(method = "putCarriedItemIntoNetwork", at = @At("HEAD"), cancellable = true)
    private void betterma$interceptClear(boolean all, CallbackInfo ci) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;

        IGridNode node = getGridNode();
        if (node == null) return;

        var grid = node.getGrid();
        if (grid == null) return;

        List<BetterMABlockEntity> assemblers = new ArrayList<>();
        for (var machine : grid.getActiveMachines(BetterMABlockEntity.class)) {
            assemblers.add(machine);
        }

        if (assemblers.isEmpty()) return;

        assemblers.sort(Comparator.comparingInt(BetterMABlockEntity::getPriority).reversed()
                .thenComparing(Comparator.comparingLong(BetterMABlockEntity::getPlacementTime).reversed()));

        int insertCount = all ? carried.getCount() : 1;
        ItemStack toInsert = carried.copy();
        toInsert.setCount(insertCount);

        for (BetterMABlockEntity assembler : assemblers) {
            if (!assembler.hasInputSpace()) continue;

            int accepted = assembler.acceptTrashItems(toInsert);
            if (accepted > 0) {
                carried.shrink(accepted);
                setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
                if (carried.isEmpty()) {
                    ci.cancel();
                }
                return;
            }
        }
    }
}
