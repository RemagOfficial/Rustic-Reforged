package rustic.content.client.overlay;

import net.minecraft.util.Mth;

/**
 * Pure transparency maths for the S9.6 fullmetal first-person overlay, lifted out of the renderer so the
 * legacy duration-fade ramp can be reasoned about (and unit-tested) without a screen.
 *
 * <p>Transcribed verbatim from the 1.12.2 {@code EventHandlerClient.onRenderFullmetalOverlayEvent}: while the
 * effect has more than five seconds ({@code >= 100} ticks) left it draws at a flat {@link #BASE_ALPHA}; in the
 * final five seconds it pulses via a {@code cos} ramp (a "flicker out" cue) that also eases the leading
 * {@code dur < 20} ticks in from nothing. {@code partialTick} smooths the pulse between ticks exactly as the
 * legacy {@code event.getPartialTicks()} did.
 */
public final class FullmetalOverlayMath {

    /** The legacy {@code baseAlpha} the overlay sits at for the bulk of the effect's duration. */
    public static final float BASE_ALPHA = 0.9625F;

    /** Below this many remaining ticks the cos-flicker fade kicks in (5 seconds). */
    public static final int FADE_THRESHOLD_TICKS = 100;

    /**
     * The multiplicative fade applied to {@link #BASE_ALPHA}. Returns {@code 1.0} while the effect has
     * {@code >= FADE_THRESHOLD_TICKS} ticks left; otherwise the legacy pulsing ramp, in {@code [0, ~1]}.
     *
     * @param durationTicks the effect's current {@link net.minecraft.world.effect.MobEffectInstance#getDuration()}
     * @param partialTick   the render partial tick, for smoothing between whole ticks
     */
    public static float durationFade(int durationTicks, float partialTick) {
        if (durationTicks >= FADE_THRESHOLD_TICKS) {
            return 1.0F;
        }
        float dur = (durationTicks + partialTick) * 2F;
        float j1 = 10 - dur / 20;
        float fade = 0.5F
                + Mth.clamp(dur / 400F, 0.0F, 0.9375F - 0.5F)
                + (Mth.cos((200 - dur) * (float) Math.PI / 200F * 9F)
                        * Mth.clamp(j1 / 10.0F * 0.25F, 0.0625F, 0.25F));
        fade = Mth.sqrt(fade);
        if (dur < 20F) {
            fade *= Mth.sqrt(dur / 20F);
        }
        return fade;
    }

    private FullmetalOverlayMath() {
    }
}
