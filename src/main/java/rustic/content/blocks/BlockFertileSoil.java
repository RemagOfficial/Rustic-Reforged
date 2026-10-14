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

/**
 * 1.12.2's {@code BlockFertileSoil}: a block of dirt-shaped ground that every plant accepts and that
 * crops grow faster on.
 *
 * <p>1.12.2 answered both questions through Forge, and 1.21.1 answers both through NeoForge with the
 * same two hooks, so the port is close to one-to-one:
 * <ul>
 *   <li>{@code isFertile(World, BlockPos)} is {@link net.neoforged.neoforge.common.extensions.IBlockExtension#isFertile},
 *   still read by {@code CropBlock} for its one-in-five bonus growth tick;</li>
 *   <li>{@code canSustainPlant(..., IPlantable)} is
 *   {@link net.neoforged.neoforge.common.extensions.IBlockExtension#canSustainPlant}, which now hands over
 *   the plant's {@link BlockState} instead of an {@code IPlantable} and answers
 *   {@link TriState} instead of a boolean - {@code TRUE} forces the plant to be accepted,
 *   {@code FALSE} forces it to be refused and {@code DEFAULT} leaves the decision to the plant's own
 *   {@code canSurvive}, which is what the legacy {@code super.canSustainPlant} call was.</li>
 * </ul>
 *
 * <p>The legacy switch was over {@code EnumPlantType}, which no longer exists, so the seven cases are
 * recovered from the plant's block class. That mapping is lossless here because the switch covered every
 * type with a constant answer: {@code Desert}, {@code Crop}, {@code Cave}, {@code Plains} and
 * {@code Beach} were true, {@code Nether} and {@code Water} were false. It also means the two refusals
 * have to be tested first - a waterlily is a {@link BushBlock} and nether wart is a
 * {@link CropBlock}, so both would otherwise fall into the accepting branch.
 *
 * <p>Plants that did not exist in 1.12.2 and so had no plant type to be refused by - the dripleaves,
 * bamboo, mangrove propagule and pitcher crop - are {@code BushBlock} or {@code CropBlock} subclasses and
 * are therefore accepted. That is the one behaviour change, and it follows from the mod's own rule
 * ("fertile soil takes anything that grows on ground") rather than from a decision here.
 */
public class BlockFertileSoil extends Block {

    public BlockFertileSoil(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** 1.12.2's {@code isFertile}: always, which is the whole point of the block. */
    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** 1.12.2's {@code canSustainPlant} switch, read out of the plant's block class instead of its plant type. */
    @Override
    public TriState canSustainPlant(
            BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant
    ) {
        // The legacy Nether and Water cases, tested before the accepting branch because both plants are
        // subclasses of one of the classes below. 1.21 registers the waterlily as Blocks.LILY_PAD, so the
        // class is the question rather than the id.
        if (plant.is(Blocks.NETHER_WART) || plant.getBlock() instanceof WaterlilyBlock) {
            return TriState.FALSE;
        }

        Block block = plant.getBlock();
        // Plains, Cave and Desert's dead bush are BushBlock subclasses; saplings and mushrooms too.
        boolean groundPlant = block instanceof BushBlock
                // Crop: wheat, carrots, potatoes, beetroots and the melon/pumpkin stems.
                || block instanceof CropBlock
                // Desert's cactus and Beach's sugar cane are their own trees of classes.
                || block instanceof CactusBlock
                || block instanceof SugarCaneBlock;

        return groundPlant ? TriState.TRUE : TriState.DEFAULT;
    }
}
