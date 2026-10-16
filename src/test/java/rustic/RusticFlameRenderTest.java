package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import rustic.content.blocks.BlockCandle;
import rustic.content.blocks.BlockCandleDouble;
import rustic.content.blocks.BlockCandleLever;
import rustic.content.blocks.BlockChandelier;
import rustic.content.blocks.BlockIronTorch;
import rustic.content.blocks.BlockLantern;
import rustic.content.blocks.BlockWoodLantern;
import rustic.content.blocks.RusticBlocks;

/**
 * Headless guard for Phase 9 slice S9.3 (the chandelier / candle / lantern flame render).
 *
 * <p><b>Finding:</b> 1.12.2 drew these flames with no {@code TileEntitySpecialRenderer} and no custom
 * baked model at all - each lit block just called {@code setLightLevel(1.0F)} and emitted a
 * {@code randomDisplayTick} flame (candles also smoke) particle, while the chandelier overrode
 * {@code randomDisplayTick} with an empty body to suppress {@code BlockFalling}'s falling-dust spawn.
 * The faithful 1.21 form is therefore pure data + the vanilla particle hook, and it was already wired
 * during the block port: {@code RusticBlockProperties.lit(...)} sets {@code lightLevel(state -> 15)}
 * and each block overrides {@code animateTick} to add the {@code ParticleTypes.FLAME}. Nothing was
 * added in S9.3; this test only locks that contract so a later slice cannot silently drop a flame.
 *
 * <ol>
 *   <li>every lit mount reports block-light 15 on its default state, and the chandelier reports 0
 *       (1.12.2 never called {@code setLightLevel} on it);</li>
 *   <li>every one of these classes overrides {@code animateTick} somewhere in the {@code rustic} tree
 *       - including the chandelier, whose override is deliberately empty.</li>
 * </ol>
 */
public class RusticFlameRenderTest {

    /** The lit flame mounts, i.e. everything here except the chandelier (which is unlit by design). */
    @Test
    public void litFlameMountsEmitFullBlockLight() {
        assertEquals(15, light(RusticBlocks.CANDLE.get()), "candle");
        assertEquals(15, light(RusticBlocks.CANDLE_DOUBLE.get()), "candle_double");
        assertEquals(15, light(RusticBlocks.CANDLE_LEVER.get()), "candle_lever");
        assertEquals(15, light(RusticBlocks.IRON_TORCH.get()), "iron_torch");
        assertEquals(15, light(RusticBlocks.IRON_LANTERN.get()), "iron_lantern");
        assertEquals(15, light(RusticBlocks.GOLDEN_LANTERN.get()), "golden_lantern");
        assertEquals(15, light(RusticBlocks.LANTERN_WOOD.get()), "lantern_wood");
        assertEquals(15, light(RusticBlocks.CANDLE_GOLD.get()), "candle_gold");
        assertEquals(15, light(RusticBlocks.CANDLE_DOUBLE_GOLD.get()), "candle_double_gold");
        assertEquals(15, light(RusticBlocks.CANDLE_LEVER_GOLD.get()), "candle_lever_gold");

        if (RusticBlocks.silverDecorEnabled()) {
            RusticBlocks.SilverDecor silver = RusticBlocks.SILVER_DECOR;
            assertEquals(15, light(silver.candle().get()), "candle_silver");
            assertEquals(15, light(silver.candleDouble().get()), "candle_double_silver");
            assertEquals(15, light(silver.candleLever().get()), "candle_lever_silver");
            assertEquals(15, light(silver.lantern().get()), "silver_lantern");
        }
    }

    /** 1.12.2 built the chandelier as unlit iron, so it must not gain a light level here. */
    @Test
    public void chandelierIsDeliberatelyUnlit() {
        assertEquals(0, light(RusticBlocks.CHANDELIER.get()), "legacy chandelier never called setLightLevel");
    }

    @Test
    public void everyFlameBlockOverridesAnimateTick() {
        assertOverridesAnimateTick(BlockCandle.class);
        assertOverridesAnimateTick(BlockCandleDouble.class);
        assertOverridesAnimateTick(BlockCandleLever.class);
        assertOverridesAnimateTick(BlockIronTorch.class);
        assertOverridesAnimateTick(BlockLantern.class);
        assertOverridesAnimateTick(BlockWoodLantern.class);
        // The chandelier's override is intentionally empty (it suppresses falling-block dust), but it
        // still has to exist, or BlockFalling's own animateTick would leak particles into 1.21 worlds.
        assertOverridesAnimateTick(BlockChandelier.class);
    }

    private static int light(Block block) {
        return block.defaultBlockState().getLightEmission();
    }

    private static void assertOverridesAnimateTick(Class<? extends Block> blockClass) {
        assertTrue(declaresAnimateTick(blockClass),
                blockClass.getSimpleName() + " must override animateTick to drive its flame particles");
    }

    /** Walks up from {@code blockClass} and reports whether a {@code rustic} class declares the hook. */
    private static boolean declaresAnimateTick(Class<?> blockClass) {
        Class<?>[] params = {BlockState.class, Level.class, BlockPos.class, RandomSource.class};
        for (Class<?> k = blockClass; k != null && k.getName().startsWith("rustic."); k = k.getSuperclass()) {
            Method method = findDeclared(k, params);
            if (method != null) {
                return true;
            }
        }
        return false;
    }

    private static Method findDeclared(Class<?> k, Class<?>[] params) {
        try {
            return k.getDeclaredMethod("animateTick", params);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
