package rustic.dynamictrees;

import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.block.soil.SoilProperties;
import com.dtteam.dynamictrees.data.GatherDataHelper;
import com.dtteam.dynamictrees.registry.NeoForgeRegistryHandler;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.Species;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import rustic.Rustic;

/**
 * Phase 10 / S10.3: the Dynamic Trees compat bridge.
 *
 * <p>Dynamic Trees for NeoForge 1.21.1 is a data-driven "tree pack" mod, not the 1.12.2 Java-subclass API the legacy
 * {@code rustic.compat.dynamictrees.*} files were written against. The olive and ironwood trees themselves are now
 * described as JSON under {@code src/main/resources/trees/rustic/} (families, species, leaves properties, fruits),
 * which Dynamic Trees discovers and loads on its own. What still needs Java is one thing only: the mod must hand
 * Dynamic Trees a registry handler for its namespace so the branch / leaves / sapling / seed / fruit blocks and items
 * the tree pack generates are actually registered into Minecraft under {@code rustic:*}.
 *
 * <p>{@link NeoForgeRegistryHandler#setup(String, IEventBus)} does exactly that - it builds a
 * {@code DeferredRegister} for {@code Rustic.MODID}, registers it on the mod bus, and files the handler in Dynamic
 * Trees' {@code RegistryHandler.REGISTRY} keyed by mod id, which is what the tree-pack loader looks up when it
 * populates a family's blocks. Calling it once from the mod constructor, before the registry events fire, is enough.
 *
 * <p>This class is the only Rustic source that touches the Dynamic Trees API. It is {@code compileOnly} and is reached
 * only behind a {@code ModList.isLoaded("dynamictrees")} guard in {@link Rustic}, so the tree-pack is inert data and this
 * class is never loaded when Dynamic Trees is absent - the same presence-gating idiom CraftTweaker and JEI use.
 */
public final class RusticDynamicTreesCompat {

    private RusticDynamicTreesCompat() {
    }

    /** Attaches Rustic's Dynamic Trees registry handler so the bundled tree pack can register its blocks and items. */
    public static void init(IEventBus modEventBus) {
        NeoForgeRegistryHandler.setup(Rustic.MODID, modEventBus);
        Rustic.LOGGER.info("Dynamic Trees compat: registry handler attached for namespace '{}'", Rustic.MODID);
    }

    /**
     * Registers the {@code /dtgen} natural-generation debug command on the NeoForge game bus. Only wired when Dynamic
     * Trees is loaded (guarded in {@link Rustic}), so this class - and {@link DtGenDebugCommand}, which touches the
     * {@code compileOnly} DT world-gen API - is never loaded when the mod is absent.
     */
    public static void registerCommands(RegisterCommandsEvent event) {
        DtGenDebugCommand.register(event.getDispatcher());
    }

    /**
     * Queues the Dynamic Trees data providers that generate what Rustic does not itself own - the branch / leaves /
     * sapling blockstates and block models, the branch / seed item models, and the tree loot tables - for the
     * {@code rustic} tree-pack namespace.
     *
     * <p>Dynamic Trees' built-in {@code gatherData} hardcodes its own namespace ({@code dynamictrees}), so it never
     * emits assets for an addon's families; the addon calls the {@link GatherDataHelper} granular methods with its mod
     * id and each provider filters the shared registries to that namespace via {@code getAllFor}.
     *
     * <p>Two of Dynamic Trees' five generators are deliberately NOT invoked here. Its language provider would write a
     * second {@code assets/rustic/lang/en_us.json} that collides with the one Rustic hand-authors, and its tag provider
     * would re-emit {@code data/rustic} tags Rustic already owns. Rustic adds the handful of Dynamic Trees translation
     * keys by hand and needs none of the tag entries for a hand-grown olive, so both are skipped.
     */
    public static void gatherData(GatherDataEvent event) {
        GatherDataHelper.gatherBlockStateAndModelData(Rustic.MODID, event,
                SoilProperties.REGISTRY, Family.REGISTRY, Species.REGISTRY, LeavesProperties.REGISTRY);
        GatherDataHelper.gatherItemModelData(Rustic.MODID, event,
                Family.REGISTRY, Species.REGISTRY, LeavesProperties.REGISTRY);
        GatherDataHelper.gatherLootData(Rustic.MODID, event);
        Rustic.LOGGER.info("Dynamic Trees compat: queued tree-pack data generation for namespace '{}'", Rustic.MODID);
    }
}
