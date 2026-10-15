package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.content.fluids.RusticFluids;
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

    // ---------------------------------------------------------------- helpers

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

    /** The recipe instance must report the identical registered type and serializer the codec under test came from. */
    private static void assertWired(net.minecraft.world.item.crafting.Recipe<?> recipe,
                                   net.minecraft.world.item.crafting.RecipeType<?> type,
                                   net.minecraft.world.item.crafting.RecipeSerializer<?> serializer) {
        assertSame(type, recipe.getType(), "the recipe reports the registered type");
        assertSame(serializer, recipe.getSerializer(), "the recipe reports the registered serializer");
    }
}
