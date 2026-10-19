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

public class RusticItemTags extends ItemTagsProvider {

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

        for (RusticBlocks.WoodFamily wood : RusticBlocks.WOOD_FAMILIES) {
            tag(ItemTags.PLANKS).add(wood.planks().get().asItem());
            tag(ItemTags.LOGS).add(wood.log().get().asItem());
            tag(ItemTags.SAPLINGS).add(wood.sapling().get().asItem());
            tag(ItemTags.LEAVES).add(wood.leaves().get().asItem());

            tag(ItemTags.WOODEN_SLABS).add(wood.slab().get().asItem());
            tag(ItemTags.WOODEN_STAIRS).add(wood.stairs().get().asItem());

            tag(ItemTags.DOORS).add(wood.door().get().asItem());
            tag(ItemTags.WOODEN_DOORS).add(wood.door().get().asItem());
        }

        tag(ItemTags.SAPLINGS).add(RusticFarmBlocks.APPLE_SAPLING.get().asItem());
        tag(ItemTags.LEAVES).add(RusticFarmBlocks.APPLE_LEAVES.get().asItem());

        tag(Tags.Items.DYES_RED).add(RusticItems.WILDBERRIES.get());
        tag(Tags.Items.DYES_PURPLE).add(RusticItems.GRAPES.get());
        tag(Tags.Items.DYES_LIGHT_GRAY).add(RusticItems.IRONBERRIES.get());
        tag(Tags.Items.DYES_BLACK).add(RusticFarmBlocks.VANTA_LILY.get().asItem());

        tag(WAX).add(RusticItems.BEESWAX.get(), RusticItems.TALLOW.get());
        tag(TALLOW).add(RusticItems.BEESWAX.get(), RusticItems.TALLOW.get());

        tag(DUSTS_TINY_IRON).add(RusticItems.IRON_DUST_TINY.get());
        tag(DUSTS_GOLD).add(RusticItems.GOLD_DUST.get());
        tag(Tags.Items.DUSTS).addTags(DUSTS_TINY_IRON, DUSTS_GOLD);

        tag(Tags.Items.FOODS_FRUIT)
                .add(RusticItems.OLIVES.get(), RusticItems.IRONBERRIES.get(),
                        RusticItems.GRAPES.get(), RusticItems.WILDBERRIES.get());
        tag(Tags.Items.FOODS_BERRY)
                .add(RusticItems.IRONBERRIES.get(), RusticItems.GRAPES.get(), RusticItems.WILDBERRIES.get());
        tag(Tags.Items.FOODS_VEGETABLE)
                .add(RusticItems.TOMATO.get(), RusticItems.CHILI_PEPPER.get(), RusticItems.GHOST_PEPPER.get());
    }

    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
