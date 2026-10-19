package rustic.content.blocks;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.items.RusticItems;
import rustic.world.RusticWorldGen;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class RusticFarmBlocks {

    private static final DeferredRegister.Blocks FARM_BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    public static final DeferredBlock<CropStakeBlock> CROP_STAKE = FARM_BLOCKS.registerBlock(
            "crop_stake", properties -> new CropStakeBlock(properties, RusticFarmBlocks::stakeTiedBlock),
            RusticBlockProperties.stakePole(2.0F));

    public static final DeferredBlock<StakeTiedBlock> STAKE_TIED = FARM_BLOCKS.registerBlock(
            "stake_tied", properties -> new StakeTiedBlock(properties, RusticFarmBlocks::cropStakeBlock),
            RusticBlockProperties.stakePole(1.0F));

    public static final DeferredBlock<TomatoCropBlock> TOMATO_CROP = FARM_BLOCKS.registerBlock(
            "tomato_crop", TomatoCropBlock::new, RusticBlockProperties.stakeCrop());

    public static final DeferredBlock<ChiliCropBlock> CHILI_CROP = FARM_BLOCKS.registerBlock(
            "chili_crop", ChiliCropBlock::new, RusticBlockProperties.stakeCrop());

    public static final DeferredBlock<HerbBlock> ALOE_VERA = selfHerb("aloe_vera");
    public static final DeferredBlock<HerbBlock> BLOOD_ORCHID = selfHerb("blood_orchid");
    public static final DeferredBlock<HerbBlock> CHAMOMILE = selfHerb("chamomile");
    public static final DeferredBlock<HerbBlock> COHOSH = selfHerb("cohosh");
    public static final DeferredBlock<HerbBlock> DEATHSTALK_MUSHROOM = selfHerb("deathstalk_mushroom");
    public static final DeferredBlock<HerbBlock> HORSETAIL = selfHerb("horsetail");
    public static final DeferredBlock<HerbBlock> MOONCAP_MUSHROOM = FARM_BLOCKS.registerBlock(
            "mooncap_mushroom", p -> new HerbBlock(p, null),
            RusticBlockProperties.crop().lightLevel(state -> 8));
    public static final DeferredBlock<HerbBlock> VANTA_LILY = selfHerb("vanta_lily");
    public static final DeferredBlock<HerbBlock> WIND_THISTLE = selfHerb("wind_thistle");

    public static final DeferredBlock<HerbBlock> CLOUDSBLUFF = cropHerb("cloudsbluff", () -> RusticItems.CLOUDSBLUFF.get());
    public static final DeferredBlock<HerbBlock> CORE_ROOT = cropHerb("core_root", () -> RusticItems.CORE_ROOT.get());
    public static final DeferredBlock<HerbBlock> GINSENG = cropHerb("ginseng", () -> RusticItems.GINSENG.get());
    public static final DeferredBlock<HerbBlock> MARSH_MALLOW = cropHerb("marsh_mallow", () -> RusticItems.MARSH_MALLOW.get());

    public static final DeferredBlock<GrapeStemBlock> GRAPE_STEM = FARM_BLOCKS.registerBlock(
            "grape_stem", GrapeStemBlock::new, RusticBlockProperties.crop());
    public static final DeferredBlock<GrapeLeavesBlock> GRAPE_LEAVES = FARM_BLOCKS.registerBlock(
            "grape_leaves", GrapeLeavesBlock::new, RusticBlockProperties.crop());

    public static final DeferredBlock<BerryBushBlock> WILDBERRY_BUSH = FARM_BLOCKS.registerBlock(
            "wildberry_bush",
            properties -> new BerryBushBlock(properties, () -> RusticItems.WILDBERRIES.get()),
            RusticBlockProperties.crop().strength(0.2F, 0.0F));

    public static final DeferredBlock<BlockBeehive> BEEHIVE = FARM_BLOCKS.registerBlock(
            "beehive", BlockBeehive::new, beehiveProperties());
    public static final DeferredBlock<BlockLeavesApple> APPLE_LEAVES = FARM_BLOCKS.registerBlock(
            "leaves_apple", BlockLeavesApple::new, RusticBlockProperties.ofBase(Blocks.OAK_LEAVES));
    public static final DeferredBlock<SaplingBlock> APPLE_SAPLING = FARM_BLOCKS.register(
            "sapling_apple",
            () -> new SaplingBlock(new TreeGrower("apple", Optional.empty(),
                    Optional.of(RusticWorldGen.APPLE_TREE), Optional.empty()),
                    RusticBlockProperties.ofBase(Blocks.OAK_SAPLING)));

    public static final DeferredBlock<AppleSeedsBlock> APPLE_SEEDS_CROP = FARM_BLOCKS.registerBlock(
            "apple_seeds",
            properties -> new AppleSeedsBlock(properties, () -> APPLE_SAPLING.get()),
            RusticBlockProperties.crop());

    private static BlockBehaviour.Properties beehiveProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .sound(SoundType.GRASS)
                .strength(0.5F, 0.0F)
                .noOcclusion();
    }

    private static DeferredBlock<HerbBlock> selfHerb(String name) {
        return FARM_BLOCKS.registerBlock(name, p -> new HerbBlock(p, null), RusticBlockProperties.crop());
    }

    private static DeferredBlock<HerbBlock> cropHerb(String name, Supplier<? extends Item> herb) {
        return FARM_BLOCKS.registerBlock(name, p -> new HerbBlock(p, herb), RusticBlockProperties.crop());
    }

    public static Block stakeTiedBlock() {
        return STAKE_TIED.get();
    }

    public static Block cropStakeBlock() {
        return CROP_STAKE.get();
    }

    // Farm blocks that get a giveable BlockItem. STAKE_TIED is deliberately absent: the rope knot has no item
    // (it is reached only by roping a crop_stake), so it stays a block-only registration like the original.
    public static List<DeferredHolder<Block, ? extends Block>> blockItemEntries() {
        return List.of(CROP_STAKE,
                ALOE_VERA, BLOOD_ORCHID, CHAMOMILE, COHOSH, DEATHSTALK_MUSHROOM, HORSETAIL,
                MOONCAP_MUSHROOM, VANTA_LILY, WIND_THISTLE, GRAPE_STEM, WILDBERRY_BUSH,

                BEEHIVE, APPLE_SAPLING, APPLE_LEAVES);
    }

    public static List<DeferredBlock<HerbBlock>> herbs() {
        return List.of(ALOE_VERA, BLOOD_ORCHID, CHAMOMILE, COHOSH, DEATHSTALK_MUSHROOM, HORSETAIL,
                MOONCAP_MUSHROOM, VANTA_LILY, WIND_THISTLE, CLOUDSBLUFF, CORE_ROOT, GINSENG, MARSH_MALLOW);
    }

    public static List<DeferredBlock<HerbBlock>> tabSelfHerbs() {
        return List.of(ALOE_VERA, BLOOD_ORCHID, CHAMOMILE, COHOSH, DEATHSTALK_MUSHROOM, HORSETAIL,
                MOONCAP_MUSHROOM, WIND_THISTLE, VANTA_LILY);
    }

    public static void setFlammables() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (DeferredBlock<HerbBlock> herb : List.of(BLOOD_ORCHID, CHAMOMILE, COHOSH, HORSETAIL, VANTA_LILY,
                WIND_THISTLE, CLOUDSBLUFF, CORE_ROOT, GINSENG, MARSH_MALLOW)) {
            fire.setFlammable(herb.get(), 60, 100);
        }

        fire.setFlammable(GRAPE_STEM.get(), 20, 100);
        fire.setFlammable(GRAPE_LEAVES.get(), 30, 60);

        fire.setFlammable(WILDBERRY_BUSH.get(), 40, 80);

        fire.setFlammable(APPLE_LEAVES.get(), 30, 60);
    }

    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return FARM_BLOCKS.getEntries();
    }

    public static void register(IEventBus modEventBus) {
        FARM_BLOCKS.register(modEventBus);
    }

    private RusticFarmBlocks() {
    }
}
