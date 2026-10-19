package rustic.content.fluids;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.content.potions.RusticMobEffects;

public class RusticBooze extends RusticDrinkableFluid {

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

    protected void affectPlayer(ServerLevel level, Player player, float quality) {
    }

    public float getQuality(FluidStack fluid) {
        Float stored = fluid.get(RusticDataComponents.QUALITY.get());
        float quality = stored == null ? 0F : stored;
        return Math.max(Math.min(quality, 1), 0);
    }

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

    public static int getQualityLabelColor(float quality) {
        if (quality >= 0.89999F) {
            return 0xC29311;

        } else if (quality >= 0.69999F) {
            return 0x4A7ABD;

        } else if (quality >= 0.5F) {
            return 0xDBD3BD;

        } else if (quality >= 0.35F) {
            return 0xDBD3BD;

        } else if (quality >= 0.2F) {
            return 0xBAA911;

        } else {
            return 0x222222;

        }
    }

    protected static boolean isBad(MobEffectInstance effect) {
        return effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
    }
}
