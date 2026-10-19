package rustic.content.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.config.RusticConfig;
import rustic.content.fluids.RusticDataComponents;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.ItemFluidBottle;
import rustic.content.items.RusticItems;
import rustic.registry.RusticRegistries;

public class RecipeOliveOil implements CraftingRecipe {

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!RusticConfig.ENABLE_OLIVE_OILING.get()) {
            return false;
        }
        ItemStack food = ItemStack.EMPTY;
        ItemStack oil = ItemStack.EMPTY;
        int stacks = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            stacks++;
            if (isOliveOilBottle(stack)) {
                if (!oil.isEmpty()) {
                    return false;
                }
                oil = stack;
            } else if (isOileableFood(stack)) {
                if (!food.isEmpty()) {
                    return false;
                }
                food = stack;
            } else {
                return false;
            }
        }
        return stacks == 2 && !food.isEmpty() && !oil.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && isOileableFood(stack)) {
                ItemStack out = stack.copyWithCount(1);
                out.set(RusticDataComponents.OILED.get(), true);
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width <= 3 && height <= 3;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RusticRegistries.OLIVE_OIL_SERIALIZER.get();
    }

    @Override
    public CraftingBookCategory category() {

        return CraftingBookCategory.MISC;
    }

    private static boolean isOliveOilBottle(ItemStack stack) {
        if (!stack.is(RusticItems.FLUID_BOTTLE.get())) {
            return false;
        }
        FluidStack content = ItemFluidBottle.getFluidContent(stack);
        return content.getFluid() == RusticFluids.OLIVE_OIL.get();
    }

    private static boolean isOileableFood(ItemStack stack) {

        if (!stack.has(DataComponents.FOOD) || stack.has(RusticDataComponents.OILED.get())) {
            return false;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean listed = false;
        for (String entry : RusticConfig.OLIVE_OIL_BLACKLIST.get()) {
            if (entry.equals(id)) {
                listed = true;
                break;
            }
        }

        return RusticConfig.OLIVE_OIL_USE_WHITELIST.get() == listed;
    }

    public static class Serializer implements RecipeSerializer<RecipeOliveOil> {

        private static final RecipeOliveOil INSTANCE = new RecipeOliveOil();

        private static final MapCodec<RecipeOliveOil> CODEC = MapCodec.unit(INSTANCE);

        private static final StreamCodec<RegistryFriendlyByteBuf, RecipeOliveOil> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<RecipeOliveOil> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RecipeOliveOil> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
