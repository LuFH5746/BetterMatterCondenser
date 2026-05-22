package com.bettermolecularassembler.block;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.me.helpers.MachineSource;
import com.bettermolecularassembler.BetterMolecularAssemblerMod;
import com.bettermolecularassembler.menu.BetterMAMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BetterMABlockEntity extends AENetworkedBlockEntity implements ICraftingMachine, MenuProvider {
    public static final int INVENTORY_SIZE = 18;
    public static final int PATTERN_SLOTS = 3;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE + PATTERN_SLOTS) {
        @Override
        public void setChanged() {
            BetterMABlockEntity.this.setChanged();
        }
    };

    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private int priority = 0;
    private long placementTime = 0;
    private boolean hasRedstoneSignal = false;
    private boolean autoExport = false;

    private boolean isCrafting = false;
    private int craftingProgress = 0;
    private int craftingTotalTime = 0;
    private ItemStack craftingResult = ItemStack.EMPTY;
    private int[] craftingInputs = new int[0];

    public BetterMABlockEntity(BlockPos pos, BlockState state) {
        super(BetterMolecularAssemblerMod.BETTER_MOLECULAR_ASSEMBLER_ENTITY.get(), pos, state);
        this.placementTime = System.currentTimeMillis();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (this.placementTime == 0) {
            this.placementTime = System.currentTimeMillis();
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("RedstoneMode", this.redstoneMode.getSerializedName());
        tag.putInt("Priority", this.priority);
        tag.putLong("PlacementTime", this.placementTime);
        tag.putBoolean("AutoExport", this.autoExport);

        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                invTag.put("Slot" + i, stack.save(registries, new CompoundTag()));
            }
        }
        tag.put("Inventory", invTag);
    }

    @Override
    public void loadTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        this.redstoneMode = RedstoneMode.valueOf(tag.getString("RedstoneMode").toUpperCase());
        this.priority = tag.getInt("Priority");
        this.placementTime = tag.getLong("PlacementTime");
        this.autoExport = tag.getBoolean("AutoExport");

        if (tag.contains("Inventory")) {
            CompoundTag invTag = tag.getCompound("Inventory");
            for (int i = 0; i < this.inventory.getContainerSize(); i++) {
                String key = "Slot" + i;
                if (invTag.contains(key)) {
                    this.inventory.setItem(i, ItemStack.parse(registries, invTag.getCompound(key)).orElse(ItemStack.EMPTY));
                } else {
                    this.inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    public void updateRedstoneState(boolean hasSignal) {
        this.hasRedstoneSignal = hasSignal;
        updateAutoExportState();
    }

    private void updateAutoExportState() {
        boolean shouldExport = this.redstoneMode.shouldExport(this.hasRedstoneSignal);
        if (shouldExport != this.autoExport) {
            this.autoExport = shouldExport;
            if (this.autoExport) {
                tryExportResults();
            }
        }
    }

    public RedstoneMode getRedstoneMode() {
        return this.redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        this.redstoneMode = mode;
        updateAutoExportState();
        this.setChanged();
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        this.setChanged();
    }

    public long getPlacementTime() {
        return this.placementTime;
    }

    public boolean isAutoExport() {
        return this.autoExport;
    }

    public Container getInventory() {
        return this.inventory;
    }

    public ItemStack getPatternInSlot(int slot) {
        return this.inventory.getItem(INVENTORY_SIZE + slot);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.bettermolecularassembler.better_molecular_assembler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new BetterMAMenu(id, playerInventory, this);
    }

    @Override
    public @NotNull AECableType getCableConnectionType(@NotNull Direction dir) {
        return AECableType.SMART;
    }

    // ICraftingMachine implementation

    @Override
    public boolean pushPattern(@NotNull IPatternDetails patternDetails, @NotNull KeyCounter[] inputHolder, @NotNull Direction where) {
        if (this.isCrafting || this.level == null) {
            return false;
        }

        this.isCrafting = true;
        this.craftingProgress = 0;
        this.craftingTotalTime = 40;
        this.craftingResult = ItemStack.EMPTY;

        var inputs = patternDetails.getInputs();
        this.craftingInputs = new int[inputs.length];
        for (int i = 0; i < inputs.length && i < INVENTORY_SIZE / 2; i++) {
            craftingInputs[i] = i;
        }

        List<GenericStack> outputs = patternDetails.getOutputs();
        if (!outputs.isEmpty()) {
            var outKey = outputs.get(0).what();
            if (outKey instanceof AEItemKey itemKey) {
                this.craftingResult = itemKey.toStack((int) outputs.get(0).amount());
            }
        }

        this.setChanged();
        return true;
    }

    @Override
    public boolean acceptsPlans() {
        return !this.isCrafting;
    }

    @Override
    public @NotNull appeng.api.implementations.blockentities.PatternContainerGroup getCraftingMachineInfo() {
        var stack = BetterMolecularAssemblerMod.BETTER_MOLECULAR_ASSEMBLER_ITEM.toStack();
        var key = appeng.api.stacks.AEItemKey.of(stack);
        return new appeng.api.implementations.blockentities.PatternContainerGroup(
                key != null ? key : appeng.api.stacks.AEItemKey.of(stack),
                this.getDisplayName(),
                java.util.List.of()
        );
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BetterMABlockEntity be) {
        if (be.isCrafting) {
            be.craftingProgress++;
            if (be.craftingProgress >= be.craftingTotalTime) {
                be.finishCrafting();
            }
        }

        if (be.autoExport) {
            be.tryExportResults();
        }
    }

    private void finishCrafting() {
        this.isCrafting = false;
        this.craftingProgress = 0;

        if (!this.craftingResult.isEmpty()) {
            ItemStack remaining = this.craftingResult.copy();
            for (int i = INVENTORY_SIZE / 2; i < INVENTORY_SIZE && !remaining.isEmpty(); i++) {
                ItemStack existing = this.inventory.getItem(i);
                if (existing.isEmpty()) {
                    this.inventory.setItem(i, remaining.copy());
                    remaining = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < existing.getMaxStackSize()) {
                    int canAdd = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                    existing.grow(canAdd);
                    remaining.shrink(canAdd);
                }
            }
            this.craftingResult = remaining;
        }

        for (int idx : this.craftingInputs) {
            if (idx >= 0 && idx < INVENTORY_SIZE / 2) {
                this.inventory.setItem(idx, ItemStack.EMPTY);
            }
        }
        this.craftingInputs = new int[0];
        this.setChanged();

        if (this.level != null) {
            this.level.setBlock(this.worldPosition, this.getBlockState().setValue(BetterMABlock.LIT, false), 3);
        }
    }

    private void tryExportResults() {
        if (this.level == null || this.level.isClientSide) return;

        var grid = this.getMainNode().getGrid();
        if (grid == null) return;

        var storage = grid.getStorageService().getInventory();
        var src = new MachineSource(this);

        for (int i = INVENTORY_SIZE / 2; i < INVENTORY_SIZE; i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (stack.isEmpty()) continue;

            AEItemKey key = AEItemKey.of(stack);
            if (key == null) continue;

            long inserted = storage.insert(key, stack.getCount(), Actionable.MODULATE, src);
            if (inserted > 0) {
                stack.shrink((int) inserted);
                if (stack.isEmpty()) {
                    this.inventory.setItem(i, ItemStack.EMPTY);
                }
                this.setChanged();
            }
        }
    }

    public boolean acceptTrashItems(ItemStack stack) {
        if (this.level == null || this.level.isClientSide) return false;
        if (this.getMainNode().getGrid() == null) return false;

        ItemStack remaining = stack.copy();
        for (int i = 0; i < INVENTORY_SIZE / 2 && !remaining.isEmpty(); i++) {
            ItemStack existing = this.inventory.getItem(i);
            if (existing.isEmpty()) {
                this.inventory.setItem(i, remaining.copy());
                remaining = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(existing, remaining) && existing.getCount() < existing.getMaxStackSize()) {
                int canAdd = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(canAdd);
                remaining.shrink(canAdd);
            }
        }

        if (remaining.getCount() < stack.getCount()) {
            this.setChanged();
            return true;
        }
        return false;
    }

    public boolean isCrafting() {
        return this.isCrafting;
    }

    public int getCraftingProgress() {
        return this.craftingProgress;
    }

    public int getCraftingTotalTime() {
        return this.craftingTotalTime;
    }
}
