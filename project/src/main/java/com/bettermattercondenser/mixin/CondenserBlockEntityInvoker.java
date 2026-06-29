package com.bettermattercondenser.mixin;

import appeng.api.inventories.InternalInventory;
import appeng.blockentity.misc.CondenserBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CondenserBlockEntity.class, remap = false)
public interface CondenserBlockEntityInvoker {

    @Invoker("canAddOutput")
    boolean callCanAddOutput();

    @Invoker("getOutputSlot")
    InternalInventory callGetOutputSlot();
}
