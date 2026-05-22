package com.bettermolecularassembler.menu;

import com.bettermolecularassembler.BetterMolecularAssemblerMod;
import com.bettermolecularassembler.block.BetterMABlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BetterMAMenu extends AbstractContainerMenu {
    private final BetterMABlockEntity blockEntity;

    public BetterMAMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf.readBlockPos()));
    }

    public BetterMAMenu(int id, Inventory playerInventory, BetterMABlockEntity blockEntity) {
        super(BetterMolecularAssemblerMod.BETTER_MOLECULAR_ASSEMBLER_MENU.get(), id);
        this.blockEntity = blockEntity;

        // Input slots (0-8)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(blockEntity.getInventory(), col + row * 3, 26 + col * 18, 17 + row * 18));
            }
        }

        // Output slots (9-17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new OutputSlot(blockEntity.getInventory(), 9 + col + row * 3, 116 + col * 18, 17 + row * 18));
            }
        }

        // Pattern slots (18-20)
        for (int i = 0; i < 3; i++) {
            this.addSlot(new PatternSlot(blockEntity.getInventory(), 18 + i, 62 + i * 18, 79));
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 111 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 169));
        }
    }

    private static BetterMABlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level() == null) return null;
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof BetterMABlockEntity ma ? ma : null;
    }

    public BetterMABlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < 21) {
                if (!this.moveItemStackTo(stack, 21, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 18, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.blockEntity != null && this.blockEntity.getInventory().stillValid(player);
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }

    private static class PatternSlot extends Slot {
        public PatternSlot(net.minecraft.world.Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem().getDescriptionId().contains("pattern");
        }
    }
}
