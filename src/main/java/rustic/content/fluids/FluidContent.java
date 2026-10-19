package rustic.content.fluids;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

public record FluidContent(FluidStack stack) {

    public FluidContent {
        stack = stack == null ? FluidStack.EMPTY : stack;
    }

    public static final Codec<FluidContent> CODEC =
            FluidStack.OPTIONAL_CODEC.xmap(FluidContent::new, FluidContent::stack);

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidContent> STREAM_CODEC =
            FluidStack.OPTIONAL_STREAM_CODEC.map(FluidContent::new, FluidContent::stack);

    public static FluidContent empty() {
        return new FluidContent(FluidStack.EMPTY);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FluidContent other
                && this.stack.getAmount() == other.stack.getAmount()
                && FluidStack.isSameFluidSameComponents(this.stack, other.stack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stack.getAmount(), this.stack.getFluid(), this.stack.getComponentsPatch());
    }
}
