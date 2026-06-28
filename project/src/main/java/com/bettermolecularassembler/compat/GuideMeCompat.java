package com.bettermolecularassembler.compat;

import com.bettermolecularassembler.BetterMolecularAssemblerMod;
import net.minecraft.resources.ResourceLocation;

public class GuideMeCompat {
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> guideBuilderClass = Class.forName("guideme.GuideBuilder");
            java.lang.reflect.Constructor<?> ctor = guideBuilderClass.getDeclaredConstructor(ResourceLocation.class);
            ctor.setAccessible(true);
            Object builder = ctor.newInstance(ResourceLocation.fromNamespaceAndPath(
                    BetterMolecularAssemblerMod.MOD_ID, "guide"));

            guideBuilderClass.getMethod("folder", String.class)
                    .invoke(builder, "guide");
            guideBuilderClass.getMethod("startPage", ResourceLocation.class)
                    .invoke(builder, ResourceLocation.fromNamespaceAndPath(
                            BetterMolecularAssemblerMod.MOD_ID, "guide/index.md"));
            guideBuilderClass.getMethod("register", boolean.class)
                    .invoke(builder, true);
            guideBuilderClass.getMethod("build").invoke(builder);

            BetterMolecularAssemblerMod.LOGGER.info("GuideMe guide registered.");
        } catch (ClassNotFoundException e) {
            BetterMolecularAssemblerMod.LOGGER.debug("GuideMe not present, skipping guide registration.");
        } catch (Exception e) {
            BetterMolecularAssemblerMod.LOGGER.error("Failed to register GuideMe guide", e);
        }
    }
}
