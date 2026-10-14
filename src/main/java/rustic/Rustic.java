package rustic;

import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rustic.config.RusticConfig;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.RopeDispenseBehavior;
import rustic.content.blocks.RusticBlocks;
import rustic.content.client.RusticClient;
import rustic.content.entities.RusticEntities;
import rustic.content.items.RusticItems;
import rustic.data.RusticDataGen;
import rustic.registry.RusticRegistries;

import java.util.UUID;

/**
 * NeoForge 1.21.1 entry point, replacing the 1.12.2 {@code rustic.core.Rustic} plus its
 * {@code CommonProxy} / {@code ClientProxy} / {@code GuiProxy} trio.
 *
 * <p>Distinction of work that used to live behind the proxies, and where it goes now:
 * <ul>
 *   <li>{@code PacketHandler.registerMessages()} - {@code RegisterPayloadHandlersEvent} on the mod
 *       bus, added with the tile entities that sync (Phase 4).</li>
 *   <li>{@code Config.readConfig()} - {@link RusticConfig}, registered as a COMMON
 *       {@link ModConfig} below; NeoForge loads and reloads it on its own.</li>
 *   <li>{@code ModFluids/ModBlocks/ModItems/ModEntities/PotionsRustic.init()} - the
 *       {@link RusticRegistries} deferred registers (Phase 2).</li>
 *   <li>{@code Recipes.initOres()} and the code-built recipe tables - tags plus datapack JSON
 *       (Phase 7).</li>
 *   <li>{@code GameRegistry.registerWorldGenerator(...)} - data-driven features and biome
 *       modifiers (Phase 8).</li>
 *   <li>{@code NetworkRegistry.registerGuiHandler(Rustic.instance, new GuiProxy())} -
 *       {@code MenuType} registration, so GuiProxy itself is not ported (Phase 4).</li>
 *   <li>{@code initFluidBottle()}, {@code BookManager.init()}, model loaders, colour handlers and
 *       the iron-skin layer - client entry points and item data components (Phases 6 and 9), all of
 *       which belong in {@link RusticClient}, and the entity renderers of {@code initRenderers()}
 *       already live there.</li>
 *   <li>{@code BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(rope, ...)} - the 1.21
 *       equivalent of a rope dispense behaviour, which {@link rustic.content.blocks.RopeDispenseBehavior}
 *       is, registered from {@link #commonSetup} below. The chicken temptation set, which 1.21 reads from
 *       item tags instead of a static field, arrives with the crops (Phase 5).</li>
 *   <li>CraftTweaker, Forestry and Dynamic Trees hooks - optional compat (Phase 10).</li>
 * </ul>
 */
@Mod(Rustic.MODID)
public class Rustic {

    public static final String MODID = "rustic";

    /** Display name and version now live in {@code src/main/templates/META-INF/neoforge.mods.toml}. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /**
     * The Eris banner pattern uuid, unchanged from 1.12.2 so old worlds/structure files that
     * reference it keep working once the banner pattern is registered again (Phase 3).
     */
    public static final UUID ERIS_UUID = UUID.fromString("8167f7a5-2db0-42ca-b177-ebc1396dbe55");

    public Rustic(IEventBus modEventBus, ModContainer modContainer) {
        RusticRegistries.register(modEventBus);
        // Blocks before items: the block items are derived from the block entries, so touching
        // RusticItems brings RusticBlocks' initialiser in first anyway, but reading top to bottom in
        // dependency order is the point of dropping the legacy init() calls.
        RusticBlocks.register(modEventBus);
        RusticItems.register(modEventBus);
        // 1.12.2's ModEntities.init(): the chair seat now, the tomato with Phase 5's crops.
        RusticEntities.register(modEventBus);
        RusticCreativeTabs.register(modEventBus);

        modEventBus.addListener(Rustic::commonSetup);
        // Only ever fired by the data generator run, so this listener is inert in a normal game.
        modEventBus.addListener(RusticDataGen::gatherData);
        // The client listeners have to stay off a dedicated server's class path, so the dist is tested
        // before the method reference is taken: the reference is only resolved when this branch runs.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RusticClient.register(modEventBus);
        }

        // Replaces the manual CommonProxy.config = new Configuration(...) + Config.readConfig() pair:
        // NeoForge writes config/rustic-common.toml, reloads it on change and pushes values to listeners.
        modContainer.registerConfig(ModConfig.Type.COMMON, RusticConfig.SPEC);

        LOGGER.info("Rustic bootstrap: registries attached to the mod bus, config spec registered");
    }

    /**
     * Setup that needs the registries to be already populated. Registry events have all finished by
     * the time this fires on the mod bus, so {@code DeferredHolder#get()} is safe here - which is why
     * the old proxy's post-init work is in a listener rather than in the constructor above.
     */
    private static void commonSetup(FMLCommonSetupEvent event) {
        RusticBlocks.setFlammables();
        // 1.12.2's CommonProxy.init(): DispenserBlock.registerBehavior is the 1.21 form of the putObject
        // call on the behaviour registry, and it takes the block's item, which every block has one of now.
        DispenserBlock.registerBehavior(RusticBlocks.ROPE.get().asItem(), new RopeDispenseBehavior());
    }
}
