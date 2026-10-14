package rustic.content.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rustic.content.blockentity.VaseBlockEntity;

/**
 * 1.12.2's {@code BlockVase}: a small stone-ceramic display block wrapping a 27-slot
 * {@code TileEntityVase}, with thirteen visual {@code variant}s selected by the item's damage value.
 *
 * <p>Per the confirmed Phase 4 scope this ports the behaviour and scaffolds the variant, not the art. The
 * legacy {@code variant}/{@code damageDropped}/{@code getStateFromMeta} trio was a metadata-based item
 * system (one block item, thirteen sub-styles drawn from its damage); 1.21 replaces that with the block
 * state, so the property is declared here as an {@link IntegerProperty} {@code VARIANT} ({@code 0}..{@code 12})
 * and defaults to {@code 0}. The client-to-server packet that flipped every vase in the inventory to a
 * chosen style ({@code MessageVaseMeta}) belonged to the vase's craft/GUI, so it - and the thirteen real
 * models it selected between - arrive with the recipe and art phases (7 and 9); until then every state draws
 * one placeholder cube, exactly like the cabinet's deferred doors.
 *
 * <p>Two legacy details that do translate: the block was {@code Material.ROCK} at hardness 0.5 (so
 * {@link RusticBlockProperties#stone(float)} and a pickaxe tag, not the axe the wooden barrel/cabinet use),
 * and its bounding box was inset to {@code 0.125..0.875} on the horizontal axes rather than a full cube -
 * kept as {@link #VASE_SHAPE} so a vase you can see through the corners is still one you bump into the
 * corners of. Unlike the cabinet, opening it was never front-gated, so {@link BlockRusticChest}'s any-face
 * {@code useWithoutItem} is inherited unchanged.
 */
public class VaseBlock extends BlockRusticChest {

    public static final MapCodec<VaseBlock> CODEC = simpleCodec(VaseBlock::new);

    /** 1.12.2 {@code BlockVase.VARIANT}: the thirteen display styles, {@code MIN_VARIANT}..{@code MAX_VARIANT}. */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 12);

    /** 1.12.2 {@code VASE_AABB}, the inset bounding box ({@code 0.125, 0, 0.125} to {@code 0.875, 1, 0.875}). */
    private static final VoxelShape VASE_SHAPE = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);

    public VaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2's default state was variant 0 (brown ceramic); the variant-selection packet is deferred.
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    public MapCodec<VaseBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 1.12.2 createBlockState(): just VARIANT. The thirteen styles are listed but all draw one
        // placeholder model until the art phase, so they are scaffolding for the deferred variants.
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
}
