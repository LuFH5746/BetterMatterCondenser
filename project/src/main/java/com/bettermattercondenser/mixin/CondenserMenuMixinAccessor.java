package com.bettermattercondenser.mixin;

public interface CondenserMenuMixinAccessor {
    int bmc$getRedstoneMode();

    void bmc$setRedstoneMode(int mode);

    int bmc$getPriority();

    void bmc$setPriority(int priority);
}
