package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;

import java.util.concurrent.CompletableFuture;

public class RusticFuels extends DataMapProvider {

    public RusticFuels(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        var fuels = builder(NeoForgeDataMaps.FURNACE_FUELS);

        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            fuels.add(wood.sapling().getKey().location(), new FurnaceFuel(100), false);
        }

        fuels.add(RusticFarmBlocks.WILDBERRY_BUSH.getKey().location(), new FurnaceFuel(200), false);
    }
}
