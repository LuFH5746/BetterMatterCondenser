package com.bettermolecularassembler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = BetterMolecularAssemblerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class BetterMAConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue EXPORT_RATE_LIMIT = BUILDER
            .comment("Maximum number of items exported to ME network per tick")
            .defineInRange("exportRateLimit", 256, 1, 10000);

    public static final ModConfigSpec.BooleanValue AE2WTLIB_COMPAT = BUILDER
            .comment("Enable AE2WTLib wireless terminal trash integration")
            .define("ae2wtlibCompat", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static int cachedExportRateLimit = 256;
    private static boolean cachedAE2WTLibCompat = true;

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            cachedExportRateLimit = EXPORT_RATE_LIMIT.get();
            cachedAE2WTLibCompat = AE2WTLIB_COMPAT.get();
            com.bettermolecularassembler.compat.AE2WTLibCompat.invalidate();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            cachedExportRateLimit = EXPORT_RATE_LIMIT.get();
            cachedAE2WTLibCompat = AE2WTLIB_COMPAT.get();
            com.bettermolecularassembler.compat.AE2WTLibCompat.invalidate();
        }
    }

    public static int getExportRateLimit() {
        return cachedExportRateLimit;
    }

    public static boolean isAE2WTLibCompatEnabled() {
        return cachedAE2WTLibCompat;
    }
}
