package rustic.content.potions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import rustic.content.fluids.RusticDataComponents;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ElixirUtils {

    public static final int NO_EFFECTS_COLOR = 16253176;

    public static final int VANTA_OIL_EFFECT_DURATION = 15 * 20;

    public static final int VANTA_OIL_EFFECT_MAX_DURATION = VANTA_OIL_EFFECT_DURATION + (5 * 20);

    @Nullable
    public static MobEffect getEffectById(ResourceLocation effectId) {
        return BuiltInRegistries.MOB_EFFECT.get(effectId);
    }

    public static List<MobEffectInstance> getEffects(ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        if (stack.isEmpty()) {
            return effects;
        }
        List<MobEffectInstance> stored = stack.get(RusticDataComponents.ELIXIR_EFFECTS.get());
        if (stored != null) {
            for (MobEffectInstance effect : stored) {

                if (effect != null && effect.getEffect().isBound()) {
                    effects.add(effect);
                }
            }
        }
        return effects;
    }

    public static void addEffect(@Nullable MobEffectInstance effect, ItemStack stack) {
        if (stack.isEmpty() || effect == null) {
            return;
        }
        List<MobEffectInstance> effects = getEffects(stack);
        effects.add(effect);
        stack.set(RusticDataComponents.ELIXIR_EFFECTS.get(), effects);
    }

    public static int getColor(ItemStack stack) {
        List<MobEffectInstance> effects = getEffects(stack);
        if (effects.isEmpty()) {
            return NO_EFFECTS_COLOR;
        }
        return effects.get(0).getEffect().value().getColor();
    }

    // ARGB tint for one elixir.json layer: index 0 = liquid overlay (uses getColor), index 1 = glass (white).
    // FastColor.ARGB32.opaque forces alpha to 0xFF - 1.21 honours alpha, so a raw 0xRRGGBB would render invisible.
    public static int getTint(ItemStack stack, int tintIndex) {
        return FastColor.ARGB32.opaque(tintIndex == 0 ? getColor(stack) : 0xFFFFFF);
    }

    @Nullable
    public static MobEffectInstance getVantaOilEffect(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return validateVantaEffect(stack.get(RusticDataComponents.VANTA_OIL.get()));
    }

    public static ItemStack setVantaOilEffect(ItemStack stack, @Nullable MobEffectInstance effect) {
        if (stack.isEmpty()) {
            return stack;
        }
        MobEffectInstance valid = validateVantaEffect(effect);
        if (valid == null) {
            stack.remove(RusticDataComponents.VANTA_OIL.get());
        } else {
            stack.set(RusticDataComponents.VANTA_OIL.get(), valid);
        }
        return stack;
    }

    @Nullable
    private static MobEffectInstance validateVantaEffect(@Nullable MobEffectInstance effect) {
        if (effect == null || !effect.getEffect().isBound()
                || effect.getDuration() < 1 || effect.getAmplifier() < 0) {
            return null;
        }
        return effect;
    }

    // Hits left in an oil charge: duration/300 full hits, plus one for a remainder over the 100-tick grace
    // (or when no full hit has been banked yet). Instant effects spend raw duration as uses.
    public static int getRemainingVantaUses(MobEffectInstance effect) {
        int duration = effect.getDuration();
        if (duration <= 0) {
            return 0;
        }
        if (effect.getEffect().value().isInstantenous()) {
            return duration;
        }

        int hits = duration / VANTA_OIL_EFFECT_DURATION;
        int remainderDuration = duration % VANTA_OIL_EFFECT_DURATION;

        if ((remainderDuration > (VANTA_OIL_EFFECT_MAX_DURATION - VANTA_OIL_EFFECT_DURATION)) || (hits == 0)) {
            hits++;
        }

        return hits;
    }

    public static int getNextVantaHitDuration(int totalDuration) {
        return (totalDuration > VANTA_OIL_EFFECT_MAX_DURATION) ? VANTA_OIL_EFFECT_DURATION : totalDuration;
    }

    private ElixirUtils() {
    }
}
