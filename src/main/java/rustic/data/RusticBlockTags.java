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
 * <p>The {@code minecraft:planks}, {@code logs}, {@code leaves}, {@code saplings}, {@code fences},
 * {@code fence_gates}, {@code slabs} and {@code stairs} entries are the 1.21 form of the ore dictionary
 * entries 1.12.2 handed out in {@code Recipes.addOreDictEntries()}: {@code plankWood} for the planks and
 * the painted wood, {@code logWood}/{@code treeWood} for the logs, {@code treeSapling},
 * {@code treeLeaves}, {@code slabWood} and {@code stairWood}. Membership in those vanilla class tags is
 * not cosmetic either - the leaf decay distance, the axe mining speed and other mods' recipes all read
 * them - so they arrive with the blocks rather than waiting for the recipe phase.
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
        for (RusticBlocks.DecorFamily family : RusticBlocks.families()) {
            // The rope's family has no tool, because 1.12.2's custom Material matched none of the entries
            // in the engine's mining-speed table. An empty tag would be worse than none: it is the tag's
            // presence that lets other mods' recipes and advancements test for it.
            if (family.tool() == null) {
                continue;
            }
            var tag = tag(family.tool());
            for (var block : family.blocks()) {
                tag.add(block.get());
            }
        }

        /*
         * 1.12.2's BlockRopeBase answered Forge's isLadder with true for the whole family, which 1.21 asks
         * the other way round: Entity#isStateClimbable tests the minecraft:climbable tag. Every hanging line
         * of that base is therefore a ladder - the three chains and the rope - while the lattice, which is
         * not a BlockRopeBase and never answered the question, is not. BlockGrapeLeaves extends the base and
         * will need its entry when the crops land.
         */
        var climbable = tag(BlockTags.CLIMBABLE);
        climbable.add(RusticBlocks.CHAIN.get(), RusticBlocks.CHAIN_GOLD.get(), RusticBlocks.ROPE.get());
        if (RusticBlocks.silverDecorEnabled()) {
            climbable.add(RusticBlocks.SILVER_DECOR.chain().get());
        }

        // Shape tags rather than material ones: other mods' recipes and advancement triggers use these
        // where 1.12.2 used the ore dictionary entries "slabStone" and "stairs".
        tag(BlockTags.SLABS).add(RusticBlocks.SLATE_ROOF_SLAB.get(), RusticBlocks.SLATE_BRICK_SLAB.get());
        tag(BlockTags.STAIRS).add(RusticBlocks.SLATE_ROOF_STAIRS.get(), RusticBlocks.SLATE_BRICK_STAIRS.get());

        // The wood family, against the ore dictionary entries listed in the class note. The wooden_* tags
        // are the subsets vanilla's own recipes use, and the plain ones are the superset that also covers
        // stone and copper variants, so both get the same blocks - which is how vanilla sorts its woods.
        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            tag(BlockTags.MINEABLE_WITH_AXE)
                    .add(wood.planks().get(), wood.log().get(), wood.leaves().get(), wood.sapling().get(),
                            wood.fence().get(), wood.fenceGate().get(), wood.slab().get(), wood.stairs().get());
            tag(BlockTags.PLANKS).add(wood.planks().get());
            tag(BlockTags.LOGS).add(wood.log().get());
            tag(BlockTags.LOGS_THAT_BURN).add(wood.log().get());
            tag(BlockTags.LEAVES).add(wood.leaves().get());
            tag(BlockTags.SAPLINGS).add(wood.sapling().get());
            tag(BlockTags.FENCES).add(wood.fence().get());
            tag(BlockTags.WOODEN_FENCES).add(wood.fence().get());
            tag(BlockTags.FENCE_GATES).add(wood.fenceGate().get());
            tag(BlockTags.SLABS).add(wood.slab().get());
            tag(BlockTags.WOODEN_SLABS).add(wood.slab().get());
            tag(BlockTags.STAIRS).add(wood.stairs().get());
            tag(BlockTags.WOODEN_STAIRS).add(wood.stairs().get());
        }
    }
}
