package rustic.content.fluids;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import rustic.Rustic;
import rustic.content.items.RusticItems;
import rustic.content.potions.RusticMobEffects;

import java.util.ArrayList;
import java.util.function.Supplier;

public final class RusticFluids {

    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Rustic.MODID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Rustic.MODID);

    public static final DeferredHolder<FluidType, FluidType> OLIVE_OIL_TYPE =
            type("oliveoil", 920, 2000, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> IRONBERRY_JUICE_TYPE =
            type("ironberryjuice", 1100, 1100, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> WILDBERRY_JUICE_TYPE =
            type("wildberryjuice", 1070, 1100, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> GRAPE_JUICE_TYPE =
            type("grapejuice", 1070, 1100, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> APPLE_JUICE_TYPE =
            type("applejuice", 1050, 1100, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> ALE_WORT_TYPE =
            type("alewort", 1004, 2000, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> HONEY_TYPE =
            type("honey", 1433, 5500, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> GOLDEN_APPLE_JUICE_TYPE =
            type("goldenapplejuice", 1050, 1100, Rarity.EPIC);
    public static final DeferredHolder<FluidType, FluidType> VANTA_OIL_TYPE =
            type("vantaoil", 920, 2000, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> ALE_TYPE =
            type("ale", 1004, 1016, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> CIDER_TYPE =
            type("cider", 1004, 1400, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> IRON_WINE_TYPE =
            type("ironwine", 1034, 1400, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> MEAD_TYPE =
            type("mead", 1034, 1500, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> WILDBERRY_WINE_TYPE =
            type("wildberrywine", 1034, 1500, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> WINE_TYPE =
            type("wine", 1034, 1500, Rarity.COMMON);
    public static final DeferredHolder<FluidType, FluidType> AMBROSIA_TYPE =
            type("ambrosia", 1004, 1400, Rarity.EPIC);

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> OLIVE_OIL = FLUIDS.register("oliveoil",
            () -> new RusticDrinkableFluid(pair(OLIVE_OIL_TYPE, RusticFluids.OLIVE_OIL, RusticFluids.OLIVE_OIL_FLOWING, 10, () -> RusticFluidBlocks.OLIVE_OIL.value(), () -> RusticItems.OLIVE_OIL_BUCKET.value()),
                    (level, player, stack, fluid) -> {
                        player.getFoodData().eat(1, 0.4F);
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 1), null);
                    }));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> OLIVE_OIL_FLOWING = FLUIDS.register("oliveoil_flow",
            () -> new RusticFlowingFluid(pair(OLIVE_OIL_TYPE, OLIVE_OIL, RusticFluids.OLIVE_OIL_FLOWING, 10, () -> RusticFluidBlocks.OLIVE_OIL.value(), () -> RusticItems.OLIVE_OIL_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> IRONBERRY_JUICE = FLUIDS.register("ironberryjuice",
            () -> new RusticDrinkableFluid(pair(IRONBERRY_JUICE_TYPE, RusticFluids.IRONBERRY_JUICE, RusticFluids.IRONBERRY_JUICE_FLOWING, 5, () -> RusticFluidBlocks.IRONBERRY_JUICE.value(), () -> RusticItems.IRONBERRY_JUICE_BUCKET.value()),
                    (level, player, stack, fluid) -> {
                        player.getFoodData().eat(1, 0.8F);
                        int duration = 30 * 20;
                        MobEffectInstance effect = player.getEffect(RusticMobEffects.FULLMETAL);
                        int total = effect == null ? duration : effect.getDuration() + duration;
                        player.addEffect(new MobEffectInstance(RusticMobEffects.FULLMETAL, total, 0, false, true), null);
                    }));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> IRONBERRY_JUICE_FLOWING = FLUIDS.register("ironberryjuice_flow",
            () -> new RusticFlowingFluid(pair(IRONBERRY_JUICE_TYPE, IRONBERRY_JUICE, RusticFluids.IRONBERRY_JUICE_FLOWING, 5, () -> RusticFluidBlocks.IRONBERRY_JUICE.value(), () -> RusticItems.IRONBERRY_JUICE_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> WILDBERRY_JUICE = FLUIDS.register("wildberryjuice",
            () -> new RusticDrinkableFluid(pair(WILDBERRY_JUICE_TYPE, RusticFluids.WILDBERRY_JUICE, RusticFluids.WILDBERRY_JUICE_FLOWING, 5, () -> RusticFluidBlocks.WILDBERRY_JUICE.value(), () -> RusticItems.WILDBERRY_JUICE_BUCKET.value()),
                    (level, player, stack, fluid) -> {
                        player.getFoodData().eat(1, 1.0F);
                        if (player.getRandom().nextFloat() < 0.2F) {
                            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 75), null);
                        }
                        if (player.getRandom().nextFloat() < 0.1F) {
                            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200), null);
                        }
                    }));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> WILDBERRY_JUICE_FLOWING = FLUIDS.register("wildberryjuice_flow",
            () -> new RusticFlowingFluid(pair(WILDBERRY_JUICE_TYPE, WILDBERRY_JUICE, RusticFluids.WILDBERRY_JUICE_FLOWING, 5, () -> RusticFluidBlocks.WILDBERRY_JUICE.value(), () -> RusticItems.WILDBERRY_JUICE_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> GRAPE_JUICE = FLUIDS.register("grapejuice",
            () -> new RusticDrinkableFluid(pair(GRAPE_JUICE_TYPE, RusticFluids.GRAPE_JUICE, RusticFluids.GRAPE_JUICE_FLOWING, 5, () -> RusticFluidBlocks.GRAPE_JUICE.value(), () -> RusticItems.GRAPE_JUICE_BUCKET.value()),
                    (level, player, stack, fluid) -> player.getFoodData().eat(1, 0.9F)));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> GRAPE_JUICE_FLOWING = FLUIDS.register("grapejuice_flow",
            () -> new RusticFlowingFluid(pair(GRAPE_JUICE_TYPE, GRAPE_JUICE, RusticFluids.GRAPE_JUICE_FLOWING, 5, () -> RusticFluidBlocks.GRAPE_JUICE.value(), () -> RusticItems.GRAPE_JUICE_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> APPLE_JUICE = FLUIDS.register("applejuice",
            () -> new RusticDrinkableFluid(pair(APPLE_JUICE_TYPE, RusticFluids.APPLE_JUICE, RusticFluids.APPLE_JUICE_FLOWING, 5, () -> RusticFluidBlocks.APPLE_JUICE.value(), () -> RusticItems.APPLE_JUICE_BUCKET.value()),
                    (level, player, stack, fluid) -> player.getFoodData().eat(1, 1.2F)));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> APPLE_JUICE_FLOWING = FLUIDS.register("applejuice_flow",
            () -> new RusticFlowingFluid(pair(APPLE_JUICE_TYPE, APPLE_JUICE, RusticFluids.APPLE_JUICE_FLOWING, 5, () -> RusticFluidBlocks.APPLE_JUICE.value(), () -> RusticItems.APPLE_JUICE_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> ALE_WORT = FLUIDS.register("alewort",
            () -> new RusticDrinkableFluid(pair(ALE_WORT_TYPE, RusticFluids.ALE_WORT, RusticFluids.ALE_WORT_FLOWING, 10, () -> RusticFluidBlocks.ALE_WORT.value(), () -> RusticItems.ALE_WORT_BUCKET.value()),
                    (level, player, stack, fluid) -> {
                        player.getFoodData().eat(1, 2.0F);
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 1), null);
                    }));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> ALE_WORT_FLOWING = FLUIDS.register("alewort_flow",
            () -> new RusticFlowingFluid(pair(ALE_WORT_TYPE, ALE_WORT, RusticFluids.ALE_WORT_FLOWING, 10, () -> RusticFluidBlocks.ALE_WORT.value(), () -> RusticItems.ALE_WORT_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> HONEY = FLUIDS.register("honey",
            () -> new RusticDrinkableFluid(pair(HONEY_TYPE, RusticFluids.HONEY, RusticFluids.HONEY_FLOWING, 27, () -> RusticFluidBlocks.HONEY.value(), () -> RusticItems.HONEY_BUCKET.value()),
                    (level, player, stack, fluid) -> {
                        player.getFoodData().eat(3, 0.4F);
                        if (player.getRandom().nextFloat() < 0.6F) {
                            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 75), null);
                        }
                    }));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> HONEY_FLOWING = FLUIDS.register("honey_flow",
            () -> new RusticFlowingFluid(pair(HONEY_TYPE, HONEY, RusticFluids.HONEY_FLOWING, 27, () -> RusticFluidBlocks.HONEY.value(), () -> RusticItems.HONEY_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> GOLDEN_APPLE_JUICE = FLUIDS.register("goldenapplejuice",
            () -> new RusticDrinkableFluid(pair(GOLDEN_APPLE_JUICE_TYPE, RusticFluids.GOLDEN_APPLE_JUICE, RusticFluids.GOLDEN_APPLE_JUICE_FLOWING, 5, () -> RusticFluidBlocks.GOLDEN_APPLE_JUICE.value(), () -> RusticItems.GOLDEN_APPLE_JUICE_BUCKET.value()),
                    (level, player, stack, fluid) -> {
                        player.getFoodData().eat(3, 1.8F);
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1), null);
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), null);
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), null);
                        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), null);
                    }));
    public static final DeferredHolder<Fluid, RusticFlowingFluid> GOLDEN_APPLE_JUICE_FLOWING = FLUIDS.register("goldenapplejuice_flow",
            () -> new RusticFlowingFluid(pair(GOLDEN_APPLE_JUICE_TYPE, GOLDEN_APPLE_JUICE, RusticFluids.GOLDEN_APPLE_JUICE_FLOWING, 5, () -> RusticFluidBlocks.GOLDEN_APPLE_JUICE.value(), () -> RusticItems.GOLDEN_APPLE_JUICE_BUCKET.value())));

    public static final DeferredHolder<Fluid, RusticDrinkableFluid> VANTA_OIL = FLUIDS.register("vantaoil",
            () -> new RusticDrinkableFluid(soloBucket(VANTA_OIL_TYPE, RusticFluids.VANTA_OIL, () -> RusticItems.VANTA_OIL_BUCKET.value()),
                    (level, player, stack, fluid) -> {
                        player.getFoodData().eat(1, 0.4F);
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 600, 0), null);
                    }));

    public static final DeferredHolder<Fluid, RusticBooze> ALE = FLUIDS.register("ale",
            () -> new RusticBooze(solo(ALE_TYPE, RusticFluids.ALE)) {
                @Override
                protected void affectPlayer(ServerLevel level, Player player, float quality) {
                    if (quality >= 0.5F) {
                        float saturation = 4F * quality;
                        player.getFoodData().eat(2, saturation);
                        int duration = 1200 + (int) (10800 * Math.max(Math.abs((quality - 0.5F) * 2F), 0F));
                        player.addEffect(new MobEffectInstance(RusticMobEffects.FULL, duration), null);
                    } else {
                        int duration = (int) (6000 * Math.max(1 - quality, 0.25F));
                        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration), null);
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration), null);
                    }
                }
            }.setInebriationChance(0.5F));

    public static final DeferredHolder<Fluid, RusticBooze> CIDER = FLUIDS.register("cider",
            () -> new RusticBooze(solo(CIDER_TYPE, RusticFluids.CIDER)) {
                @Override
                protected void affectPlayer(ServerLevel level, Player player, float quality) {
                    if (quality >= 0.5F) {
                        float saturation = 2F * quality;
                        player.getFoodData().eat(1, saturation);
                        int duration = 1200 + (int) (10800 * Math.max(Math.abs((quality - 0.5F) * 2F), 0F));
                        player.addEffect(new MobEffectInstance(RusticMobEffects.MAGIC_RESISTANCE, duration), null);
                    } else {
                        int duration = (int) (1200 * Math.max(1 - quality, 0.25F));
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, duration), null);
                        duration = (int) (6000 * Math.max(1 - quality, 0.25F));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration), null);
                    }
                }
            }.setInebriationChance(0.5F));

    public static final DeferredHolder<Fluid, RusticBooze> IRON_WINE = FLUIDS.register("ironwine",
            () -> new RusticBooze(solo(IRON_WINE_TYPE, RusticFluids.IRON_WINE)) {
                @Override
                protected void affectPlayer(ServerLevel level, Player player, float quality) {
                    if (quality >= 0.5F) {
                        float saturation = 2F * quality;
                        float absorption = 10F * Math.max((quality - 0.5F) * 2F, 0F);
                        player.getFoodData().eat(1, saturation);
                        player.setAbsorptionAmount(Math.max(Math.min(player.getAbsorptionAmount() + absorption, 20F),
                                player.getAbsorptionAmount()));
                    } else {
                        int duration = (int) (6000 * Math.max(1 - quality, 0.25F));
                        float damage = 10F * Math.max(Math.abs(quality - 0.5F) + 0.1F, 0.25F);
                        player.hurt(level.damageSources().magic(), damage);
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration), null);
                    }
                }
            }.setInebriationChance(0.5F));

    public static final DeferredHolder<Fluid, RusticBooze> MEAD = FLUIDS.register("mead",
            () -> new RusticBooze(solo(MEAD_TYPE, RusticFluids.MEAD)) {
                @Override
                protected void affectPlayer(ServerLevel level, Player player, float quality) {
                    if (quality >= 0.5F) {
                        float saturation = 2F * quality;
                        player.getFoodData().eat(1, saturation);
                        int duration = 1200 + (int) (6000 * Math.max(Math.abs((quality - 0.5F) * 2F), 0F));
                        player.addEffect(new MobEffectInstance(RusticMobEffects.WITHER_WARD, duration), null);
                    } else {
                        int duration = (int) (800 * Math.max(1 - quality, 0.25F));
                        player.addEffect(new MobEffectInstance(MobEffects.WITHER, duration), null);
                        duration = (int) (6000 * Math.max(1 - quality, 0.25F));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration), null);
                    }
                }
            }.setInebriationChance(0.5F));

    public static final DeferredHolder<Fluid, RusticBooze> WILDBERRY_WINE = FLUIDS.register("wildberrywine",
            () -> new RusticBooze(solo(WILDBERRY_WINE_TYPE, RusticFluids.WILDBERRY_WINE)) {
                @Override
                protected void affectPlayer(ServerLevel level, Player player, float quality) {
                    if (quality >= 0.5F) {
                        float saturation = 2F * quality;
                        player.getFoodData().eat(1, saturation);
                        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                            if (!isBad(effect) && effect.getAmplifier() < 2) {
                                player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(),
                                        effect.getAmplifier() + 1, effect.isAmbient(), effect.isVisible()), null);
                            }
                        }
                    } else {
                        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                            if (!isBad(effect)) {
                                if (effect.getAmplifier() > 0) {
                                    player.removeEffect(effect.getEffect());
                                    player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(),
                                            effect.getAmplifier() - 1, effect.isAmbient(), effect.isVisible()), null);
                                } else if (effect.getAmplifier() == 0) {
                                    player.removeEffect(effect.getEffect());
                                }
                            }
                        }
                        int duration = (int) (6000 * Math.max(1 - quality, 0.25F));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration), null);
                    }
                }
            }.setInebriationChance(0.85F));

    public static final DeferredHolder<Fluid, RusticBooze> WINE = FLUIDS.register("wine",
            () -> new RusticBooze(solo(WINE_TYPE, RusticFluids.WINE)) {
                @Override
                protected void affectPlayer(ServerLevel level, Player player, float quality) {
                    if (quality >= 0.5F) {
                        float saturation = 2F * quality;
                        player.getFoodData().eat(1, saturation);
                        int durationIncrease = 600 + (int) (2400 * ((quality - 0.5F) * 2F));
                        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                            if (!isBad(effect) && effect.getDuration() < 12000) {
                                int duration = Math.max(Math.min(effect.getDuration() + durationIncrease, 12000),
                                        effect.getDuration());
                                player.addEffect(new MobEffectInstance(effect.getEffect(), duration,
                                        effect.getAmplifier(), effect.isAmbient(), effect.isVisible()), null);
                            }
                        }
                    } else {
                        int durationDecrease = (int) (2400 * Math.abs(quality - 0.6F));
                        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
                            if (!isBad(effect)) {
                                int duration = effect.getDuration() - durationDecrease;
                                if (duration > 0) {
                                    player.removeEffect(effect.getEffect());
                                    player.addEffect(new MobEffectInstance(effect.getEffect(), duration,
                                            effect.getAmplifier(), effect.isAmbient(), effect.isVisible()), null);
                                } else {
                                    player.removeEffect(effect.getEffect());
                                }
                            }
                        }
                        int duration = (int) (6000 * Math.max(1 - quality, 0F));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration), null);
                    }
                }
            }.setInebriationChance(0.5F));

    public static final DeferredHolder<Fluid, RusticBooze> AMBROSIA = FLUIDS.register("ambrosia",
            () -> new RusticBooze(solo(AMBROSIA_TYPE, RusticFluids.AMBROSIA)) {
                @Override
                protected void affectPlayer(ServerLevel level, Player player, float quality) {
                    if (quality >= 0.5F) {
                        float saturation = 2F * quality;
                        player.getFoodData().eat(2, saturation);
                        int duration;
                        if (quality > 0.99F) {
                            duration = 999999;
                            player.removeEffect(RusticMobEffects.UNDYING);
                        } else {
                            duration = (20 * 300) + (int) (18000 * Math.max(Math.abs((quality - 0.5F) * 2F), 0.0F));
                        }
                        player.addEffect(new MobEffectInstance(RusticMobEffects.UNDYING, duration, 0, false, false), null);
                    } else {

                        player.kill();
                    }
                }

                @Override
                protected void inebriate(ServerLevel level, Player player, float quality) {
                    int duration = (quality >= 0.5F)
                            ? ((int) (12000 * Math.max(1 - Math.abs(quality - 0.75F), 0.5F)))
                            : ((int) (12000 * Math.max(1 - (quality * 0.5F), 0.5F)));
                    MobEffectInstance tipsyEffect = player.getEffect(RusticMobEffects.TIPSY);
                    if (level.random.nextFloat() < this.inebriationChance) {
                        if (tipsyEffect == null) {
                            int amp = (quality > 0.99F) ? 1 : 0;
                            player.addEffect(new MobEffectInstance(RusticMobEffects.TIPSY, duration, amp, false, false), null);
                        } else if (tipsyEffect.getAmplifier() < 3) {
                            int newAmp = Math.min(tipsyEffect.getAmplifier() + ((quality > 0.99F) ? 2 : 1), 3);
                            player.addEffect(new MobEffectInstance(RusticMobEffects.TIPSY,
                                    Math.max(duration, tipsyEffect.getDuration()), newAmp, false, false), null);
                        }
                    }
                }
            }.setInebriationChance(1.0F));

    private static DeferredHolder<FluidType, FluidType> type(String name, int density, int viscosity, Rarity rarity) {
        return FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .density(density).viscosity(viscosity).rarity(rarity)));
    }

    private static BaseFlowingFluid.Properties pair(DeferredHolder<FluidType, FluidType> type,
                                                    DeferredHolder<Fluid, ? extends Fluid> still,
                                                    DeferredHolder<Fluid, ? extends Fluid> flowing, int tickRate,
                                                    Supplier<? extends LiquidBlock> block,
                                                    Supplier<? extends Item> bucket) {
        return new BaseFlowingFluid.Properties(type, still, flowing).tickRate(tickRate).block(block).bucket(bucket);
    }

    private static BaseFlowingFluid.Properties solo(DeferredHolder<FluidType, FluidType> type,
                                                    DeferredHolder<Fluid, ? extends Fluid> self) {
        return new BaseFlowingFluid.Properties(type, self, self);
    }

    private static BaseFlowingFluid.Properties soloBucket(DeferredHolder<FluidType, FluidType> type,
                                                          DeferredHolder<Fluid, ? extends Fluid> self,
                                                          Supplier<? extends Item> bucket) {
        return new BaseFlowingFluid.Properties(type, self, self).bucket(bucket);
    }

    public static void register(IEventBus modEventBus) {

        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }

    private RusticFluids() {
    }
}
