package com.bettermolecularassembler.compat;

import appeng.api.networking.IGrid;
import com.bettermolecularassembler.BetterMAConfig;
import com.bettermolecularassembler.BetterMolecularAssemblerMod;
import com.bettermolecularassembler.block.BetterMABlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AE2WTLibCompat {
    private static boolean initialized = false;
    private static boolean loaded = false;

    private static Class<?> trashMenuClass;
    private static Class<?> wctMenuHostClass;
    private static Class<?> internalInventoryClass;
    private static Field trashMenuHostField;
    private static Method getSubInventoryMethod;
    private static Method getActionableNodeMethod;
    private static Method getGridMethod;
    private static Method sizeMethod;
    private static Method getStackInSlotMethod;
    private static Method setItemDirectMethod;
    private static Object invTrashConstant;

    public static boolean isLoaded() {
        if (!initialized) {
            init();
        }
        return loaded;
    }

    private static void init() {
        initialized = true;
        if (!BetterMAConfig.isAE2WTLibCompatEnabled()) {
            BetterMolecularAssemblerMod.LOGGER.debug("AE2WTLib compatibility disabled in config.");
            return;
        }
        try {
            trashMenuClass = Class.forName("de.mari_023.ae2wtlib.wct.TrashMenu");
            wctMenuHostClass = Class.forName("de.mari_023.ae2wtlib.wct.WCTMenuHost");
            internalInventoryClass = Class.forName("appeng.api.inventories.InternalInventory");

            trashMenuHostField = trashMenuClass.getDeclaredField("host");
            trashMenuHostField.setAccessible(true);

            getSubInventoryMethod = wctMenuHostClass.getMethod("getSubInventory", ResourceLocation.class);
            getActionableNodeMethod = wctMenuHostClass.getMethod("getActionableNode");

            Class<?> gridNodeClass = Class.forName("appeng.api.networking.IGridNode");
            getGridMethod = gridNodeClass.getMethod("getGrid");

            sizeMethod = internalInventoryClass.getMethod("size");
            getStackInSlotMethod = internalInventoryClass.getMethod("getStackInSlot", int.class);
            setItemDirectMethod = internalInventoryClass.getMethod("setItemDirect", int.class, ItemStack.class);

            invTrashConstant = wctMenuHostClass.getField("INV_TRASH").get(null);

            loaded = true;
            BetterMolecularAssemblerMod.LOGGER.info("AE2WTLib compatibility loaded.");
        } catch (Exception e) {
            loaded = false;
            BetterMolecularAssemblerMod.LOGGER.debug("AE2WTLib not present or incompatible: {}", e.getMessage());
        }
    }

    public static void onServerTick(ServerLevel level) {
        if (!isLoaded()) return;

        for (ServerPlayer player : level.players()) {
            processPlayerTrash(player);
        }
    }

    private static void processPlayerTrash(ServerPlayer player) {
        if (player.containerMenu == null) return;
        if (!trashMenuClass.isInstance(player.containerMenu)) return;

        try {
            Object host = trashMenuHostField.get(player.containerMenu);
            if (host == null) return;

            Object actionableNode = getActionableNodeMethod.invoke(host);
            if (actionableNode == null) return;

            Object gridObj = getGridMethod.invoke(actionableNode);
            if (!(gridObj instanceof IGrid grid)) return;

            Object trash = getSubInventoryMethod.invoke(host, invTrashConstant);
            if (trash == null) return;

            int size = (int) sizeMethod.invoke(trash);
            for (int i = 0; i < size; i++) {
                ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(trash, i);
                if (stack.isEmpty()) continue;

                int accepted = BetterMABlockEntity.acceptTrashItems(player.serverLevel(), stack, grid);
                if (accepted > 0) {
                    stack.shrink(accepted);
                    setItemDirectMethod.invoke(trash, i, stack);
                }
            }
        } catch (Exception e) {
            BetterMolecularAssemblerMod.LOGGER.error("Failed to process AE2WTLib trash", e);
        }
    }

    public static void invalidate() {
        initialized = false;
        loaded = false;
    }
}
