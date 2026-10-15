package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import rustic.content.blockentity.CondenserAdvancedBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;
import rustic.content.blocks.CondenserAdvancedBlock;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.menu.RusticMenus;

/**
 * Phase 4b gate for the advanced condenser's two-block pairing. Unlike the double cabinet - where each half keeps its
 * own 27 slots and a {@code CompoundContainer} merges them into 54 at {@code createMenu} time - the advanced condenser
 * runs the legacy {@code TileEntityCondenserAdvancedTop} model: the whole seven-slot inventory, water tank and brew
 * timers live in the <em>bottom</em> half's block entity, and the top half carries no block entity at all, only
 * forwarding a player's click down. There is therefore one logical inventory, and the headless-observable invariant is
 * "exactly one half owns a block entity".
 *
 * <p>Runs in ModDevGradle's FML unit-test JVM so the block/BE/menu registries resolve and block entities construct.
 * The live "clicking either half opens the same bottom container" behaviour still needs a ticking {@code Level} and is
 * a live-game check; what is pinned here is the state shape and the {@code newBlockEntity} contract that guarantee it.
 */
public class RusticCondenserPairingTest {

    /** The block is the two-block TOP-property column and defaults to the inventory-owning bottom half. */
    @Test
    public void advancedCondenserIsATwoBlockColumnDefaultingToTheBottomHalf() {
        CondenserAdvancedBlock block = (CondenserAdvancedBlock) RusticMachineBlocks.CONDENSER_ADVANCED.get();
        BlockState state = block.defaultBlockState();
        assertTrue(state.hasProperty(CondenserAdvancedBlock.TOP), "the advanced condenser lost its TOP property");
        assertFalseTop(state, "a freshly declared block is the inventory-owning bottom half");
    }

    /**
     * The single-inventory invariant: the bottom state produces a block entity, the top state produces none. This is
     * exactly what makes the two halves share one logical container - only one half can own the slots and tank.
     */
    @Test
    public void onlyTheBottomHalfOwnsABlockEntity() {
        CondenserAdvancedBlock block = (CondenserAdvancedBlock) RusticMachineBlocks.CONDENSER_ADVANCED.get();

        BlockState bottom = block.defaultBlockState().setValue(CondenserAdvancedBlock.TOP, false);
        BlockEntity bottomEntity = block.newBlockEntity(BlockPos.ZERO, bottom);
        assertNotNull(bottomEntity, "the bottom half must own a block entity");
        assertTrue(bottomEntity instanceof CondenserAdvancedBlockEntity,
                "the bottom block entity is the advanced condenser's container");

        BlockState top = block.defaultBlockState().setValue(CondenserAdvancedBlock.TOP, true);
        assertNull(block.newBlockEntity(BlockPos.ZERO, top),
                "the top half carries no block entity - it forwards to the bottom, so there is one inventory");
    }

    /** The bottom block entity is the seven-slot, eight-bucket container the whole column runs on. */
    @Test
    public void theBottomBlockEntityBacksTheWholeColumnInventory() {
        CondenserAdvancedBlockEntity be = new CondenserAdvancedBlockEntity(BlockPos.ZERO,
                RusticMachineBlocks.CONDENSER_ADVANCED.get().defaultBlockState());
        assertSame(RusticBlockEntities.CONDENSER_ADVANCED.get(), be.getType(),
                "the block entity reports the registered advanced-condenser type");
        assertEquals(7, be.getItemHandler().getSlots(), "result/fuel/bottle/modifier/three ingredients in one inventory");
        assertEquals(8 * 1000, be.getTank().getCapacity(), "the shared water tank holds eight buckets");

        // The bottom container opens on the advanced-condenser menu type. Building that menu needs a live player
        // Inventory and Player - the in-game pass the plan defers - so pin the registered menu type the container
        // resolves to rather than construct it headlessly.
        assertSame(RusticMenus.CONDENSER_ADVANCED.get(),
                BuiltInRegistries.MENU.get(RusticMenus.CONDENSER_ADVANCED.getId()),
                "the advanced-condenser container's menu type is registered under its holder id");
    }

    /** The pairing's block entity type is registered under the block id and valid for both halves of the column. */
    @Test
    public void theBlockEntityTypeIsValidForTheColumn() {
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE
                .get(RusticMachineBlocks.CONDENSER_ADVANCED.getId());
        assertNotNull(type, "no block entity type registered for the advanced condenser");
        assertTrue(type.getValidBlocks().contains(RusticMachineBlocks.CONDENSER_ADVANCED.get()),
                "the advanced-condenser block entity type is not valid for the block");
    }

    private static void assertFalseTop(BlockState state, String message) {
        assertEquals(false, state.getValue(CondenserAdvancedBlock.TOP), message);
    }
}
