package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.items.RusticItems;

import java.util.concurrent.CompletableFuture;

/**
 * Item tags: the 1.21 home for the {@code Recipes.addOreDictEntries()} entries the recipe datagen reads.
 *
 * <p>Two distinct jobs happen here, and both mirror what {@link RusticBlockTags} already did on the block
 * side. The wood-family items join the vanilla class tags ({@code minecraft:planks}/{@code logs}/
 * {@code saplings}/{@code leaves}/{@code wooden_slabs}/{@code wooden_stairs}) - 1.12.2 registered the same
 * variants under {@code plankWood}/
 * {@code logWood}/{@code treeSapling}/{@code treeLeaves}/{@code slabWood}/{@code stairWood}, and a recipe
 * that asks for "any plank" or "any wooden slab" now asks for those tags, so a Rustic wood part has to be
 * in them for the chair, table, clay-wall, barrel and crushing-tub recipes to accept one. NeoForge already
 * put every vanilla plank in these tags; only the Rustic members are added here.
 *
 * <p>The rest are the interop ore-dict names: the four produce/herb dyes ({@code dyeRed} etc. became
 * {@code c:dyes/<color>}), the {@code wax}/{@code tallow} pair shared by beeswax and tallow, the two
 * smelting dusts under the {@code c:dusts} family, and the produce food groups that replace
 * {@code listAllfruit}/{@code listAllberry}/{@code listAllveggie}. None of the machine or crafting recipes
 * this phase generates consume the last three as ingredients - they are the entries other mods test for,
 * preserved verbatim from the legacy ore dictionary rather than dropped.
 *
 * <p>The legacy {@code painted_wood_*} blocks are deliberately <em>not</em> added to {@code planks}: they
 * only reached {@code plankWood} behind {@code enable_painted_wood}, and {@link RusticBlockTags} likewise
 * left them out of the block {@code planks} tag, so the item side stays symmetric with what already ships.
 */
public class RusticItemTags extends ItemTagsProvider {

    // The ore-dict names with no NeoForge/vanilla constant: the "wax"/"tallow" pair beeswax and tallow
    // both belonged to, and the two dusts, which belong under the c:dusts family.
    private static final TagKey<Item> WAX = cTag("wax");
    private static final TagKey<Item> TALLOW = cTag("tallow");
    private static final TagKey<Item> DUSTS_TINY_IRON = cTag("dusts/tiny_iron");
    private static final TagKey<Item> DUSTS_GOLD = cTag("dusts/gold");

    public RusticItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                          CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Rustic.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Wood families: the item mirror of the vanilla class tags RusticBlockTags set on the blocks.
        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            tag(ItemTags.PLANKS).add(wood.planks().get().asItem());
            tag(ItemTags.LOGS).add(wood.log().get().asItem());
            tag(ItemTags.SAPLINGS).add(wood.sapling().get().asItem());
            tag(ItemTags.LEAVES).add(wood.leaves().get().asItem());
            // The legacy slabWood/stairWood ore names held exactly these two Rustic variants, and the
            // barrel, liquid barrel, brewing barrel and crushing tub recipes ask for those names - so the
            // vanilla class tags have to carry them here for a Rustic slab or stair to count as one.
            tag(ItemTags.WOODEN_SLABS).add(wood.slab().get().asItem());
            tag(ItemTags.WOODEN_STAIRS).add(wood.stairs().get().asItem());
        }

        // Phase 8: the apple tree's parts, the item mirror of the class tags RusticBlockTags set on the blocks
        // (the legacy treeSapling/treeLeaves ore names). A recipe or advancement that asks for "any sapling" or
        // "any leaves" now counts an apple one, exactly as it counted the olive and ironwood variants.
        tag(ItemTags.SAPLINGS).add(RusticFarmBlocks.APPLE_SAPLING.get().asItem());
        tag(ItemTags.LEAVES).add(RusticFarmBlocks.APPLE_LEAVES.get().asItem());

        // The four legacy dye colours the produce/herbs stood for (dyeRed, dyePurple, dyeLightGray, dyeBlack).
        tag(Tags.Items.DYES_RED).add(RusticItems.WILDBERRIES.get());
        tag(Tags.Items.DYES_PURPLE).add(RusticItems.GRAPES.get());
        tag(Tags.Items.DYES_LIGHT_GRAY).add(RusticItems.IRONBERRIES.get());
        tag(Tags.Items.DYES_BLACK).add(RusticFarmBlocks.VANTA_LILY.get().asItem());

        // Beeswax and tallow were registered under both the "wax" and the "tallow" ore names; keep both tags.
        tag(WAX).add(RusticItems.BEESWAX.get(), RusticItems.TALLOW.get());
        tag(TALLOW).add(RusticItems.BEESWAX.get(), RusticItems.TALLOW.get());

        // The evaporating-basin dusts (dustTinyIron, dustGold), as per-material members of c:dusts.
        tag(DUSTS_TINY_IRON).add(RusticItems.IRON_DUST_TINY.get());
        tag(DUSTS_GOLD).add(RusticItems.GOLD_DUST.get());
        tag(Tags.Items.DUSTS).addTags(DUSTS_TINY_IRON, DUSTS_GOLD);

        // The produce food groups, replacing listAllfruit/listAllberry/listAllveggie.
        tag(Tags.Items.FOODS_FRUIT)
                .add(RusticItems.OLIVES.get(), RusticItems.IRONBERRIES.get(),
                        RusticItems.GRAPES.get(), RusticItems.WILDBERRIES.get());
        tag(Tags.Items.FOODS_BERRY)
                .add(RusticItems.IRONBERRIES.get(), RusticItems.GRAPES.get(), RusticItems.WILDBERRIES.get());
        tag(Tags.Items.FOODS_VEGETABLE)
                .add(RusticItems.TOMATO.get(), RusticItems.CHILI_PEPPER.get(), RusticItems.GHOST_PEPPER.get());
    }

    /** A conventional-namespaced ({@code c:}) item tag key. */
    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
