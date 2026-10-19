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

/**
 * The Phase 5 farm blocks - the stake-crop trio and, as later slices land, the vinery, the berry bush and
 * the herbs - kept in their own {@link DeferredRegister.Blocks} for the same reason {@link
 * RusticMachineBlocks} is separate from {@link RusticBlocks}: these blocks hang crop, seed and loot
 * behaviour off them that the plain decor has none of, and the register is what groups them.
 *
 * <p>The one wrinkle the item pipeline has to know about: a {@link StakeCropBlock} is <em>never</em> given a
 * block item. Its {@code getCloneItemStack} hands back the seed, the harvest comes from its loot table and
 * it is planted by clicking a seed onto a stake - so a creative-pick or a drop of "the crop block" would be
 * wrong. Only the {@link #CROP_STAKE} pole is placeable from an item and so gets a block item; the
 * {@link #STAKE_TIED} knot reproduces legacy {@code BlockStakeTied}, which ran {@code super(mat, name, false)}
 * and registered the block only (no {@code ItemBlock}): it is made by clicking rope onto a pole, picks as that
 * rope and reverts to the pole on break, so it needs no item of its own. This is what
 * {@link #blockItemEntries()} narrows the register down to for
 * {@link rustic.content.items.RusticItems}' third item loop.
 */
public final class RusticFarmBlocks {

    private static final DeferredRegister.Blocks FARM_BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    // ------------------------------------------------------------------
    // The stake-crop trio. CROP_STAKE is the pole a vine climbs and reverts to; STAKE_TIED is the same
    // pole knotted with rope. Both are 1.12.2 wood: CROP_STAKE hardness 2 with setResistance(5F) (a
    // reported 3.0F), STAKE_TIED hardness 1 with the same resistance - hence stakePole() for both:
    // woodResistant() plus forceSolidOff, because the pole's thin full-height collision box would
    // otherwise compute as solid and revert the farmland under it (a 1.16+ mechanic legacy lacked).
    // ------------------------------------------------------------------
    public static final DeferredBlock<CropStakeBlock> CROP_STAKE = FARM_BLOCKS.registerBlock(
            "crop_stake", properties -> new CropStakeBlock(properties, RusticFarmBlocks::stakeTiedBlock),
            RusticBlockProperties.stakePole(2.0F));

    public static final DeferredBlock<StakeTiedBlock> STAKE_TIED = FARM_BLOCKS.registerBlock(
            "stake_tied", properties -> new StakeTiedBlock(properties, RusticFarmBlocks::cropStakeBlock),
            RusticBlockProperties.stakePole(1.0F));

    // The two stake crops. No crop block item (see the class notes), so neither appears in blockItemEntries.
    // stakeCrop() rather than crop(): the vines keep the pole's thin collision box (StakeCropBlock overrides
    // getCollisionShape), so only forceSolidOff keeps a mature vine from reverting its farmland to dirt.
    public static final DeferredBlock<TomatoCropBlock> TOMATO_CROP = FARM_BLOCKS.registerBlock(
            "tomato_crop", TomatoCropBlock::new, RusticBlockProperties.stakeCrop());

    public static final DeferredBlock<ChiliCropBlock> CHILI_CROP = FARM_BLOCKS.registerBlock(
            "chili_crop", ChiliCropBlock::new, RusticBlockProperties.stakeCrop());

    // ------------------------------------------------------------------
    // Phase 5 herbs: thirteen single-block HerbBlock plants. Nine are "self-herbs" - the block's own item
    // IS the herb, so they appear in blockItemEntries() and get a plain BlockItem. The other four
    // (cloudsbluff, core root, ginseng, marsh mallow) are "crop-herbs": they have NO block item, and their
    // produce-and-seed is a separate ItemHerbEdible registered under the same id in RusticItems, which is
    // handed in as a lazy supplier so the block can resolve getHerb(). Every herb takes crop() properties;
    // the mooncap glows at the legacy light level of 8. Biome worldgen that seeds these is Phase 8, and the
    // herb recipes are Phase 7 - both deliberately absent here.
    // ------------------------------------------------------------------
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

    // The four crop-herbs. Their block references the edible item lazily; the edible references this block
    // lazily in RusticItems, so neither class reads the other while its own initialiser is running.
    public static final DeferredBlock<HerbBlock> CLOUDSBLUFF = cropHerb("cloudsbluff", () -> RusticItems.CLOUDSBLUFF.get());
    public static final DeferredBlock<HerbBlock> CORE_ROOT = cropHerb("core_root", () -> RusticItems.CORE_ROOT.get());
    public static final DeferredBlock<HerbBlock> GINSENG = cropHerb("ginseng", () -> RusticItems.GINSENG.get());
    public static final DeferredBlock<HerbBlock> MARSH_MALLOW = cropHerb("marsh_mallow", () -> RusticItems.MARSH_MALLOW.get());

    // ------------------------------------------------------------------
    // Phase 5 vinery: a placeable GrapeStemBlock (the legacy "Grape Seeds" block item) that ripens in place and
    // then converts a horizontal rope overhead into a GrapeLeavesBlock. The leaves belong to the rope family
    // (they extend BlockRopeBase) but are a plant: they spread along the rope and ripen grapes, and they carry
    // no block item - a leaf picks and loots as its grapes, exactly like the stake crops pick as their seeds.
    // The grape-seed recipe is Phase 7 and the leaf foliage tint / per-age art is Phase 9, both absent here.
    // ------------------------------------------------------------------
    public static final DeferredBlock<GrapeStemBlock> GRAPE_STEM = FARM_BLOCKS.registerBlock(
            "grape_stem", GrapeStemBlock::new, RusticBlockProperties.crop());
    public static final DeferredBlock<GrapeLeavesBlock> GRAPE_LEAVES = FARM_BLOCKS.registerBlock(
            "grape_leaves", GrapeLeavesBlock::new, RusticBlockProperties.crop());

    // ------------------------------------------------------------------
    // Phase 5 berry bush: a placeable six-age ground bush harvested by right-click (picking resets it to age 0),
    // whose produce is a separate fast-eating WILDBERRIES item. It is the legacy BlockBerryBush, whose single
    // abstract getBerries() subclass is here expressed as an item Supplier. Natural-worldgen seeding is Phase 8 and
    // the berry recipes are Phase 7, both absent.
    // ------------------------------------------------------------------
    public static final DeferredBlock<BerryBushBlock> WILDBERRY_BUSH = FARM_BLOCKS.registerBlock(
            "wildberry_bush",
            properties -> new BerryBushBlock(properties, () -> RusticItems.WILDBERRIES.get()),
            RusticBlockProperties.crop().strength(0.2F, 0.0F));

    // ------------------------------------------------------------------
    // Phase 8 prerequisite blocks: the world-generated beehive and the apple tree's own parts.
    //
    // BEEHIVE is 1.12.2's BlockBeehive - a horizontal-facing hive that hangs under leaves and drops honeycomb
    // plus bees (see BlockBeehive for why the leaves-above rule lives in the feature, not the block, and
    // RusticBlockLoot for the drop roll). The legacy Material.GOURD had no SoundType.SLIME equivalent in 1.21,
    // so the sound/map-colour below approximate it; hardness is the legacy 0.5F and the tool is an axe.
    //
    // APPLE_SAPLING + APPLE_LEAVES are the sapling-grown apple line the legacy WorldGenAllTrees never placed
    // in the world: the tree is vanilla OAK_LOG plus APPLE_LEAVES (BlockLeavesApple, its fruit AGE kept as a
    // state the future harvest hook reads) and grows only from this sapling, whose TreeGrower names the
    // configured feature emitted by RusticWorldGenProvider. See RusticWorldGen for the shared key.
    // ------------------------------------------------------------------
    public static final DeferredBlock<BlockBeehive> BEEHIVE = FARM_BLOCKS.registerBlock(
            "beehive", BlockBeehive::new, beehiveProperties());
    public static final DeferredBlock<BlockLeavesApple> APPLE_LEAVES = FARM_BLOCKS.registerBlock(
            "leaves_apple", BlockLeavesApple::new, RusticBlockProperties.ofBase(Blocks.OAK_LEAVES));
    public static final DeferredBlock<SaplingBlock> APPLE_SAPLING = FARM_BLOCKS.register(
            "sapling_apple",
            () -> new SaplingBlock(new TreeGrower("apple", Optional.empty(),
                    Optional.of(RusticWorldGen.APPLE_TREE), Optional.empty()),
                    RusticBlockProperties.ofBase(Blocks.OAK_SAPLING)));

    // ------------------------------------------------------------------
    // Phase 14 (§A3): the apple-seeds crop the crushing tub's apple byproduct plants. 1.12.2's
    // BlockAppleSeeds was a two-age growable crop that, once ripe, replaced itself with an APPLE_SAPLING -
    // the missing gameplay half of the apple line the trees (Phase 8) landed without. It is a plain
    // BushBlock on the crop() properties (Material.PLANTS, hardness 0, plant sound), so unlike the stake
    // crops it carries a block item (the seed, registered under the same id in RusticItems) and is planted
    // by right-clicking that seed on farmland. The sapling it ripens into is passed lazily for the same
    // construction-order reason the poles take theirs.
    // ------------------------------------------------------------------
    public static final DeferredBlock<AppleSeedsBlock> APPLE_SEEDS_CROP = FARM_BLOCKS.registerBlock(
            "apple_seeds",
            properties -> new AppleSeedsBlock(properties, () -> APPLE_SAPLING.get()),
            RusticBlockProperties.crop());

    /**
     * The legacy beehive's properties: {@code Material.GOURD} at hardness 0.5F, {@code SoundType.SLIME} (which
     * 1.21 has no name for, hence the closest plant sound here), {@code setLightOpacity(0)} and a
     * non-opaque/non-full-cube shape, so {@code noOcclusion()} and the narrower {@link BlockBeehive} box.
     * No harvest level was set in 1.12.2, so the hive always dropped its loot even by hand.
     */
    private static BlockBehaviour.Properties beehiveProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .sound(SoundType.GRASS)
                .strength(0.5F, 0.0F)
                .noOcclusion();
    }

    /** A self-herb: a {@link HerbBlock} whose herb item is its own block item (the legacy {@code hasItemBlock} case). */
    private static DeferredBlock<HerbBlock> selfHerb(String name) {
        return FARM_BLOCKS.registerBlock(name, p -> new HerbBlock(p, null), RusticBlockProperties.crop());
    }

    /** A crop-herb: a {@link HerbBlock} whose produce-and-seed is the supplied {@link ItemHerbEdible}. */
    private static DeferredBlock<HerbBlock> cropHerb(String name, Supplier<? extends Item> herb) {
        return FARM_BLOCKS.registerBlock(name, p -> new HerbBlock(p, herb), RusticBlockProperties.crop());
    }

    /**
     * Lazy accessors for the two poles, referenced from within the pole constructors' own suppliers. A
     * direct {@code STAKE_TIED.get()} inside {@code CROP_STAKE}'s initialiser is an illegal forward
     * reference; a call into one of these methods is not, and each only runs at world-interaction time,
     * long after both holders are assigned.
     */
    public static Block stakeTiedBlock() {
        return STAKE_TIED.get();
    }

    public static Block cropStakeBlock() {
        return CROP_STAKE.get();
    }

    /**
     * The farm blocks that are placeable from an item and so need a block item: the crop_stake pole and the
     * nine self-herbs (plus the placeable vinery, bush, beehive and apple parts). {@link rustic.content.items.RusticItems}
     * loops this, not {@link #entries()}, so the stake crops, the {@code stake_tied} knot and the four crop-herbs
     * are deliberately left item-less - matching legacy, where {@code BlockStakeTied} registered the block only
     * ({@code super(mat, name, false)}); its pick result and loot are the rope and it reverts to the pole on break.
     */
    public static List<DeferredHolder<Block, ? extends Block>> blockItemEntries() {
        return List.of(CROP_STAKE,
                ALOE_VERA, BLOOD_ORCHID, CHAMOMILE, COHOSH, DEATHSTALK_MUSHROOM, HORSETAIL,
                MOONCAP_MUSHROOM, VANTA_LILY, WIND_THISTLE, GRAPE_STEM, WILDBERRY_BUSH,
                // Phase 8: the placeable beehive and the apple tree's own parts, all giveable in creative.
                BEEHIVE, APPLE_SAPLING, APPLE_LEAVES);
    }

    /** Every herb block, so the blockstate, item-model and loot generators can iterate the family. */
    public static List<DeferredBlock<HerbBlock>> herbs() {
        return List.of(ALOE_VERA, BLOOD_ORCHID, CHAMOMILE, COHOSH, DEATHSTALK_MUSHROOM, HORSETAIL,
                MOONCAP_MUSHROOM, VANTA_LILY, WIND_THISTLE, CLOUDSBLUFF, CORE_ROOT, GINSENG, MARSH_MALLOW);
    }

    /**
     * The nine self-herbs in the legacy {@code Herbs.init} creation order - the order 1.12.2's alchemy tab
     * showed them in, because {@code BlockHerbBase}'s constructor registered the {@code ItemHerbBase} at the
     * block's registry position. It differs from the register (and {@link #herbs()}) only in that wind_thistle
     * precedes vanta_lily; the four crop-herbs are absent, they show as their {@code ItemHerbEdible} produce.
     */
    public static List<DeferredBlock<HerbBlock>> tabSelfHerbs() {
        return List.of(ALOE_VERA, BLOOD_ORCHID, CHAMOMILE, COHOSH, DEATHSTALK_MUSHROOM, HORSETAIL,
                MOONCAP_MUSHROOM, WIND_THISTLE, VANTA_LILY);
    }

    /**
     * The legacy {@code Blocks.FIRE.setFireInfo(herb, 60, 100)} for the ten burnable herbs, applied after
     * registration exactly as {@link RusticBlocks#setFlammables()} does for the wood - the fire lookup table
     * still lives on the fire block in 1.21.1. Aloe vera and the two mushrooms were never set alight in the
     * legacy mod, so they are absent here.
     */
    public static void setFlammables() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        for (DeferredBlock<HerbBlock> herb : List.of(BLOOD_ORCHID, CHAMOMILE, COHOSH, HORSETAIL, VANTA_LILY,
                WIND_THISTLE, CLOUDSBLUFF, CORE_ROOT, GINSENG, MARSH_MALLOW)) {
            fire.setFlammable(herb.get(), 60, 100);
        }
        // 1.12.2 setFireInfo on the vinery: the stem was 20/100, the leaves 30/60.
        fire.setFlammable(GRAPE_STEM.get(), 20, 100);
        fire.setFlammable(GRAPE_LEAVES.get(), 30, 60);
        // 1.12.2 setFireInfo on the wildberry bush: 40/80.
        fire.setFlammable(WILDBERRY_BUSH.get(), 40, 80);
        // 1.12.2's BlockLeavesApple constructor asked for Blocks.FIRE.setFireInfo(this, 30, 60) - the same
        // pair the wood-family leaves carry, because an apple tree's foliage burns like any other leaf.
        fire.setFlammable(APPLE_LEAVES.get(), 30, 60);
    }

    /** Every farm block, for the loot generator, which special-cases the crops and the tied stake. */
    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return FARM_BLOCKS.getEntries();
    }

    /** Attaches the farm-block register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        FARM_BLOCKS.register(modEventBus);
    }

    private RusticFarmBlocks() {
    }
}
