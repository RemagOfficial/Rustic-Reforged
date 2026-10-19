package rustic.content.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.registry.RusticRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecipeNonIngredientReturn implements CraftingRecipe {

    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;

    public RecipeNonIngredientReturn(ItemStack result, List<Ingredient> ingredients) {
        this.result = result;
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        this.ingredients = list;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<Ingredient> remaining = new ArrayList<>(this.ingredients);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {

        NonNullList<ItemStack> ret = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < ret.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.hasCraftingRemainingItem()) {
                ret.set(i, stack.getCraftingRemainingItem());
            }
        }

        for (int i = 0; i < ret.size(); i++) {
            ItemStack remainder = ret.get(i);
            if (remainder.is(Items.BUCKET) || remainder.is(Items.GLASS_BOTTLE)
                    || remainder.is(RusticMachineBlocks.LIQUID_BARREL.get().asItem())) {
                ret.set(i, ItemStack.EMPTY);
            }
        }
        return ret;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
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
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RusticRegistries.NON_INGREDIENT_RETURN_SERIALIZER.get();
    }

    @Override
    public CraftingBookCategory category() {

        return CraftingBookCategory.MISC;
    }

    public static class Serializer implements RecipeSerializer<RecipeNonIngredientReturn> {

        private static final MapCodec<RecipeNonIngredientReturn> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                        Ingredient.LIST_CODEC.fieldOf("ingredients")
                                .forGetter(recipe -> new ArrayList<>(recipe.ingredients))
                ).apply(instance, RecipeNonIngredientReturn::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, RecipeNonIngredientReturn> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<RecipeNonIngredientReturn> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RecipeNonIngredientReturn> streamCodec() {
            return STREAM_CODEC;
        }

        private static RecipeNonIngredientReturn fromNetwork(RegistryFriendlyByteBuf buffer) {
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            int count = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            return new RecipeNonIngredientReturn(result, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, RecipeNonIngredientReturn recipe) {
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
        }
    }
}
