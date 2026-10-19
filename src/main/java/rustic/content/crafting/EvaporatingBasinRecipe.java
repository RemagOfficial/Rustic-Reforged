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

public class EvaporatingBasinRecipe implements Recipe<FluidRecipeInput> {

    private final FluidStack input;
    private final ItemStack output;

    public EvaporatingBasinRecipe(FluidStack input, ItemStack output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public boolean matches(FluidRecipeInput in, Level level) {
        FluidStack offered = in.fluid();
        return !offered.isEmpty()
                && offered.getFluid() == this.input.getFluid()
                && offered.getAmount() >= this.input.getAmount();
    }

    @Override
    public ItemStack assemble(FluidRecipeInput in, HolderLookup.Provider registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RusticRegistries.EVAPORATING_BASIN_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RusticRegistries.EVAPORATING_BASIN_TYPE.get();
    }

    public FluidStack getInput() {
        return this.input.copy();
    }

    public ItemStack getOutput() {
        return this.output.copy();
    }

    // Evaporation countdown is data-driven from the input fluid amount (no separate time field), as in the original.
    public int getTime() {
        return this.input.getAmount();
    }

    public static class Serializer implements RecipeSerializer<EvaporatingBasinRecipe> {

        private static final MapCodec<EvaporatingBasinRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        FluidStack.CODEC.fieldOf("fluid").forGetter(recipe -> recipe.input),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.output)
                ).apply(instance, EvaporatingBasinRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, EvaporatingBasinRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<EvaporatingBasinRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EvaporatingBasinRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static EvaporatingBasinRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            FluidStack input = FluidStack.STREAM_CODEC.decode(buffer);
            ItemStack output = ItemStack.STREAM_CODEC.decode(buffer);
            return new EvaporatingBasinRecipe(input, output);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, EvaporatingBasinRecipe recipe) {
            FluidStack.STREAM_CODEC.encode(buffer, recipe.input);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
        }
    }
}
