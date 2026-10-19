package rustic.content.fluids;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public interface DrinkableFluid {

    void onDrank(ServerLevel level, Player player, ItemStack stack, FluidStack fluid);
}
