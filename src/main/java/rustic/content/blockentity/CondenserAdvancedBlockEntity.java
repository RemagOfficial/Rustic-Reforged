package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.menu.CondenserAdvancedMenu;

public class CondenserAdvancedBlockEntity extends CondenserBlockEntityBase {

    public static final int SLOT_NUM = 7;

    public static final int SLOT_MODIFIER = SLOT_INGREDIENTS_START;

    private static final int[] INGREDIENTS = {SLOT_INGREDIENTS_START + 1, SLOT_INGREDIENTS_START + 2,
            SLOT_INGREDIENTS_START + 3};

    public CondenserAdvancedBlockEntity(BlockPos pos, BlockState state) {
        super(RusticBlockEntities.CONDENSER_ADVANCED.get(), pos, state, SLOT_NUM);
    }

    @Override
    protected int[] ingredientSlots() {
        return INGREDIENTS;
    }

    @Override
    protected int modifierSlot() {
        return SLOT_MODIFIER;
    }

    @Override
    protected boolean recipeAllowed(CondenserRecipe recipe) {

        return true;
    }

    @Override
    protected String getMenuTitleKey() {
        return "container.rustic.condenser_advanced";
    }

    @Override
    protected AbstractContainerMenu createCondenserMenu(int containerId, Inventory playerInventory) {
        return new CondenserAdvancedMenu(containerId, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CondenserAdvancedBlockEntity be) {
        be.tick();
    }
}
