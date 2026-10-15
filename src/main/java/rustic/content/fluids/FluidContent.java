package rustic.content.fluids;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Objects;

/**
 * The value stored in the {@code rustic:fluid_content} item data component: one {@link FluidStack}, wrapped.
 *
 * <p><b>Why a wrapper at all.</b> NeoForge 21.1.259 validates that every {@code DataComponentType} value class
 * overrides {@code equals}/{@code hashCode} (they must also be immutable) - the check fires the moment a stack
 * carrying the component lands in a creative tab or is compared, throwing
 * {@code "Data components must implement equals and hashCode ... Problematic class: FluidStack"} otherwise. The
 * engine's own {@code FluidStack} does not override those (it stays identity-based, matching its mutable design),
 * so it cannot be a data-component value directly. This thin immutable record is what makes it one, keeping the
 * whole {@link FluidStack} - with its nested {@code rustic:quality} component on a booze - intact rather than
 * flattening the fluid id/amount/quality into separate fields.
 *
 * <p>The codecs delegate straight to {@link FluidStack#OPTIONAL_CODEC}/{@link FluidStack#OPTIONAL_STREAM_CODEC},
 * so an emptied bottle still serialises as absent exactly as the unwrapped component did. {@code equals} folds in
 * the amount on top of {@link FluidStack#isSameFluidSameComponents} (which ignores it) and {@code hashCode}
 * mirrors that contract off the fluid singleton, the amount and the {@link FluidStack#getComponentsPatch()}; two
 * bottles of the same fluid and quality compare equal even though their {@code FluidStack}s are different objects.
 */
public record FluidContent(FluidStack stack) {

    public FluidContent {
        stack = stack == null ? FluidStack.EMPTY : stack;
    }

    /** Persist through the fluid stack's own optional codec, so an empty content stays absent. */
    public static final Codec<FluidContent> CODEC =
            FluidStack.OPTIONAL_CODEC.xmap(FluidContent::new, FluidContent::stack);

    /** Network-synchronise through the fluid stack's optional stream codec, for the same reason. */
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidContent> STREAM_CODEC =
            FluidStack.OPTIONAL_STREAM_CODEC.map(FluidContent::new, FluidContent::stack);

    /** An empty content, the value a drained bottle holds. */
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
