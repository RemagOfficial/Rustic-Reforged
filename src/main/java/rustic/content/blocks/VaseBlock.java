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

/**
 * 1.12.2's {@code BlockVase}: a small stone-ceramic display block wrapping a 27-slot
 * {@code TileEntityVase}, with thirteen visual {@code variant}s selected by the item's damage value.
 *
 * <p>Per the confirmed Phase 4 scope this ports the behaviour and scaffolds the variant, not the art. The
 * legacy {@code variant}/{@code damageDropped}/{@code getStateFromMeta} trio was a metadata-based item
 * system (one block item, thirteen sub-styles drawn from its damage); 1.21 replaces that with the block
 * state, so the property is declared here as an {@link IntegerProperty} {@code VARIANT} ({@code 0}..{@code 12})
 * and defaults to {@code 0}.
 *
 * <p>Phase 16 restores the variant picker. 1.12.2's {@code EventHandlerClient.onVaseMouseWheel} recoloured every
 * vase stack in the player's inventories (main + off hand) through {@code MessageVaseMeta} while the player sneaked
 * and scrolled the wheel holding the block item; the chosen style rode the item's damage value. 1.21 drops
 * per-item metadata, so the picked variant lives in the stack's {@link DataComponents#BLOCK_STATE} component,
 * which {@code BlockItem} applies to the placed block automatically - the faithful replacement for legacy's
 * {@code getStateForPlacement(meta)}. {@link #nextVariant(int, int)} reproduces the wheel-stepping arithmetic
 * verbatim (the {@link #ITEM_ORDER} table branch and the raw wrap-around fallback); the packet is
 * {@code RusticPayloads.VaseVariantPayload} and the scroll listener is {@code RusticClient.onVaseMouseWheel}.
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

    /** 1.12.2 {@code BlockVase.MIN_VARIANT}/{@code MAX_VARIANT}. */
    public static final int MIN_VARIANT = 0, MAX_VARIANT = 12;

    /**
     * 1.12.2 {@code BlockVase.ITEM_ORDER} verbatim: the order the mouse wheel cycles the styles through, which is
     * deliberately not the numeric variant order (brown, brown-horse, white, white-blue, white-brown, light gray,
     * dark gray, green, then the four alt-model styles). The variant values it lists are a full permutation of
     * {@code 0..12}.
     */
    public static final int[] ITEM_ORDER = {
            0,  // brown ceramic
            5,  // brown ceramic w/ horse design
            6,  // white ceramic
            2,  // white ceramic w/ blue pattern
            1,  // white ceramic w/ brown pattern
            7,  // light gray ceramic
            4,  // dark gray ceramic
            3,  // green ceramic
            8,  // alt model brown ceramic
            9,  // alt model white ceramic
            10, // alt model white ceramic w/ blue pattern
            12, // alt model medium gray ceramic
            11, // alt model dark gray ceramic
    };

    /** Count of variants, for the wrap-around maths. */
    private static final int VARIANT_COUNT = MAX_VARIANT - MIN_VARIANT + 1;

    /** 1.12.2 {@code VASE_AABB}, the inset bounding box ({@code 0.125, 0, 0.125} to {@code 0.875, 1, 0.875}). */
    private static final VoxelShape VASE_SHAPE = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);

    public VaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // 1.12.2's default state was variant 0 (brown ceramic).
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    public MapCodec<VaseBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // 1.12.2 createBlockState(): just VARIANT, and every one of the thirteen values now resolves to its
        // own authored model (vase_0..12 statics) through the generated blockstate.
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

    /**
     * The wheel arithmetic of legacy {@code EventHandlerClient.onVaseMouseWheel}, as a pure function so a test
     * can pin it without a mouse. {@code offset} is the signed notch count (the old {@code dwheel / 120}).
     * If {@code current} is one of the cycled styles - always true for a legal variant, since {@link #ITEM_ORDER}
     * is a permutation of 0..12 - the step walks the ITEM_ORDER table and wraps inside it; the raw 0..12
     * wrap-around is kept for the legacy else-branch (an out-of-table damage value, unreachable through the
     * picker but reachable through old saves). Negative offsets scroll the other way; both branches normalise
     * with the {@code ((x % n) + n) % n} idiom exactly as the 1.12 handler did.
     */
    public static int nextVariant(int current, int offset) {
        for (int i = 0; i < ITEM_ORDER.length; i++) {
            if (ITEM_ORDER[i] == current) {
                return ITEM_ORDER[((i + offset) % VARIANT_COUNT + VARIANT_COUNT) % VARIANT_COUNT];
            }
        }
        return ((current + offset) % VARIANT_COUNT + VARIANT_COUNT) % VARIANT_COUNT;
    }

    /**
     * A vase block item carrying {@code variant}, the 1.21 form of the legacy "item damage is the variant".
     * {@code BlockItem}'s placement applies the {@link DataComponents#BLOCK_STATE} component to the placed
     * state for free, so no {@code getStateForPlacement} override is needed.
     */
    public static ItemStack variantStack(int variant) {
        ItemStack stack = new ItemStack(RusticMachineBlocks.VASE.get());
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(VARIANT, variant));
        return stack;
    }

    /** The variant a stack will place, or 0 when it carries no {@link DataComponents#BLOCK_STATE}. */
    public static int variantOf(ItemStack stack) {
        Integer variant = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(VARIANT);
        return variant == null ? 0 : variant;
    }

    @Override
    @SuppressWarnings("deprecation") // NeoForge marks this deprecated in favour of its own hook, but this
    // three-argument form is still the vanilla call path pick-block goes through in 1.21.1.
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        // Middle-click pick-block must copy the placed style, matching legacy's damage-based ItemBlock.
        return variantStack(state.getValue(VARIANT));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // Vanilla 1.21 drops go through the block's loot table, which knows nothing about variants (legacy's
        // equivalent was damageDropped()). Decorating the looted vase stacks here keeps breaking a green vase
        // green instead of silently converting it to brown ceramic.
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
