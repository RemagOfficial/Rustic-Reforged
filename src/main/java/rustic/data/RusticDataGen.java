package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
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
        generator.addProvider(event.includeServer(), new RusticBlockTags(output, lookupProvider, existingFileHelper));
    }

    private RusticDataGen() {
    }
}
