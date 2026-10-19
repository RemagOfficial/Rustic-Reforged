package rustic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;

import java.util.concurrent.CompletableFuture;

public class RusticBlockTags extends BlockTagsProvider {

    public RusticBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                           ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Rustic.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (RusticBlocks.DecorFamily family : RusticBlocks.families()) {

            if (family.tool() == null) {
                continue;
            }
            var tag = tag(family.tool());
            for (var block : family.blocks()) {
                tag.add(block.get());
            }
        }

        var climbable = tag(BlockTags.CLIMBABLE);
        climbable.add(RusticBlocks.CHAIN.get(), RusticBlocks.CHAIN_GOLD.get(), RusticBlocks.ROPE.get());
        if (RusticBlocks.silverDecorEnabled()) {
            climbable.add(RusticBlocks.SILVER_DECOR.chain().get());
        }

        tag(BlockTags.MINEABLE_WITH_AXE).add(RusticMachineBlocks.BARREL.get(), RusticMachineBlocks.CABINET.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(RusticMachineBlocks.VASE.get());

        tag(BlockTags.MINEABLE_WITH_AXE).add(RusticMachineBlocks.LIQUID_BARREL.get(),
                RusticMachineBlocks.CRUSHING_TUB.get(), RusticMachineBlocks.BREWING_BARREL.get());

        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(RusticMachineBlocks.EVAPORATING_BASIN.get(),
                RusticMachineBlocks.CONDENSER.get(), RusticMachineBlocks.CONDENSER_ADVANCED.get(),
                RusticMachineBlocks.RETORT.get(), RusticMachineBlocks.RETORT_ADVANCED.get());

        tag(BlockTags.MINEABLE_WITH_AXE).add(
                RusticFarmBlocks.CROP_STAKE.get(), RusticFarmBlocks.STAKE_TIED.get());

        tag(BlockTags.SLABS).add(RusticBlocks.SLATE_ROOF_SLAB.get(), RusticBlocks.SLATE_BRICK_SLAB.get());
        tag(BlockTags.STAIRS).add(RusticBlocks.SLATE_ROOF_STAIRS.get(), RusticBlocks.SLATE_BRICK_STAIRS.get());

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

            tag(BlockTags.DOORS).add(wood.door().get());
            tag(BlockTags.WOODEN_DOORS).add(wood.door().get());
        }

        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(RusticFarmBlocks.BEEHIVE.get(), RusticFarmBlocks.APPLE_SAPLING.get(), RusticFarmBlocks.APPLE_LEAVES.get());
        tag(BlockTags.SAPLINGS).add(RusticFarmBlocks.APPLE_SAPLING.get());
        tag(BlockTags.LEAVES).add(RusticFarmBlocks.APPLE_LEAVES.get());

        tag(BlockTags.MINEABLE_WITH_AXE).add(RusticMachineBlocks.APIARY.get());
    }
}
