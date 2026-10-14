package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import rustic.content.potions.BlazingTrailEffect;
import rustic.content.potions.FeatherEffect;
import rustic.content.potions.FullmetalEffect;
import rustic.content.potions.IronSkinEffect;
import rustic.content.potions.MarkerEffect;
import rustic.content.potions.RusticMobEffects;
import rustic.content.potions.ShameEffect;
import rustic.content.potions.TipsyEffect;

/**
 * Phase 6 characterisation test for the eleven Rustic potion effects ported from 1.12.2's
 * {@code PotionsRustic}. It runs in ModDevGradle's FML unit-test JVM, so {@code BuiltInRegistries} is
 * populated and the deferred holders resolve.
 *
 * <p>What is pinned here is the contract the rest of the phase (and the golden baseline tests) leans on:
 * the eleven registry ids are the legacy paths verbatim, each holder resolves to the same object in the
 * live registry, the six behavioural effects are their own {@link MobEffect} subclasses while the five
 * marker effects are the shared {@link MarkerEffect}, and the two attribute-carrying effects
 * ({@link FullmetalEffect}, {@link IronSkinEffect}) expose exactly the modifier set the legacy
 * {@code registerPotionAttributeModifier} calls produced — including the deliberate amplifier-scaling
 * split the port records (fullmetal's amounts are flat at every amplifier, ironskin's scale by
 * {@code amplifier + 1}).
 *
 * <p>Per-tick behaviour ({@code applyEffectTick}) is not exercised: it needs a ticking {@code Level} and a
 * real {@code LivingEntity}, which is a live-game check, and the plan parks in-game feel until the
 * post-Phase-11 comparison. This is a registration + arithmetic gate, not a simulation.
 */
public class RusticPotionRegistrationTest {

    private static final List<String> LEGACY_IDS = List.of(
            "ironskin", "feather", "blazing_trail", "shame", "fullmetal", "tipsy",
            "fire_power", "full", "magic_resistance", "wither_ward", "undying");

    // ------------------------------------------------------------------ the eleven ids

    /**
     * Every effect resolves in the vanilla mob-effect registry under its legacy path, and Rustic owns
     * exactly these eleven — no more (nothing was registered twice), no fewer (the count is the ledger).
     */
    @Test
    public void allElevenEffectsAreRegisteredUnderTheirLegacyIds() {
        List<String> registered = new ArrayList<>();
        for (ResourceLocation key : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (key.getNamespace().equals(Rustic.MODID)) {
                registered.add(key.getPath());
            }
        }
        assertEquals(LEGACY_IDS.size(), registered.size(),
                "Rustic registered " + registered + ", expected the eleven legacy ids");
        for (String id : LEGACY_IDS) {
            assertTrue(registered.contains(id), "the legacy effect id rustic:" + id + " is not registered");
        }
    }

    /** Each deferred holder is backed by the identical instance the registry holds. */
    @Test
    public void everyDeferredHolderResolvesToTheRegisteredInstance() {
        for (var holder : List.of(RusticMobEffects.IRON_SKIN, RusticMobEffects.FEATHER,
                RusticMobEffects.BLAZING_TRAIL, RusticMobEffects.SHAME, RusticMobEffects.FULLMETAL,
                RusticMobEffects.TIPSY, RusticMobEffects.FIRE_POWER, RusticMobEffects.FULL,
                RusticMobEffects.MAGIC_RESISTANCE, RusticMobEffects.WITHER_WARD, RusticMobEffects.UNDYING)) {
            MobEffect effect = holder.value();
            assertSame(effect, BuiltInRegistries.MOB_EFFECT.get(holder.getId()),
                    holder.getId() + " does not resolve to its deferred effect");
            assertEquals(Rustic.MODID, holder.getId().getNamespace(), holder.getId() + " has the wrong namespace");
        }
    }

    // ------------------------------------------------------------------ class shape: 6 behavioural + 5 marker

    /**
     * The six effects that tick are their own subclasses; the five whose logic lives in the potion event
     * handler are the shared {@link MarkerEffect}. Guarding the concrete types keeps a later slice from
     * silently registering a bare marker where a behaviour was ported, or vice-versa.
     */
    @Test
    public void theBehaviouralEffectsAreTypedAndTheMarkersAreMarkerEffect() {
        assertSame(IronSkinEffect.class, RusticMobEffects.IRON_SKIN.value().getClass());
        assertSame(FeatherEffect.class, RusticMobEffects.FEATHER.value().getClass());
        assertSame(BlazingTrailEffect.class, RusticMobEffects.BLAZING_TRAIL.value().getClass());
        assertSame(ShameEffect.class, RusticMobEffects.SHAME.value().getClass());
        assertSame(FullmetalEffect.class, RusticMobEffects.FULLMETAL.value().getClass());
        assertSame(TipsyEffect.class, RusticMobEffects.TIPSY.value().getClass());

        for (var marker : List.of(RusticMobEffects.FIRE_POWER, RusticMobEffects.FULL,
                RusticMobEffects.MAGIC_RESISTANCE, RusticMobEffects.WITHER_WARD, RusticMobEffects.UNDYING)) {
            assertSame(MarkerEffect.class, marker.value().getClass(),
                    marker.getId() + " must be the shared MarkerEffect");
        }
    }

    // ------------------------------------------------------------------ colour + category (legacy PotionBase args)

    /** Colours are the 1.12.2 {@code PotionBase(isBad, colour, name)} arguments, category the isBad flag. */
    @Test
    public void coloursAndCategoriesMatchTheLegacyPotionBaseArguments() {
        assertColourAndCategory(RusticMobEffects.IRON_SKIN.value(), 16777148, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.FEATHER.value(), 14474460, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.BLAZING_TRAIL.value(), 16738816, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.SHAME.value(), 16409650, MobEffectCategory.HARMFUL);
        assertColourAndCategory(RusticMobEffects.FULLMETAL.value(), 8220521, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.TIPSY.value(), 7900290, MobEffectCategory.HARMFUL);
        assertColourAndCategory(RusticMobEffects.FIRE_POWER.value(), 0xFFCE6D, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.FULL.value(), 6563840, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.MAGIC_RESISTANCE.value(), 10511560, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.WITHER_WARD.value(), 11842760, MobEffectCategory.BENEFICIAL);
        assertColourAndCategory(RusticMobEffects.UNDYING.value(), 0xEADB84, MobEffectCategory.BENEFICIAL);
    }

    private static void assertColourAndCategory(MobEffect effect, int colour, MobEffectCategory category) {
        String id = BuiltInRegistries.MOB_EFFECT.getKey(effect).toString();
        assertEquals(colour, effect.getColor(), id + " drifted from the legacy colour");
        assertSame(category, effect.getCategory(), id + " changed the legacy isBad category");
    }

    // ------------------------------------------------------------------ fullmetal attribute map (flat)

    /**
     * Fullmetal carries four modifiers whose amounts are flat at every amplifier (the legacy class
     * overrode {@code getAttributeModifierAmount} to ignore the amplifier, which the port reproduces with
     * NeoForge's constant-curve overload). Movement/flying/attack speed are total-multiplier negatives,
     * knockback resistance is a huge ADD_VALUE. The legacy SWIM_SPEED modifier is deliberately absent —
     * 1.21 has no such attribute, and MOVEMENT_SPEED already covers submersion.
     */
    @Test
    public void fullmetalAppliesFourFlatAttributeModifiersAtEveryAmplifier() {
        for (int amplifier : new int[] {0, 2}) {
            Map<ResourceLocation, Bound> mods = modifiersAt(RusticMobEffects.FULLMETAL.value(), amplifier);
            assertEquals(4, mods.size(), "fullmetal modifier count at amplifier " + amplifier);

            assertModifier(mods, "rustic:fullmetal_movement_speed", Attributes.MOVEMENT_SPEED,
                    -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier);
            assertModifier(mods, "rustic:fullmetal_flying_speed", Attributes.FLYING_SPEED,
                    -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier);
            assertModifier(mods, "rustic:fullmetal_attack_speed", Attributes.ATTACK_SPEED,
                    -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier);
            assertModifier(mods, "rustic:fullmetal_knockback_resistance", Attributes.KNOCKBACK_RESISTANCE,
                    9001.0, AttributeModifier.Operation.ADD_VALUE, amplifier);
        }
        // The dropped swim-speed modifier must not have crept back in under some other id.
        assertTrue(modifiersAt(RusticMobEffects.FULLMETAL.value(), 0).size() == 4,
                "a fifth fullmetal modifier appeared (the legacy SWIM_SPEED modifier was dropped on purpose)");
    }

    // ------------------------------------------------------------------ ironskin attribute map (scaled)

    /**
     * Iron Skin is armor +3 and toughness +2, and unlike fullmetal these DO scale with the amplifier
     * (the legacy class left {@code getAttributeModifierAmount} alone, so vanilla's {@code amount *
     * (amplifier + 1)} is the faithful form): +3/+2 at amplifier 0, +6/+4 at amplifier 1.
     */
    @Test
    public void ironSkinScalesItsTwoArmorModifiersWithTheAmplifier() {
        Map<ResourceLocation, Bound> base = modifiersAt(RusticMobEffects.IRON_SKIN.value(), 0);
        assertEquals(2, base.size(), "ironskin carries exactly the armor + toughness pair");
        assertModifier(base, "rustic:ironskin_armor", Attributes.ARMOR,
                3.0, AttributeModifier.Operation.ADD_VALUE, 0);
        assertModifier(base, "rustic:ironskin_toughness", Attributes.ARMOR_TOUGHNESS,
                2.0, AttributeModifier.Operation.ADD_VALUE, 0);

        Map<ResourceLocation, Bound> next = modifiersAt(RusticMobEffects.IRON_SKIN.value(), 1);
        assertEquals(6.0, next.get(ResourceLocation.parse("rustic:ironskin_armor")).modifier().amount(), 0.0001,
                "ironskin armor must scale by amplifier + 1 (legacy default MobEffect behaviour)");
        assertEquals(4.0, next.get(ResourceLocation.parse("rustic:ironskin_toughness")).modifier().amount(), 0.0001,
                "ironskin toughness must scale by amplifier + 1");
    }

    // ------------------------------------------------------------------ description ids / lang keys

    /** The description key derives to {@code effect.rustic.<path>}, the lang entries the plan requires. */
    @Test
    public void descriptionKeysFollowTheEffectRusticLangConvention() {
        for (String id : LEGACY_IDS) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse("rustic:" + id));
            assertEquals("effect.rustic." + id, effect.getDescriptionId(),
                    "rustic:" + id + " has a non-standard description key");
        }
    }

    // ------------------------------------------------------------------ helpers

    /** A modifier bound to the attribute it targets, as the engine emits it from {@code createModifiers}. */
    private record Bound(Holder<Attribute> attribute, AttributeModifier modifier) {
    }

    /** Enumerates an effect's modifiers through the engine's own amplifier-aware {@code createModifiers}. */
    private static Map<ResourceLocation, Bound> modifiersAt(MobEffect effect, int amplifier) {
        Map<ResourceLocation, Bound> byId = new java.util.LinkedHashMap<>();
        effect.createModifiers(amplifier, (attribute, modifier) -> byId.put(modifier.id(), new Bound(attribute, modifier)));
        return byId;
    }

    private static void assertModifier(Map<ResourceLocation, Bound> mods, String id,
            Holder<Attribute> expectedAttribute, double expectedAmount, AttributeModifier.Operation expectedOperation,
            int amplifier) {
        Bound bound = mods.get(ResourceLocation.parse(id));
        assertTrue(bound != null, id + " is missing from the effect at amplifier " + amplifier);
        assertSame(expectedAttribute, bound.attribute(), id + " targets the wrong attribute");
        assertEquals(expectedAmount, bound.modifier().amount(), 0.0001, id + " amount at amplifier " + amplifier);
        assertSame(expectedOperation, bound.modifier().operation(), id + " operation");
    }
}
