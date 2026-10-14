package rustic.content.potions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import rustic.Rustic;

/**
 * Ported from 1.12.2 {@code PotionIronSkin}: {@code armor +3} and {@code armor toughness +2}, both
 * flat ADD_VALUE modifiers that scale with the amplifier exactly as vanilla does (legacy did not
 * override {@code getAttributeModifierAmount}, so the plain {@code addAttributeModifier} overload —
 * which multiplies the amount by {@code amplifier + 1} — is the faithful form).
 *
 * <p>The legacy manual tracker broadcasts on add/remove are dropped: 1.21 syncs the modifiers itself.
 * The custom {@code LayerIronSkin} body paint (450 lines, multi-texture vertex format) is a Phase 9
 * render-layer rewrite and is not here — the effect grants the protection with no visible skin, which
 * is the one knowingly-imperfect part of this slice, per the user's "fix visuals after Phase 11" rule.
 */
public final class IronSkinEffect extends MobEffect {

    private static final ResourceLocation ARMOR = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "ironskin_armor");
    private static final ResourceLocation ARMOR_TOUGHNESS = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "ironskin_toughness");

    public IronSkinEffect() {
        // 1.12.2 PotionBase(false, 16777148, "ironskin").
        super(MobEffectCategory.BENEFICIAL, 16777148);
        addAttributeModifier(Attributes.ARMOR, ARMOR, 3.0, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_VALUE);
    }
}
