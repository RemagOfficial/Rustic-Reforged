package rustic.content.fluids;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

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

    @FunctionalInterface
    public interface Drink {
        void onDrank(ServerLevel level, Player player, ItemStack stack, FluidStack fluid);
    }
}
