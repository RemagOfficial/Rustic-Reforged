package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import rustic.content.items.ItemElixir;
import rustic.content.items.RusticItems;
import rustic.content.potions.ElixirUtils;

public class ElixirTooltipDurationTest {

    @Test
    public void aTimedEffectCarriesTheDurationWrapper() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.REGENERATION, 900, 0);
        MutableComponent line = tooltipLine(effect);

        assertEquals("potion.withDuration", key(line), "a 45-second effect must be wrapped");
        Object[] args = contents(line).getArgs();
        assertEquals(2, args.length, "potion.withDuration takes the name and the duration");
        assertEquals(MobEffects.REGENERATION.value().getDescriptionId(),
                key((Component) args[0]), "the wrapped component is the effect name");
        assertEquals("00:45", ((Component) args[1]).getString(), "900 ticks at factor 1.0 / 20 tps is 45s");
        assertEquals(blue(), line.getStyle(), "regeneration is a benign effect");
    }

    @Test
    public void amplifierAndDurationCombine() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1);
        MutableComponent line = tooltipLine(effect);

        assertEquals("potion.withDuration", key(line), "a 3-minute effect is wrapped");
        Object[] args = contents(line).getArgs();
        MutableComponent name = (MutableComponent) args[0];
        assertEquals(MobEffects.DAMAGE_BOOST.value().getDescriptionId(), key(name),
                "the inner component still leads with the effect name");

        boolean potency = name.getSiblings().stream().anyMatch(sibling -> "potion.potency.1".equals(key(sibling)));
        assertTrue(potency, "amplifier 1 must append potion.potency.1");
        assertEquals("03:00", ((Component) args[1]).getString(), "3600 ticks is three minutes");
        assertEquals(blue(), line.getStyle(), "strength is a benign effect");
    }

    @Test
    public void anInstantEffectShowsNoDuration() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.HEAL, 1, 0);
        MutableComponent line = tooltipLine(effect);

        assertEquals(MobEffects.HEAL.value().getDescriptionId(), key(line),
                "the heal line stays the bare effect name");
        assertFalse("potion.withDuration".equals(key(line)), "an instant effect must not be wrapped");
    }

    @Test
    public void aHarmfulEffectIsRed() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.WITHER, 900, 0);
        MutableComponent line = tooltipLine(effect);

        assertEquals("potion.withDuration", key(line), "wither is timed, so it is wrapped");
        assertEquals(red(), line.getStyle(), "wither is HARMFUL on both versions");
    }

    @Test
    public void anEmptyElixirSaysNoEffect() {
        ItemStack empty = new ItemStack(RusticItems.ELIXIR.get());
        List<Component> tooltip = new ArrayList<>();
        ((ItemElixir) empty.getItem()).appendHoverText(empty, null, tooltip, TooltipFlag.Default.NORMAL);

        assertEquals(1, tooltip.size(), "an empty elixir prints exactly one line");
        assertEquals("effect.none", key((Component) tooltip.get(0)), "the legacy empty branch");
        assertEquals(gray(), ((MutableComponent) tooltip.get(0)).getStyle(), "effect.none is gray");
    }

    private static MutableComponent tooltipLine(MobEffectInstance effect) {
        ItemStack stack = new ItemStack(RusticItems.ELIXIR.get());
        ElixirUtils.addEffect(effect, stack);
        List<Component> tooltip = new ArrayList<>();
        ((ItemElixir) stack.getItem()).appendHoverText(stack, null, tooltip, TooltipFlag.Default.NORMAL);
        assertEquals(1, tooltip.size(), "one stored effect must print exactly one line");
        return (MutableComponent) tooltip.get(0);
    }

    private static TranslatableContents contents(Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents,
                "expected a translatable component but got " + component.getContents());
        return (TranslatableContents) component.getContents();
    }

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    private static Style blue() {
        return Component.literal("x").withStyle(ChatFormatting.BLUE).getStyle();
    }

    private static Style red() {
        return Component.literal("x").withStyle(ChatFormatting.RED).getStyle();
    }

    private static Style gray() {
        return Component.literal("x").withStyle(ChatFormatting.GRAY).getStyle();
    }
}
