package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import rustic.content.blockentity.VaseBlockEntity;

public class VaseBlock extends BlockRusticChest {

    public static final MapCodec<VaseBlock> CODEC = simpleCodec(VaseBlock::new);

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 12);

    public static final int MIN_VARIANT = 0, MAX_VARIANT = 12;

    public static final int[] ITEM_ORDER = {
            0,

            5,

            6,

            2,

            1,

            7,

            4,

            3,

            8,

            9,

            10,

            12,

            11,

    };

    private static final int VARIANT_COUNT = MAX_VARIANT - MIN_VARIANT + 1;

    private static final VoxelShape VASE_SHAPE = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);

    public VaseBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    public MapCodec<VaseBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(VARIANT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VASE_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaseBlockEntity(pos, state);
    }

    public static int nextVariant(int current, int offset) {
        for (int i = 0; i < ITEM_ORDER.length; i++) {
            if (ITEM_ORDER[i] == current) {
                return ITEM_ORDER[((i + offset) % VARIANT_COUNT + VARIANT_COUNT) % VARIANT_COUNT];
            }
        }
        return ((current + offset) % VARIANT_COUNT + VARIANT_COUNT) % VARIANT_COUNT;
    }

    public static ItemStack variantStack(int variant) {
        ItemStack stack = new ItemStack(RusticMachineBlocks.VASE.get());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(VARIANT, variant));
        return stack;
    }

    public static int variantOf(ItemStack stack) {
        Integer variant = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(VARIANT);
        return variant == null ? 0 : variant;
    }

    @Override
    @SuppressWarnings("deprecation")

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {

        return variantStack(state.getValue(VARIANT));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {

        List<ItemStack> drops = super.getDrops(state, params);
        int variant = state.getValue(VARIANT);
        if (variant != 0) {
            BlockItemStateProperties blockState = BlockItemStateProperties.EMPTY.with(VARIANT, variant);
            for (ItemStack drop : drops) {
                if (drop.is(state.getBlock().asItem())) {
                    drop.set(DataComponents.BLOCK_STATE, blockState);
                }
            }
        }
        return drops;
    }
}
