package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.menu.CondenserMenu;

/**
 * The basic condenser, the 1.21 form of legacy {@code TileEntityCondenser}: a single block behind a five-slot
 * inventory (result / fuel / bottle / two ingredients) and the shared eight-bucket water tank, all driven by
 * {@link CondenserBlockEntityBase}. It runs only non-advanced {@link CondenserRecipe}s and offers an empty modifier
 * to the matcher, so the recipe's EMPTY modifier gate is what keeps advanced recipes out.
 */
public class CondenserBlockEntity extends CondenserBlockEntityBase {

    /** Legacy {@code TileEntityCondenser.SLOT_NUM}. */
    public static final int SLOT_NUM = 5;

    private static final int[] INGREDIENTS = {SLOT_INGREDIENTS_START, SLOT_INGREDIENTS_START + 1};

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.CONDENSER.get(), pos, state, SLOT_NUM);
    }

    @Override
    protected int[] ingredientSlots() {
        return INGREDIENTS;
    }

    @Override
    protected int modifierSlot() {
        return -1;
    }

    @Override
    protected boolean recipeAllowed(CondenserRecipe recipe) {
        return !recipe.isAdvanced();
    }

    @Override
    protected String getMenuTitleKey() {
        return "container.rustic.condenser";
    }

    @Override
    protected AbstractContainerMenu createCondenserMenu(int containerId, Inventory playerInventory) {
        return new CondenserMenu(containerId, playerInventory, this);
    }

    /** Server tick entry point the block's ticker calls (see {@link CondenserBlock#getTicker}). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CondenserBlockEntity be) {
        be.tick();
    }
}
