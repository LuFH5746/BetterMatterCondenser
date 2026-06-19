package com.bettermolecularassembler;

import com.bettermolecularassembler.screen.BetterMAScreen;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

@EventBusSubscriber(modid = BetterMolecularAssemblerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventHandler {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BetterMolecularAssemblerMod.BETTER_MOLECULAR_ASSEMBLER_ENTITY.get(),
                (be, side) -> new InvWrapper(be.getInventory())
        );
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(BetterMolecularAssemblerMod.BETTER_MOLECULAR_ASSEMBLER_MENU.get(), BetterMAScreen::new);
    }

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(BetterMolecularAssemblerMod.BETTER_MOLECULAR_ASSEMBLER_ITEM);
        }
    }
}
