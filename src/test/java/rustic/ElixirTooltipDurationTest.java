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

/**
 * Bug-fix gate (2026-10-19) for the elixir tooltip: legacy {@code ElixirUtils.addPotionTooltip} printed the
 * effect name, then a potency suffix when the amplifier was above zero, then the duration whenever the effect
 * lasted longer than 20 ticks - so the instant-health elixir showed no clock while regeneration showed 45
 * seconds. Vanilla 1.21 builds that same line as {@code potion.withDuration{name, MobEffectUtil
 * .formatDuration(effect, 1.0F, 20.0F)}} inside {@code PotionContents.addPotionTooltip}, and {@code
 * ItemElixir.appendHoverText} now mirrors it (the wrapper only when {@code !effect.endsWithin(20)}).
 *
 * <p>The tooltip is asserted <em>structurally</em> - reading the {@link TranslatableContents} key/args and the
 * {@link Style} colour directly - because headless there is no language map, so the resolved text is not
 * available. The clock value is pinned separately through {@code MobEffectUtil.formatDuration}: on this
 * version {@code StringUtil.formatTickDuration} returns the zero-padded {@code "%02d:%02d"} with no
 * parentheses (the {@code " (%s)"} grouping comes from the lang template, not the value).
 */
public class ElixirTooltipDurationTest {

    /** A timed effect gets the vanilla duration wrapper; the inner component stays the plain effect name. */
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

    /** Potency and duration stack the way legacy nested them: withDuration(name + " potion.potency.N", time). */
    @Test
    public void amplifierAndDurationCombine() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 1);
        MutableComponent line = tooltipLine(effect);

        assertEquals("potion.withDuration", key(line), "a 3-minute effect is wrapped");
        Object[] args = contents(line).getArgs();
        MutableComponent name = (MutableComponent) args[0];
        assertEquals(MobEffects.DAMAGE_BOOST.value().getDescriptionId(), key(name),
                "the inner component still leads with the effect name");
        // The port appends " potion.potency.N" as siblings, legacy's s1 += " " + ... form.
        boolean potency = name.getSiblings().stream().anyMatch(sibling -> "potion.potency.1".equals(key(sibling)));
        assertTrue(potency, "amplifier 1 must append potion.potency.1");
        assertEquals("03:00", ((Component) args[1]).getString(), "3600 ticks is three minutes");
        assertEquals(blue(), line.getStyle(), "strength is a benign effect");
    }

    /** An instant effect (legacy's duration <= 20 check, vanilla's endsWithin(20)) shows no clock at all. */
    @Test
    public void anInstantEffectShowsNoDuration() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.HEAL, 1, 0);
        MutableComponent line = tooltipLine(effect);

        assertEquals(MobEffects.HEAL.value().getDescriptionId(), key(line),
                "the heal line stays the bare effect name");
        assertFalse("potion.withDuration".equals(key(line)), "an instant effect must not be wrapped");
    }

    /** A harmful effect is red - the legacy isBadEffect / vanilla MobEffectCategory.HARMFUL branch. */
    @Test
    public void aHarmfulEffectIsRed() {
        MobEffectInstance effect = new MobEffectInstance(MobEffects.WITHER, 900, 0);
        MutableComponent line = tooltipLine(effect);

        assertEquals("potion.withDuration", key(line), "wither is timed, so it is wrapped");
        assertEquals(red(), line.getStyle(), "wither is HARMFUL on both versions");
    }

    /** The empty elixir keeps the legacy gray "effect.none" line. */
    @Test
    public void anEmptyElixirSaysNoEffect() {
        ItemStack empty = new ItemStack(RusticItems.ELIXIR.get());
        List<Component> tooltip = new ArrayList<>();
        ((ItemElixir) empty.getItem()).appendHoverText(empty, null, tooltip, TooltipFlag.Default.NORMAL);

        assertEquals(1, tooltip.size(), "an empty elixir prints exactly one line");
        assertEquals("effect.none", key((Component) tooltip.get(0)), "the legacy empty branch");
        assertEquals(gray(), ((MutableComponent) tooltip.get(0)).getStyle(), "effect.none is gray");
    }

    // ------------------------------------------------------------------ helpers

    /** The single styled tooltip line the item produces for a stack carrying exactly {@code effect}. */
    private static MutableComponent tooltipLine(MobEffectInstance effect) {
        ItemStack stack = new ItemStack(RusticItems.ELIXIR.get());
        ElixirUtils.addEffect(effect, stack);
        List<Component> tooltip = new ArrayList<>();
        ((ItemElixir) stack.getItem()).appendHoverText(stack, null, tooltip, TooltipFlag.Default.NORMAL);
        assertEquals(1, tooltip.size(), "one stored effect must print exactly one line");
        return (MutableComponent) tooltip.get(0);
    }

    /** The contents of a translatable component (asserts the assumption that it is one). */
    private static TranslatableContents contents(Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents,
                "expected a translatable component but got " + component.getContents());
        return (TranslatableContents) component.getContents();
    }

    /** The lang key of a translatable component, or {@code ""} for a non-translatable one (a literal sibling). */
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
