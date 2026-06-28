package com.bettermattercondenser;

import net.neoforged.neoforge.common.ModConfigSpec;

public class BMCConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue EXPORT_RATE_LIMIT;
    public static final ModConfigSpec.BooleanValue WIRELESS_EXPORT_ENABLED;
    public static final ModConfigSpec.BooleanValue AE2WT_LIB_COMPAT;
    public static final ModConfigSpec.EnumValue<PrioritySelectionMode> PRIORITY_SELECTION_MODE;

    static {
        BUILDER.push("general");

        EXPORT_RATE_LIMIT = BUILDER
                .comment("Maximum number of items exported to the network per tick.")
                .defineInRange("exportRateLimit", 256, 1, 10000);

        WIRELESS_EXPORT_ENABLED = BUILDER
                .comment("Experimental: enable exporting products through ME Wireless Access Points.")
                .define("wirelessExportEnabled", false);

        AE2WT_LIB_COMPAT = BUILDER
                .comment("Enable AE2WTLib standalone trash can compatibility.")
                .define("ae2wtlibCompat", true);

        PRIORITY_SELECTION_MODE = BUILDER
                .comment("How to select which condenser receives trashed items: LAST_PLACED or PRIORITY.")
                .defineEnum("prioritySelectionMode", PrioritySelectionMode.LAST_PLACED);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int getExportRateLimit() {
        return EXPORT_RATE_LIMIT.get();
    }

    public static boolean isWirelessExportEnabled() {
        return WIRELESS_EXPORT_ENABLED.get();
    }

    public static boolean isAE2WTLibCompatEnabled() {
        return AE2WT_LIB_COMPAT.get();
    }

    public static PrioritySelectionMode getPrioritySelectionMode() {
        return PRIORITY_SELECTION_MODE.get();
    }

    public enum PrioritySelectionMode {
        LAST_PLACED,
        PRIORITY
    }
}
