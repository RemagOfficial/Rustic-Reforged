package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The datagen entry point, replacing the assets 1.12.2 kept in checked-in JSON.
 *
 * <p>The legacy mod had no data generators: blockstates, models and item models were written by hand
 * into {@code src/main/resources/assets/rustic}, block drops came from the engine default, and the
 * only thing that existed in code was {@code initModel()}, which mapped each block item onto
 * {@code <registry name>:inventory}. Everything equivalent to those files is now generated into
 * {@code src/generated/resources}, which the build adds as a resource directory.
 *
 * <p>Runs on the mod bus, added from the mod constructor with
 * {@code modEventBus.addListener(RusticDataGen::gatherData)}, and is reached with {@code gradlew runData}.
 */
public final class RusticDataGen {

    public static void gatherData(GatherDataEvent event) {
        // Phase 10 / S10.3: Dynamic Trees generates the branch / leaves / sapling / seed / fruit blockstates, models,
        // item models and loot for the rustic tree-pack in a DEDICATED pass (runDtTreeData sets the marker property).
        // Its DTBlockStateProvider / DTItemModelProvider / DTLootTableProvider reuse the "rustic" namespace, so their
        // provider names ("Block States: rustic", "Item Models: rustic", "Loot Tables") are identical to the ones
        // Rustic registers below and NeoForge rejects the duplicate in a single run. Running the two sets in separate
        // invocations keeps the names unique per run while both accumulate into src/generated/resources, which datagen
        // writes additively. Guarded by ModList so the DT classes are never loaded when the optional dependency is off.
        if (ModList.get().isLoaded("dynamictrees") && System.getProperty("rustic.dtTreeDatagen") != null) {
            rustic.dynamictrees.RusticDynamicTreesCompat.gatherData(event);
            return;
        }

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // Client: blockstates, block models and the item models that parent on them.
        generator.addProvider(event.includeClient(), new RusticBlockStates(output, existingFileHelper));
        // Phase 5: the standalone item models (produce and seeds) that parent on nothing.
        generator.addProvider(event.includeClient(), new RusticItemModels(output, existingFileHelper));

        // Server: drops, then the tags that carry the old Material's tool behaviour.
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(RusticBlockLoot::new, LootContextParamSets.BLOCK)),
                lookupProvider));
        RusticBlockTags blockTags = new RusticBlockTags(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        // Phase 7: the item tags that finish the old ore-dict story on the ItemStack side - the wood class
        // tags RusticBlockTags opened on the blocks, plus the dye/wax/tallow/dust/produce interop names the
        // recipe datagen reads. Chained on the block tags so it can mirror vanilla entries for free.
        generator.addProvider(event.includeServer(),
                new RusticItemTags(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
        // Phase 7: the crafting/smelting JSON replacing 1.12.2's code-built Recipes.init().
        generator.addProvider(event.includeServer(), new RusticRecipeProvider(output, lookupProvider));
        // Phase 7: the legacy IFuelHandler as the neoforge:furnace_fuels item data map.
        generator.addProvider(event.includeServer(), new RusticFuels(output, lookupProvider));
        // Phase 8: the world-generation dynamic-registry entries - slate ore and the olive/ironwood/apple trees as
        // rustic:config_gated configured features, their placed features, and the neoforge:add_features biome
        // modifiers. Chained last because it bootstraps three registries that reference each other by key.
        generator.addProvider(event.includeServer(), new RusticWorldGenProvider(output, lookupProvider));
    }

    private RusticDataGen() {
    }
}
