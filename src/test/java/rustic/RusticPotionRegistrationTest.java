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

public class RusticPotionRegistrationTest {

    private static final List<String> LEGACY_IDS = List.of(
            "ironskin", "feather", "blazing_trail", "shame", "fullmetal", "tipsy",
            "fire_power", "full", "magic_resistance", "wither_ward", "undying");

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

        assertTrue(modifiersAt(RusticMobEffects.FULLMETAL.value(), 0).size() == 4,
                "a fifth fullmetal modifier appeared (the legacy SWIM_SPEED modifier was dropped on purpose)");
    }

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

    @Test
    public void descriptionKeysFollowTheEffectRusticLangConvention() {
        for (String id : LEGACY_IDS) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse("rustic:" + id));
            assertEquals("effect.rustic." + id, effect.getDescriptionId(),
                    "rustic:" + id + " has a non-standard description key");
        }
    }

    private record Bound(Holder<Attribute> attribute, AttributeModifier modifier) {
    }

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
