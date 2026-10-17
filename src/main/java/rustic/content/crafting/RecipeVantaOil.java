package rustic.content.crafting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.Rustic;
import rustic.config.RusticConfig;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.ItemFluidBottle;
import rustic.content.items.RusticItems;
import rustic.content.potions.ElixirUtils;
import rustic.registry.RusticRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The 1.21 port of 1.12.2's {@code RecipeVantaOil} - a dynamic, {@code Unit}-codec crafting-table recipe that coats a
 * vanta-oilable weapon with a {@link MobEffect}. The grid must hold exactly one {@code rustic:fluid_bottle} of vanta
 * oil, exactly one oilable weapon, and at least one potion/elixir whose single stored effect supplies the oil; all
 * effects must be the same {@link MobEffect}, and any effect already oiled on the weapon must match it too. The
 * merged effect's duration is the sum over ingredients (plus the weapon's existing oil) and its amplifier the
 * smallest seen, exactly the legacy arithmetic; the result is a copy of the weapon with the new
 * {@code rustic:vanta_oil} written via {@link ElixirUtils}.
 *
 * <p>Legacy kept the whole computation in two near-duplicate {@code matches}/{@code getCraftingResult} loops; this
 * collapses them into one {@link #computeResult} so a grid that matches always assembles the identical stack (and a
 * grid that fails returns {@link ItemStack#EMPTY} from both). The legacy harvest-level fallback and the
 * {@code @damage} meta in the whitelist are dropped: 1.21 replaced damage values with data components and the tool
 * class test, and the plan scoped oilable detection to the sword/axe category plus the whitelist
 * ({@link RusticConfig#VANTA_OIL_WHITELIST}, {@code #}-prefixed entries are tags).
 */
public class RecipeVantaOil implements CraftingRecipe {

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !computeResult(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return computeResult(input);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        // Dynamic recipe: the result is a weapon copy carrying merged effects, so there is no static output.
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
        return RusticRegistries.VANTA_OIL_SERIALIZER.get();
    }

    // RecipeType comes from CraftingRecipe's default (RecipeType.CRAFTING) so the vanilla crafting table matches it.
    @Override
    public CraftingBookCategory category() {
        // isSpecial() keeps this out of the recipe book, so the tab is never opened; MISC mirrors legacy.
        return CraftingBookCategory.MISC;
    }

    /** The legacy {@code getCraftingResult} walk, reused verbatim by {@link #matches}: EMPTY on any failure. */
    private ItemStack computeResult(CraftingInput input) {
        ItemStack weapon = ItemStack.EMPTY;
        ItemStack oil = ItemStack.EMPTY;
        Holder<MobEffect> ingEffect = null;
        int amplifier = 0;
        int duration = 0;
        MobEffectInstance weaponEffect = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (isVantaOilBottle(stack)) {
                if (!oil.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                oil = stack;
                continue;
            }

            MobEffectInstance ingredientEffect = getIngredientEffect(stack);
            if (ingredientEffect != null) {
                Holder<MobEffect> effect = ingredientEffect.getEffect();
                if (ingEffect != null) {
                    if (!ingEffect.equals(effect)) {
                        return ItemStack.EMPTY;
                    }
                    if (ingredientEffect.getAmplifier() >= 0 && ingredientEffect.getAmplifier() < amplifier) {
                        amplifier = ingredientEffect.getAmplifier();
                    }
                    duration += effect.value().isInstantenous() ? 1 : Math.max(ingredientEffect.getDuration(), 1);
                } else {
                    ingEffect = effect;
                    amplifier = ingredientEffect.getAmplifier();
                    duration = effect.value().isInstantenous() ? 1 : Math.max(ingredientEffect.getDuration(), 1);
                    if (weaponEffect != null && !weaponEffect.getEffect().equals(ingEffect)) {
                        return ItemStack.EMPTY;
                    }
                }
                continue;
            }

            if (isVantaOilableWeapon(stack)) {
                if (!weapon.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                weapon = stack;
                weaponEffect = ElixirUtils.getVantaOilEffect(weapon);
                if (weaponEffect != null && ingEffect != null && !weaponEffect.getEffect().equals(ingEffect)) {
                    return ItemStack.EMPTY;
                }
                continue;
            }

            return ItemStack.EMPTY;
        }

        if (weapon.isEmpty() || oil.isEmpty() || ingEffect == null) {
            return ItemStack.EMPTY;
        }

        if (weaponEffect != null) {
            duration += Math.max(weaponEffect.getDuration(), 0);
            if (weaponEffect.getAmplifier() >= 0 && weaponEffect.getAmplifier() < amplifier) {
                amplifier = weaponEffect.getAmplifier();
            }
        }

        ItemStack result = weapon.copyWithCount(1);
        ElixirUtils.setVantaOilEffect(result, new MobEffectInstance(ingEffect, duration, amplifier));
        return result;
    }

    // ------------------------------------------------------------------ ingredient helpers (legacy RusticUtils)

    /** A {@code rustic:fluid_bottle} whose content is vanta oil. */
    public static boolean isVantaOilBottle(ItemStack stack) {
        if (!stack.is(RusticItems.FLUID_BOTTLE.get())) {
            return false;
        }
        FluidStack content = ItemFluidBottle.getFluidContent(stack);
        return content.getFluid() == RusticFluids.VANTA_OIL.get();
    }

    /** A sword/axe, or an item named (or tag-membership) in {@link RusticConfig#VANTA_OIL_WHITELIST}. */
    public static boolean isVantaOilableWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof AxeItem) {
            return true;
        }
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        List<? extends String> whitelist = RusticConfig.VANTA_OIL_WHITELIST.get();
        for (String entry : whitelist) {
            if (entry.startsWith("#")) {
                // tryParse returns null instead of throwing when the tag does not spell a valid 1.21 id
                // (a bare name defaults to the minecraft namespace). This runs inside matches() for every
                // grid slot, so an exception here would abort RecipeManager.getRecipeFor and ghost the whole
                // crafting table for every recipe. The legacy OreDictionary.doesOreNameExist was lenient the
                // same way, so an unspellable entry is simply ignored rather than allowed to break crafting.
                ResourceLocation tag = ResourceLocation.tryParse(entry.substring(1));
                if (tag == null) {
                    Rustic.LOGGER.warn("Vanta oil whitelist entry '{}' is not a valid item tag id; ignoring it", entry);
                    continue;
                }
                if (stack.is(TagKey.create(Registries.ITEM, tag))) {
                    return true;
                }
            } else if (entry.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The single effect of a potion or elixir ingredient, or {@code null} when the stack is not a lone-effect
     * source. Vanilla potions read through the {@code minecraft:potion_contents} component; elixirs through
     * {@link ElixirUtils}. A stack with zero or multiple effects yields nothing, as the legacy check required.
     */
    @Nullable
    public static MobEffectInstance getIngredientEffect(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                MobEffectInstance single = null;
                int count = 0;
                for (MobEffectInstance effect : contents.getAllEffects()) {
                    single = effect;
                    if (++count > 1) {
                        break;
                    }
                }
                if (count == 1) {
                    return single;
                }
            }
            return null;
        } else if (item == RusticItems.ELIXIR.get()) {
            List<MobEffectInstance> effects = ElixirUtils.getEffects(stack);
            if (effects.size() == 1) {
                return effects.get(0);
            }
        }
        return null;
    }

    /** Serializer: a stateless dynamic recipe; both codecs are {@code unit} codecs pinned to one shared instance. */
    public static class Serializer implements RecipeSerializer<RecipeVantaOil> {

        // One canonical instance shared by BOTH codecs. A split instance breaks the
        // server-side 'update_recipes' network encode (StreamCodec.unit compares by reference).
        private static final RecipeVantaOil INSTANCE = new RecipeVantaOil();

        private static final MapCodec<RecipeVantaOil> CODEC = MapCodec.unit(INSTANCE);

        private static final StreamCodec<RegistryFriendlyByteBuf, RecipeVantaOil> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public MapCodec<RecipeVantaOil> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RecipeVantaOil> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
