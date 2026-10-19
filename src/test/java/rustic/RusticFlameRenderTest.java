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

public class RusticFlameRenderTest {

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

        assertOverridesAnimateTick(BlockChandelier.class);
    }

    private static int light(Block block) {
        return block.defaultBlockState().getLightEmission();
    }

    private static void assertOverridesAnimateTick(Class<? extends Block> blockClass) {
        assertTrue(declaresAnimateTick(blockClass),
                blockClass.getSimpleName() + " must override animateTick to drive its flame particles");
    }

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
