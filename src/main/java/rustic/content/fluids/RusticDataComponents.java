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

public final class RusticDataComponents {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Rustic.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> QUALITY =
            COMPONENTS.registerComponentType("quality",
                    builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidContent>> FLUID_CONTENT =
            COMPONENTS.registerComponentType("fluid_content",
                    builder -> builder.persistent(FluidContent.CODEC)
                            .networkSynchronized(FluidContent.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<MobEffectInstance>>> ELIXIR_EFFECTS =
            COMPONENTS.registerComponentType("elixir_effects",
                    builder -> builder.persistent(MobEffectInstance.CODEC.listOf())
                            .networkSynchronized(MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list())));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MobEffectInstance>> VANTA_OIL =
            COMPONENTS.registerComponentType("vanta_oil",
                    builder -> builder.persistent(MobEffectInstance.CODEC)
                            .networkSynchronized(MobEffectInstance.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> OILED =
            COMPONENTS.registerComponentType("oiled",
                    builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    private RusticDataComponents() {
    }
}
