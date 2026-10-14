package rustic.content.fluids;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.potions.RusticMobEffects;

/**
 * The base of the seven Rustic boozes, ported from 1.12.2's {@code FluidBooze}. A booze is a
 * {@link RusticDrinkableFluid} whose drink effect is derived from a per-{@link FluidStack} quality value:
 * drinking it always rolls for the {@code rustic:tipsy} effect (via {@link #inebriate}) and then applies the
 * booze-specific {@link #affectPlayer} outcome, exactly the two-step {@code onDrank} the legacy class ran.
 *
 * <p><b>Quality storage deviation.</b> Legacy read a {@code "Quality"} FLOAT tag off the fluid stack's NBT.
 * 1.21 replaces that with the typed {@code rustic:quality} data component (see {@link RusticDataComponents}).
 * The observable contract the golden {@code BoozeQualityBaselineTest} pins is preserved: a stack with no
 * quality reads as {@code 0.0F} (the baseline default, deliberately <em>not</em> {@code 0.5F} the way the plan
 * prose suggested), and every read is clamped to {@code [0, 1]}.</p>
 *
 * <p><b>Booze fluid shape.</b> The boozes are source-only and self-referencing — no flowing pair and no block
 * — so they can live inside barrels and bottles but never as a placed fluid, matching the baseline's
 * {@code getBlock() == null} expectation for all seven plus vanta oil.</p>
 */
public class RusticBooze extends RusticDrinkableFluid {

    /** Legacy {@code FluidBooze.QUALITY_NBT_KEY}; kept named so the port's intent stays traceable. */
    public static final String QUALITY_NBT_KEY = "Quality";

    protected float inebriationChance = 0.5F;

    public RusticBooze(Properties properties) {
        super(properties, null);
    }

    public RusticBooze setInebriationChance(float chance) {
        this.inebriationChance = chance;
        return this;
    }

    public float getInebriationChance() {
        return inebriationChance;
    }

    @Override
    public void onDrank(ServerLevel level, Player player, ItemStack stack, FluidStack fluid) {
        float quality = getQuality(fluid);
        inebriate(level, player, quality);
        affectPlayer(level, player, quality);
    }

    /** The per-booze outcome. Empty here, exactly as legacy {@code FluidBooze.affectPlayer} was for an anonymous booze. */
    protected void affectPlayer(ServerLevel level, Player player, float quality) {
    }

    /**
     * The clamped quality read off a stack. Only the {@code rustic:quality} component counts; absent or unset
     * reads as {@code 0.0F}, and the raw value is clamped to {@code [0, 1]} — the two golden behaviours.
     */
    public float getQuality(FluidStack fluid) {
        Float stored = fluid.get(RusticDataComponents.QUALITY.get());
        float quality = stored == null ? 0F : stored;
        return Math.max(Math.min(quality, 1), 0);
    }

    /**
     * The tipsy roll and stacking, ported verbatim: the duration curve, the {@code chanceMod} that thins the
     * roll around {@code quality == 0.75}, and the amplifier ladder that tops out at 3. Ambrosia overrides this.
     */
    protected void inebriate(ServerLevel level, Player player, float quality) {
        int duration = (quality >= 0.5F)
                ? ((int) (12000 * (Math.max(1 - Math.abs(quality - 0.75F), 0.5F))))
                : ((int) (12000 * (Math.max(1 - (quality * 0.5F), 0.5F))));
        float inebriationChanceMod = Math.max(Math.min(1 - Math.abs(0.67F * (quality - 0.75F)), 1), 0);
        MobEffectInstance tipsyEffect = player.getEffect(RusticMobEffects.TIPSY);
        if (level.random.nextFloat() < this.inebriationChance * inebriationChanceMod) {
            if (tipsyEffect == null) {
                player.addEffect(new MobEffectInstance(RusticMobEffects.TIPSY, duration, 0, false, false), null);
            } else if (tipsyEffect.getAmplifier() < 3) {
                player.addEffect(new MobEffectInstance(RusticMobEffects.TIPSY,
                        Math.max(duration, tipsyEffect.getDuration()), tipsyEffect.getAmplifier() + 1,
                        false, false), null);
            }
        }
    }

    /**
     * The quality tier -> label colour mapping the legacy client {@code ClientUtils.getQualityLabelColor} used
     * for the bottle tooltip. It is plain arithmetic with no client dependency, so it lives here server-safe;
     * the {@code RusticBoozeQualityTest} pins the exact {@code 0.89999F}/{@code 0.69999F}/{@code 0.5F}/{@code
     * 0.35F}/{@code 0.2F} bounds, so they are written as literals rather than rounded.
     */
    public static int getQualityLabelColor(float quality) {
        if (quality >= 0.89999F) {
            return 0xC29311; // highest tier, gold
        } else if (quality >= 0.69999F) {
            return 0x4A7ABD; // high tier, blue
        } else if (quality >= 0.5F) {
            return 0xDBD3BD; // high-ish tier, parchment
        } else if (quality >= 0.35F) {
            return 0xDBD3BD; // low-ish tier, parchment
        } else if (quality >= 0.2F) {
            return 0xBAA911; // low tier, yellow
        } else {
            return 0x222222; // lowest tier, black
        }
    }

    /**
     * Whether an active effect counts as a "bad" one for the boost/cull loops the wine boozes run. Legacy asked
     * {@code Potion.isBadEffect()}; 1.21 keys the same idea off {@link MobEffectCategory}, harmful being bad.
     */
    protected static boolean isBad(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }
}
