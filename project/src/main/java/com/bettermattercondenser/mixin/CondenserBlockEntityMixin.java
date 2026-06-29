package com.bettermattercondenser.mixin;

import appeng.api.networking.IGrid;
import appeng.blockentity.misc.CondenserBlockEntity;
import com.bettermattercondenser.CondenserLogic;
import com.bettermattercondenser.RedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CondenserBlockEntity.class, remap = false)
public abstract class CondenserBlockEntityMixin implements CondenserBlockEntityMixinAccessor {

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
            CondenserLogic.register(self);
            this.bmc$updateRedstoneState(self.getLevel().hasNeighborSignal(self.getBlockPos()));
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void bmc$onRemoved(CallbackInfo ci) {
        CondenserLogic.unregister((CondenserBlockEntity) (Object) this);
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

    @Override
    public RedstoneMode bmc$getRedstoneMode() {
        return this.bmc$redstoneMode;
    }

    @Override
    public int bmc$getPriority() {
        return this.bmc$priority;
    }

    @Override
    public long bmc$getPlacementTime() {
        return this.bmc$placementTime;
    }

    @Override
    public void bmc$updateRedstoneState(boolean hasSignal) {
        if (this.bmc$hasRedstoneSignal != hasSignal) {
            this.bmc$hasRedstoneSignal = hasSignal;
            this.bmc$updateAutoExportState();
        }
    }

    @Override
    public void bmc$updateAutoExportState() {
        var self = (CondenserBlockEntity) (Object) this;
        IGrid grid = CondenserLogic.getGrid(self.getLevel(), self.getBlockPos());
        if (grid == null) {
            this.bmc$autoExport = false;
            return;
        }
        this.bmc$autoExport = this.bmc$redstoneMode.shouldExport(this.bmc$hasRedstoneSignal);
    }

    @Override
    public boolean bmc$isAutoExport() {
        return this.bmc$autoExport;
    }

    @Override
    public void bmc$tryExportResults(int rateLimit) {
        CondenserLogic.tryExportResults((CondenserBlockEntity) (Object) this, rateLimit);
    }
}
