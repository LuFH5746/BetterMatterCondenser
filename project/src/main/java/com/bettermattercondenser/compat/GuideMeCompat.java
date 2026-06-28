package com.bettermattercondenser.compat;

import com.bettermattercondenser.BetterMatterCondenserMod;
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
                    BetterMatterCondenserMod.MOD_ID, "guide"));

            guideBuilderClass.getMethod("folder", String.class)
                    .invoke(builder, "guide");
            guideBuilderClass.getMethod("startPage", ResourceLocation.class)
                    .invoke(builder, ResourceLocation.fromNamespaceAndPath(
                            BetterMatterCondenserMod.MOD_ID, "guide/index.md"));
            guideBuilderClass.getMethod("register", boolean.class)
                    .invoke(builder, true);
            guideBuilderClass.getMethod("build").invoke(builder);

            BetterMatterCondenserMod.LOGGER.info("GuideMe guide registered.");
        } catch (ClassNotFoundException e) {
            BetterMatterCondenserMod.LOGGER.debug("GuideMe not present, skipping guide registration.");
        } catch (Exception e) {
            BetterMatterCondenserMod.LOGGER.error("Failed to register GuideMe guide", e);
        }
    }
}
