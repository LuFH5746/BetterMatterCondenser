package com.bettermolecularassembler.network;

import com.bettermolecularassembler.BetterMolecularAssemblerMod;
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

public record SetPriorityPacket(BlockPos pos, int priority) implements CustomPacketPayload {
    public static final Type<SetPriorityPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(BetterMolecularAssemblerMod.MOD_ID, "set_priority")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPriorityPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetPriorityPacket::pos,
            ByteBufCodecs.INT, SetPriorityPacket::priority,
            SetPriorityPacket::new
    );

    @Override
    public @NotNull Type<SetPriorityPacket> type() {
        return TYPE;
    }

    public static void handle(SetPriorityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BlockEntity be = serverPlayer.serverLevel().getBlockEntity(packet.pos);
                if (be instanceof com.bettermolecularassembler.block.BetterMABlockEntity ma) {
                    ma.setPriority(packet.priority);
                }
            }
        });
    }
}
