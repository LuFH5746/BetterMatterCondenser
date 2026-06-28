package com.bettermolecularassembler;

import com.bettermolecularassembler.block.BetterMABlock;
import com.bettermolecularassembler.block.BetterMABlockEntity;
import com.bettermolecularassembler.compat.AE2WTLibCompat;
import com.bettermolecularassembler.compat.GuideMeCompat;
import com.bettermolecularassembler.menu.BetterMAMenu;
import com.bettermolecularassembler.network.SetPriorityPacket;
import com.bettermolecularassembler.network.SetRedstoneModePacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BetterMolecularAssemblerMod.MOD_ID)
public class BetterMolecularAssemblerMod {
    public static final String MOD_ID = "bettermolecularassembler";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredBlock<Block> BETTER_MOLECULAR_ASSEMBLER = BLOCKS.registerBlock(
            "better_molecular_assembler",
            BetterMABlock::new,
            Block.Properties.of().strength(2.0f).requiresCorrectToolForDrops()
    );

    public static final DeferredItem<BlockItem> BETTER_MOLECULAR_ASSEMBLER_ITEM = ITEMS.registerSimpleBlockItem(
            "better_molecular_assembler",
            BETTER_MOLECULAR_ASSEMBLER
    );

    @SuppressWarnings("DataFlowIssue")
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<BetterMABlockEntity>> BETTER_MOLECULAR_ASSEMBLER_ENTITY = BLOCK_ENTITY_TYPES.register(
            "better_molecular_assembler",
            () -> BlockEntityType.Builder.of(BetterMABlockEntity::new, BETTER_MOLECULAR_ASSEMBLER.get()).build(null)
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<BetterMAMenu>> BETTER_MOLECULAR_ASSEMBLER_MENU = MENU_TYPES.register(
            "better_molecular_assembler",
            () -> IMenuTypeExtension.create(BetterMAMenu::new)
    );

    public BetterMolecularAssemblerMod(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, BetterMAConfig.SPEC);

        modEventBus.addListener(BetterMolecularAssemblerMod::registerPackets);
        NeoForge.EVENT_BUS.addListener(BetterMolecularAssemblerMod::registerServerTick);

        GuideMeCompat.init();
    }

    private static void registerServerTick(final net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AE2WTLibCompat.onServerTick(serverLevel);
        }
    }

    private static void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID).versioned("1.0.0");
        registrar.playToServer(SetPriorityPacket.TYPE, SetPriorityPacket.STREAM_CODEC, SetPriorityPacket::handle);
        registrar.playToServer(SetRedstoneModePacket.TYPE, SetRedstoneModePacket.STREAM_CODEC, SetRedstoneModePacket::handle);
    }
}
