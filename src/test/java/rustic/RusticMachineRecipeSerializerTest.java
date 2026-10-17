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

/**
 * Phase 4b machine serializer round-trip gate. The plan's test note is explicit that the machine-phase proxy for
 * "the serializer works" is a codec encode&#8594;decode round-trip, because the real recipe <em>data</em> (the JSON
 * files) is deferred to Phase 7. Each of the four custom {@code RecipeSerializer}s registered in {@code M0} gets a
 * sample built in code, encoded through its own {@link MapCodec}, decoded back, and the decoded recipe is checked
 * field-by-field against the sample (a {@code Recipe} has no {@code equals}, so the accessors are compared).
 *
 * <p>Runs in ModDevGradle's FML unit-test JVM, so {@code BuiltInRegistries} and the deferred holders resolve. The
 * codecs ({@code FluidStack.CODEC}, {@code ItemStack.STRICT_CODEC}, {@code Ingredient.CODEC_NONEMPTY}) are all backed
 * by captured built-in registries, so a plain {@link JsonOps} round-trips a vanilla-sample recipe with no server and
 * no {@code HolderLookup.Provider} - exactly how {@code RecipeManager.fromJson} parses a {@code JsonObject}.
 *
 * <p>Each case also re-asserts the recipe reports the identical registered type and serializer instance, tying the
 * codec under test back to the object the block entities query at runtime.
 */
public class RusticMachineRecipeSerializerTest {

    /** The codec round-trips an item-in / fluid-out crushing tub recipe, preserving fluid, amount and byproduct. */
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

    /** The codec round-trips a fluid-in / dust-out basin recipe, preserving the input amount that also drives the time. */
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

    /** The codec round-trips a juice-in / booze-out brewing recipe with the ported Rustic fluid instances. */
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

    /** The codec round-trips an advanced condenser recipe: the modifier gate, exact ingredients, water, bottle and time. */
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

    /**
     * Phase 7.2 gate: the machine-recipe <em>data</em> that 4b deferred now exists. Each generated JSON under
     * {@code data/rustic/recipe/<machine>/} is read back off the test classpath, stripped of its {@code type}
     * discriminator and decoded through the registered serializer's {@link MapCodec} - the same path
     * {@code RecipeManager.fromJson} takes at load time. This proves the {@code RusticRecipeProvider} emission
     * produces well-formed, correctly-typed recipes for all four machines, not just in-memory samples.
     */
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

    /**
     * Phase 7.2 condenser gate: an elixir result carrying a {@link MobEffectInstance} survives the full
     * encode&#8594;decode round-trip. This is the effects-component half of "the condenser result stack round-trips
     * its effects component" - the {@code rustic:elixir_effects} data component written by {@link ElixirUtils}
     * must be preserved by the serializer codec so the shipped recipe data reaches the block entity intact.
     */
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

    // ---------------------------------------------------------------- helpers

    /**
     * Phase 7.3 serializer gate: the three dynamic crafting-table recipes each have a registered {@code Recipe} +
     * {@code RecipeSerializer}. The two oiling recipes (olive, vanta) are stateless {@code Unit} codecs, so a
     * round-trip folds to an empty body and decodes back to the pinned singleton; the data-driven
     * non-ingredient-return recipe carries a real result + ingredient list. Each case re-asserts the decoded recipe
     * reports the vanilla {@code CRAFTING} type (so a crafting table matches it) and the identical registered
     * serializer. The cabinet is not here: it became a plain {@code minecraft:crafting_shaped} recipe, gated in
     * {@link RusticRecipeDatagenTest}.
     */
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

    /**
     * Phase 7.3 matching gate for the data-driven shapeless type (config- and tag-free, so it runs headlessly):
     * the exact bread + sugar + water-bucket grid matches, and the bucket the water bucket would normally leave
     * behind is cleared - that leftover suppression is the whole point of the non-ingredient-return recipe.
     */
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
        // The water bucket's crafting remainder (the empty bucket) is dropped, so no spurious bucket is returned.
        for (ItemStack leftover : recipe.getRemainingItems(grid)) {
            assertTrue(leftover.isEmpty(), "no container remainder is returned");
        }
    }

    /**
     * Phase 7.3 generated-data gate: the shipped crafting-table stub decodes through its registered serializer into
     * the vanilla CRAFTING type (the ale wort row; the cabinet is plain shaped JSON now, gated by
     * {@link RusticRecipeDatagenTest}).
     */
    @Test
    public void generatedCraftingTableRecipesDecodeToTheirRegisteredType() {
        RecipeNonIngredientReturn aleWort = decodeGenerated(RusticRegistries.NON_INGREDIENT_RETURN_SERIALIZER.get().codec(),
                RecipeType.CRAFTING, "ale_wort.json");
        assertTrue(aleWort.getResultItem(null).is(RusticItems.ALE_WORT_BUCKET.get()), "the generated ale wort result survived");
        assertEquals(3, aleWort.getIngredients().size(), "the generated ale wort carries bread + sugar + water bucket");
    }

    /**
     * Regression guard for the world-join crash {@code Failed to encode packet 'update_recipes'} /
     * {@code Can't encode 'RecipeOliveOil@..', expected 'RecipeOliveOil@..'}. The stateless oiling recipes use
     * {@code unit} codecs: the {@link com.mojang.serialization.MapCodec} loads the recipe the server stores, and the
     * {@link net.minecraft.network.codec.StreamCodec} writes it to the client on join. A {@code StreamCodec.unit}
     * compares by reference identity, so if the two codecs pin different instances the encode throws and the player
     * is kicked. This asserts the recipe the map codec produces is writable by the stream codec (and that both fold
     * to the one shared singleton) for olive oil and vanta oil.
     */
    @Test
    public void statelessUnitCodecsShareOneInstanceSoTheRecipeSyncEncodes() {
        assertUnitSyncEncodes(RusticRegistries.OLIVE_OIL_SERIALIZER.get(), new RecipeOliveOil());
        assertUnitSyncEncodes(RusticRegistries.VANTA_OIL_SERIALIZER.get(), new RecipeVantaOil());
    }

    /** The instance the {@code MapCodec} produces must survive a {@code StreamCodec} encode/decode by identity. */
    private static <T extends Recipe<?>> void assertUnitSyncEncodes(net.minecraft.world.item.crafting.RecipeSerializer<T> serializer, T sample) {
        T loaded = roundTrip(serializer.codec(), sample);
        // The unit stream codec writes nothing but reference-checks the value against its pinned instance; a split
        // instance (the bug) throws EncoderException here, exactly as the server did on world join.
        serializer.streamCodec().encode(null, loaded);
        assertSame(loaded, serializer.streamCodec().decode(null),
                "the map codec's recipe and the stream codec's singleton must be the same instance");
    }

    /**
     * Phase 7.3 vanta unit: an elixir holding exactly one effect supplies the oil ingredient, while an elixir with
     * none yields nothing - the pure half of {@link RecipeVantaOil#getIngredientEffect}, testable without config or
     * tags. The full grid match additionally reads {@code VANTA_OIL_WHITELIST} + potion effects and is exercised
     * in the live-game checklist rather than headlessly.
     */
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

    /**
     * Regression guard for the whole-crafting-table ghosting crash. {@link RecipeVantaOil} is a dynamic recipe, so
     * its {@code matches()} runs for every slot of every grid before any other recipe resolves; when it threw on an
     * unparseable {@code #} whitelist entry (the legacy {@code "#stickWood"} ore name, invalid as a 1.21 tag id) the
     * exception aborted {@code RecipeManager.getRecipeFor} and left the client's placed ingredients unconfirmed -
     * ghosts. The port now parses {@code #} entries with {@code ResourceLocation.tryParse} and skips nulls, exactly
     * the graceful miss {@code OreDictionary.doesOreNameExist} used to give. This asserts matching an ordinary grid
     * (a plain food item reaches the whitelist walk) never throws.
     */
    @Test
    public void vantaOilMatchingAnOrdinaryGridNeverThrows() {
        RecipeVantaOil recipe = new RecipeVantaOil();
        CraftingInput grid = craftingInput(new ItemStack(Items.APPLE), new ItemStack(Items.BOOK),
                new ItemStack(Items.IRON_NUGGET));
        assertFalse(assertDoesNotThrow(() -> recipe.matches(grid, null),
                "a non-oil grid must not throw out of matches() and abort every crafting recipe"));
    }

    /**
     * The whitelist's id and class branches resolve without touching tag data, so they are safe to assert
     * headlessly: {@code minecraft:bone} matches by raw id, swords/axes by class, and an unlisted plain item (or an
     * empty stack) matches nothing. The {@code #c:rods/wooden} branch additionally needs loaded tags and is checked
     * in the live-game checklist.
     */
    @Test
    public void vantaOilWhitelistResolvesIdAndClassEntriesWithoutTags() {
        assertTrue(RecipeVantaOil.isVantaOilableWeapon(new ItemStack(Items.BONE)), "a raw-id whitelist entry matches");
        assertTrue(RecipeVantaOil.isVantaOilableWeapon(new ItemStack(Items.IRON_SWORD)), "a sword is oilable by class");
        assertFalse(RecipeVantaOil.isVantaOilableWeapon(new ItemStack(Items.APPLE)), "an unlisted food is not oilable");
        assertFalse(RecipeVantaOil.isVantaOilableWeapon(ItemStack.EMPTY), "an empty stack is not oilable");
    }

    /** A 3x3 crafting grid from the given stacks (empty padding to nine slots, row-major). */
    private static CraftingInput craftingInput(ItemStack... stacks) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; i++) {
            items.set(i, stacks[i]);
        }
        // The record constructor is private; the public factory crops the grid to the non-empty bounding box, which is
        // irrelevant to the order-insensitive shapeless matching under test here.
        return CraftingInput.of(3, 3, items);
    }

    /**
     * Encode then decode a recipe through its own {@link MapCodec}, failing loudly on any codec error. {@code MapCodec}
     * is field-oriented, so it has no {@code encodeStart}/{@code decode(JsonElement)}: encode folds the value into a
     * {@code RecordBuilder} and builds the object, then {@code getMap} hands the {@code MapLike} back to {@code decode}
     * - the same {@code DynamicOps}/{@code MapLike} pair {@code Recipe.CODEC} uses when {@code RecipeManager} parses a
     * {@code JsonObject}.
     */
    private static <T> T roundTrip(MapCodec<T> codec, T sample) {
        JsonElement json = codec.encode(sample, JsonOps.INSTANCE, JsonOps.INSTANCE.mapBuilder())
                .build(JsonOps.INSTANCE.empty())
                .getOrThrow(error -> new AssertionError("encode failed: " + error));
        MapLike<JsonElement> map = JsonOps.INSTANCE.getMap(json)
                .getOrThrow(error -> new AssertionError("the encoded recipe is not a JSON object: " + error));
        return codec.decode(JsonOps.INSTANCE, map)
                .getOrThrow(error -> new AssertionError("decode failed: " + error));
    }

    /**
     * Read a generated recipe JSON off the classpath, drop the {@code type} discriminator (the {@link MapCodec}
     * covers only the recipe body) and decode it through the serializer's codec - asserting it resolves to the
     * expected registered {@link RecipeType}. Mirrors {@code RecipeManager.fromJson}'s parse path.
     */
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

    /** The recipe instance must report the identical registered type and serializer the codec under test came from. */
    private static void assertWired(net.minecraft.world.item.crafting.Recipe<?> recipe,
                                   net.minecraft.world.item.crafting.RecipeType<?> type,
                                   net.minecraft.world.item.crafting.RecipeSerializer<?> serializer) {
        assertSame(type, recipe.getType(), "the recipe reports the registered type");
        assertSame(serializer, recipe.getSerializer(), "the recipe reports the registered serializer");
    }
}
