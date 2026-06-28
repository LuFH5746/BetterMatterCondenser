package appeng.blockentity.misc;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.me.helpers.MachineSource;
import com.bettermattercondenser.BMCConfig;
import com.bettermattercondenser.RedstoneMode;
import com.bettermattercondenser.mixin.CondenserBlockEntityMixinAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = CondenserBlockEntity.class, remap = false)
public abstract class CondenserBlockEntityMixin implements CondenserBlockEntityMixinAccessor {

    @Unique
    private static final Set<CondenserBlockEntity> bmc$SERVER_ENTITIES = ConcurrentHashMap.newKeySet();

    @Unique
    private static final int bmc$PRIORITY_MIN = -999;
    @Unique
    private static final int bmc$PRIORITY_MAX = 999;

    @Unique
    private RedstoneMode bmc$redstoneMode = RedstoneMode.IGNORE;
    @Unique
    private int bmc$priority = 0;
    @Unique
    private long bmc$placementTime = 0;
    @Unique
    private boolean bmc$hasRedstoneSignal = false;
    @Unique
    private boolean bmc$autoExport = false;

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void bmc$onSetLevel(Level level, CallbackInfo ci) {
        if (this.bmc$placementTime == 0 && level != null) {
            this.bmc$placementTime = level.getGameTime();
        }
    }

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void bmc$onLoad(CallbackInfo ci) {
        var self = (CondenserBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide()) {
            bmc$SERVER_ENTITIES.add(self);
            this.bmc$updateRedstoneState(self.getLevel().hasNeighborSignal(self.getBlockPos()));
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void bmc$onRemoved(CallbackInfo ci) {
        bmc$SERVER_ENTITIES.remove((CondenserBlockEntity) (Object) this);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void bmc$saveAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        var child = new CompoundTag();
        child.putString("RedstoneMode", this.bmc$redstoneMode.name());
        child.putInt("Priority", this.bmc$priority);
        child.putLong("PlacementTime", this.bmc$placementTime);
        tag.put("bmc_data", child);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void bmc$loadAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains("bmc_data", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var child = tag.getCompound("bmc_data");
            String modeStr = child.getString("RedstoneMode");
            if (modeStr.isEmpty()) {
                modeStr = "IGNORE";
            }
            try {
                this.bmc$redstoneMode = RedstoneMode.valueOf(modeStr);
            } catch (IllegalArgumentException e) {
                this.bmc$redstoneMode = RedstoneMode.IGNORE;
            }
            this.bmc$priority = bmc$clampPriority(child.getInt("Priority"));
            this.bmc$placementTime = child.getLong("PlacementTime");
        }
    }

    @Unique
    private static int bmc$clampPriority(int value) {
        return Math.max(bmc$PRIORITY_MIN, Math.min(bmc$PRIORITY_MAX, value));
    }

    @Override
    public void bmc$setRedstoneMode(RedstoneMode mode) {
        this.bmc$redstoneMode = mode;
        this.bmc$updateAutoExportState();
        var self = (CondenserBlockEntity) (Object) this;
        self.setChanged();
    }

    @Override
    public void bmc$setPriority(int priority) {
        this.bmc$priority = bmc$clampPriority(priority);
        var self = (CondenserBlockEntity) (Object) this;
        self.setChanged();
    }

    @Unique
    public RedstoneMode bmc$getRedstoneMode() {
        return this.bmc$redstoneMode;
    }

    @Unique
    public int bmc$getPriority() {
        return this.bmc$priority;
    }

    @Unique
    public long bmc$getPlacementTime() {
        return this.bmc$placementTime;
    }

    @Unique
    public void bmc$updateRedstoneState(boolean hasSignal) {
        if (this.bmc$hasRedstoneSignal != hasSignal) {
            this.bmc$hasRedstoneSignal = hasSignal;
            this.bmc$updateAutoExportState();
        }
    }

    @Unique
    public void bmc$updateAutoExportState() {
        var self = (CondenserBlockEntity) (Object) this;
        IGrid grid = bmc$getGrid(self.getLevel(), self.getBlockPos());
        if (grid == null) {
            this.bmc$autoExport = false;
            return;
        }
        this.bmc$autoExport = this.bmc$redstoneMode.shouldExport(this.bmc$hasRedstoneSignal);
    }

    @Unique
    public boolean bmc$isAutoExport() {
        return this.bmc$autoExport;
    }

    @Unique
    public static int bmc$acceptTrashItems(ServerLevel level, ItemStack stack, @Nullable IGrid grid) {
        if (level == null || stack.isEmpty()) return 0;

        List<CondenserBlockEntity> candidates = new ArrayList<>();
        for (CondenserBlockEntity condenser : bmc$SERVER_ENTITIES) {
            if (condenser.getLevel() != level) continue;
            IGrid condenserGrid = bmc$getGrid(condenser.getLevel(), condenser.getBlockPos());
            if (condenserGrid == null) continue;
            if (grid != null && condenserGrid != grid) continue;
            if (!condenser.canAddOutput()) continue;
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
            int accepted = bmc$insertTrash(condenser, stack.copyWithCount(remainingCount));
            remainingCount -= accepted;
            if (remainingCount <= 0) break;
        }

        return stack.getCount() - remainingCount;
    }

    @Unique
    private static int bmc$insertTrash(CondenserBlockEntity condenser, ItemStack stack) {
        var input = condenser.getExternalInv();
        var remaining = input.insertItem(0, stack, false);
        return stack.getCount() - remaining.getCount();
    }

    @Unique
    public static void bmc$tickAll(ServerLevel level) {
        int rateLimit = BMCConfig.getExportRateLimit();
        for (CondenserBlockEntity condenser : bmc$SERVER_ENTITIES) {
            if (condenser.getLevel() != level) continue;
            var accessor = (CondenserBlockEntityMixinAccessor) condenser;
            accessor.bmc$updateAutoExportState();
            if (accessor.bmc$isAutoExport()) {
                accessor.bmc$tryExportResults(rateLimit);
            }
        }
    }

    @Unique
    public void bmc$tryExportResults(int rateLimit) {
        var self = (CondenserBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) return;

        IGridNode node = bmc$getActionableNode(level, self.getBlockPos());
        if (node == null) return;

        IGrid grid = node.getGrid();
        if (grid == null) return;

        var storage = grid.getStorageService().getInventory();
        var src = new MachineSource(() -> node);

        var outputSlot = self.getOutputSlot();
        ItemStack stack = outputSlot.getStackInSlot(0);
        if (stack.isEmpty()) return;

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) return;

        int toExport = Math.min(stack.getCount(), rateLimit);
        long inserted = storage.insert(key, toExport, Actionable.MODULATE, src);
        if (inserted > 0) {
            outputSlot.extractItem(0, (int) inserted, false);
            self.setChanged();
        }
    }

    @Unique
    @Nullable
    public static IGrid bmc$getGrid(Level level, BlockPos pos) {
        var node = bmc$getActionableNode(level, pos);
        return node != null ? node.getGrid() : null;
    }

    @Unique
    @Nullable
    public static IGridNode bmc$getActionableNode(Level level, BlockPos pos) {
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
