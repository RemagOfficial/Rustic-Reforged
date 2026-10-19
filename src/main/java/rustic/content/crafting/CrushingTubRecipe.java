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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.registry.RusticRegistries;

public class CrushingTubRecipe implements Recipe<SingleRecipeInput> {

    private final Ingredient ingredient;
    private final FluidStack fluid;
    private final ItemStack byproduct;

    public CrushingTubRecipe(Ingredient ingredient, FluidStack fluid, ItemStack byproduct) {
        this.ingredient = ingredient;
        this.fluid = fluid;
        this.byproduct = byproduct;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return this.byproduct.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {

        return this.byproduct;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.ingredient);
        return list;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RusticRegistries.CRUSHING_TUB_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RusticRegistries.CRUSHING_TUB_TYPE.get();
    }

    public Ingredient getInput() {
        return this.ingredient;
    }

    public FluidStack getFluid() {
        return this.fluid.copy();
    }

    public ItemStack getByproduct() {
        return this.byproduct.copy();
    }

    public static class Serializer implements RecipeSerializer<CrushingTubRecipe> {

        private static final MapCodec<CrushingTubRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(recipe -> recipe.ingredient),
                        FluidStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.fluid),
                        ItemStack.OPTIONAL_CODEC.optionalFieldOf("byproduct", ItemStack.EMPTY).forGetter(recipe -> recipe.byproduct)
                ).apply(instance, CrushingTubRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, CrushingTubRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<CrushingTubRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrushingTubRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static CrushingTubRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            FluidStack fluid = FluidStack.STREAM_CODEC.decode(buffer);
            ItemStack byproduct = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            return new CrushingTubRecipe(ingredient, fluid, byproduct);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, CrushingTubRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
            FluidStack.STREAM_CODEC.encode(buffer, recipe.fluid);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.byproduct);
        }
    }
}
