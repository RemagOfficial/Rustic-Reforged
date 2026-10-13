package rustic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rustic.config.RusticConfig;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.RusticBlocks;
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
 *       the iron-skin layer - client entry points and item data components (Phases 6 and 9).</li>
 *   <li>{@code BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(rope, ...)} - the 1.21
 *       equivalent of a rope dispense behaviour, and the chicken temptation set, which 1.21 reads
 *       from item tags instead of a static field (Phase 3/5).</li>
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
        RusticCreativeTabs.register(modEventBus);

        modEventBus.addListener(Rustic::commonSetup);
        // Only ever fired by the data generator run, so this listener is inert in a normal game.
        modEventBus.addListener(RusticDataGen::gatherData);

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
    }
}
