package rustic.content.potions;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

/**
 * The eleven Rustic potion effects from 1.12.2's {@code PotionsRustic}, registered through a
 * {@link DeferredRegister} instead of the legacy hand-rolled {@code GameRegistry.findRegistry(Potion.class)
 * .register(...)} calls in {@code PotionsRustic.init()}.
 *
 * <p>Ownership note — this register is owned here, exactly like {@code RusticEntities},
 * {@code RusticBlockEntities} and {@code RusticMenus} own theirs, rather than filled through the empty
 * shared handle that happens to also exist in {@code RusticRegistries}. The legacy {@code Potion} type is
 * the {@code MobEffect} type in 1.21, and its registry is a vanilla one, so several registers can point at
 * it without collision.
 *
 * <p>Id fidelity — the registry paths are the legacy ones verbatim ({@code ironskin}, {@code feather},
 * {@code blazing_trail}, {@code shame}, {@code fullmetal}, {@code fire_power}, {@code full},
 * {@code magic_resistance}, {@code wither_ward}, {@code undying}, {@code tipsy}), including the un-snake-cased
 * {@code ironskin}/<wbr>{@code fullmetal} spelling 1.12.2 used, because those names appear in elixir and
 * booze NBT/component payloads and the description-key {@code effect.rustic.*} lang entries.
 *
 * <p>Deferred to Phase 9: the status-icon sprites (legacy {@code setIconIndex} over
 * {@code rustic:textures/gui/potion_icons.png}). 1.21 assigns effect icons through the client-extensions
 * API, not on the effect object, so nothing here references the icon sheet yet.
 */
public final class RusticMobEffects {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Rustic.MODID);

    // The five behavioural effects, each a class of its own.
    public static final DeferredHolder<MobEffect, IronSkinEffect> IRON_SKIN =
            MOB_EFFECTS.register("ironskin", IronSkinEffect::new);
    public static final DeferredHolder<MobEffect, FeatherEffect> FEATHER =
            MOB_EFFECTS.register("feather", FeatherEffect::new);
    public static final DeferredHolder<MobEffect, BlazingTrailEffect> BLAZING_TRAIL =
            MOB_EFFECTS.register("blazing_trail", BlazingTrailEffect::new);
    public static final DeferredHolder<MobEffect, ShameEffect> SHAME =
            MOB_EFFECTS.register("shame", ShameEffect::new);
    public static final DeferredHolder<MobEffect, FullmetalEffect> FULLMETAL =
            MOB_EFFECTS.register("fullmetal", FullmetalEffect::new);
    public static final DeferredHolder<MobEffect, TipsyEffect> TIPSY =
            MOB_EFFECTS.register("tipsy", TipsyEffect::new);

    // The five marker effects: no per-tick logic, every behaviour lives in RusticEventHandlerPotions.
    // Colours are the legacy PotionBase(...) arguments verbatim; all are beneficial in 1.12.2.
    public static final DeferredHolder<MobEffect, MarkerEffect> FIRE_POWER =
            MOB_EFFECTS.register("fire_power", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xFFCE6D));
    public static final DeferredHolder<MobEffect, MarkerEffect> FULL =
            MOB_EFFECTS.register("full", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 6563840));
    public static final DeferredHolder<MobEffect, MarkerEffect> MAGIC_RESISTANCE =
            MOB_EFFECTS.register("magic_resistance", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 10511560));
    public static final DeferredHolder<MobEffect, MarkerEffect> WITHER_WARD =
            MOB_EFFECTS.register("wither_ward", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 11842760));
    public static final DeferredHolder<MobEffect, MarkerEffect> UNDYING =
            MOB_EFFECTS.register("undying", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xEADB84));

    /** Attaches the mob-effect register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    private RusticMobEffects() {
    }
}
