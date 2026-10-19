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

public record AlcoholItemPredicate(Optional<Fluid> fluid, MinMaxBounds.Doubles quality) implements ItemSubPredicate {

    public static final Codec<AlcoholItemPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid").forGetter(AlcoholItemPredicate::fluid),
            MinMaxBounds.Doubles.CODEC.optionalFieldOf("quality", MinMaxBounds.Doubles.ANY).forGetter(AlcoholItemPredicate::quality)
    ).apply(instance, AlcoholItemPredicate::new));

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

        if (this.fluid.isPresent() && this.fluid.get() != contained) {
            return false;
        }

        return this.quality.matches(booze.getQuality(content));
    }
}
