package com.bettermattercondenser;

import com.bettermattercondenser.compat.AE2WTLibCompat;
import com.bettermattercondenser.compat.GuideMeCompat;
import com.bettermattercondenser.network.SetPriorityPacket;
import com.bettermattercondenser.network.SetRedstoneModePacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BetterMatterCondenserMod.MOD_ID)
public class BetterMatterCondenserMod {
    public static final String MOD_ID = "bettermattercondenser";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);

    public BetterMatterCondenserMod(IEventBus modEventBus, ModContainer modContainer) {
        MENU_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, BMCConfig.SPEC);

        modEventBus.addListener(BetterMatterCondenserMod::registerPackets);
        NeoForge.EVENT_BUS.addListener(BetterMatterCondenserMod::onLevelTick);

        GuideMeCompat.init();
    }

    private static void onLevelTick(final net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            CondenserLogic.tickAll(serverLevel);
            AE2WTLibCompat.onServerTick(serverLevel);
        }
    }

    private static void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID).versioned("1.0.0");
        registrar.playToServer(SetPriorityPacket.TYPE, SetPriorityPacket.STREAM_CODEC, SetPriorityPacket::handle);
        registrar.playToServer(SetRedstoneModePacket.TYPE, SetRedstoneModePacket.STREAM_CODEC, SetRedstoneModePacket::handle);
    }
}
