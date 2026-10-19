package rustic.content.crafting;

import com.mojang.serialization.Codec;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CondenserRecipe implements Recipe<CondenserRecipeInput> {

    private final boolean advanced;
    private final Ingredient modifier;
    private final NonNullList<Ingredient> ingredients;
    private final FluidStack fluid;
    private final ItemStack bottle;
    private final ItemStack result;
    private final int time;

    public CondenserRecipe(boolean advanced, Ingredient modifier, List<Ingredient> ingredients, FluidStack fluid,
                           ItemStack bottle, ItemStack result, int time) {
        this.advanced = advanced;
        this.modifier = modifier;
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        this.ingredients = list;
        this.fluid = fluid;
        this.bottle = bottle;
        this.result = result;
        this.time = time;
    }

    @Override
    public boolean matches(CondenserRecipeInput in, Level level) {
        if (this.ingredients.isEmpty()) {
            return false;
        }

        if (in.bottle().isEmpty() || !ItemStack.isSameItem(in.bottle(), this.bottle)) {
            return false;
        }

        FluidStack offered = in.fluid();
        if (offered.isEmpty() || offered.getFluid() != this.fluid.getFluid()
                || offered.getAmount() < this.fluid.getAmount()) {
            return false;
        }

        // Modifier gate doubles as the basic/advanced discriminator: an EMPTY ingredient only matches an empty
        // slot (basic), a real one demands a matching herb (advanced).
        if (!this.modifier.test(in.modifier())) {
            return false;
        }

        // Exact multiset match: every offered stack consumes one required ingredient; any unmatched stack or
        // leftover ingredient fails.
        List<Ingredient> remaining = new ArrayList<>(this.ingredients);
        for (ItemStack stack : in.ingredients()) {
            if (stack.isEmpty()) {
                continue;
            }
            boolean matched = false;
            Iterator<Ingredient> it = remaining.iterator();
            while (it.hasNext()) {
                if (it.next().test(stack)) {
                    it.remove();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(CondenserRecipeInput in, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {

        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RusticRegistries.CONDENSER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RusticRegistries.CONDENSER_TYPE.get();
    }

    public boolean isAdvanced() {
        return this.advanced;
    }

    public Ingredient getModifier() {
        return this.modifier;
    }

    public FluidStack getFluid() {
        return this.fluid.copy();
    }

    public ItemStack getBottle() {
        return this.bottle.copy();
    }

    public int getTime() {
        return this.time;
    }

    public static class Serializer implements RecipeSerializer<CondenserRecipe> {

        private static final MapCodec<CondenserRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Codec.BOOL.optionalFieldOf("advanced", false).forGetter(recipe -> recipe.advanced),
                        Ingredient.CODEC_NONEMPTY.optionalFieldOf("modifier", Ingredient.EMPTY).forGetter(recipe -> recipe.modifier),
                        Ingredient.LIST_CODEC.fieldOf("ingredients").forGetter(recipe -> new ArrayList<>(recipe.ingredients)),
                        FluidStack.CODEC.fieldOf("water").forGetter(recipe -> recipe.fluid),
                        ItemStack.STRICT_CODEC.fieldOf("bottle").forGetter(recipe -> recipe.bottle),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                        Codec.INT.fieldOf("time").forGetter(recipe -> recipe.time)
                ).apply(instance, CondenserRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, CondenserRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<CondenserRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CondenserRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static CondenserRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            boolean advanced = buffer.readBoolean();
            Ingredient modifier = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            int count = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            FluidStack fluid = FluidStack.STREAM_CODEC.decode(buffer);
            ItemStack bottle = ItemStack.STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            int time = buffer.readInt();
            return new CondenserRecipe(advanced, modifier, ingredients, fluid, bottle, result, time);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, CondenserRecipe recipe) {
            buffer.writeBoolean(recipe.advanced);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.modifier);
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            FluidStack.STREAM_CODEC.encode(buffer, recipe.fluid);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.bottle);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeInt(recipe.time);
        }
    }
}
