package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;

import net.minecraft.core.NonNullList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.content.crafting.RecipeNonIngredientReturn;
import rustic.content.crafting.RecipeOliveOil;
import rustic.content.crafting.RecipeVantaOil;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.RusticItems;
import rustic.content.potions.ElixirUtils;
import rustic.registry.RusticRegistries;

public class RusticMachineRecipeSerializerTest {

    @Test
    public void crushingTubSerializerRoundTripsThroughItsCodec() {
        CrushingTubRecipe sample = new CrushingTubRecipe(
                Ingredient.of(Items.WHEAT),
                new FluidStack(Fluids.WATER, 250),
                new ItemStack(Items.WHEAT_SEEDS));

        CrushingTubRecipe decoded = roundTrip(RusticRegistries.CRUSHING_TUB_SERIALIZER.get().codec(), sample);

        FluidStack fluid = decoded.getFluid();
        assertSame(Fluids.WATER, fluid.getFluid(), "the output fluid type survived the round-trip");
        assertEquals(250, fluid.getAmount(), "the output fluid amount survived the round-trip");
        assertTrue(decoded.getByproduct().is(Items.WHEAT_SEEDS), "the byproduct item survived");
        assertTrue(decoded.getInput().test(new ItemStack(Items.WHEAT)), "wheat still matches the decoded ingredient");
        assertFalse(decoded.getInput().test(new ItemStack(Items.CARROT)), "a non-matching item still fails");

        assertWired(sample, RusticRegistries.CRUSHING_TUB_TYPE.get(), RusticRegistries.CRUSHING_TUB_SERIALIZER.get());
    }

    @Test
    public void evaporatingBasinSerializerRoundTripsThroughItsCodec() {
        EvaporatingBasinRecipe sample = new EvaporatingBasinRecipe(
                new FluidStack(Fluids.WATER, 500),
                new ItemStack(Items.SUGAR));

        EvaporatingBasinRecipe decoded = roundTrip(RusticRegistries.EVAPORATING_BASIN_SERIALIZER.get().codec(), sample);

        assertSame(Fluids.WATER, decoded.getInput().getFluid(), "the input fluid type survived");
        assertEquals(500, decoded.getInput().getAmount(), "the input fluid amount survived");
        assertEquals(500, decoded.getTime(), "the countdown is the input amount, as legacy getTime() did");
        assertTrue(decoded.getOutput().is(Items.SUGAR), "the dust output survived");

        assertWired(sample, RusticRegistries.EVAPORATING_BASIN_TYPE.get(), RusticRegistries.EVAPORATING_BASIN_SERIALIZER.get());
    }

    @Test
    public void brewingBarrelSerializerRoundTripsThroughItsCodec() {
        BrewingBarrelRecipe sample = new BrewingBarrelRecipe(
                new FluidStack(RusticFluids.GRAPE_JUICE.value(), 1000),
                new FluidStack(RusticFluids.WINE.value(), 1000));

        BrewingBarrelRecipe decoded = roundTrip(RusticRegistries.BREWING_BARREL_SERIALIZER.get().codec(), sample);

        assertSame(RusticFluids.GRAPE_JUICE.value(), decoded.getInput().getFluid(), "the ferment input survived");
        assertEquals(1000, decoded.getInput().getAmount(), "the input amount survived");
        assertSame(RusticFluids.WINE.value(), decoded.getOutputFluid().getFluid(), "the booze output survived");

        assertWired(sample, RusticRegistries.BREWING_BARREL_TYPE.get(), RusticRegistries.BREWING_BARREL_SERIALIZER.get());
    }

    @Test
    public void condenserSerializerRoundTripsThroughItsCodec() {
        CondenserRecipe sample = new CondenserRecipe(
                true,
                Ingredient.of(Items.SUGAR),
                java.util.List.of(Ingredient.of(Items.WHEAT), Ingredient.of(Items.CARROT)),
                new FluidStack(Fluids.WATER, 125),
                new ItemStack(Items.GLASS_BOTTLE),
                new ItemStack(Items.GLOWSTONE_DUST),
                200);

        CondenserRecipe decoded = roundTrip(RusticRegistries.CONDENSER_SERIALIZER.get().codec(), sample);

        assertTrue(decoded.isAdvanced(), "the advanced flag survived");
        assertEquals(200, decoded.getTime(), "the brew time survived");
        assertSame(Fluids.WATER, decoded.getFluid().getFluid(), "the water fluid survived");
        assertEquals(125, decoded.getFluid().getAmount(), "the required water amount survived");
        assertTrue(decoded.getBottle().is(Items.GLASS_BOTTLE), "the consumed bottle survived");
        assertTrue(decoded.getResultItem(null).is(Items.GLOWSTONE_DUST), "the elixir result item survived");
        assertEquals(2, decoded.getIngredients().size(), "both required ingredients survived");
        assertTrue(decoded.getModifier().test(new ItemStack(Items.SUGAR)), "the modifier ingredient gate survived");

        assertWired(sample, RusticRegistries.CONDENSER_TYPE.get(), RusticRegistries.CONDENSER_SERIALIZER.get());
    }

    @Test
    public void generatedMachineRecipesDecodeToTheirRegisteredType() {
        CrushingTubRecipe olive = decodeGenerated(RusticRegistries.CRUSHING_TUB_SERIALIZER.get().codec(),
                RusticRegistries.CRUSHING_TUB_TYPE.get(), "crushing_tub/olive_oil.json");
        assertTrue(olive.getInput().test(new ItemStack(RusticItems.OLIVES.get())), "olive input survived");
        assertSame(RusticFluids.OLIVE_OIL.value(), olive.getFluid().getFluid(), "olive fluid survived");
        assertEquals(250, olive.getFluid().getAmount(), "olive fluid amount survived");

        EvaporatingBasinRecipe iron = decodeGenerated(RusticRegistries.EVAPORATING_BASIN_SERIALIZER.get().codec(),
                RusticRegistries.EVAPORATING_BASIN_TYPE.get(), "evaporating_basin/iron_dust_tiny.json");
        assertSame(RusticFluids.IRONBERRY_JUICE.value(), iron.getInput().getFluid(), "basin input fluid survived");
        assertEquals(500, iron.getInput().getAmount(), "basin input amount (drives the countdown) survived");
        assertTrue(iron.getOutput().is(RusticItems.IRON_DUST_TINY.get()), "tiny iron dust output survived");

        BrewingBarrelRecipe wine = decodeGenerated(RusticRegistries.BREWING_BARREL_SERIALIZER.get().codec(),
                RusticRegistries.BREWING_BARREL_TYPE.get(), "brewing_barrel/wine.json");
        assertSame(RusticFluids.GRAPE_JUICE.value(), wine.getInput().getFluid(), "brewing input fluid survived");
        assertSame(RusticFluids.WINE.value(), wine.getOutputFluid().getFluid(), "brewing booze output survived");

        CondenserRecipe regen = decodeGenerated(RusticRegistries.CONDENSER_SERIALIZER.get().codec(),
                RusticRegistries.CONDENSER_TYPE.get(), "condenser/regeneration_basic.json");
        assertFalse(regen.isAdvanced(), "the basic condenser row stays basic");
        assertEquals(400, regen.getTime(), "basic condenser time survived");
    }

    @Test
    public void condenserResultRoundTripsItsElixirEffectsComponent() {
        ItemStack result = new ItemStack(RusticItems.ELIXIR.get());
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 0), result);

        CondenserRecipe sample = new CondenserRecipe(
                false,
                Ingredient.EMPTY,
                List.of(Ingredient.of(RusticFarmBlocks.COHOSH.get()), Ingredient.of(RusticItems.HONEYCOMB.get())),
                new FluidStack(Fluids.WATER, 125),
                new ItemStack(Items.GLASS_BOTTLE),
                result,
                400);

        CondenserRecipe decoded = roundTrip(RusticRegistries.CONDENSER_SERIALIZER.get().codec(), sample);

        assertTrue(decoded.getResultItem(null).is(RusticItems.ELIXIR.get()), "the elixir result item survived");
        List<MobEffectInstance> effects = ElixirUtils.getEffects(decoded.getResultItem(null));
        assertEquals(1, effects.size(), "the elixir effect list survived the round-trip");
        assertSame(MobEffects.REGENERATION.value(), effects.get(0).getEffect().value(), "the effect identity survived");
        assertEquals(900, effects.get(0).getDuration(), "the effect duration survived");
    }

    @Test
    public void dynamicCraftingSerializersRoundTripThroughTheirCodec() {
        RecipeOliveOil olive = roundTrip(RusticRegistries.OLIVE_OIL_SERIALIZER.get().codec(), new RecipeOliveOil());
        assertWired(olive, RecipeType.CRAFTING, RusticRegistries.OLIVE_OIL_SERIALIZER.get());

        RecipeVantaOil vanta = roundTrip(RusticRegistries.VANTA_OIL_SERIALIZER.get().codec(), new RecipeVantaOil());
        assertWired(vanta, RecipeType.CRAFTING, RusticRegistries.VANTA_OIL_SERIALIZER.get());

        RecipeNonIngredientReturn sample = new RecipeNonIngredientReturn(new ItemStack(Items.WATER_BUCKET),
                List.of(Ingredient.of(Items.BREAD), Ingredient.of(Items.SUGAR), Ingredient.of(Items.WATER_BUCKET)));
        RecipeNonIngredientReturn decoded =
                roundTrip(RusticRegistries.NON_INGREDIENT_RETURN_SERIALIZER.get().codec(), sample);
        assertTrue(decoded.getResultItem(null).is(Items.WATER_BUCKET), "the non-ingredient-return result survived");
        assertEquals(3, decoded.getIngredients().size(), "all three shapeless ingredients survived");
        assertWired(decoded, RecipeType.CRAFTING,
                RusticRegistries.NON_INGREDIENT_RETURN_SERIALIZER.get());
    }

    @Test
    public void nonIngredientReturnMatchesShapelesslyAndDropsTheContainerRemainder() {
        RecipeNonIngredientReturn recipe = new RecipeNonIngredientReturn(new ItemStack(RusticItems.ALE_WORT_BUCKET.get()),
                List.of(Ingredient.of(Items.BREAD), Ingredient.of(Items.SUGAR), Ingredient.of(Items.WATER_BUCKET)));

        CraftingInput grid = craftingInput(new ItemStack(Items.BREAD), new ItemStack(Items.SUGAR),
                new ItemStack(Items.WATER_BUCKET));
        assertTrue(recipe.matches(grid, null), "the exact three-ingredient grid matches");
        assertFalse(recipe.matches(craftingInput(new ItemStack(Items.BREAD), new ItemStack(Items.SUGAR)), null),
                "a missing water bucket fails");

        assertTrue(recipe.assemble(grid, null).is(RusticItems.ALE_WORT_BUCKET.get()), "the ale wort bucket is the result");

        for (ItemStack leftover : recipe.getRemainingItems(grid)) {
            assertTrue(leftover.isEmpty(), "no container remainder is returned");
        }
    }

    @Test
    public void generatedCraftingTableRecipesDecodeToTheirRegisteredType() {
        RecipeNonIngredientReturn aleWort = decodeGenerated(RusticRegistries.NON_INGREDIENT_RETURN_SERIALIZER.get().codec(),
                RecipeType.CRAFTING, "ale_wort.json");
        assertTrue(aleWort.getResultItem(null).is(RusticItems.ALE_WORT_BUCKET.get()), "the generated ale wort result survived");
        assertEquals(3, aleWort.getIngredients().size(), "the generated ale wort carries bread + sugar + water bucket");
    }

    @Test
    public void statelessUnitCodecsShareOneInstanceSoTheRecipeSyncEncodes() {
        assertUnitSyncEncodes(RusticRegistries.OLIVE_OIL_SERIALIZER.get(), new RecipeOliveOil());
        assertUnitSyncEncodes(RusticRegistries.VANTA_OIL_SERIALIZER.get(), new RecipeVantaOil());
    }

    private static <T extends Recipe<?>> void assertUnitSyncEncodes(net.minecraft.world.item.crafting.RecipeSerializer<T> serializer, T sample) {
        T loaded = roundTrip(serializer.codec(), sample);

        serializer.streamCodec().encode(null, loaded);
        assertSame(loaded, serializer.streamCodec().decode(null),
                "the map codec's recipe and the stream codec's singleton must be the same instance");
    }

    @Test
    public void vantaOilReadsOnlyALoneEffectElixirIngredient() {
        ItemStack oneEffect = new ItemStack(RusticItems.ELIXIR.get());
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 0), oneEffect);
        MobEffectInstance effect = RecipeVantaOil.getIngredientEffect(oneEffect);
        assertTrue(effect != null, "a single-effect elixir supplies the oil ingredient");
        assertSame(MobEffects.REGENERATION.value(), effect.getEffect().value(), "the ingredient effect survived");

        ItemStack bareElixir = new ItemStack(RusticItems.ELIXIR.get());
        assertTrue(RecipeVantaOil.getIngredientEffect(bareElixir) == null, "an effectless elixir supplies nothing");
    }

    @Test
    public void vantaOilMatchingAnOrdinaryGridNeverThrows() {
        RecipeVantaOil recipe = new RecipeVantaOil();
        CraftingInput grid = craftingInput(new ItemStack(Items.APPLE), new ItemStack(Items.BOOK),
                new ItemStack(Items.IRON_NUGGET));
        assertFalse(assertDoesNotThrow(() -> recipe.matches(grid, null),
                "a non-oil grid must not throw out of matches() and abort every crafting recipe"));
    }

    @Test
    public void vantaOilWhitelistResolvesIdAndClassEntriesWithoutTags() {
        assertTrue(RecipeVantaOil.isVantaOilableWeapon(new ItemStack(Items.BONE)), "a raw-id whitelist entry matches");
        assertTrue(RecipeVantaOil.isVantaOilableWeapon(new ItemStack(Items.IRON_SWORD)), "a sword is oilable by class");
        assertFalse(RecipeVantaOil.isVantaOilableWeapon(new ItemStack(Items.APPLE)), "an unlisted food is not oilable");
        assertFalse(RecipeVantaOil.isVantaOilableWeapon(ItemStack.EMPTY), "an empty stack is not oilable");
    }

    private static CraftingInput craftingInput(ItemStack... stacks) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; i++) {
            items.set(i, stacks[i]);
        }

        return CraftingInput.of(3, 3, items);
    }

    private static <T> T roundTrip(MapCodec<T> codec, T sample) {
        JsonElement json = codec.encode(sample, JsonOps.INSTANCE, JsonOps.INSTANCE.mapBuilder())
                .build(JsonOps.INSTANCE.empty())
                .getOrThrow(error -> new AssertionError("encode failed: " + error));
        MapLike<JsonElement> map = JsonOps.INSTANCE.getMap(json)
                .getOrThrow(error -> new AssertionError("the encoded recipe is not a JSON object: " + error));
        return codec.decode(JsonOps.INSTANCE, map)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));
    }

    private static <T extends Recipe<?>> T decodeGenerated(MapCodec<T> codec, RecipeType<?> expectedType, String relativePath) {
        String path = "/data/rustic/recipe/" + relativePath;
        JsonObject obj;
        try (InputStream in = RusticMachineRecipeSerializerTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "missing generated recipe on the classpath: " + path
                    + " - re-run runData rather than writing the file by hand");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                obj = JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new AssertionError("could not read " + path, e);
        }
        obj.remove("type");
        MapLike<JsonElement> map = JsonOps.INSTANCE.getMap(obj)
                .getOrThrow(error -> new AssertionError("not a JSON object: " + path + " - " + error));
        T recipe = codec.decode(JsonOps.INSTANCE, map)
                .getOrThrow(error -> new AssertionError("failed to decode generated recipe " + path + ": " + error));
        assertSame(expectedType, recipe.getType(), relativePath + " decodes to its registered recipe type");
        return recipe;
    }

    private static void assertWired(net.minecraft.world.item.crafting.Recipe<?> recipe,
                                   net.minecraft.world.item.crafting.RecipeType<?> type,
                                   net.minecraft.world.item.crafting.RecipeSerializer<?> serializer) {
        assertSame(type, recipe.getType(), "the recipe reports the registered type");
        assertSame(serializer, recipe.getSerializer(), "the recipe reports the registered serializer");
    }
}
