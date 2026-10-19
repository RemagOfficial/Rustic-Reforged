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

public class RusticCondenserPairingTest {

    @Test
    public void advancedCondenserIsATwoBlockColumnDefaultingToTheBottomHalf() {
        CondenserAdvancedBlock block = (CondenserAdvancedBlock) RusticMachineBlocks.CONDENSER_ADVANCED.get();
        BlockState state = block.defaultBlockState();
        assertTrue(state.hasProperty(CondenserAdvancedBlock.TOP), "the advanced condenser lost its TOP property");
        assertFalseTop(state, "a freshly declared block is the inventory-owning bottom half");
    }

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

    @Test
    public void theBottomBlockEntityBacksTheWholeColumnInventory() {
        CondenserAdvancedBlockEntity be = new CondenserAdvancedBlockEntity(BlockPos.ZERO,
                RusticMachineBlocks.CONDENSER_ADVANCED.get().defaultBlockState());
        assertSame(RusticBlockEntities.CONDENSER_ADVANCED.get(), be.getType(),
                "the block entity reports the registered advanced-condenser type");
        assertEquals(7, be.getItemHandler().getSlots(), "result/fuel/bottle/modifier/three ingredients in one inventory");
        assertEquals(8 * 1000, be.getTank().getCapacity(), "the shared water tank holds eight buckets");

        assertSame(RusticMenus.CONDENSER_ADVANCED.get(),
                BuiltInRegistries.MENU.get(RusticMenus.CONDENSER_ADVANCED.getId()),
                "the advanced-condenser container's menu type is registered under its holder id");
    }

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
