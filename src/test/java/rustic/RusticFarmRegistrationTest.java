package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.ChiliCropBlock;
import rustic.content.blocks.BlockRopeBase;
import rustic.content.blocks.BerryBushBlock;
import rustic.content.blocks.CropStakeBlock;
import rustic.content.blocks.GrapeLeavesBlock;
import rustic.content.blocks.GrapeStemBlock;
import rustic.content.blocks.HerbBlock;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.StakeCropBlock;
import rustic.content.blocks.StakeTiedBlock;
import rustic.content.blocks.TomatoCropBlock;
import rustic.content.items.RusticItems;
import rustic.content.items.ItemHerbEdible;
import rustic.content.items.StakeCropSeedItem;

/**
 * Phase 5 characterisation test for the stake-crop slice: the two placeable poles ({@code crop_stake},
 * {@code stake_tied}), the two climbing vines ({@code tomato_crop}, {@code chili_crop}), their seed items and
 * their produce, plus the datagen that makes each render and drop.
 *
 * <p>{@link RusticDecorRegistrationTest} deliberately excludes these ids (see its {@code farmIds()} helper),
 * so this is where the crop pipeline's one structural oddity is asserted: a {@link StakeCropBlock} is
 * <em>never</em> given a block item - it is planted by clicking a seed onto a stake, picked as its seed and
 * harvested through its loot table - while the two poles are ordinary placeable wood. A slice that gave a
 * vine a block item would show a ghost "crop" cube in the creative menu and let a player place an
 * unsupported vine, which is exactly the failure this guards against.
 *
 * <p>Like the machine test this runs in ModDevGradle's FML unit-test JVM, which makes {@code BuiltInRegistries}
 * populated and the creative tab readable. It asserts the wiring a live game depends on exists (register
 * shape, properties, loot, models, tab membership), not the growth tick itself - the random-tick climb and the
 * {@code canSurvive} light check need a real level and are a live-game check.
 */
public class RusticFarmRegistrationTest {

    // ------------------------------------------------------------------ the two poles

    /** The crop stake was Material.WOOD at hardness 2 with the legacy {@code setResistance(5F)}. */
    @Test
    public void cropStakeIsRegisteredAsAFarmBlockWithTheLegacyWoodNumbers() {
        Block stake = RusticFarmBlocks.CROP_STAKE.get();
        assertSame(CropStakeBlock.class, stake.getClass(), "rustic:crop_stake is not a CropStakeBlock");
        assertSame(stake, BuiltInRegistries.BLOCK.get(RusticFarmBlocks.CROP_STAKE.getId()),
                "the farm register does not resolve rustic:crop_stake to the deferred block");

        assertEquals(2.0F, stake.defaultDestroyTime(), 0.0001F, "crop_stake hardness (1.12.2: 2)");
        assertEquals(3.0F, stake.getExplosionResistance(stake.defaultBlockState(), null, null, null),
                0.0001F, "crop_stake resistance, the legacy setResistance(5F) reported as 3.0F");
        assertSame(SoundType.WOOD, stake.getSoundType(stake.defaultBlockState(), null, null, null),
                "Material.WOOD sound");
        assertFalse(stake.defaultBlockState().requiresCorrectToolForDrops(),
                "crop_stake would stop dropping its item when mined by hand");
    }

    /** The tied stake was the same wood family at hardness 1 with the same reported resistance. */
    @Test
    public void stakeTiedIsRegisteredAsAFarmBlockWithTheLegacyWoodNumbers() {
        Block tied = RusticFarmBlocks.STAKE_TIED.get();
        assertSame(StakeTiedBlock.class, tied.getClass(), "rustic:stake_tied is not a StakeTiedBlock");

        assertEquals(1.0F, tied.defaultDestroyTime(), 0.0001F, "stake_tied hardness (1.12.2: 1)");
        assertEquals(3.0F, tied.getExplosionResistance(tied.defaultBlockState(), null, null, null),
                0.0001F, "stake_tied resistance, the legacy setResistance(5F) reported as 3.0F");
        assertSame(SoundType.WOOD, tied.getSoundType(tied.defaultBlockState(), null, null, null),
                "Material.WOOD sound");
    }

    /** 1.12.2 gave every placeable block a block item under its own id; the two poles are no exception. */
    @Test
    public void theTwoPolesHaveBlockItemsUnderTheirOwnIds() {
        for (var holder : RusticFarmBlocks.blockItemEntries()) {
            Block block = holder.get();
            Item item = block.asItem();
            assertTrue(item instanceof BlockItem, holder.getId() + " has no block item");
            assertEquals(holder.getId().getPath(), BuiltInRegistries.ITEM.getKey(item).getPath(),
                    "the pole block item drifted from the block id");
            assertSame(block, ((BlockItem) item).getBlock());
        }
    }

    /**
     * The tied stake stores its four horizontal connections as real properties (the legacy
     * {@code getActualState} is gone in 1.21), all false in a freshly declared knot.
     */
    @Test
    public void stakeTiedStoresFourConnectionFlagsAllOffByDefault() {
        BlockState state = RusticFarmBlocks.STAKE_TIED.get().defaultBlockState();
        for (var property : List.of(StakeTiedBlock.NORTH, StakeTiedBlock.WEST,
                StakeTiedBlock.SOUTH, StakeTiedBlock.EAST)) {
            assertTrue(state.hasProperty(property), "the tied stake lost its " + property.getName() + " flag");
            assertFalse(state.getValue(property), "a lone knot connects to nothing: " + property.getName());
        }
    }

    /** 1.12.2's {@code getPickBlock}: middle-clicking the knot hands back the rope, not the stake. */
    @Test
    public void stakeTiedPicksAsRope() {
        ItemStack picked = RusticFarmBlocks.STAKE_TIED.get()
                .getCloneItemStack(null, BlockPos.ZERO, RusticFarmBlocks.STAKE_TIED.get().defaultBlockState());
        assertEquals("rope", BuiltInRegistries.ITEM.getKey(picked.getItem()).getPath(),
                "the tied stake should pick as the rope that made it");
    }

    // ------------------------------------------------------------------ the two vines

    /**
     * The stake crops are neither wood nor a vanilla {@code CropBlock}: they are a {@link BushBlock}
     * (a plant that reads {@code canSustainPlant} for its support) that is also {@link BonemealableBlock},
     * built from the legacy {@code Material.FOLIAGE} numbers - hardness 0, broken instantly, plant sound.
     */
    @Test
    public void eachCropIsALightlessBonemealableBushWithTheLegacyFoliageNumbers() {
        for (var holder : List.of(RusticFarmBlocks.TOMATO_CROP, RusticFarmBlocks.CHILI_CROP)) {
            Block crop = holder.get();
            assertTrue(crop instanceof BushBlock, holder.getId() + " must be a BushBlock");
            assertTrue(crop instanceof BonemealableBlock, holder.getId() + " must be bonemealable");

            assertEquals(0.0F, crop.defaultDestroyTime(), 0.0001F, holder.getId() + " hardness (legacy: 0)");
            assertEquals(0.0F, crop.getExplosionResistance(crop.defaultBlockState(), null, null, null),
                    0.0001F, holder.getId() + " resistance");
            assertSame(SoundType.CROP, crop.getSoundType(crop.defaultBlockState(), null, null, null),
                    holder.getId() + " Material.FOLIAGE became the CROP sound");
            assertFalse(crop.defaultBlockState().requiresCorrectToolForDrops(), holder.getId().toString());
        }
    }

    /** {@code PropertyInteger AGE 0..3} is {@link BlockStateProperties#AGE_3}, defaulting to age 0. */
    @Test
    public void eachCropCarriesTheLegacyAgePropertyZeroToThreeAndDefaultsToZero() {
        for (var holder : List.of(RusticFarmBlocks.TOMATO_CROP, RusticFarmBlocks.CHILI_CROP)) {
            StakeCropBlock crop = (StakeCropBlock) holder.get();
            assertSame(BlockStateProperties.AGE_3, crop.getStateDefinition().getProperty("age"),
                    holder.getId() + " must use vanilla's AGE_3, the legacy PropertyInteger 0..3");
            assertEquals(List.of(0, 1, 2, 3), List.copyOf(((IntegerProperty) StakeCropBlock.AGE).getPossibleValues()),
                    holder.getId() + " lost an age step");
            assertEquals(0, crop.getAge(crop.defaultBlockState()), holder.getId() + " starts at age 0");
            assertTrue(crop.isMaxAge(crop.getStateForAge(3)), holder.getId() + " age 3 must be the top");
            assertFalse(crop.isMaxAge(crop.getStateForAge(2)), holder.getId() + " age 2 must not be the top");
        }
    }

    /**
     * The one legacy difference between the two vines besides the produce: a tomato climbed three stakes,
     * a chili only two. That is {@code getMaxHeight()}, which the random-tick column guard reads.
     */
    @Test
    public void theTomatoClimbsTallerThanTheChili() {
        assertEquals(3, RusticFarmBlocks.TOMATO_CROP.get().getMaxHeight(), "1.12.2: tomato maxHeight 3");
        assertEquals(2, RusticFarmBlocks.CHILI_CROP.get().getMaxHeight(), "1.12.2: chili maxHeight 2");
    }

    /**
     * The structural oddity of the crop pipeline: a vine must not have a block item. Its pick result is the
     * seed, its harvest comes from the loot table and it is planted by clicking a seed onto a stake, so a
     * block item would be both unplaceable-by-design and a ghost entry in the creative menu.
     */
    @Test
    public void theCropsAreNeverGivenABlockItemAndPickAsTheirSeed() {
        assertCropHasNoItem(RusticFarmBlocks.TOMATO_CROP.get());
        assertCropHasNoItem(RusticFarmBlocks.CHILI_CROP.get());

        ItemStack tomatoPick = RusticFarmBlocks.TOMATO_CROP.get()
                .getCloneItemStack(null, BlockPos.ZERO, RusticFarmBlocks.TOMATO_CROP.get().defaultBlockState());
        assertSame(RusticItems.TOMATO_SEEDS.get(), tomatoPick.getItem(), "the tomato picks as its seed");
        ItemStack chiliPick = RusticFarmBlocks.CHILI_CROP.get()
                .getCloneItemStack(null, BlockPos.ZERO, RusticFarmBlocks.CHILI_CROP.get().defaultBlockState());
        assertSame(RusticItems.CHILI_PEPPER_SEEDS.get(), chiliPick.getItem(), "the chili picks as its seed");
    }

    private static void assertCropHasNoItem(Block crop) {
        assertFalse(crop.asItem() instanceof BlockItem,
                BuiltInRegistries.BLOCK.getKey(crop) + " must not have a block item");
    }

    /** Every farm block registers under its legacy id: two poles, two vines, thirteen herbs and the Phase 8 beehive/apple trio. */
    @Test
    public void everyLegacyFarmBlockIsRegistered() {
        List<String> ids = new ArrayList<>();
        RusticFarmBlocks.entries().forEach(holder -> ids.add(holder.getId().getPath()));
        assertEquals(List.of(
                "aloe_vera", "beehive", "blood_orchid", "chamomile", "chili_crop", "cloudsbluff", "cohosh",
                "core_root", "crop_stake", "deathstalk_mushroom", "ginseng", "grape_leaves", "grape_stem",
                "horsetail", "leaves_apple", "marsh_mallow", "mooncap_mushroom", "sapling_apple", "stake_tied",
                "tomato_crop", "vanta_lily", "wildberry_bush", "wind_thistle"),
                ids.stream().sorted().toList(), "the full farm register (Phase 5 crops plus the Phase 8 beehive/apple blocks)");
    }

    // ------------------------------------------------------------------ seeds and produce

    /** 1.12.2's {@code ItemStakeCropSeed}: a seed that carries the vine it plants, as its age-0 state. */
    @Test
    public void eachSeedItemCarriesItsOwnCropAndYieldsAnAgeZeroState() {
        StakeCropSeedItem tomato = RusticItems.TOMATO_SEEDS.get();
        assertSame(TomatoCropBlock.class, tomato.getCrop().getClass(), "tomato_seeds plant a TomatoCropBlock");
        BlockState tomatoState = tomato.getCropState();
        assertSame(RusticFarmBlocks.TOMATO_CROP.get(), tomatoState.getBlock());
        assertEquals(0, tomatoState.getValue(StakeCropBlock.AGE), "a clicked-in vine starts at age 0");

        StakeCropSeedItem chili = RusticItems.CHILI_PEPPER_SEEDS.get();
        assertSame(ChiliCropBlock.class, chili.getCrop().getClass(), "chili_pepper_seeds plant a ChiliCropBlock");
        assertEquals(0, chili.getCropState().getValue(StakeCropBlock.AGE), "a clicked-in vine starts at age 0");
    }

    /**
     * The three produce are plain foods - {@code ItemFoodBase(nutrition, saturation)} read off the legacy
     * fields. The potion-carrying edibles are Phase 6, so none of these may have a food effect.
     */
    @Test
    public void theProduceArePlainFoodsWithTheLegacyHungerNumbers() {
        assertPlainFood(RusticItems.TOMATO.get(), 4, 0.4F);
        assertPlainFood(RusticItems.CHILI_PEPPER.get(), 3, 0.4F);
        assertPlainFood(RusticItems.GHOST_PEPPER.get(), 4, 0.7F);
        // The grapes carry the legacy ItemFoodBase(3, 0.3F) numbers; their half-length eat time is the
        // FoodProperties fast() flag, which assertPlainFood does not touch, so it is asserted separately below.
        assertPlainFood(RusticItems.GRAPES.get(), 3, 0.3F);
        // The berries carry the legacy ItemFoodBase(2, 0.5F); their half-length eat time is fast(), asserted with
        // the block behaviour below.
        assertPlainFood(RusticItems.WILDBERRIES.get(), 2, 0.5F);
    }

    private static void assertPlainFood(Item item, int nutrition, float saturationModifier) {
        FoodProperties food = item.getFoodProperties(new ItemStack(item), null);
        assertNotNull(food, BuiltInRegistries.ITEM.getKey(item) + " is not a food");
        assertEquals(nutrition, food.nutrition(), "nutrition");
        // 1.21 stores the folded saturation, not the legacy modifier; replay the same constant so the
        // assertion checks the modifier that was handed to the builder rather than a restated number.
        assertEquals(FoodConstants.saturationByModifier(nutrition, saturationModifier), food.saturation(),
                0.0001F, "saturation, the legacy modifier folded through 1.21's constant");
        assertTrue(food.effects().isEmpty(), "these three had no setPotionEffect in 1.12.2; effects are Phase 6");
    }

    /** The stake crops and their produce are offered in the farming tab; the vines are not (no item). */
    @Test
    public void theFarmingTabOffersThePolesSeedsAndProduce() {
        CreativeModeTab farming = RusticCreativeTabs.FARMING.get();
        farming.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> shown = new ArrayList<>();
        for (ItemStack stack : farming.getDisplayItems()) {
            shown.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        }
        for (String id : List.of("crop_stake", "stake_tied", "tomato_seeds", "chili_pepper_seeds",
                "tomato", "chili_pepper", "ghost_pepper", "grape_stem", "grapes", "wildberry_bush", "wildberries")) {
            assertTrue(shown.contains(id), "rustic:" + id + " is missing from the FARMING tab: " + shown);
        }
        // The vines themselves have no item, so they can never appear.
        assertFalse(shown.contains("tomato_crop"), "a vine leaked a block item into the FARMING tab");
        assertFalse(shown.contains("chili_crop"), "a vine leaked a block item into the FARMING tab");
        assertFalse(shown.contains("grape_leaves"), "a grape leaf leaked a block item into the FARMING tab");
    }

    // ------------------------------------------------------------------ vinery

    /**
     * The grape stem is the vinery's placeable half: a lightless bonemealable {@link BushBlock} on the crop
     * family's FOLIAGE numbers that carries a block item (the legacy "Grape Seeds") and ripens through AGE_3.
     */
    @Test
    public void theGrapeStemIsAPlaceableBonemealableBushWithTheCropNumbers() {
        GrapeStemBlock stem = RusticFarmBlocks.GRAPE_STEM.get();
        assertSame(GrapeStemBlock.class, stem.getClass(), "rustic:grape_stem is not a GrapeStemBlock");
        assertTrue(stem instanceof BushBlock, "grape_stem must be a BushBlock");
        assertTrue(stem instanceof BonemealableBlock, "grape_stem must be bonemealable");

        assertEquals(0.0F, stem.defaultDestroyTime(), 0.0001F, "grape_stem hardness (crop family: 0)");
        assertSame(SoundType.CROP, stem.getSoundType(stem.defaultBlockState(), null, null, null),
                "grape_stem uses the CROP plant sound");
        assertSame(BlockStateProperties.AGE_3, stem.getStateDefinition().getProperty("age"),
                "grape_stem uses vanilla's AGE_3, the legacy PropertyInteger 0..3");
        assertEquals(0, stem.getAge(stem.defaultBlockState()), "a planted stem starts at age 0");
        assertTrue(stem.isMaxAge(stem.getStateForAge(3)), "grape_stem age 3 is the top");

        // Unlike the stake crops, the stem is placeable, so it does have a block item.
        assertTrue(stem.asItem() instanceof BlockItem, "grape_stem is placeable and must have a block item");
    }

    /**
     * The grape leaf is the vinery's climbing half: a rope-family plant ({@link BlockRopeBase}) that stores the
     * legacy grapes/distance flags, carries no block item, and picks as its grapes.
     */
    @Test
    public void theGrapeLeavesAreARopeFamilyPlantThatPicksAsGrapes() {
        GrapeLeavesBlock leaves = RusticFarmBlocks.GRAPE_LEAVES.get();
        assertSame(GrapeLeavesBlock.class, leaves.getClass(), "rustic:grape_leaves is not a GrapeLeavesBlock");
        assertTrue(leaves instanceof BlockRopeBase, "grape leaves belong to the rope family");
        assertTrue(leaves instanceof BonemealableBlock, "grape leaves must be bonemealable");

        assertNotNull(leaves.getStateDefinition().getProperty("grapes"), "grape leaves lost the grapes flag");
        assertNotNull(leaves.getStateDefinition().getProperty("distance"), "grape leaves lost the distance flag");
        assertSame(BlockRopeBase.AXIS, leaves.getStateDefinition().getProperty("axis"),
                "grape leaves reuse the rope family's axis");
        assertNotNull(leaves.getStateDefinition().getProperty("dangle"),
                "1.12.2's computed dangle is a stored property here");

        BlockState def = leaves.defaultBlockState();
        assertFalse(def.getValue(GrapeLeavesBlock.GRAPES), "a fresh leaf carries no grapes");
        assertEquals(0, def.getValue(GrapeLeavesBlock.DIST), "a fresh leaf sits at distance 0");
        assertFalse(leaves.asItem() instanceof BlockItem, "grape leaves must not have a block item");

        ItemStack picked = leaves.getCloneItemStack(null, BlockPos.ZERO, def);
        assertSame(RusticItems.GRAPES.get(), picked.getItem(), "a grape leaf picks as its grapes");
    }

    /**
     * The vinery's datagen: the stem maps its four ages to two placeholder crosses (the real per-age art and the
     * mature log model are Phase 9), the leaves resolve every state to one placeholder cross, the stem's block item
     * is the flat "Grape Seeds" sprite, and the leaves loot their grapes only when carrying fruit.
     */
    @Test
    public void datagenProducedTheGrapeStatesModelsAndFruitGatedLoot() {
        JsonObject stemVariants = json("/assets/rustic/blockstates/grape_stem.json").getAsJsonObject("variants");
        assertEquals("rustic:block/grape_stem",
                stemVariants.getAsJsonObject("age=0").get("model").getAsString(), "grape_stem young stage");
        assertEquals("rustic:block/grape_stem_tall",
                stemVariants.getAsJsonObject("age=3").get("model").getAsString(), "grape_stem ripe stage");

        JsonObject stemItem = json("/assets/rustic/models/item/grape_stem.json");
        assertEquals("minecraft:item/generated", stemItem.get("parent").getAsString(),
                "the stem's block item is the flat Grape Seeds sprite");
        assertEquals("rustic:item/grape_seeds", stemItem.getAsJsonObject("textures").get("layer0").getAsString(),
                "the Grape Seeds sprite reads rustic:item/grape_seeds");

        // Every leaf state resolves to the one placeholder cross; spot-check a fruiting horizontal variant exists.
        JsonObject leavesVariants = json("/assets/rustic/blockstates/grape_leaves.json").getAsJsonObject("variants");
        String fruitingKey = leavesVariants.entrySet().stream()
                .map(java.util.Map.Entry::getKey)
                .filter(k -> k.contains("grapes=true"))
                .findFirst().orElse(null);
        assertNotNull(fruitingKey, "grape_leaves lost its fruiting variant");
        assertEquals("rustic:block/grape_leaves",
                leavesVariants.getAsJsonObject(fruitingKey).get("model").getAsString(),
                "the fruiting leaf draws the placeholder cross");

        // The stem drops itself; the leaves drop grapes only when grapes=true.
        assertEquals("rustic:grape_stem", lootFirstEntry("/data/rustic/loot_table/blocks/grape_stem.json"));
        JsonObject leavesLoot = json("/data/rustic/loot_table/blocks/grape_leaves.json");
        assertEquals("rustic:grapes", lootFirstEntry("/data/rustic/loot_table/blocks/grape_leaves.json"),
                "a grape leaf's base drop is its grapes");
        JsonObject condition = leavesLoot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("rustic:grape_leaves", condition.get("block").getAsString(), "grapes condition names the leaf");
        assertEquals("true", condition.getAsJsonObject("properties").get("grapes").getAsString(),
                "grapes only drop when the leaf is carrying them");
    }

    // ------------------------------------------------------------------ wildberry bush

    /**
     * The wildberry bush is a placeable, bonemealable ground bush (the legacy {@code BlockBerryBush}) on a lighter
     * hardness than the crop family ({@code 0.2}), aged 0..5, harvested by hand rather than broken; its produce is
     * the separate WILDBERRIES item and it carries a block item because it is placeable.
     */
    @Test
    public void theWildberryBushIsAPlaceableBonemealableBushThatYieldsBerries() {
        BerryBushBlock bush = RusticFarmBlocks.WILDBERRY_BUSH.get();
        assertSame(BerryBushBlock.class, bush.getClass(), "rustic:wildberry_bush is not a BerryBushBlock");
        assertTrue(bush instanceof BushBlock, "wildberry_bush must be a BushBlock");
        assertTrue(bush instanceof BonemealableBlock, "wildberry_bush must be bonemealable");

        assertEquals(0.2F, bush.defaultDestroyTime(), 0.0001F, "wildberry_bush hardness (1.12.2: 0.2)");
        assertSame(SoundType.CROP, bush.getSoundType(bush.defaultBlockState(), null, null, null),
                "wildberry_bush uses the crop-family plant sound");

        assertSame(BlockStateProperties.AGE_5, bush.getStateDefinition().getProperty("age"),
                "wildberry_bush uses vanilla's AGE_5, the legacy PropertyInteger 0..5");
        assertEquals(List.of(0, 1, 2, 3, 4, 5), List.copyOf(((IntegerProperty) BerryBushBlock.AGE).getPossibleValues()),
                "wildberry_bush lost an age step");
        assertEquals(0, bush.getAge(bush.defaultBlockState()), "a fresh bush starts at age 0");
        assertTrue(bush.isMaxAge(bush.getStateForAge(5)), "bush age 5 is the top");
        assertFalse(bush.isMaxAge(bush.getStateForAge(4)), "bush age 4 is not the top");
        assertSame(RusticItems.WILDBERRIES.get(), bush.getBerries(), "the bush's produce is WILDBERRIES");

        // The bush is placeable, so unlike a vine it does have a block item under its own id.
        assertTrue(bush.asItem() instanceof BlockItem, "wildberry_bush is placeable and must have a block item");
    }

    /**
     * The bush's datagen: every age collapses to one placeholder cross (the eight per-age berry overlays and the
     * foliage tint are Phase 9), the block item is a draw of the block itself (as the legacy parent was), the berry
     * produce is a flat sprite, and the loot drops the bush plus one berry gated on {@code age=5}.
     */
    @Test
    public void datagenProducedTheBushStateModelAndRipeBerryLoot() {
        JsonObject state = json("/assets/rustic/blockstates/wildberry_bush.json");
        assertEquals("rustic:block/wildberry_bush",
                state.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString(),
                "wildberry_bush blockstate");

        JsonObject bushItem = json("/assets/rustic/models/item/wildberry_bush.json");
        assertEquals("rustic:block/wildberry_bush", bushItem.get("parent").getAsString(),
                "the bush's block item parents the block model, exactly as the legacy item did");

        JsonObject berryItem = json("/assets/rustic/models/item/wildberries.json");
        assertEquals("minecraft:item/generated", berryItem.get("parent").getAsString(),
                "the berry produce is a flat sprite");
        assertEquals("rustic:item/wildberries", berryItem.getAsJsonObject("textures").get("layer0").getAsString(),
                "the berry sprite reads rustic:item/wildberries");

        String resource = "/data/rustic/loot_table/blocks/wildberry_bush.json";
        assertEquals("rustic:wildberry_bush", lootFirstEntry(resource), "a broken bush drops itself");
        JsonArray pools = json(resource).getAsJsonArray("pools");
        assertEquals(2, pools.size(), "the bush has a self pool and a ripe-berry pool");
        assertEquals("rustic:wildberries",
                pools.get(1).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject()
                        .get("name").getAsString(), "the ripe pool drops berries");
        JsonObject condition = pools.get(1).getAsJsonObject().getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("rustic:wildberry_bush", condition.get("block").getAsString(),
                "the berry condition names the bush");
        assertEquals("5", condition.getAsJsonObject("properties").get("age").getAsString(),
                "berries only drop at full age");
    }

    // ------------------------------------------------------------------ herbs

    /** The four crop-herbs have no block item; their produce is a separate ItemHerbEdible under the same id. */
    private static final List<String> CROP_HERB_IDS = List.of("cloudsbluff", "core_root", "ginseng", "marsh_mallow");

    /** Every herb is a lightless bonemealable BushBlock on the legacy FOLIAGE numbers, aged 0..3. */
    @Test
    public void everyHerbIsABonemealableBushWithTheLegacyFoliageNumbersAndAgeThree() {
        for (var holder : RusticFarmBlocks.herbs()) {
            HerbBlock herb = holder.get();
            assertTrue(herb instanceof BushBlock, holder.getId() + " must be a BushBlock");
            assertTrue(herb instanceof BonemealableBlock, holder.getId() + " must be bonemealable");
            assertEquals(0.0F, herb.defaultDestroyTime(), 0.0001F, holder.getId() + " hardness (legacy: 0)");
            assertSame(SoundType.CROP, herb.getSoundType(herb.defaultBlockState(), null, null, null),
                    holder.getId() + " Material.FOLIAGE became the CROP sound");
            assertSame(BlockStateProperties.AGE_3, herb.getStateDefinition().getProperty("age"),
                    holder.getId() + " must use vanilla's AGE_3");
            assertEquals(0, herb.getAge(herb.defaultBlockState()), holder.getId() + " starts at age 0");
            assertTrue(herb.isMaxAge(herb.getStateForAge(3)), holder.getId() + " age 3 must be the top");
        }
    }

    /** The nine self-herbs get a block item and yield it; the four crop-herbs get none and yield their edible. */
    @Test
    public void selfHerbsHaveABlockItemAndCropHerbsYieldTheirEdibleInstead() {
        for (var holder : RusticFarmBlocks.herbs()) {
            HerbBlock herb = holder.get();
            String id = holder.getId().getPath();
            if (CROP_HERB_IDS.contains(id)) {
                assertFalse(herb.asItem() instanceof BlockItem, id + " crop-herb must not have a block item");
                Item produce = herb.getHerb();
                assertTrue(produce instanceof ItemHerbEdible, id + " produce should be an ItemHerbEdible");
                assertEquals(id, BuiltInRegistries.ITEM.getKey(produce).getPath(), id + " edible shares the block id");
                ItemStack picked = herb.getCloneItemStack(null, BlockPos.ZERO, herb.defaultBlockState());
                assertSame(produce, picked.getItem(), id + " crop-herb picks as its edible");
            } else {
                assertTrue(herb.asItem() instanceof BlockItem, id + " self-herb must have a block item");
                assertSame(herb.asItem(), herb.getHerb(), id + " self-herb yields its own block item");
                assertEquals(id, BuiltInRegistries.ITEM.getKey(herb.asItem()).getPath(), id + " block item shares the id");
            }
        }
    }

    /** Only the mooncap glowed in 1.12.2 (light 8); the other twelve are dark like ordinary plants. */
    @Test
    public void onlyTheMooncapEmitsLight() {
        assertEquals(8, RusticFarmBlocks.MOONCAP_MUSHROOM.get().defaultBlockState().getLightEmission(),
                "mooncap glowed at the legacy light level 8");
        for (var holder : RusticFarmBlocks.herbs()) {
            if (holder == RusticFarmBlocks.MOONCAP_MUSHROOM) {
                continue;
            }
            assertEquals(0, holder.get().defaultBlockState().getLightEmission(),
                    holder.getId() + " must not emit light");
        }
    }

    /**
     * The four crop-herb edibles carry the legacy ItemFoodBase numbers. Only cloudsbluff had a potion effect,
     * and 1.21 folds it into FoodProperties as the vanilla levitation for 400 ticks (four seconds).
     */
    @Test
    public void theHerbEdiblesCarryTheLegacyFoodValuesAndCloudsbluffTheLevitation() {
        assertPlainFood(RusticItems.CORE_ROOT.get(), 2, 0.3F);
        assertPlainFood(RusticItems.GINSENG.get(), 2, 0.3F);
        assertPlainFood(RusticItems.MARSH_MALLOW.get(), 3, 0.3F);

        Item cloudsbluff = RusticItems.CLOUDSBLUFF.get();
        FoodProperties food = cloudsbluff.getFoodProperties(new ItemStack(cloudsbluff), null);
        assertNotNull(food, "cloudsbluff is not a food");
        assertEquals(2, food.nutrition(), "cloudsbluff nutrition (legacy: 2)");
        assertEquals(FoodConstants.saturationByModifier(2, 0.2F), food.saturation(), 0.0001F, "cloudsbluff saturation");
        assertTrue(food.canAlwaysEat(), "cloudsbluff was alwaysEdible in 1.12.2");
        assertEquals(1, food.effects().size(), "cloudsbluff carried exactly one effect");
        MobEffectInstance effect = food.effects().get(0).effect();
        assertSame(MobEffects.LEVITATION.value(), effect.getEffect().value(),
                "the cloudsbluff effect is the vanilla levitation, not a Rustic potion effect (those are Phase 6)");
        assertEquals(400, effect.getDuration(), "four seconds of levitation");
    }

    /**
     * Each herb's datagen: a two-stage blockstate (a seedling cross for ages 0..2, a mature cross for age 3,
     * the mushrooms reading a {@code _spore} seedling), a cutout {@code block/cross} mature model, and a flat
     * {@code item/generated} sprite - eleven from {@code item/herbs}, the two mushrooms reusing {@code block/herbs}.
     */
    @Test
    public void datagenProducedTwoStageHerbStatesAndFlatItemSprites() {
        for (String id : List.of("aloe_vera", "blood_orchid", "chamomile", "cloudsbluff", "cohosh", "core_root",
                "deathstalk_mushroom", "ginseng", "horsetail", "marsh_mallow", "mooncap_mushroom", "vanta_lily",
                "wind_thistle")) {
            boolean mushroom = id.equals("deathstalk_mushroom") || id.equals("mooncap_mushroom");
            String seedling = mushroom ? "_spore" : "_seed";

            JsonObject variants = json("/assets/rustic/blockstates/" + id + ".json").getAsJsonObject("variants");
            for (String age : List.of("age=0", "age=1", "age=2")) {
                assertEquals("rustic:block/" + id + seedling,
                        variants.getAsJsonObject(age).get("model").getAsString(), id + " seedling stage " + age);
            }
            assertEquals("rustic:block/" + id,
                    variants.getAsJsonObject("age=3").get("model").getAsString(), id + " mature stage");

            JsonObject mature = json("/assets/rustic/models/block/" + id + ".json");
            assertEquals("minecraft:block/cross", mature.get("parent").getAsString(), id + " mature is a cross");
            assertEquals("rustic:block/herbs/" + id,
                    mature.getAsJsonObject("textures").get("cross").getAsString(), id + " mature texture");

            JsonObject itemModel = json("/assets/rustic/models/item/" + id + ".json");
            assertEquals("minecraft:item/generated", itemModel.get("parent").getAsString(), id + " item is a flat sprite");
            String expectedTexture = mushroom ? "rustic:block/herbs/" + id : "rustic:item/herbs/" + id;
            assertEquals(expectedTexture, itemModel.getAsJsonObject("textures").get("layer0").getAsString(),
                    id + " item sprite location");
        }
    }

    /** Each herb's loot drops its own herb (block item or edible) plus a maturity bonus gated on age 3. */
    @Test
    public void datagenProducedHerbLootThatDropsTheHerbWithARipeBonus() {
        for (String id : List.of("aloe_vera", "blood_orchid", "chamomile", "cloudsbluff", "cohosh", "core_root",
                "deathstalk_mushroom", "ginseng", "horsetail", "marsh_mallow", "mooncap_mushroom", "vanta_lily",
                "wind_thistle")) {
            String resource = "/data/rustic/loot_table/blocks/" + id + ".json";
            JsonObject loot = json(resource);
            assertEquals("rustic:" + id, lootFirstEntry(resource), id + " base pool drops the herb itself");
            JsonArray pools = loot.getAsJsonArray("pools");
            assertEquals(2, pools.size(), id + " should have a base pool and a ripe-bonus pool");
            JsonObject bonus = pools.get(1).getAsJsonObject();
            assertEquals("rustic:" + id, bonus.getAsJsonArray("entries").get(0).getAsJsonObject()
                    .get("name").getAsString(), id + " bonus pool");
            JsonObject condition = bonus.getAsJsonArray("conditions").get(0).getAsJsonObject();
            assertEquals("rustic:" + id, condition.get("block").getAsString(), id + " bonus condition names the herb");
            assertEquals("3", condition.getAsJsonObject("properties").get("age").getAsString(),
                    id + " bonus only at full age");
        }
    }

    // ------------------------------------------------------------------ datagen output

    /**
     * The datagen that makes the two poles render and drop themselves. Each pole is a plain variant-less
     * cube (the four-way knot is Phase 9 art), a block item model parenting that cube, a drop-myself loot
     * table and the axe-mineable tag entry the legacy {@code Material.WOOD} implied.
     */
    @Test
    public void datagenProducedThePoleStatesModelsLootAndTags() {
        for (String pole : List.of("crop_stake", "stake_tied")) {
            JsonObject state = json("/assets/rustic/blockstates/" + pole + ".json");
            assertEquals("rustic:block/" + pole, state.getAsJsonObject("variants").getAsJsonObject("")
                    .get("model").getAsString(), pole + " blockstate");

            JsonObject itemModel = json("/assets/rustic/models/item/" + pole + ".json");
            assertEquals("rustic:block/" + pole, itemModel.get("parent").getAsString(),
                    pole + " item model parents the block model");
        }

        // crop_stake drops itself; stake_tied returns the rope that turned the plain stake into a knot.
        assertEquals("rustic:crop_stake", lootFirstEntry("/data/rustic/loot_table/blocks/crop_stake.json"));
        assertEquals("rustic:rope", lootFirstEntry("/data/rustic/loot_table/blocks/stake_tied.json"));

        List<String> axe = tagValues("mineable/axe");
        assertTrue(axe.contains("rustic:crop_stake"), "Material.WOOD means the stake is axe-mined");
        assertTrue(axe.contains("rustic:stake_tied"), "Material.WOOD means the knot is axe-mined");
    }

    /**
     * Each vine's datagen: a blockstate that maps every age to the one cutout placeholder (the per-age art is
     * Phase 9), a cutout block model, and the two-pool loot table - always one-to-two seeds, plus the produce
     * gated on {@code age=3}.
     */
    @Test
    public void datagenProducedTheCropStatesAndSeedPlusRipeProduceLoot() {
        assertCropState("tomato_crop", "rustic:block/tomato_crop");
        assertCropState("chili_crop", "rustic:block/chili_crop");

        assertCropLoot("/data/rustic/loot_table/blocks/tomato_crop.json", "tomato_crop",
                "rustic:tomato_seeds", "rustic:tomato");
        assertCropLoot("/data/rustic/loot_table/blocks/chili_crop.json", "chili_crop",
                "rustic:chili_pepper_seeds", "rustic:chili_pepper");
    }

    private static void assertCropState(String id, String model) {
        JsonObject variants = json("/assets/rustic/blockstates/" + id + ".json").getAsJsonObject("variants");
        for (String age : List.of("age=0", "age=1", "age=2", "age=3")) {
            assertTrue(variants.has(age), id + " lost the " + age + " variant");
            assertEquals(model, variants.getAsJsonObject(age).get("model").getAsString(),
                    id + " " + age + " should draw the one placeholder cross while the per-age art is deferred");
        }
    }

    private static void assertCropLoot(String resource, String cropId, String seed, String produce) {
        JsonObject loot = json(resource);
        assertEquals("minecraft:block", loot.get("type").getAsString());
        JsonArray pools = loot.getAsJsonArray("pools");
        assertEquals(2, pools.size(), resource + " should have a seed pool and a ripe-produce pool");

        // Pool 0: the seed, 1..2 of it, unconditional (a broken young vine still returns its seeds).
        JsonObject seedPool = pools.get(0).getAsJsonObject();
        JsonObject seedEntry = seedPool.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(seed, seedEntry.get("name").getAsString(), resource + " seed pool");
        assertEquals("minecraft:uniform",
                seedEntry.getAsJsonArray("functions").get(0).getAsJsonObject()
                        .getAsJsonObject("count").get("type").getAsString(),
                resource + " seed count should be a uniform roll");

        // Pool 1: the produce, gated on the named crop block being fully ripe (age=3).
        JsonObject producePool = pools.get(1).getAsJsonObject();
        assertEquals(produce, producePool.getAsJsonArray("entries").get(0).getAsJsonObject()
                .get("name").getAsString(), resource + " produce pool");
        JsonObject condition = producePool.getAsJsonArray("conditions").get(0).getAsJsonObject();
        assertEquals("minecraft:block_state_property", condition.get("condition").getAsString());
        assertEquals("rustic:" + cropId, condition.get("block").getAsString(),
                resource + " the ripe condition should name the crop block");
        assertEquals("3", condition.getAsJsonObject("properties").get("age").getAsString(),
                resource + " only drops its produce when fully ripe");
    }

    /** The four standalone (non-block) items each get an {@code item/generated} model on their own texture. */
    @Test
    public void datagenProducedAModelForEveryStandaloneItem() {
        for (String id : List.of("tomato", "chili_pepper", "ghost_pepper", "tomato_seeds", "chili_pepper_seeds",
                "grapes", "wildberries")) {
            JsonObject model = json("/assets/rustic/models/item/" + id + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), id + " item model");
            assertEquals("rustic:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString(),
                    id + " must texture from rustic:item/, not the legacy rustic:items/ folder");
        }
    }

    /** The vines are tool-less (legacy FOLIAGE mined by hand), so they must not appear in any mineable tag. */
    @Test
    public void theCropsAreInNoMineableTag() {
        for (String tool : List.of("axe", "pickaxe", "shovel")) {
            List<String> values = tagValues("mineable/" + tool);
            assertFalse(values.contains("rustic:tomato_crop"), "tomato_crop is in mineable/" + tool);
            assertFalse(values.contains("rustic:chili_crop"), "chili_crop is in mineable/" + tool);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String lootFirstEntry(String resource) {
        JsonObject loot = json(resource);
        return loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
    }

    private static List<String> tagValues(String name) {
        JsonObject json = json("/data/minecraft/tags/block/" + name + ".json");
        List<String> ids = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("values")) {
            ids.add(element.isJsonObject() ? element.getAsJsonObject().get("id").getAsString() : element.getAsString());
        }
        return ids;
    }

    private static JsonObject json(String classpathResource) {
        try (Reader reader = new InputStreamReader(resourceStream(classpathResource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static java.io.InputStream resourceStream(String resource) {
        java.io.InputStream in = RusticFarmRegistrationTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource
                + " - if this block is new, re-run runData rather than writing the file by hand");
        return in;
    }
}
