package rustic;

import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rustic.config.RusticConfig;
import rustic.content.RusticCreativeTabs;
import rustic.content.blockentity.RusticBlockEntities;
import rustic.content.blocks.RopeDispenseBehavior;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.client.RusticClient;
import rustic.content.entities.RusticEntities;
import rustic.content.fluids.RusticDataComponents;
import rustic.content.fluids.RusticFluidBlocks;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.RusticItems;
import rustic.content.menu.RusticMenus;
import rustic.content.network.RusticPayloads;
import rustic.content.potions.RusticEventHandlerPotions;
import rustic.content.potions.RusticMobEffects;
import rustic.content.world.RusticWorldGenFeatures;
import rustic.data.RusticDataGen;
import rustic.registry.RusticRegistries;

import java.util.UUID;

@Mod(Rustic.MODID)
public class Rustic {

    public static final String MODID = "rustic";

    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final UUID ERIS_UUID = UUID.fromString("8167f7a5-2db0-42ca-b177-ebc1396dbe55");

    public Rustic(IEventBus modEventBus, ModContainer modContainer) {
        RusticRegistries.register(modEventBus);

        RusticDataComponents.register(modEventBus);
        RusticFluids.register(modEventBus);

        RusticBlocks.register(modEventBus);

        RusticMachineBlocks.register(modEventBus);

        RusticFarmBlocks.register(modEventBus);

        RusticFluidBlocks.register(modEventBus);
        RusticItems.register(modEventBus);

        RusticEntities.register(modEventBus);

        RusticBlockEntities.register(modEventBus);
        RusticMenus.register(modEventBus);
        RusticCreativeTabs.register(modEventBus);

        RusticMobEffects.register(modEventBus);

        RusticWorldGenFeatures.register(modEventBus);

        RusticPayloads.register(modEventBus);

        if (ModList.get().isLoaded("dynamictrees")) {
            rustic.dynamictrees.RusticDynamicTreesCompat.init(modEventBus);

            NeoForge.EVENT_BUS.addListener(rustic.dynamictrees.RusticDynamicTreesCompat::registerCommands);
        }

        modEventBus.addListener(Rustic::commonSetup);

        modEventBus.addListener(RusticBlockEntities::registerCapabilities);

        modEventBus.addListener(RusticItems::registerCapabilities);

        modEventBus.addListener(RusticDataGen::gatherData);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RusticClient.register(modEventBus);
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, RusticConfig.SPEC);

        LOGGER.info("Rustic bootstrap: registries attached to the mod bus, config spec registered");
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        RusticBlocks.setFlammables();

        RusticFarmBlocks.setFlammables();

        DispenserBlock.registerBehavior(RusticBlocks.ROPE.get().asItem(), new RopeDispenseBehavior());

        NeoForge.EVENT_BUS.register(new RusticEventHandlerPotions());
    }
}
