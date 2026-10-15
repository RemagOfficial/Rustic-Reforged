package rustic.content.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.registry.RusticRegistries;

/**
 * 1.12.2's {@code BrewingBarrelRecipe}: an input booze fluid (juice) ferments into an output fluid (ale/wine) over
 * the barrel's brew countdown. Legacy matched purely on the input {@code Fluid}'s name (amount was ignored) and the
 * {@code getResult} overload stamped a random {@code QUALITY_NBT_KEY} onto the output at match time.
 *
 * <p>The port keeps only the two fluids in recipe data. Quality is deliberately <b>not</b> stored here: in 1.21 it is
 * applied at runtime by the brewing barrel block entity through the {@code RusticBooze}/{@code FluidContent} model,
 * so the recipe is a plain input-fluid &rarr; output-fluid mapping. The {@code Recipe} result item is always empty
 * because the output is a fluid; the machine reads it through {@link #getOutputFluid()}.
 */
public class BrewingBarrelRecipe implements Recipe<FluidRecipeInput> {

    private final FluidStack input;
    private final FluidStack output;

    public BrewingBarrelRecipe(FluidStack input, FluidStack output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean matches(FluidRecipeInput in, Level level) {
        FluidStack offered = in.fluid();
        // Legacy compared fluid identity by name and ignored the amount.
        return !offered.isEmpty() && !this.input.isEmpty()
                && offered.getFluid() == this.input.getFluid();
    }

    @Override
    public ItemStack assemble(FluidRecipeInput in, HolderLookup.Provider registries) {
        // Output is a fluid, not an item; the barrel reads it via getOutputFluid().
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RusticRegistries.BREWING_BARREL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RusticRegistries.BREWING_BARREL_TYPE.get();
    }

    public FluidStack getInput() {
        return this.input.copy();
    }

    /** The fermented output fluid, before quality is applied by the block entity at runtime. */
    public FluidStack getOutputFluid() {
        return this.output.copy();
    }

    /** Serializer following the vanilla nested-{@code Serializer} codec pattern. */
    public static class Serializer implements RecipeSerializer<BrewingBarrelRecipe> {

        private static final MapCodec<BrewingBarrelRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        FluidStack.CODEC.fieldOf("ingredient").forGetter(recipe -> recipe.input),
                        FluidStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.output)
                ).apply(instance, BrewingBarrelRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, BrewingBarrelRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<BrewingBarrelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BrewingBarrelRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static BrewingBarrelRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            FluidStack input = FluidStack.STREAM_CODEC.decode(buffer);
            FluidStack output = FluidStack.STREAM_CODEC.decode(buffer);
            return new BrewingBarrelRecipe(input, output);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, BrewingBarrelRecipe recipe) {
            FluidStack.STREAM_CODEC.encode(buffer, recipe.input);
            FluidStack.STREAM_CODEC.encode(buffer, recipe.output);
        }
    }
}
