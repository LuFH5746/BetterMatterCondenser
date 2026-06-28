package com.bettermattercondenser;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum RedstoneMode implements StringRepresentable {
    IGNORE("ignore"),
    NORMAL("normal"),
    INVERTED("inverted");

    public static final Codec<RedstoneMode> CODEC = StringRepresentable.fromEnum(RedstoneMode::values);

    private final String name;

    RedstoneMode(String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return this.name;
    }

    public RedstoneMode next() {
        return switch (this) {
            case IGNORE -> NORMAL;
            case NORMAL -> INVERTED;
            case INVERTED -> IGNORE;
        };
    }

    public boolean shouldExport(boolean hasRedstoneSignal) {
        return switch (this) {
            case IGNORE -> false;
            case NORMAL -> hasRedstoneSignal;
            case INVERTED -> !hasRedstoneSignal;
        };
    }
}
