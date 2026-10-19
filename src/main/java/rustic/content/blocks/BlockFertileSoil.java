package rustic.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;

public class BlockFertileSoil extends Block {

    public BlockFertileSoil(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public TriState canSustainPlant(
            BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant
    ) {

        if (plant.is(Blocks.NETHER_WART) || plant.getBlock() instanceof WaterlilyBlock) {
            return TriState.FALSE;
        }

        Block block = plant.getBlock();

        boolean groundPlant = block instanceof BushBlock

                || block instanceof CropBlock

                || block instanceof CactusBlock
                || block instanceof SugarCaneBlock;

        return groundPlant ? TriState.TRUE : TriState.DEFAULT;
    }
}
