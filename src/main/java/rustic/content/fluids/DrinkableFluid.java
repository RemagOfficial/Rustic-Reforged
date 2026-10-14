package rustic.content.fluids;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Implemented by the drinkable Rustic fluids. 1.12.2 declared this as the abstract
 * {@code FluidDrinkable#onDrank(World, EntityPlayer, ItemStack, FluidStack)} override point that
 * {@code ItemFluidBottle.finishUsingItem} called; the port keeps the same dispatch shape but narrows the
 * world argument to {@link ServerLevel}, because every body writes to the player's food/effect state and so
 * runs server-side only.
 *
 * <p>The {@code stack} argument is the bottle (or other container) item being drunk from; the {@code fluid}
 * argument is the exact {@link FluidStack} consumed, which carries the {@code rustic:quality} component the
 * booze subclasses read.</p>
 */
public interface DrinkableFluid {

    void onDrank(ServerLevel level, Player player, ItemStack stack, FluidStack fluid);
}
