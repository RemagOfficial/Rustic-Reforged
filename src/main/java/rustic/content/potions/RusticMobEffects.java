package rustic.content.potions;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

public final class RusticMobEffects {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Rustic.MODID);

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

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    private RusticMobEffects() {
    }
}
