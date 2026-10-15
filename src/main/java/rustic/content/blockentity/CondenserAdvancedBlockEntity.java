package rustic.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.menu.CondenserAdvancedMenu;

/**
 * The advanced condenser's functional half, the 1.21 form of legacy {@code TileEntityCondenserAdvancedBottom}: the
 * seven-slot inventory (result / fuel / bottle / modifier / three ingredients) and the shared eight-bucket water tank
 * behind {@link CondenserBlockEntityBase}. It is the lower block of the two-block advanced machine and owns the whole
 * inventory; the upper half ({@link rustic.content.blocks.CondenserAdvancedBlock} with {@code top=true}) carries no
 * block entity and forwards the player's click down here, so there is a single logical inventory rather than a
 * {@code CompoundContainer} merge.
 *
 * <p>Against the basic machine it differs in exactly two shape answers: it has a modifier slot (the legacy
 * {@code SLOT_INGREDIENTS_START}) and it will run any recipe the {@link CondenserRecipe} modifier gate accepts.
 */
public class CondenserAdvancedBlockEntity extends CondenserBlockEntityBase {

    /** Legacy {@code TileEntityCondenserAdvancedBottom.SLOT_NUM}. */
    public static final int SLOT_NUM = 7;

    /** The modifier herb sits at the first "ingredients start" slot; the three real ingredients follow it. */
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
        // The modifier gate in CondenserRecipe.matches already keeps a basic recipe from taking a loaded modifier and
        // an advanced recipe from running without one, so no extra filter is needed here (mirrors legacy).
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

    /** Server tick entry point the block's ticker calls for the lower (non-top) half. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CondenserAdvancedBlockEntity be) {
        be.tick();
    }
}
