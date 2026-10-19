package rustic.content.potions;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import rustic.Rustic;

public final class IronSkinEffect extends MobEffect {

    private static final ResourceLocation ARMOR = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "ironskin_armor");
    private static final ResourceLocation ARMOR_TOUGHNESS = ResourceLocation.fromNamespaceAndPath(Rustic.MODID, "ironskin_toughness");

    public IronSkinEffect() {

        super(MobEffectCategory.BENEFICIAL, 16777148);
        addAttributeModifier(Attributes.ARMOR, ARMOR, 3.0, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_VALUE);
    }
}
