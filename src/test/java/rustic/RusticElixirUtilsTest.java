package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rustic.content.fluids.RusticDataComponents;
import rustic.content.potions.ElixirUtils;

public class RusticElixirUtilsTest {

    @Test
    public void effectsRoundTripThroughTheComponentInOrder() {
        ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
        assertTrue(ElixirUtils.getEffects(stack).isEmpty(), "a bare stack must store no effects");

        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1), stack);
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 0), stack);

        List<MobEffectInstance> effects = ElixirUtils.getEffects(stack);
        assertEquals(2, effects.size());
        assertSame(MobEffects.MOVEMENT_SPEED.value(), effects.get(0).getEffect().value());
        assertEquals(100, effects.get(0).getDuration());
        assertEquals(1, effects.get(0).getAmplifier());
        assertSame(MobEffects.HEAL.value(), effects.get(1).getEffect().value());
        assertEquals(1, effects.get(1).getDuration());
        assertEquals(0, effects.get(1).getAmplifier());

        List<MobEffectInstance> stored = stack.get(RusticDataComponents.ELIXIR_EFFECTS.get());
        assertNotNull(stored, "the effects live in the rustic:elixir_effects component");
        assertEquals(2, stored.size());
    }

    @Test
    public void colorDefaultsToThePinnedWaterBottleBlue() {
        assertEquals(16253176, ElixirUtils.getColor(new ItemStack(Items.GLASS_BOTTLE)));
    }

    @Test
    public void colorIsTheFirstStoredEffects() {
        ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0), stack);

        assertEquals(9154528, ElixirUtils.getColor(stack));
        assertEquals(MobEffects.MOVEMENT_SLOWDOWN.value().getColor(), ElixirUtils.getColor(stack));

        ItemStack two = new ItemStack(Items.GLASS_BOTTLE);
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0), two);
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0), two);
        assertEquals(MobEffects.MOVEMENT_SLOWDOWN.value().getColor(), ElixirUtils.getColor(two));

        ItemStack strength = new ItemStack(Items.GLASS_BOTTLE);
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0), strength);
        assertEquals(16762624, ElixirUtils.getColor(strength));
    }

    @Test
    public void vantaOilRoundTripsThroughTheComponent() {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ElixirUtils.setVantaOilEffect(sword, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));

        MobEffectInstance read = ElixirUtils.getVantaOilEffect(sword);
        assertNotNull(read);
        assertSame(MobEffects.DAMAGE_BOOST.value(), read.getEffect().value());
        assertEquals(600, read.getDuration());
        assertEquals(1, read.getAmplifier());
    }

    @Test
    public void setVantaOilEffectRejectsUslessEffects() {

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        assertNull(ElixirUtils.getVantaOilEffect(sword));

        ElixirUtils.setVantaOilEffect(sword, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        ElixirUtils.setVantaOilEffect(sword, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 0, 1));
        assertNull(ElixirUtils.getVantaOilEffect(sword), "an expired stash must not survive over a live one");

        ElixirUtils.setVantaOilEffect(sword, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, -1));
        MobEffectInstance clamped = ElixirUtils.getVantaOilEffect(sword);
        assertNotNull(clamped);
        assertEquals(0, clamped.getAmplifier());

        ElixirUtils.setVantaOilEffect(sword, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1, 0));
        assertNotNull(ElixirUtils.getVantaOilEffect(sword));
        assertEquals(1, ElixirUtils.getVantaOilEffect(sword).getDuration());
        assertEquals(0, ElixirUtils.getVantaOilEffect(sword).getAmplifier());
    }

    @Test
    public void clearingVantaOilLeavesEverythingElseIntact() {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("rusticTestMarker"));

        ElixirUtils.setVantaOilEffect(sword, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        assertEquals(600, ElixirUtils.getVantaOilEffect(sword).getDuration());

        ElixirUtils.setVantaOilEffect(sword, null);

        assertNull(ElixirUtils.getVantaOilEffect(sword));
        assertEquals(Component.literal("rusticTestMarker"), sword.get(DataComponents.CUSTOM_NAME),
                "the untouched component must survive the vanta oil removal");
    }

    @Test
    public void vantaOilIsNeverWrittenToEmptyStacks() {

        ElixirUtils.setVantaOilEffect(ItemStack.EMPTY, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        assertNull(ElixirUtils.getVantaOilEffect(ItemStack.EMPTY));
        ElixirUtils.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0), ItemStack.EMPTY);
        assertTrue(ElixirUtils.getEffects(ItemStack.EMPTY).isEmpty());
    }

    @Test
    public void remainingVantaUsesGroupsDurationIntoThirtySecondCharges() {

        assertEquals(300, ElixirUtils.VANTA_OIL_EFFECT_DURATION);
        assertEquals(400, ElixirUtils.VANTA_OIL_EFFECT_MAX_DURATION);

        assertEquals(0, uses(0));
        assertEquals(1, uses(1));
        assertEquals(1, uses(250));
        assertEquals(1, uses(300));
        assertEquals(1, uses(350));
        assertEquals(2, uses(401));
        assertEquals(2, uses(600));
        assertEquals(2, uses(700));
        assertEquals(3, uses(701));
    }

    @Test
    public void instantPotionDurationIsCountedAsOneUsePerTick() {

        assertEquals(12, ElixirUtils.getRemainingVantaUses(
                new MobEffectInstance(MobEffects.HEAL, 12, 0)));
    }

    @Test
    public void nextVantaHitDurationSplitsOnlyOversizedStashes() {
        assertEquals(250, ElixirUtils.getNextVantaHitDuration(250));
        assertEquals(400, ElixirUtils.getNextVantaHitDuration(400));
        assertEquals(300, ElixirUtils.getNextVantaHitDuration(401));
        assertEquals(300, ElixirUtils.getNextVantaHitDuration(600));
    }

    private static int uses(int duration) {
        return ElixirUtils.getRemainingVantaUses(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
    }
}
