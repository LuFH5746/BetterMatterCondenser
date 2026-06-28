package appeng.client.gui.implementations;

import appeng.menu.implementations.CondenserMenu;
import com.bettermattercondenser.RedstoneMode;
import com.bettermattercondenser.mixin.CondenserMenuMixinAccessor;
import com.bettermattercondenser.network.SetPriorityPacket;
import com.bettermattercondenser.network.SetRedstoneModePacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CondenserScreen.class, remap = false)
public abstract class CondenserScreenMixin {

    private static final int BMC_PRIORITY_MIN = -999;
    private static final int BMC_PRIORITY_MAX = 999;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Unique
    private Button bmc$redstoneButton;

    @Unique
    private EditBox bmc$priorityField;

    @Unique
    private int bmc$lastSentPriority = Integer.MIN_VALUE;

    @Inject(method = "init", at = @At("TAIL"))
    private void bmc$init(CallbackInfo ci) {
        var screen = (CondenserScreen) (Object) this;

        this.bmc$redstoneButton = Button.builder(
                Component.literal(bmc$getRedstoneLabel()),
                b -> bmc$cycleRedstoneMode()
        ).bounds(this.leftPos + 118, this.topPos + 75, 50, 12).build();
        addRenderableWidget(this.bmc$redstoneButton);

        this.bmc$priorityField = new EditBox(
                screen.getMinecraft().font,
                this.leftPos + 8,
                this.topPos + 103,
                40,
                12,
                Component.empty()
        );
        this.bmc$priorityField.setMaxLength(4);
        this.bmc$priorityField.setFilter(bmc$priorityFilter());
        this.bmc$priorityField.setResponder(this::bmc$onPriorityChanged);
        addRenderableWidget(this.bmc$priorityField);

        addRenderableWidget(Button.builder(
                Component.translatable("gui.bettermattercondenser.priority.top"),
                b -> bmc$setPriority(BMC_PRIORITY_MAX)
        ).bounds(this.leftPos + 52, this.topPos + 103, 28, 12).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.bettermattercondenser.priority.bottom"),
                b -> bmc$setPriority(BMC_PRIORITY_MIN)
        ).bounds(this.leftPos + 82, this.topPos + 103, 28, 12).build());

        bmc$syncPriorityFromMenu();
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void bmc$updateBeforeRender(CallbackInfo ci) {
        if (this.bmc$redstoneButton != null) {
            this.bmc$redstoneButton.setMessage(Component.literal(bmc$getRedstoneLabel()));
        }

        var menu = bmc$getMenu();
        if (menu instanceof CondenserMenuMixinAccessor accessor) {
            String currentText = this.bmc$priorityField.getValue();
            int menuPriority = accessor.bmc$getPriority();
            try {
                int textPriority = currentText.isEmpty() ? 0 : Integer.parseInt(currentText);
                if (textPriority != menuPriority && bmc$lastSentPriority != menuPriority) {
                    this.bmc$priorityField.setValue(String.valueOf(menuPriority));
                }
            } catch (NumberFormatException ignored) {
                this.bmc$priorityField.setValue(String.valueOf(menuPriority));
            }
        }
    }

    @Unique
    private void bmc$syncPriorityFromMenu() {
        var menu = bmc$getMenu();
        if (menu instanceof CondenserMenuMixinAccessor accessor) {
            this.bmc$priorityField.setValue(String.valueOf(accessor.bmc$getPriority()));
        }
    }

    @Unique
    private java.util.function.Predicate<String> bmc$priorityFilter() {
        return text -> {
            if (text.isEmpty()) return true;
            if (text.equals("-")) return true;
            return text.matches("-?\\d{0,4}");
        };
    }

    @Unique
    private void bmc$onPriorityChanged(String text) {
        if (text.isEmpty() || text.equals("-")) return;
        try {
            int value = Integer.parseInt(text);
            value = Math.max(BMC_PRIORITY_MIN, Math.min(BMC_PRIORITY_MAX, value));
            bmc$sendPriority(value);
        } catch (NumberFormatException ignored) {
        }
    }

    @Unique
    private void bmc$setPriority(int value) {
        value = Math.max(BMC_PRIORITY_MIN, Math.min(BMC_PRIORITY_MAX, value));
        this.bmc$priorityField.setValue(String.valueOf(value));
        bmc$sendPriority(value);
    }

    @Unique
    private void bmc$sendPriority(int value) {
        if (value == bmc$lastSentPriority) return;
        bmc$lastSentPriority = value;

        var menu = bmc$getMenu();
        if (menu == null) return;
        var pos = menu.getBlockEntity().getBlockPos();
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new SetPriorityPacket(pos, value));
    }

    @Unique
    private String bmc$getRedstoneLabel() {
        var mode = bmc$getCurrentRedstoneMode();
        return switch (mode) {
            case IGNORE -> "OFF";
            case NORMAL -> "ON";
            case INVERTED -> "INV";
        };
    }

    @Unique
    private RedstoneMode bmc$getCurrentRedstoneMode() {
        var menu = bmc$getMenu();
        if (menu instanceof CondenserMenuMixinAccessor accessor) {
            int ordinal = accessor.bmc$getRedstoneMode();
            if (ordinal >= 0 && ordinal < RedstoneMode.values().length) {
                return RedstoneMode.values()[ordinal];
            }
        }
        return RedstoneMode.IGNORE;
    }

    @Unique
    private CondenserMenu bmc$getMenu() {
        return ((CondenserScreen) (Object) this).getMenu();
    }

    @Unique
    private void bmc$cycleRedstoneMode() {
        var menu = bmc$getMenu();
        if (!(menu instanceof CondenserMenuMixinAccessor accessor)) return;

        RedstoneMode current = bmc$getCurrentRedstoneMode();
        RedstoneMode next = current.next();
        accessor.bmc$setRedstoneMode(next.ordinal());
        if (this.bmc$redstoneButton != null) {
            this.bmc$redstoneButton.setMessage(Component.literal(bmc$getRedstoneLabel()));
        }

        var pos = menu.getBlockEntity().getBlockPos();
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new SetRedstoneModePacket(pos, next));
    }
}
