package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;

import java.util.concurrent.CompletableFuture;

/**
 * Furnace fuels: the 1.21 home for the legacy {@code Recipes.addFuels()} {@code IFuelHandler}.
 *
 * <p>NeoForge 1.21.1 no longer reads burn time from a registered {@code IFuelHandler} - the data-driven
 * replacement is the {@code neoforge:furnace_fuels} {@linkplain NeoForgeDataMaps#FURNACE_FUELS item data
 * map}, whose {@link FurnaceFuel} value is the burn time in ticks (the same unit the 1.12.2 handler
 * returned, so the numbers carry over verbatim). It emits
 * {@code data/neoforge/data_maps/item/furnace_fuels.json}.
 *
 * <p>The legacy handler burned the Rustic sapling ({@code ModBlocks.SAPLING}, the olive/ironwood variants
 * now split into one block per family) and the apple sapling for 100 ticks, and the wildberry bush for 200.
 * 1.12.2's {@code ModBlocks.SAPLING} was a single meta block, so every wood-family sapling this mod ships
 * gets the 100-tick value; the {@code APPLE_SAPLING} is deferred to Phase 8 worldgen (that block is not
 * ported yet), so only the two wood families and the bush are emitted here.
 */
public class RusticFuels extends DataMapProvider {

    public RusticFuels(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        var fuels = builder(NeoForgeDataMaps.FURNACE_FUELS);
        // The legacy sapling fuel (100 ticks) - one entry per Rustic wood family. The block-item id is the
        // block's own id, which is what the item-registry data map keys on.
        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            fuels.add(wood.sapling().getKey().location(), new FurnaceFuel(100), false);
        }
        // The wildberry bush, the legacy 200-tick fuel.
        fuels.add(RusticFarmBlocks.WILDBERRY_BUSH.getKey().location(), new FurnaceFuel(200), false);
    }
}
