package rustic.content.fluids;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * The still (source) half of a Rustic drinkable fluid. 1.12.2 modelled drinkability by extending
 * {@code Fluid} in the abstract {@code FluidDrinkable}; 1.21 has no plain drinkable fluid concept, so the
 * drinkable fluids are {@link BaseFlowingFluid.Source}s that also implement {@link DrinkableFluid}.
 *
 * <p>Two shapes use this class:</p>
 * <ul>
 *   <li><b>The eight placeable fluids</b> (olive oil, the four juices, ale wort, honey, golden apple juice)
 *   pair a source here with a {@link RusticFlowingFluid} and get their flowing block in Slice 3.</li>
 *   <li><b>Vanta oil</b> is source-only, self-referencing (its {@code still} and {@code flowing} suppliers both
 *   point back at itself) and with no block, which is how the port reproduces the 1.12.2 baseline
 *   "vanta oil is bucket-able but has no flowing block".</li>
 * </ul>
 *
 * <p>The {@link Drink} behaviour is the ported legacy {@code onDrank} body, passed in as a lambda because
 * these fluids do not subclass per-fluid the way the boozes do — their drink effect is a one-shot body with
 * nothing to override.</p>
 */
public class RusticDrinkableFluid extends BaseFlowingFluid.Source implements DrinkableFluid {

    @Nullable
    private final Drink drink;

    public RusticDrinkableFluid(Properties properties, @Nullable Drink drink) {
        super(properties);
        this.drink = drink;
    }

    @Override
    public void onDrank(ServerLevel level, Player player, ItemStack stack, FluidStack fluid) {
        if (drink != null) {
            drink.onDrank(level, player, stack, fluid);
        }
    }

    /** The one-shot drink effect a legacy {@code FluidDrinkable} subclass expressed as an {@code onDrank} override. */
    @FunctionalInterface
    public interface Drink {
        void onDrank(ServerLevel level, Player player, ItemStack stack, FluidStack fluid);
    }
}
