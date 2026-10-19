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

    public static final DeferredBlock<HoneyFluidBlock> HONEY =
            BLOCKS.register("honey", () -> new HoneyFluidBlock(RusticFluids.HONEY.value(), waterLike()));

    public static final DeferredBlock<GoldenAppleJuiceFluidBlock> GOLDEN_APPLE_JUICE =
            BLOCKS.register("golden_apple_juice",
                    () -> new GoldenAppleJuiceFluidBlock(RusticFluids.GOLDEN_APPLE_JUICE.value(), waterLike()));

    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return BLOCKS.getEntries();
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

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
