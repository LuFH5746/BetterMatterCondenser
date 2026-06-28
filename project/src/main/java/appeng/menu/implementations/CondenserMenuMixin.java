package appeng.menu.implementations;

import appeng.blockentity.misc.CondenserBlockEntity;
import appeng.menu.guisync.GuiSync;
import com.bettermattercondenser.mixin.CondenserBlockEntityMixinAccessor;
import com.bettermattercondenser.mixin.CondenserMenuMixinAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CondenserMenu.class, remap = false)
public class CondenserMenuMixin implements CondenserMenuMixinAccessor {

    @Shadow
    private appeng.blockentity.misc.CondenserBlockEntity condenser;

    @GuiSync(3)
    public int bmc$redstoneMode = 0; // 0=IGNORE, 1=NORMAL, 2=INVERTED

    @GuiSync(4)
    public int bmc$priority = 0;

    @Inject(method = "broadcastChanges", at = @At("TAIL"))
    private void bmc$broadcastChanges(CallbackInfo ci) {
        if (!((CondenserMenu) (Object) this).isClientSide()) {
            var accessor = (CondenserBlockEntityMixinAccessor) condenser;
            this.bmc$redstoneMode = accessor.bmc$getRedstoneMode().ordinal();
            this.bmc$priority = accessor.bmc$getPriority();
        }
    }

    @Override
    @Unique
    public int bmc$getRedstoneMode() {
        return this.bmc$redstoneMode;
    }

    @Override
    @Unique
    public void bmc$setRedstoneMode(int mode) {
        this.bmc$redstoneMode = mode;
    }

    @Override
    @Unique
    public int bmc$getPriority() {
        return this.bmc$priority;
    }

    @Override
    @Unique
    public void bmc$setPriority(int priority) {
        this.bmc$priority = priority;
    }
}
