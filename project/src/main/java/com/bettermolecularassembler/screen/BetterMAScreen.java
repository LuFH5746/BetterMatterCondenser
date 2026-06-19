package com.bettermolecularassembler.screen;

import com.bettermolecularassembler.BetterMolecularAssemblerMod;
import com.bettermolecularassembler.block.BetterMABlockEntity;
import com.bettermolecularassembler.block.RedstoneMode;
import com.bettermolecularassembler.menu.BetterMAMenu;
import com.bettermolecularassembler.network.SetPriorityPacket;
import com.bettermolecularassembler.network.SetRedstoneModePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class BetterMAScreen extends AbstractContainerScreen<BetterMAMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BetterMolecularAssemblerMod.MOD_ID, "textures/gui/molecular_assembler.png"
    );

    private EditBox priorityField;
    private Button redstoneButton;
    private Button topButton;
    private Button bottomButton;

    public BetterMAScreen(BetterMAMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 193;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        this.priorityField = new EditBox(this.font, left + 7, top + 79, 40, 12,
                Component.literal("Priority"));
        this.priorityField.setValue(String.valueOf(this.menu.getBlockEntity().getPriority()));
        this.priorityField.setFilter(s -> s.matches("-?\\d*"));
        this.priorityField.setResponder(this::onPriorityChanged);
        this.addRenderableWidget(this.priorityField);

        this.topButton = Button.builder(Component.literal("T"), b -> this.setPriority(BetterMABlockEntity.PRIORITY_MAX))
                .bounds(left + 49, top + 79, 12, 12).build();
        this.addRenderableWidget(this.topButton);

        this.bottomButton = Button.builder(Component.literal("B"), b -> this.setPriority(BetterMABlockEntity.PRIORITY_MIN))
                .bounds(left + 63, top + 79, 12, 12).build();
        this.addRenderableWidget(this.bottomButton);

        this.redstoneButton = Button.builder(
                Component.literal(getRedstoneModeLabel(this.menu.getBlockEntity().getRedstoneMode())),
                b -> this.cycleRedstoneMode()
        ).bounds(left + 118, top + 79, 50, 12).build();
        this.addRenderableWidget(this.redstoneButton);
    }

    private void setPriority(int value) {
        this.menu.getBlockEntity().setPriority(value);
        this.priorityField.setValue(String.valueOf(value));
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new SetPriorityPacket(this.menu.getBlockEntity().getBlockPos(), value)
        );
    }

    private static String getRedstoneModeLabel(RedstoneMode mode) {
        return switch (mode) {
            case IGNORE -> "OFF";
            case NORMAL -> "ON";
            case INVERTED -> "INV";
        };
    }

    private void onPriorityChanged(String value) {
        try {
            int priority = value.isEmpty() || value.equals("-") ? 0 : Integer.parseInt(value);
            this.menu.getBlockEntity().setPriority(priority);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new SetPriorityPacket(this.menu.getBlockEntity().getBlockPos(), priority)
            );
        } catch (NumberFormatException ignored) {
        }
    }

    private void cycleRedstoneMode() {
        RedstoneMode next = this.menu.getBlockEntity().getRedstoneMode().next();
        this.menu.getBlockEntity().setRedstoneMode(next);
        this.redstoneButton.setMessage(Component.literal(getRedstoneModeLabel(next)));
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new SetRedstoneModePacket(this.menu.getBlockEntity().getBlockPos(), next)
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        if (this.redstoneButton.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    Component.translatable("gui.bettermolecularassembler.redstone_mode.tooltip",
                            this.menu.getBlockEntity().getRedstoneMode().getSerializedName()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.getBlockEntity().isCrafting()) {
            int progress = this.menu.getBlockEntity().getCraftingProgress();
            int total = this.menu.getBlockEntity().getCraftingTotalTime();
            int width = total > 0 ? progress * 24 / total : 0;
            graphics.blit(TEXTURE, this.leftPos + 76, this.topPos + 34, 176, 0, width, 17);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.redstoneButton.setMessage(
                Component.literal(getRedstoneModeLabel(this.menu.getBlockEntity().getRedstoneMode()))
        );
    }
}
