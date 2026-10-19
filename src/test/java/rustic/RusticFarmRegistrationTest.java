package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import rustic.content.RusticCreativeTabs;
import rustic.content.blocks.AppleSeedsBlock;
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

public class RusticFarmRegistrationTest {

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

    @Test
    public void thePlaceableFarmBlocksHaveBlockItemsUnderTheirOwnIds() {
        for (var holder : RusticFarmBlocks.blockItemEntries()) {
            Block block = holder.get();
            Item item = block.asItem();
            assertTrue(item instanceof BlockItem, holder.getId() + " has no block item");
            assertEquals(holder.getId().getPath(), BuiltInRegistries.ITEM.getKey(item).getPath(),
                    "the block item drifted from the block id");
            assertSame(block, ((BlockItem) item).getBlock());
        }
    }

    @Test
    public void stakeTiedStoresFourConnectionFlagsAllOffByDefault() {
        BlockState state = RusticFarmBlocks.STAKE_TIED.get().defaultBlockState();
        for (var property : List.of(StakeTiedBlock.NORTH, StakeTiedBlock.WEST,
                StakeTiedBlock.SOUTH, StakeTiedBlock.EAST)) {
            assertTrue(state.hasProperty(property), "the tied stake lost its " + property.getName() + " flag");
            assertFalse(state.getValue(property), "a lone knot connects to nothing: " + property.getName());
        }
    }

    @Test
    public void stakeTiedPicksAsRope() {
        ItemStack picked = RusticFarmBlocks.STAKE_TIED.get()
                .getCloneItemStack(null, BlockPos.ZERO, RusticFarmBlocks.STAKE_TIED.get().defaultBlockState());
        assertEquals("rope", BuiltInRegistries.ITEM.getKey(picked.getItem()).getPath(),
                "the tied stake should pick as the rope that made it");
    }

    @Test
    public void stakeTiedHasNoBlockItem() {
        Block tied = RusticFarmBlocks.STAKE_TIED.get();
        assertFalse(tied.asItem() instanceof BlockItem,
                "rustic:stake_tied must have no block item, matching legacy's block-only registration");
    }

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

    @Test
    public void theTomatoClimbsTallerThanTheChili() {
        assertEquals(3, RusticFarmBlocks.TOMATO_CROP.get().getMaxHeight(), "1.12.2: tomato maxHeight 3");
        assertEquals(2, RusticFarmBlocks.CHILI_CROP.get().getMaxHeight(), "1.12.2: chili maxHeight 2");
    }

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

    @Test
    public void everyLegacyFarmBlockIsRegistered() {
        List<String> ids = new ArrayList<>();
        RusticFarmBlocks.entries().forEach(holder -> ids.add(holder.getId().getPath()));
        assertEquals(List.of(
                "aloe_vera", "apple_seeds", "beehive", "blood_orchid", "chamomile", "chili_crop", "cloudsbluff",
                "cohosh", "core_root", "crop_stake", "deathstalk_mushroom", "ginseng", "grape_leaves",
                "grape_stem", "horsetail", "leaves_apple", "marsh_mallow", "mooncap_mushroom", "sapling_apple",
                "stake_tied", "tomato_crop", "vanta_lily", "wildberry_bush", "wind_thistle"),
                ids.stream().sorted().toList(),
                "the full farm register (Phase 5 crops plus the Phase 8 beehive/apple blocks and the Phase 14 apple crop)");
    }

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

    @Test
    public void theProduceArePlainFoodsWithTheLegacyHungerNumbers() {
        assertPlainFood(RusticItems.TOMATO.get(), 4, 0.4F);
        assertPlainFood(RusticItems.CHILI_PEPPER.get(), 3, 0.4F);
        assertPlainFood(RusticItems.GHOST_PEPPER.get(), 4, 0.7F);

        assertPlainFood(RusticItems.GRAPES.get(), 3, 0.3F);

        assertPlainFood(RusticItems.WILDBERRIES.get(), 2, 0.5F);
    }

    private static void assertPlainFood(Item item, int nutrition, float saturationModifier) {
        FoodProperties food = item.getFoodProperties(new ItemStack(item), null);
        assertNotNull(food, BuiltInRegistries.ITEM.getKey(item) + " is not a food");
        assertEquals(nutrition, food.nutrition(), "nutrition");

        assertEquals(FoodConstants.saturationByModifier(nutrition, saturationModifier), food.saturation(),
                0.0001F, "saturation, the legacy modifier folded through 1.21's constant");
        assertTrue(food.effects().isEmpty(), "these three had no setPotionEffect in 1.12.2; effects are Phase 6");
    }

    @Test
    public void theFarmingTabOffersThePolesSeedsAndProduce() {
        CreativeModeTab farming = RusticCreativeTabs.FARMING.get();
        farming.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> shown = new ArrayList<>();
        for (ItemStack stack : farming.getDisplayItems()) {
            shown.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        }
        for (String id : List.of("crop_stake", "tomato_seeds", "chili_pepper_seeds",
                "tomato", "chili_pepper", "ghost_pepper", "grape_stem", "grapes", "wildberry_bush", "wildberries",
                "apple_seeds")) {
            assertTrue(shown.contains(id), "rustic:" + id + " is missing from the FARMING tab: " + shown);
        }

        assertFalse(shown.contains("stake_tied"), "stake_tied is block-only in legacy and must not be offered");
        assertFalse(shown.contains("tomato_crop"), "a vine leaked a block item into the FARMING tab");
        assertFalse(shown.contains("chili_crop"), "a vine leaked a block item into the FARMING tab");
        assertFalse(shown.contains("grape_leaves"), "a grape leaf leaked a block item into the FARMING tab");
    }

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

        assertTrue(stem.asItem() instanceof BlockItem, "grape_stem is placeable and must have a block item");
    }

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

    @Test
    public void datagenProducedTheGrapeStatesModelsAndFruitGatedLoot() {
        JsonObject stemVariants = json("/assets/rustic/blockstates/grape_stem.json").getAsJsonObject("variants");
        for (int age = 0; age <= 3; age++) {
            assertEquals("rustic:block/grape_stem_" + age,
                    stemVariants.getAsJsonObject("age=" + age).get("model").getAsString(),
                    "grape_stem age " + age + " draws its restored model");
        }

        JsonObject stemItem = json("/assets/rustic/models/item/grape_stem.json");
        assertEquals("minecraft:item/generated", stemItem.get("parent").getAsString(),
                "the stem's block item is the flat Grape Seeds sprite");
        assertEquals("rustic:item/grape_seeds", stemItem.getAsJsonObject("textures").get("layer0").getAsString(),
                "the Grape Seeds sprite reads rustic:item/grape_seeds");

        JsonArray leavesParts = json("/assets/rustic/blockstates/grape_leaves.json").getAsJsonArray("multipart");
        assertMultiPart(leavesParts, "rustic:block/grape_leaves_0", null, "distance", "0");
        assertMultiPart(leavesParts, "rustic:block/grape_stem_top", null, "distance", "0");
        assertMultiPart(leavesParts, "rustic:block/grape_leaves_1", 90, "axis", "x", "distance", "1");
        assertMultiPart(leavesParts, "rustic:block/grape_leaves_1", null, "axis", "z", "distance", "1");
        assertMultiPart(leavesParts, "rustic:block/grape_vine", 90, "axis", "x");
        assertMultiPart(leavesParts, "rustic:block/grape_vine", null, "axis", "z");
        assertMultiPart(leavesParts, "rustic:block/rope_horizontal", 90, "axis", "x");
        assertMultiPart(leavesParts, "rustic:block/rope_horizontal", null, "axis", "z");
        assertMultiPart(leavesParts, "rustic:block/grapes", null, "grapes", "true");

        assertNull(RusticFarmRegistrationTest.class.getResource("/assets/rustic/models/block/grape_leaves.json"),
                "the stale grape_leaves placeholder model outlived the multipart restore");

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

        assertTrue(bush.asItem() instanceof BlockItem, "wildberry_bush is placeable and must have a block item");
    }

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

    @Test
    public void theAppleSeedsCropIsABonemealableBushCarryingItsOwnSeedItem() {
        AppleSeedsBlock crop = RusticFarmBlocks.APPLE_SEEDS_CROP.get();
        assertSame(AppleSeedsBlock.class, crop.getClass(), "rustic:apple_seeds is not an AppleSeedsBlock");
        assertTrue(crop instanceof BushBlock, "apple_seeds must be a BushBlock");
        assertTrue(crop instanceof BonemealableBlock, "apple_seeds must be bonemealable");

        assertEquals(0.0F, crop.defaultDestroyTime(), 0.0001F, "apple_seeds hardness (legacy Material.PLANTS: 0)");
        assertSame(SoundType.CROP, crop.getSoundType(crop.defaultBlockState(), null, null, null),
                "apple_seeds uses the plant-family CROP sound");

        assertSame(BlockStateProperties.AGE_1, crop.getStateDefinition().getProperty("age"),
                "apple_seeds uses vanilla's AGE_1, the legacy PropertyInteger 0..1");
        assertEquals(List.of(0, 1), List.copyOf(((IntegerProperty) AppleSeedsBlock.AGE).getPossibleValues()),
                "apple_seeds lost an age step");
        assertEquals(0, crop.getAge(crop.defaultBlockState()), "a just-planted seed starts at age 0");
        assertTrue(crop.isMaxAge(crop.getStateForAge(1)), "apple_seeds age 1 is the top");
        assertFalse(crop.isMaxAge(crop.getStateForAge(0)), "apple_seeds age 0 is not the top");

        Item seedItem = crop.asItem();
        assertTrue(seedItem instanceof BlockItem, "apple_seeds must have a plantable block item");
        assertSame(RusticItems.APPLE_SEEDS.get(), seedItem, "the RusticItems seed is the crop's block item");
        assertSame(crop, ((BlockItem) seedItem).getBlock(), "the seed item places the crop");
    }

    @Test
    public void datagenProducedTheAppleSeedsStatesModelsAndSeedLoot() {
        JsonObject variants = json("/assets/rustic/blockstates/apple_seeds.json").getAsJsonObject("variants");
        assertEquals("rustic:block/apple_seeds_0",
                variants.getAsJsonObject("age=0").get("model").getAsString(), "apple_seeds age 0 cross");
        assertEquals("rustic:block/apple_seeds_1",
                variants.getAsJsonObject("age=1").get("model").getAsString(), "apple_seeds age 1 cross");

        for (int age = 0; age <= 1; age++) {
            JsonObject model = json("/assets/rustic/models/block/apple_seeds_" + age + ".json");
            assertEquals("minecraft:block/cross", model.get("parent").getAsString(),
                    "apple_seeds_" + age + " is a cross model");
            assertEquals("rustic:block/apple_seeds_" + age,
                    model.getAsJsonObject("textures").get("cross").getAsString(),
                    "apple_seeds_" + age + " draws its restored texture");
        }

        JsonObject seedModel = json("/assets/rustic/models/item/apple_seeds.json");
        assertEquals("minecraft:item/generated", seedModel.get("parent").getAsString(),
                "the seed item is a flat sprite, not the world-facing cross");
        assertEquals("rustic:item/apple_seeds",
                seedModel.getAsJsonObject("textures").get("layer0").getAsString(),
                "the seed sprite reads rustic:item/, not the legacy rustic:items/ folder");

        assertEquals("rustic:apple_seeds", lootFirstEntry("/data/rustic/loot_table/blocks/apple_seeds.json"),
                "a broken crop loots the seed itself");
    }

    @Test
    public void noPlantStateIsSolidOrBlocksMovement() {
        for (var holder : RusticFarmBlocks.entries()) {
            Block block = holder.get();
            boolean bushPlant = block instanceof BushBlock;
            boolean poleCollision = block instanceof StakeCropBlock
                    || block instanceof CropStakeBlock || block instanceof StakeTiedBlock;
            if (!bushPlant && !poleCollision) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                String what = holder.getId() + " " + state;
                assertFalse(state.isSolid(),
                        what + " would revert the farmland under it to dirt (FarmBlock.canSurvive)");
                if (bushPlant) {
                    assertSame(PushReaction.DESTROY, state.getPistonPushReaction(),
                            what + " must break under a piston, like every vanilla plant");
                }
                if (poleCollision) {
                    assertFalse(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty(),
                            what + " must keep the stake pole's thin collision box, as legacy did");
                } else {
                    assertTrue(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty(),
                            what + " must be walk-through, as Material.PLANTS/FOLIAGE were");
                }
            }
        }
    }

    private static final List<String> CROP_HERB_IDS = List.of("cloudsbluff", "core_root", "ginseng", "marsh_mallow");

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

    @Test
    public void datagenProducedThePoleStatesModelsLootAndTags() {

        assertEquals("rustic:block/crop_stake",
                json("/assets/rustic/blockstates/crop_stake.json").getAsJsonObject("variants")
                        .getAsJsonObject("").get("model").getAsString(), "crop_stake blockstate");

        JsonArray tiedParts = json("/assets/rustic/blockstates/stake_tied.json").getAsJsonArray("multipart");
        assertMultiPart(tiedParts, "rustic:block/stake_tied", null);
        assertMultiPart(tiedParts, "rustic:block/rope_attachment_n", null, "north", "true");
        assertMultiPart(tiedParts, "rustic:block/rope_attachment_n", 90, "east", "true");
        assertMultiPart(tiedParts, "rustic:block/rope_attachment_n", 180, "south", "true");
        assertMultiPart(tiedParts, "rustic:block/rope_attachment_n", 270, "west", "true");
        assertEquals(5, tiedParts.size(), "stake_tied multipart grew beyond the knot plus four attachments");

        JsonObject stakeItemModel = json("/assets/rustic/models/item/crop_stake.json");
        assertEquals("rustic:block/crop_stake", stakeItemModel.get("parent").getAsString(),
                "crop_stake item model parents the block model");
        assertNull(RusticFarmRegistrationTest.class.getResource("/assets/rustic/models/item/stake_tied.json"),
                "stake_tied must have no item model - it is a block-only, item-less legacy block");

        assertEquals("rustic:crop_stake", lootFirstEntry("/data/rustic/loot_table/blocks/crop_stake.json"));
        assertEquals("rustic:rope", lootFirstEntry("/data/rustic/loot_table/blocks/stake_tied.json"));

        List<String> axe = tagValues("mineable/axe");
        assertTrue(axe.contains("rustic:crop_stake"), "Material.WOOD means the stake is axe-mined");
        assertTrue(axe.contains("rustic:stake_tied"), "Material.WOOD means the knot is axe-mined");
    }

    @Test
    public void datagenProducedTheCropStatesAndSeedPlusRipeProduceLoot() {
        assertCropState("tomato_crop");
        assertCropState("chili_crop");

        assertCropLoot("/data/rustic/loot_table/blocks/tomato_crop.json", "tomato_crop",
                "rustic:tomato_seeds", "rustic:tomato");
        assertCropLoot("/data/rustic/loot_table/blocks/chili_crop.json", "chili_crop",
                "rustic:chili_pepper_seeds", "rustic:chili_pepper");
    }

    private static void assertCropState(String id) {
        JsonObject variants = json("/assets/rustic/blockstates/" + id + ".json").getAsJsonObject("variants");
        for (int age = 0; age <= 3; age++) {
            String key = "age=" + age;
            assertTrue(variants.has(key), id + " lost the " + key + " variant");
            assertEquals("rustic:block/" + id + "_" + age,
                    variants.getAsJsonObject(key).get("model").getAsString(),
                    id + " " + key + " should draw its restored per-age model");
        }
    }

    private static void assertCropLoot(String resource, String cropId, String seed, String produce) {
        JsonObject loot = json(resource);
        assertEquals("minecraft:block", loot.get("type").getAsString());
        JsonArray pools = loot.getAsJsonArray("pools");
        assertEquals(2, pools.size(), resource + " should have a seed pool and a ripe-produce pool");

        JsonObject seedPool = pools.get(0).getAsJsonObject();
        JsonObject seedEntry = seedPool.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(seed, seedEntry.get("name").getAsString(), resource + " seed pool");
        assertEquals("minecraft:uniform",
                seedEntry.getAsJsonArray("functions").get(0).getAsJsonObject()
                        .getAsJsonObject("count").get("type").getAsString(),
                resource + " seed count should be a uniform roll");

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

    @Test
    public void theCropsAreInNoMineableTag() {
        for (String tool : List.of("axe", "pickaxe", "shovel")) {
            List<String> values = tagValues("mineable/" + tool);
            assertFalse(values.contains("rustic:tomato_crop"), "tomato_crop is in mineable/" + tool);
            assertFalse(values.contains("rustic:chili_crop"), "chili_crop is in mineable/" + tool);
        }
    }

    private static String lootFirstEntry(String resource) {
        JsonObject loot = json(resource);
        return loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
    }

    private static void assertMultiPart(JsonArray parts, String model, Integer y, String... propPairs) {
        for (JsonElement element : parts) {
            JsonObject part = element.getAsJsonObject();
            JsonObject apply = part.getAsJsonObject("apply");
            if (apply == null || !model.equals(apply.get("model").getAsString())) {
                continue;
            }
            int actualY = apply.has("y") ? apply.get("y").getAsInt() : 0;
            if (actualY != (y == null ? 0 : y)) {
                continue;
            }
            JsonObject when = part.getAsJsonObject("when");
            boolean matched;
            if (propPairs.length == 0) {

                matched = true;
            } else {
                matched = when != null;
                for (int i = 0; i < propPairs.length && matched; i += 2) {
                    JsonElement value = when.get(propPairs[i]);
                    matched = value != null && propPairs[i + 1].equals(value.getAsString());
                }
            }
            if (matched) {
                return;
            }
        }
        fail("multipart part not found: " + model + (y != null ? " y=" + y : "") + " " + java.util.Arrays.toString(propPairs));
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
