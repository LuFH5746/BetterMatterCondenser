package com.bettermolecularassembler.network;

import com.bettermolecularassembler.BetterMolecularAssemblerMod;
import com.bettermolecularassembler.block.BetterMABlockEntity;
import com.bettermolecularassembler.block.RedstoneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SetRedstoneModePacket(BlockPos pos, RedstoneMode mode) implements CustomPacketPayload {
    public static final Type<SetRedstoneModePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BetterMolecularAssemblerMod.MOD_ID, "set_redstone_mode")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SetRedstoneModePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetRedstoneModePacket::pos,
            ByteBufCodecs.fromCodecWithRegistries(RedstoneMode.CODEC), SetRedstoneModePacket::mode,
            SetRedstoneModePacket::new
    );

    @Override
    public @NotNull Type<SetRedstoneModePacket> type() {
        return TYPE;
    }

    public static void handle(SetRedstoneModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.distanceToSqr(packet.pos.getCenter()) > 64) return;
                BlockEntity be = serverPlayer.serverLevel().getBlockEntity(packet.pos);
                if (be instanceof BetterMABlockEntity ma) {
                    ma.setRedstoneMode(packet.mode);
                }
            }
        });
    }
}
