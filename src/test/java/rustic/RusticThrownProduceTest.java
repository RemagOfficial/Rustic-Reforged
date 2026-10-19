package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import rustic.content.RusticCreativeTabs;
import rustic.content.entities.EntityTomato;
import rustic.content.entities.RusticEntities;
import rustic.content.items.ItemGhostPepper;
import rustic.content.items.ItemIronberries;
import rustic.content.items.ItemOlives;
import rustic.content.items.ItemTomato;
import rustic.content.items.RusticItems;

public class RusticThrownProduceTest {

    @Test
    public void theTomatoEntityIsRegisteredUnderItsLegacyId() {
        EntityType<EntityTomato> type = RusticEntities.TOMATO.get();
        assertSame(type, BuiltInRegistries.ENTITY_TYPE.get(RusticEntities.TOMATO.getId()),
                "rustic:tomato does not resolve to the deferred entity type");
        assertEquals("tomato", RusticEntities.TOMATO.getId().getPath());
        assertEquals(Rustic.MODID, RusticEntities.TOMATO.getId().getNamespace());
    }

    @Test
    public void theTomatoEntityCarriesTheLegacyThrowableHitbox() {
        EntityType<EntityTomato> type = RusticEntities.TOMATO.get();
        assertEquals(0.25F, type.getWidth(), 0.0001F, "the legacy EntityThrowable width was 0.25");
        assertEquals(0.25F, type.getHeight(), 0.0001F, "the legacy EntityThrowable height was 0.25");
    }

    @Test
    public void theEntityTomatoIsAThrownItemProjectileTheVanillaRendererCanDraw() {
        assertTrue(net.minecraft.world.entity.projectile.ThrowableItemProjectile.class
                        .isAssignableFrom(EntityTomato.class),
                "rustic:tomato must extend ThrowableItemProjectile so ThrownItemRenderer (an ItemSupplier "
                        + "renderer) can draw it, the legacy RenderTomato behaviour");
        assertTrue(net.minecraft.world.entity.Entity.class.isAssignableFrom(EntityTomato.class));
    }

    @Test
    public void theProduceItemsAreThePortedSubclasses() {
        assertSame(ItemTomato.class, RusticItems.TOMATO.get().getClass());
        assertSame(ItemGhostPepper.class, RusticItems.GHOST_PEPPER.get().getClass());
        assertSame(ItemOlives.class, RusticItems.OLIVES.get().getClass());
        assertSame(ItemIronberries.class, RusticItems.IRONBERRIES.get().getClass());
    }

    @Test
    public void theTomatoIsAPlainFoodWithTheLegacyNumbers() {
        FoodProperties food = foodOf(RusticItems.TOMATO.get());
        assertEquals(4, food.nutrition(), "tomato nutrition (1.12.2: 4)");
        assertEquals(FoodConstants.saturationByModifier(4, 0.4F), food.saturation(), 0.0001F, "tomato saturation");
        assertFalse(food.canAlwaysEat(), "the tomato was not always edible");
        assertTrue(food.effects().isEmpty(), "the tomato carries no food effect");
        assertEquals(32, RusticItems.TOMATO.get().getUseDuration(new ItemStack(RusticItems.TOMATO.get()), null),
                "the tomato had no getMaxItemUseDuration override, so it eats at the 32-tick default");
    }

    @Test
    public void theGhostPepperIsAnAlwaysEdibleFoodWithNoDataEffect() {
        FoodProperties food = foodOf(RusticItems.GHOST_PEPPER.get());
        assertEquals(4, food.nutrition(), "ghost pepper nutrition (1.12.2: 4)");
        assertEquals(FoodConstants.saturationByModifier(4, 0.7F), food.saturation(), 0.0001F, "ghost pepper saturation");
        assertTrue(food.canAlwaysEat(), "the ghost pepper was setAlwaysEdible in 1.12.2");
        assertTrue(food.effects().isEmpty(), "the burn and fire power are code in ItemGhostPepper, not food effects");
    }

    @Test
    public void theOlivesCarryTheProbabilisticNauseaAndTheLegacyTwentyFourTickEat() {
        FoodProperties food = foodOf(RusticItems.OLIVES.get());
        assertEquals(1, food.nutrition(), "olives nutrition (1.12.2: 1)");
        assertEquals(FoodConstants.saturationByModifier(1, 0.4F), food.saturation(), 0.0001F, "olives saturation");
        assertFalse(food.canAlwaysEat(), "olives were not always edible in 1.12.2");

        assertEquals(1, food.effects().size(), "olives carried exactly one food effect (the nausea)");
        FoodProperties.PossibleEffect possible = food.effects().get(0);
        MobEffectInstance effect = possible.effect();
        assertSame(MobEffects.CONFUSION.value(), effect.getEffect().value(),
                "the legacy MobEffects.NAUSEA is 1.20.5's MobEffects.CONFUSION");
        assertEquals(200, effect.getDuration(), "the legacy nausea duration was 200 ticks");
        assertEquals(1, effect.getAmplifier(), "the legacy nausea amplifier was 1");
        assertEquals(0.95F, possible.probability(), 0.0001F, "the legacy setPotionEffect probability was 0.95F");

        assertEquals(24, RusticItems.OLIVES.get().getUseDuration(new ItemStack(RusticItems.OLIVES.get()), null),
                "the legacy getMaxItemUseDuration override was 24 ticks, carried in code by ItemOlives");
    }

    @Test
    public void theIronberriesAreAlwaysEdibleFastFoodsWithNoDataEffect() {
        FoodProperties food = foodOf(RusticItems.IRONBERRIES.get());
        assertEquals(2, food.nutrition(), "ironberries nutrition (1.12.2: 2)");
        assertEquals(FoodConstants.saturationByModifier(2, 0.4F), food.saturation(), 0.0001F, "ironberries saturation");
        assertTrue(food.canAlwaysEat(), "the legacy initFood override set setAlwaysEdible");
        assertEquals(0.8F, food.eatSeconds(), 0.0001F, "the 16-tick eat override is 1.21's fast() (0.8 s)");
        assertTrue(food.effects().isEmpty(), "the stacking fullmetal is code in ItemIronberries, not a food effect");
    }

    @Test
    public void theFarmingTabIconIsTheOlivesItem() {
        CreativeModeTab farming = RusticCreativeTabs.FARMING.get();
        assertSame(RusticItems.OLIVES.get(), farming.getIconItem().getItem(),
                "the farming tab icon is the legacy ModItems.OLIVES, not the Phase 5 wheat stand-in");
    }

    @Test
    public void theFarmingTabOffersBothTreeFruits() {
        CreativeModeTab farming = RusticCreativeTabs.FARMING.get();
        farming.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> shown = new ArrayList<>();
        for (ItemStack stack : farming.getDisplayItems()) {
            shown.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        }
        assertTrue(shown.contains("olives"), "rustic:olives is missing from the FARMING tab: " + shown);
        assertTrue(shown.contains("ironberries"), "rustic:ironberries is missing from the FARMING tab: " + shown);
    }

    @Test
    public void theNewLangKeysArePresent() {
        JsonObject lang = json("/assets/rustic/lang/en_us.json");
        assertEquals("Olives", lang.get("item.rustic.olives").getAsString());
        assertEquals("Ironberries", lang.get("item.rustic.ironberries").getAsString());
        assertTrue(lang.has("tooltip.rustic.ghost_pepper"),
                "the ghost pepper tooltip line needs a lang key the 1.12.2 file never carried");
    }

    @Test
    public void datagenProducedATreeFruitSpriteForEach() {
        for (String id : List.of("olives", "ironberries")) {
            JsonObject model = json("/assets/rustic/models/item/" + id + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), id + " item model");
            assertEquals("rustic:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString(),
                    id + " must texture from rustic:item/, not the legacy rustic:items/ folder");
        }
    }

    private static FoodProperties foodOf(Item item) {
        FoodProperties food = item.getFoodProperties(new ItemStack(item), null);
        assertNotNull(food, BuiltInRegistries.ITEM.getKey(item) + " is not a food");
        return food;
    }

    private static JsonObject json(String classpathResource) {
        try (Reader reader = new InputStreamReader(resourceStream(classpathResource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static java.io.InputStream resourceStream(String resource) {
        java.io.InputStream in = RusticThrownProduceTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource
                + " - if this is new content, re-run runData rather than writing the file by hand");
        return in;
    }
}
