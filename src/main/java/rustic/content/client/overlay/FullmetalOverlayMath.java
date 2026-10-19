package rustic.content.client.overlay;

import net.minecraft.util.Mth;

public final class FullmetalOverlayMath {

    public static final float BASE_ALPHA = 0.9625F;

    public static final int FADE_THRESHOLD_TICKS = 100;

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
