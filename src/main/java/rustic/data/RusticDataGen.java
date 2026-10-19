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

public final class RusticDataGen {

    public static void gatherData(GatherDataEvent event) {

        if (ModList.get().isLoaded("dynamictrees") && System.getProperty("rustic.dtTreeDatagen") != null) {
            rustic.dynamictrees.RusticDynamicTreesCompat.gatherData(event);
            return;
        }

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new RusticBlockStates(output, existingFileHelper));

        generator.addProvider(event.includeClient(), new RusticItemModels(output, existingFileHelper));

        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(RusticBlockLoot::new, LootContextParamSets.BLOCK)),
                lookupProvider));
        RusticBlockTags blockTags = new RusticBlockTags(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);

        generator.addProvider(event.includeServer(),
                new RusticItemTags(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));

        generator.addProvider(event.includeServer(), new RusticRecipeProvider(output, lookupProvider));

        generator.addProvider(event.includeServer(), new RusticFuels(output, lookupProvider));

        generator.addProvider(event.includeServer(), new RusticWorldGenProvider(output, lookupProvider));
    }

    private RusticDataGen() {
    }
}
