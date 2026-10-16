package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import rustic.content.client.overlay.FullmetalOverlayMath;

/**
 * Baseline guard for {@link FullmetalOverlayMath}, the pure alpha ramp behind the S9.6 fullmetal first-person
 * overlay. Values are transcribed from the 1.12.2 {@code EventHandlerClient.onRenderFullmetalOverlayEvent} so a
 * refactor of the cos-flicker formula fails here rather than only shifting the in-game fade.
 */
public class FullmetalOverlayMathTest {

    @Test
    void baseAlphaAndThresholdMatchLegacy() {
        assertEquals(0.9625F, FullmetalOverlayMath.BASE_ALPHA, 0.0F, "legacy baseAlpha constant");
        assertEquals(100, FullmetalOverlayMath.FADE_THRESHOLD_TICKS, "fade starts under 5 seconds");
    }

    @Test
    void fullFadeIsFlatWhilePlentyOfDurationRemains() {
        // >= 100 ticks left -> the cos pulse is bypassed entirely.
        assertEquals(1.0F, FullmetalOverlayMath.durationFade(100, 0.0F), 1e-6F, "exactly at threshold holds flat");
        assertEquals(1.0F, FullmetalOverlayMath.durationFade(101, 0.5F), 1e-6F, "above threshold holds flat");
        assertEquals(1.0F, FullmetalOverlayMath.durationFade(6000, 0.0F), 1e-6F, "long effect holds flat");
    }

    @Test
    void vanishesAtTheFinalTick() {
        // dur = 0 -> sqrt(0.25)=0.5 then eased in by sqrt(dur/20)=0 -> fully transparent at the very end.
        assertEquals(0.0F, FullmetalOverlayMath.durationFade(0, 0.0F), 1e-6F, "effect expiry fades to nothing");
    }

    @Test
    void midFadeStaysBoundedAndSmooth() {
        // Every sub-threshold sample must be a finite alpha in [0, 1]; the cos pulse is scaled so it never exceeds 1.
        for (int tick = 1; tick < FullmetalOverlayMath.FADE_THRESHOLD_TICKS; tick++) {
            float fade = FullmetalOverlayMath.durationFade(tick, 0.5F);
            assertFalse(Float.isNaN(fade), "fade must not be NaN at tick " + tick);
            assertTrue(fade >= -1e-6F && fade <= 1.0F + 1e-6F, "fade out of [0,1] at tick " + tick + ": " + fade);
        }
        // A representative mid-ramp value (tick 50) sits high in the range, matching the legacy 0.75 -> sqrt ~= 0.866.
        assertEquals(0.866F, FullmetalOverlayMath.durationFade(50, 0.0F), 0.01F, "tick 50 pulse height");
    }
}
