package rustic.content.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.fluids.RusticBooze;
import rustic.content.items.ItemFluidBottle;

import java.util.Optional;

/**
 * The 1.21 form of 1.12.2's {@code rustic.common.advancements.AlcoholItemPredicate}, the custom item predicate the
 * legacy advancement tree hung its alcohol progress off ({@code the_good_stuff}, {@code winemaxxing}). It is the
 * only runtime code in the data-migration phase and is registered as {@code rustic:alcohol} against
 * {@link net.minecraft.core.registries.Registries#ITEM_SUB_PREDICATE_TYPE}.
 *
 * <p><b>Where the legacy {@code ItemPredicate} subclass fits now.</b> 1.12.2 modelled this by subclassing
 * {@code ItemPredicate} and reading the fluid off the stack's {@code IFluidHandlerItem} capability, with the JSON
 * written as a top-level {@code {"type":"rustic:alcohol", ...}} object. 1.21 reworked item matching into a
 * composable {@link ItemSubPredicate} that lives under an {@code ItemPredicate}'s {@code predicates} map, so the
 * advancement JSON is now {@code {"predicates": {"rustic:alcohol": {"fluid": "...", "quality": {...}}}}}. The
 * behaviour is otherwise identical: a stack matches only when it holds a {@link RusticBooze} whose quality satisfies
 * the bounds, optionally narrowed to one specific booze.</p>
 *
 * <p><b>Storage.</b> The bottle's fluid and its nested {@code rustic:quality} component replace the legacy
 * {@code Fluid} NBT compound; they are read through {@link ItemFluidBottle#getFluidContent(ItemStack)} and
 * {@link RusticBooze#getQuality(FluidStack)} so this predicate stays in lock-step with the tooltip/barrel quality
 * contract pinned by the golden booze tests.</p>
 */
public record AlcoholItemPredicate(Optional<Fluid> fluid, MinMaxBounds.Doubles quality) implements ItemSubPredicate {

    /** {@code fluid} is the optional specific booze (a registry id); {@code quality} is the optional [0,1] bound. */
    public static final Codec<AlcoholItemPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid").forGetter(AlcoholItemPredicate::fluid),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("quality", MinMaxBounds.Doubles.ANY).forGetter(AlcoholItemPredicate::quality)
    ).apply(instance, AlcoholItemPredicate::new));

    /** The registry entry payload; {@code rustic:alcohol} maps to this codec. */
    public static final ItemSubPredicate.Type<AlcoholItemPredicate> TYPE = new ItemSubPredicate.Type<>(CODEC);

    @Override
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        FluidStack content = ItemFluidBottle.getFluidContent(stack);
        if (content.isEmpty()) {
            return false;
        }
        Fluid contained = content.getFluid();
        if (!(contained instanceof RusticBooze booze)) {
            return false;
        }
        // Legacy: (this.fluid != null && this.fluid != fluid) -> reject. The registry gives one Fluid instance per id.
        if (this.fluid.isPresent() && this.fluid.get() != contained) {
            return false;
        }
        // Legacy: (this.quality != null && !this.quality.test(quality)) -> reject; ANY matches everything when unset.
        return this.quality.matches(booze.getQuality(content));
    }
}
