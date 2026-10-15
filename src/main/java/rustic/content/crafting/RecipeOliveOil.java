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

/**
 * The 1.21 port of 1.12.2's {@code RecipeOliveOil} - a dynamic, {@code Unit}-codec crafting-table recipe that oils
 * exactly one edible food with one {@code rustic:fluid_bottle} of olive oil. The result is a single copy of the food
 * carrying the {@code rustic:oiled} marker; the bottle is consumed with no glass remainder (the default empty
 * {@code getRemainingItems} delivers that, as the legacy override did).
 *
 * <p>The legacy mutable {@code foodSlot} field is gone: the food slot is re-derived in {@link #assemble} so the recipe
 * instance stays stateless and safe to share across decodes. Food eligibility honours
 * {@link RusticConfig#OLIVE_OIL_USE_WHITELIST} against {@link RusticConfig#OLIVE_OIL_BLACKLIST} exactly like the
 * legacy {@code USE_WHITELIST != blacklist.contains(id)} test, and the whole recipe no-ops when
 * {@link RusticConfig#ENABLE_OLIVE_OILING} is off (the legacy registration gate, folded into matching).
 */
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
        // Dynamic recipe: the result is derived from the matched food, so there is no static output.
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

    // RecipeType comes from CraftingRecipe's default (RecipeType.CRAFTING) so the vanilla crafting table matches it.
    @Override
    public CraftingBookCategory category() {
        // isSpecial() keeps this out of the recipe book, so the tab is never opened; MISC mirrors legacy.
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
        // The 1.21 form of legacy's "instanceof ItemFood": a food is any stack carrying the FOOD component.
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
        // USE_WHITELIST true -> accept only listed; false (blacklist) -> accept everything but listed.
        return RusticConfig.OLIVE_OIL_USE_WHITELIST.get() == listed;
    }

    /** Serializer: a stateless dynamic recipe; both codecs are {@code unit} codecs pinned to one shared instance. */
    public static class Serializer implements RecipeSerializer<RecipeOliveOil> {

        // One canonical instance shared by BOTH codecs. A split instance breaks the
        // server-side 'update_recipes' network encode (StreamCodec.unit compares by reference).
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
