package com.bettermattercondenser;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.misc.CondenserBlockEntity;
import appeng.me.helpers.MachineSource;
import com.bettermattercondenser.mixin.CondenserBlockEntityInvoker;
import com.bettermattercondenser.mixin.CondenserBlockEntityMixinAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CondenserLogic {

    private static final Set<CondenserBlockEntity> SERVER_ENTITIES = ConcurrentHashMap.newKeySet();

    private CondenserLogic() {
    }

    public static void register(CondenserBlockEntity condenser) {
        SERVER_ENTITIES.add(condenser);
    }

    public static void unregister(CondenserBlockEntity condenser) {
        SERVER_ENTITIES.remove(condenser);
    }

    public static int acceptTrashItems(ServerLevel level, ItemStack stack, @Nullable IGrid grid) {
        if (level == null || stack.isEmpty()) return 0;

        List<CondenserBlockEntity> candidates = new ArrayList<>();
        for (CondenserBlockEntity condenser : SERVER_ENTITIES) {
            if (condenser.getLevel() != level) continue;
            IGrid condenserGrid = getGrid(condenser.getLevel(), condenser.getBlockPos());
            if (condenserGrid == null) continue;
            if (grid != null && condenserGrid != grid) continue;
            if (!canAddOutput(condenser)) continue;
            candidates.add(condenser);
        }

        if (candidates.isEmpty()) return 0;

        var mode = BMCConfig.getPrioritySelectionMode();
        Comparator<CondenserBlockEntity> comparator;
        if (mode == BMCConfig.PrioritySelectionMode.LAST_PLACED) {
            comparator = Comparator
                    .comparingLong((CondenserBlockEntity c) -> ((CondenserBlockEntityMixinAccessor) c).bmc$getPlacementTime())
                    .reversed();
        } else {
            comparator = Comparator
                    .comparingInt((CondenserBlockEntity c) -> ((CondenserBlockEntityMixinAccessor) c).bmc$getPriority())
                    .reversed()
                    .thenComparingLong(c -> ((CondenserBlockEntityMixinAccessor) c).bmc$getPlacementTime()).reversed();
        }

        candidates.sort(comparator);

        int remainingCount = stack.getCount();
        for (CondenserBlockEntity condenser : candidates) {
            int accepted = insertTrash(condenser, stack.copyWithCount(remainingCount));
            remainingCount -= accepted;
            if (remainingCount <= 0) break;
        }

        return stack.getCount() - remainingCount;
    }

    public static void tickAll(ServerLevel level) {
        int rateLimit = BMCConfig.getExportRateLimit();
        for (CondenserBlockEntity condenser : SERVER_ENTITIES) {
            if (condenser.getLevel() != level) continue;
            var accessor = (CondenserBlockEntityMixinAccessor) condenser;
            accessor.bmc$updateAutoExportState();
            if (accessor.bmc$isAutoExport()) {
                accessor.bmc$tryExportResults(rateLimit);
            }
        }
    }

    public static boolean canAddOutput(CondenserBlockEntity condenser) {
        return ((CondenserBlockEntityInvoker) condenser).callCanAddOutput();
    }

    public static InternalInventory getOutputSlot(CondenserBlockEntity condenser) {
        return ((CondenserBlockEntityInvoker) condenser).callGetOutputSlot();
    }

    public static int insertTrash(CondenserBlockEntity condenser, ItemStack stack) {
        var input = condenser.getExternalInv();
        var remaining = input.insertItem(0, stack, false);
        return stack.getCount() - remaining.getCount();
    }

    public static boolean tryExportResults(CondenserBlockEntity condenser, int rateLimit) {
        Level level = condenser.getLevel();
        if (level == null || level.isClientSide()) return false;

        IGridNode node = getActionableNode(level, condenser.getBlockPos());
        if (node == null) return false;

        IGrid grid = node.getGrid();
        if (grid == null) return false;

        var storage = grid.getStorageService().getInventory();
        var src = new MachineSource((IActionHost) () -> node);

        var outputSlot = getOutputSlot(condenser);
        if (outputSlot == null) return false;
        ItemStack stack = outputSlot.getStackInSlot(0);
        if (stack.isEmpty()) return false;

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) return false;

        int toExport = Math.min(stack.getCount(), rateLimit);
        long inserted = storage.insert(key, toExport, Actionable.MODULATE, src);
        if (inserted > 0) {
            outputSlot.extractItem(0, (int) inserted, false);
            condenser.setChanged();
            return true;
        }
        return false;
    }

    @Nullable
    public static IGrid getGrid(Level level, BlockPos pos) {
        var node = getActionableNode(level, pos);
        return node != null ? node.getGrid() : null;
    }

    @Nullable
    public static IGridNode getActionableNode(Level level, BlockPos pos) {
        if (level == null || level.isClientSide()) return null;
        for (Direction dir : Direction.values()) {
            var node = appeng.api.networking.GridHelper.getExposedNode(level, pos.relative(dir), dir.getOpposite());
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
