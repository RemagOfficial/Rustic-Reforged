package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import rustic.content.client.overlay.FullmetalOverlayMath;

public class FullmetalOverlayMathTest {

    @Test
    void baseAlphaAndThresholdMatchLegacy() {
        assertEquals(0.9625F, FullmetalOverlayMath.BASE_ALPHA, 0.0F, "legacy baseAlpha constant");
        assertEquals(100, FullmetalOverlayMath.FADE_THRESHOLD_TICKS, "fade starts under 5 seconds");
    }

    @Test
    void fullFadeIsFlatWhilePlentyOfDurationRemains() {

        assertEquals(1.0F, FullmetalOverlayMath.durationFade(100, 0.0F), 1e-6F, "exactly at threshold holds flat");
        assertEquals(1.0F, FullmetalOverlayMath.durationFade(101, 0.5F), 1e-6F, "above threshold holds flat");
        assertEquals(1.0F, FullmetalOverlayMath.durationFade(6000, 0.0F), 1e-6F, "long effect holds flat");
    }

    @Test
    void vanishesAtTheFinalTick() {

        assertEquals(0.0F, FullmetalOverlayMath.durationFade(0, 0.0F), 1e-6F, "effect expiry fades to nothing");
    }

    @Test
    void midFadeStaysBoundedAndSmooth() {

        for (int tick = 1; tick < FullmetalOverlayMath.FADE_THRESHOLD_TICKS; tick++) {
            float fade = FullmetalOverlayMath.durationFade(tick, 0.5F);
            assertFalse(Float.isNaN(fade), "fade must not be NaN at tick " + tick);
            assertTrue(fade >= -1e-6F && fade <= 1.0F + 1e-6F, "fade out of [0,1] at tick " + tick + ": " + fade);
        }

        assertEquals(0.866F, FullmetalOverlayMath.durationFade(50, 0.0F), 0.01F, "tick 50 pulse height");
    }
}
