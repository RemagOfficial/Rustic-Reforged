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

/**
 * 1.12.2's {@code CrushingTubRecipe}: one input item crushes into an output {@link FluidStack} plus an optional
 * byproduct item (the apple/seed split for juice). The legacy class matched on {@code (item, metadata)} against a
 * hand-built list in {@code Recipes.crushingTubRecipes}; here it is a real {@link Recipe} so it lives in the recipe
 * manager (Phase 7 fills the data) and the crushing tub block entity asks {@code getAllRecipesFor(CRUSHING_TUB_TYPE)}.
 *
 * <p>The {@code Recipe} interface forces an {@link ItemStack} result, but the crushing tub's primary output is a
 * fluid, so {@link #getResultItem} reports the byproduct (often empty) and the machine reads the fluid through
 * {@link #getFluid()}. This recipe never appears in a crafting table; {@link #canCraftInDimensions} is a benign
 * {@code true} for exactly that reason.
 */
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
        // The crushing tub's real output is the fluid (getFluid()); the item interface can only report the byproduct.
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

    /** Serializer following the vanilla {@code SmithingTransformRecipe.Serializer} shape: {@code MapCodec} + {@code StreamCodec.of}. */
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
