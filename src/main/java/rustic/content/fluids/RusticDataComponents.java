package rustic.content.fluids;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

import java.util.List;

/**
 * The data components the ported fluid/booze content stores on {@link net.neoforged.neoforge.fluids.FluidStack}s
 * and item stacks. 1.12.2 kept these as loose NBT tags read by string key; 1.21 replaces that with typed,
 * registered {@link net.minecraft.core.component.DataComponentType}s, which is what the plan's
 * "quality as a FluidStack data component" refers to.
 *
 * <p><b>{@code rustic:quality}.</b> Replaces the legacy {@code FluidBooze.QUALITY_NBT_KEY = "Quality"} FLOAT tag
 * (NBT type 5). It is a plain {@code Float}, {@link Codec#FLOAT persistent} and
 * {@link ByteBufCodecs#FLOAT network-synchronised} so barrels, bottles and the elixir utils can carry it through
 * both save and network the way the old tag did. {@link RusticBooze#getQuality} is the single reader and keeps the
 * legacy contract exactly: absent reads as {@code 0.0F} (the golden baseline pins the default, not {@code 0.5}),
 * and the raw value is clamped to {@code [0, 1]}.</p>
 */
public final class RusticDataComponents {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Rustic.MODID);

    /** Booze quality, {@code [0, 1]}, default {@code 0.0F} when absent. The 1.21 form of the legacy "Quality" tag. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> QUALITY =
            COMPONENTS.registerComponentType("quality",
                    builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    /**
     * The fluid inside a {@code rustic:fluid_bottle}, replacing the legacy {@code ItemFluidBottle.FLUID_NBT_KEY}
     * {@code "Fluid"} compound tag. It is a whole {@link FluidStack} - and the {@code rustic:quality} component of a
     * booze rides <em>inside</em> this stack's own components, exactly where legacy stored it under the fluid tag's
     * nested {@code Tag} compound - so {@link RusticBooze#getQuality} reads it straight off the bottle's content.
     * {@link FluidStack#OPTIONAL_CODEC} / {@link FluidStack#OPTIONAL_STREAM_CODEC} are used (not the non-optional
     * forms) so an emptied bottle serialises as absent rather than throwing on an empty stack.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidStack>> FLUID_CONTENT =
            COMPONENTS.registerComponentType("fluid_content",
                    builder -> builder.persistent(FluidStack.OPTIONAL_CODEC)
                            .networkSynchronized(FluidStack.OPTIONAL_STREAM_CODEC));

    /**
     * The effects stored in an elixir, the 1.21 form of the legacy {@code ElixirUtils} {@code "ElixirEffects"}
     * NBT list (order-preserving {@code [{Effect,Duration,Amplifier}]} compounds).
     *
     * <p><b>Plan deviation.</b> The plan named vanilla {@code minecraft:custom_effects} as the storage, but that
     * component does not exist in 1.21.1 - Mojang only added it in a later version (verified against
     * {@code DataComponents.java} in the 21.1.259 sources jar: no {@code CUSTOM_EFFECTS} entry). This mod-owned
     * {@code rustic:elixir_effects} list is the drop-in replacement. A side benefit matches the legacy contract
     * anyway: the typed {@link MobEffectInstance#CODEC} rejects unknown effect ids at the save boundary, the
     * job the legacy {@code deserializeEffects} registry-contains check did in memory.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<MobEffectInstance>>> ELIXIR_EFFECTS =
            COMPONENTS.registerComponentType("elixir_effects",
                    builder -> builder.persistent(MobEffectInstance.CODEC.listOf())
                            .networkSynchronized(MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /**
     * The single effect oiled onto a weapon, the 1.21 form of the legacy {@code vanta_oil} subcompound the old
     * {@code ElixirUtils} set/get/clear guarded. Absent means no oil; {@link rustic.content.potions.ElixirUtils}
     * re-validates duration/amplifier on read, exactly like the legacy {@code getVantaOilEffect(tag)} did.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MobEffectInstance>> VANTA_OIL =
            COMPONENTS.registerComponentType("vanta_oil",
                    builder -> builder.persistent(MobEffectInstance.CODEC)
                            .networkSynchronized(MobEffectInstance.STREAM_CODEC));

    /** Attaches the component register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    private RusticDataComponents() {
    }
}
