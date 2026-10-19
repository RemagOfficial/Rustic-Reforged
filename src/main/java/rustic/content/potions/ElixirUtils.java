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

/**
 * The elixir/vanta-oil utilities, ported from 1.12.2's {@code rustic.common.util.ElixirUtils}.
 *
 * <p><b>Storage deviation.</b> Legacy kept effects in a freeform {@code {ElixirEffects:[{Effect,Duration,
 * Amplifier}]}} NBT list and the weapon oil in a {@code vanta_oil} subcompound, both read by string key. 1.21
 * replaces them with the typed {@code rustic:elixir_effects} ({@code List<MobEffectInstance>}) and
 * {@code rustic:vanta_oil} ({@code MobEffectInstance}) data components. The plan named vanilla
 * {@code minecraft:custom_effects} as the effects home, but that component does not exist in 1.21.1 (verified
 * against the sources jar), hence the mod-owned one. 1.12.2 save compatibility is waived by plan assumption #101.
 * The legacy "skip unknown effects on read" check is now enforced by the registry codec at the save boundary;
 * {@link #getEffects} additionally drops unbound holders so a hand-built in-memory list behaves the same.
 *
 * <p><b>Colour deviation.</b> {@link #getColor} reads {@link MobEffect#getColor()} where legacy read the
 * {@code Potion.getLiquidColor()} of the same effects; the two tables diverge for vanilla effects (slowness
 * 9154528 vs the legacy 9003986, strength 16762624 vs 14303116). The fallback colour stays the pinned
 * {@value #NO_EFFECTS_COLOR}, and the ported test pins first-effect-wins against the 1.21 values.
 *
 * <p><b>Gap.</b> The weapon-hit consumption of the oil lived in legacy {@code EventHandlerCommon}; that handler
 * is not part of Phase 6, so only these utilities port. The consumption call site lands with the weapon/condenser
 * phases and reuses {@link #getNextVantaHitDuration} / {@link #getRemainingVantaUses} unchanged.
 */
public final class ElixirUtils {

    /** Legacy fallback colour, decimal {@code 16253176} = {@code 0xF800F8}; pinned by the baseline tests. */
    public static final int NO_EFFECTS_COLOR = 16253176;

    /** Per-hit oil charge length: 15 seconds. The golden vanta arithmetic is pinned to this and {@code MAX}. */
    public static final int VANTA_OIL_EFFECT_DURATION = 15 * 20;
    /** Cap above which a stash starts splitting off full 300-tick hits: {@code 300 + 5 * 20 = 400} ticks. */
    public static final int VANTA_OIL_EFFECT_MAX_DURATION = VANTA_OIL_EFFECT_DURATION + (5 * 20);

    /** The 1.21 form of legacy {@code getPotionById}: the registered effect, or {@code null} when unknown. */
    @Nullable
    public static MobEffect getEffectById(ResourceLocation effectId) {
        return BuiltInRegistries.MOB_EFFECT.get(effectId);
    }

    /** The stored effects in insertion order; an empty <em>modifiable</em> list when the stack holds none. */
    public static List<MobEffectInstance> getEffects(ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        if (stack.isEmpty()) {
            return effects;
        }
        List<MobEffectInstance> stored = stack.get(RusticDataComponents.ELIXIR_EFFECTS.get());
        if (stored != null) {
            for (MobEffectInstance effect : stored) {
                // Unknown-effect-skip, the in-memory half of the legacy deserializeEffects contract.
                if (effect != null && effect.getEffect().isBound()) {
                    effects.add(effect);
                }
            }
        }
        return effects;
    }

    /** Appends {@code effect}, preserving order - legacy {@code addEffect} appended to the NBT list tail. */
    public static void addEffect(@Nullable MobEffectInstance effect, ItemStack stack) {
        if (stack.isEmpty() || effect == null) {
            return;
        }
        List<MobEffectInstance> effects = getEffects(stack);
        effects.add(effect);
        stack.set(RusticDataComponents.ELIXIR_EFFECTS.get(), effects);
    }

    /**
     * The elixir tint: the first effect's colour, or {@value #NO_EFFECTS_COLOR} when no effects are stored -
     * the exact contract of the legacy {@code getColor(ItemStack)}.
     */
    public static int getColor(ItemStack stack) {
        List<MobEffectInstance> effects = getEffects(stack);
        if (effects.isEmpty()) {
            return NO_EFFECTS_COLOR;
        }
        return effects.get(0).getEffect().value().getColor();
    }

    /**
     * The item-colour handler result for one tint layer of {@code models/item/elixir.json}, in the ARGB form the
     * 1.21 renderer expects. Index 0 is the {@code elixir_overlay} liquid mask, coloured by {@link #getColor};
     * index 1 is the glass bottle, left white - exactly the legacy {@code ItemElixir.colorMultiplier} split.
     *
     * <p><b>Why {@link FastColor.ARGB32#opaque(int)}.</b> 1.12.2 ignored the alpha byte of the returned colour,
     * so the legacy {@code 0xFFFFFF} literal rendered fine. 1.21 honours it: a raw {@code 0xRRGGBB} (alpha
     * {@code 0x00}) makes the layer fully transparent, and since {@link #getColor} can itself return the
     * alpha-less {@link #NO_EFFECTS_COLOR} fallback, both layers went clear and the elixir rendered invisible.
     * Vanilla's own {@code ItemColors} wraps every tint the same way (see the {@code POTION}/{@code SPAWN_EGG}
     * handlers), so we force opacity on both branches here.
     */
    public static int getTint(ItemStack stack, int tintIndex) {
        return FastColor.ARGB32.opaque(tintIndex == 0 ? getColor(stack) : 0xFFFFFF);
    }

    // ------------------------------------------------------------------ vanta oil

    /** The oiled effect, re-validated on read; {@code null} when absent or illegal (duration &lt; 1, amp &lt; 0). */
    @Nullable
    public static MobEffectInstance getVantaOilEffect(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return validateVantaEffect(stack.get(RusticDataComponents.VANTA_OIL.get()));
    }

    /**
     * Sets, or with an invalid/absent effect clears, the oil. The guards are the legacy
     * {@code setVantaOilEffect(tag, effect)} pair verbatim: {@code null} effect, {@code duration < 1} or
     * {@code amplifier < 0} never get written - they take the {@code null}-return path, which at stack level
     * meant removing the {@code vanta_oil} subcompound (so an expired effect clears a live one, as legacy did).
     * The amplifier guard is defensive under 1.21: {@link MobEffectInstance}'s constructor already clamps it
     * into {@code [0, 255]}, so a hand-built negative can only arrive as a normalised zero. Only this one
     * component is touched; every other component on the stack survives the clear.
     */
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

    /** The legacy field validation: Effect present + registered, Duration &ge; 1, Amplifier &ge; 0. */
    @Nullable
    private static MobEffectInstance validateVantaEffect(@Nullable MobEffectInstance effect) {
        if (effect == null || !effect.getEffect().isBound()
                || effect.getDuration() < 1 || effect.getAmplifier() < 0) {
            return null;
        }
        return effect;
    }

    /**
     * Hits left in an oil charge, the legacy arithmetic pinned by the baseline: an empty charge is 0, an
     * instant effect spends its raw duration as uses, otherwise duration/300 full hits plus one for any
     * remainder over the 100-tick grace (or when nothing full has been banked yet).
     */
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

    /** How much of a stash one hit spends: a full {@link #VANTA_OIL_EFFECT_DURATION} only above the cap. */
    public static int getNextVantaHitDuration(int totalDuration) {
        return (totalDuration > VANTA_OIL_EFFECT_MAX_DURATION) ? VANTA_OIL_EFFECT_DURATION : totalDuration;
    }

    private ElixirUtils() {
    }
}
