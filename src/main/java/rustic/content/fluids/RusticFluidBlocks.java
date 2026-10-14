package rustic.content.fluids;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

import java.util.Collection;

/**
 * The eight placeable Rustic fluid blocks, ported from 1.12.2's {@code ModFluids.BLOCK_*} pair of
 * {@code BlockFluidRustic}s.
 *
 * <p>1.12.2's {@code BlockFluidRustic} extended Forge's {@code BlockFluidClassic} and hand-implemented the
 * whole fluid-block contract: {@code isOpaqueCube}/{@code isFullCube}/{@code getRenderType}/
 * {@code getStateFromMeta}/{@code shouldSideBeRendered}/{@code canFlowInto}/{@code displaceIfPossible} and a
 * custom {@code FluidStateMapper} model. None of that is ported: 1.21's own {@link LiquidBlock} derives every
 * one of those answers from the flowing fluid it wraps, so the legacy class collapses to a constructor call.
 * Only the two blocks that overrode {@code onEntityCollidedWithBlock} keep a subclass of their own.</p>
 *
 * <p>The legacy {@code setQuantaPerBlock} values (olive oil and ale wort 4, juices and golden 6, honey 2) are
 * the closest thing 1.21 has to honey's "thick" look. Modern flowing fluids are fixed at 8 levels, so the
 * quanta numbers have no field to land on; the visible level split is a Phase 9 render concern and the flow
 * sluggishness is already carried by the fluids' tick rates (see {@link RusticFluids}). This is recorded as a
 * deviation for the {@code RusticFluidRegistrationTest} to assert against the water-like material instead.</p>
 */
public final class RusticFluidBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    public static final DeferredBlock<LiquidBlock> OLIVE_OIL =
            BLOCKS.register("olive_oil", () -> new LiquidBlock(RusticFluids.OLIVE_OIL.value(), waterLike()));
    public static final DeferredBlock<LiquidBlock> IRONBERRY_JUICE =
            BLOCKS.register("ironberry_juice", () -> new LiquidBlock(RusticFluids.IRONBERRY_JUICE.value(), waterLike()));
    public static final DeferredBlock<LiquidBlock> WILDBERRY_JUICE =
            BLOCKS.register("wildberry_juice", () -> new LiquidBlock(RusticFluids.WILDBERRY_JUICE.value(), waterLike()));
    public static final DeferredBlock<LiquidBlock> GRAPE_JUICE =
            BLOCKS.register("grape_juice", () -> new LiquidBlock(RusticFluids.GRAPE_JUICE.value(), waterLike()));
    public static final DeferredBlock<LiquidBlock> APPLE_JUICE =
            BLOCKS.register("apple_juice", () -> new LiquidBlock(RusticFluids.APPLE_JUICE.value(), waterLike()));
    public static final DeferredBlock<LiquidBlock> ALE_WORT =
            BLOCKS.register("ale_wort", () -> new LiquidBlock(RusticFluids.ALE_WORT.value(), waterLike()));

    /** Legacy {@code BLOCK_HONEY.onEntityCollidedWithBlock}: motionX/Y/Z each halved while an entity is inside. */
    public static final DeferredBlock<HoneyFluidBlock> HONEY =
            BLOCKS.register("honey", () -> new HoneyFluidBlock(RusticFluids.HONEY.value(), waterLike()));

    /** Legacy {@code BLOCK_GOLDEN_APPLE_JUICE.onEntityCollidedWithBlock}: regeneration IV pulse every 10 ticks. */
    public static final DeferredBlock<GoldenAppleJuiceFluidBlock> GOLDEN_APPLE_JUICE =
            BLOCKS.register("golden_apple_juice",
                    () -> new GoldenAppleJuiceFluidBlock(RusticFluids.GOLDEN_APPLE_JUICE.value(), waterLike()));

    /** Every fluid block, for the block-item derivation loop in {@code RusticItems}. */
    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return BLOCKS.getEntries();
    }

    /** Attaches the fluid-block register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    /**
     * The shared water-like block properties, copied from vanilla's {@code Blocks.WATER} exactly: the legacy
     * blocks all used {@code Material.WATER}, and this is the 1.21 expression of that material plus the
     * non-opaque / non-full-cube / model-render facts the baseline checked.
     */
    private static BlockBehaviour.Properties waterLike() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }

    private RusticFluidBlocks() {
    }

    /**
     * Honey slows anything standing in it, the way legacy multiplied the entity's motion by {@code 0.5F} in
     * {@code onEntityCollidedWithBlock}. Applied server-side only (motion is authoritative there), a deliberate
     * narrowing from legacy which ran the multiply on both sides.
     */
    static final class HoneyFluidBlock extends LiquidBlock {
        HoneyFluidBlock(net.minecraft.world.level.material.FlowingFluid fluid, BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!level.isClientSide) {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5D, 0.5D, 0.5D));
            }
        }
    }

    /**
     * Golden apple juice regenerates living entities standing in it, once every ten ticks, exactly the legacy
     * {@code ticksExisted % 10 == 0} pulse (1.21's equivalent of {@code ticksExisted} is {@code tickCount}).
     */
    static final class GoldenAppleJuiceFluidBlock extends LiquidBlock {
        GoldenAppleJuiceFluidBlock(net.minecraft.world.level.material.FlowingFluid fluid,
                                   BlockBehaviour.Properties properties) {
            super(fluid, properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!level.isClientSide && entity instanceof LivingEntity living && (living.tickCount % 10) == 0) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, true), null);
            }
        }
    }
}
