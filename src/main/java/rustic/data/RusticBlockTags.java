package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;

import java.util.concurrent.CompletableFuture;

/**
 * Block tags, the 1.21 home for the information 1.12.2 kept on {@code Material} and in ore dictionary
 * entries.
 *
 * <p>The {@code minecraft:mineable/*} tags are the ones that change how the mod plays: they decide
 * which tool breaks a Rustic block fastest, which in 1.12.2 came from the block's material - ROCK for
 * a pickaxe, WOOD for an axe, CLAY for a spade. Each family carries its own tag in
 * {@link RusticBlocks.DecorFamily}, so the tool cannot drift away from the block.
 *
 * <p>Effectiveness is a different question, and there the answer stays "any tool works": the legacy
 * blocks declared no harvest level, so they always dropped their item, and the port keeps that by
 * never calling {@code requiresCorrectToolForDrops()}. No {@code minecraft:incorrect_for_*} tag is
 * written here for the same reason.
 */
public class RusticBlockTags extends BlockTagsProvider {

    public RusticBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                           ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Rustic.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (RusticBlocks.DecorFamily family : RusticBlocks.DECOR_FAMILIES) {
            var tag = tag(family.tool());
            for (var block : family.blocks()) {
                tag.add(block.get());
            }
        }

        // Shape tags rather than material ones: other mods' recipes and advancement triggers use these
        // where 1.12.2 used the ore dictionary entries "slabStone" and "stairs".
        tag(BlockTags.SLABS).add(RusticBlocks.SLATE_ROOF_SLAB.get(), RusticBlocks.SLATE_BRICK_SLAB.get());
        tag(BlockTags.STAIRS).add(RusticBlocks.SLATE_ROOF_STAIRS.get(), RusticBlocks.SLATE_BRICK_STAIRS.get());
    }
}
