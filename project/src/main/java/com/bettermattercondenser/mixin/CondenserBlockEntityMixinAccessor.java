package com.bettermattercondenser.mixin;

import com.bettermattercondenser.RedstoneMode;

public interface CondenserBlockEntityMixinAccessor {
    RedstoneMode bmc$getRedstoneMode();

    void bmc$setRedstoneMode(RedstoneMode mode);

    int bmc$getPriority();

    void bmc$setPriority(int priority);

    long bmc$getPlacementTime();

    boolean bmc$isAutoExport();

    void bmc$updateRedstoneState(boolean hasSignal);

    void bmc$updateAutoExportState();

    void bmc$tryExportResults(int rateLimit);
}
