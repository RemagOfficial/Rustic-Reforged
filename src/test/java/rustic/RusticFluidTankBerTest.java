package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.junit.jupiter.api.Test;
import rustic.content.blockentity.CrushingTubBlockEntity;
import rustic.content.blockentity.EvaporatingBasinBlockEntity;
import rustic.content.blockentity.LiquidBarrelBlockEntity;
import rustic.content.client.renderer.CrushingTubRenderer;
import rustic.content.client.renderer.EvaporatingBasinRenderer;
import rustic.content.client.renderer.FluidTankRenderUtil;
import rustic.content.client.renderer.LiquidBarrelRenderer;

/**
 * Headless guard for Phase 9 slice S9.1 (the fluid-tank BERs). The three renderers are {@code runClient}-only
 * visuals, so the test asserts the two parts that are checkable without a GL context:
 *
 * <ol>
 *   <li>each renderer is a {@link BlockEntityRenderer} bound, by its generic type argument, to the block entity its
 *       {@code BlockEntityType} actually holds - the same pairing {@code RusticClient.registerRenderers} feeds to
 *       {@code registerBlockEntityRenderer};</li>
 *   <li>the extracted {@code amount/capacity} surface-height maths reproduces the legacy per-block constants.</li>
 * </ol>
 */
public class RusticFluidTankBerTest {

    @Test
    public void crushingTubBerBindsItsBlockEntity() {
        assertBerBinding(CrushingTubRenderer.class, CrushingTubBlockEntity.class);
    }

    @Test
    public void evaporatingBasinBerBindsItsBlockEntity() {
        assertBerBinding(EvaporatingBasinRenderer.class, EvaporatingBasinBlockEntity.class);
    }

    @Test
    public void liquidBarrelBerBindsItsBlockEntity() {
        assertBerBinding(LiquidBarrelRenderer.class, LiquidBarrelBlockEntity.class);
    }

    /** Empty tank sits at the base; full reaches base+scale; a half tank lands exactly in between. */
    @Test
    public void surfaceHeightMatchesLegacyFillMath() {
        // Crushing tub: y = 0.0625 + 0.5 * amount/capacity.
        assertEquals(0.0625F, FluidTankRenderUtil.surfaceHeight(0, 8000, 0.0625F, 0.5F), 0.0F);
        assertEquals(0.5625F, FluidTankRenderUtil.surfaceHeight(8000, 8000, 0.0625F, 0.5F), 0.0F);
        assertEquals(0.3125F, FluidTankRenderUtil.surfaceHeight(4000, 8000, 0.0625F, 0.5F), 0.0F);
        // Liquid barrel: y = 0.125 + 0.8125 * amount/capacity.
        assertEquals(0.125F, FluidTankRenderUtil.surfaceHeight(0, 16000, 0.125F, 0.8125F), 0.0F);
        assertEquals(0.9375F, FluidTankRenderUtil.surfaceHeight(16000, 16000, 0.125F, 0.8125F), 0.0F);
        // Evaporating basin: y = 0.0625 + 0.1875 * amount/capacity.
        assertEquals(0.0625F, FluidTankRenderUtil.surfaceHeight(0, 6000, 0.0625F, 0.1875F), 0.0F);
        assertEquals(0.25F, FluidTankRenderUtil.surfaceHeight(6000, 6000, 0.0625F, 0.1875F), 0.0F);
    }

    private static void assertBerBinding(Class<?> renderer, Class<?> expectedBlockEntity) {
        for (Type iface : renderer.getGenericInterfaces()) {
            if (iface instanceof ParameterizedType parameterized
                    && parameterized.getRawType() == BlockEntityRenderer.class) {
                Type argument = parameterized.getActualTypeArguments()[0];
                assertNotNull(argument);
                assertSame(expectedBlockEntity, argument,
                        renderer.getSimpleName() + " must render " + expectedBlockEntity.getSimpleName());
                return;
            }
        }
        fail(renderer.getSimpleName() + " does not directly implement BlockEntityRenderer<"
                + expectedBlockEntity.getSimpleName() + ">");
    }
}
