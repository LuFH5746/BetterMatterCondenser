package com.bettermattercondenser.network;

import appeng.blockentity.misc.CondenserBlockEntity;
import com.bettermattercondenser.BetterMatterCondenserMod;
import com.bettermattercondenser.RedstoneMode;
import com.bettermattercondenser.mixin.CondenserBlockEntityMixinAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetRedstoneModePacket(BlockPos pos, RedstoneMode mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetRedstoneModePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(BetterMatterCondenserMod.MOD_ID, "set_redstone_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetRedstoneModePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetRedstoneModePacket::pos,
            ByteBufCodecs.STRING_UTF8.map(RedstoneMode::valueOf, RedstoneMode::name), SetRedstoneModePacket::mode,
            SetRedstoneModePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetRedstoneModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null || player.level().isClientSide()) return;

            BlockEntity be = player.level().getBlockEntity(packet.pos);
            if (be instanceof CondenserBlockEntity condenser) {
                var accessor = (CondenserBlockEntityMixinAccessor) condenser;
                accessor.bmc$setRedstoneMode(packet.mode);
            }
        });
    }
}
