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

/**
 * Phase 6 characterisation test for the last content slice: the thrown tomato (its entity, the item that
 * lobs it, the client renderer binding) and the two tree fruits whose bite carries a Rustic effect - the
 * nausea-giving {@code OLIVES} and the fullmetal-stacking {@code IRONBERRIES} - plus the burn-carrying
 * {@code GHOST_PEPPER}.
 *
 * <p>Runs in ModDevGradle's FML unit-test JVM, so {@code BuiltInRegistries} is populated and every deferred
 * holder resolves. What is pinned here is the registration contract the live game leans on: the tomato
 * entity is a {@link EntityTomato} type at the legacy 0.25 hitbox, the four foods are their ported item
 * subclasses with the exact 1.12.2 nutrition/saturation/eat-time/effect numbers, the farming tab now leads
 * with olives and lists both tree fruits, and the datagen/lang entries a player actually sees are present.
 *
 * <p>Not exercised (live-game checks the plan parks): the sneak-throw motion arc, the SHAME-on-impact and
 * splat particle of {@code EntityTomato}, the ghost pepper's hurt damage and the ironberries' duration
 * stacking in {@code finishUsingItem} - each needs a ticking {@code Level} and a real entity. This is a
 * registration + arithmetic gate, not a simulation.
 */
public class RusticThrownProduceTest {

    // ------------------------------------------------------------------ the thrown tomato entity

    /** 1.12.2 registered the tomato as an entity; here it resolves under {@code rustic:tomato}. */
    @Test
    public void theTomatoEntityIsRegisteredUnderItsLegacyId() {
        EntityType<EntityTomato> type = RusticEntities.TOMATO.get();
        assertSame(type, BuiltInRegistries.ENTITY_TYPE.get(RusticEntities.TOMATO.getId()),
                "rustic:tomato does not resolve to the deferred entity type");
        assertEquals("tomato", RusticEntities.TOMATO.getId().getPath());
        assertEquals(Rustic.MODID, RusticEntities.TOMATO.getId().getNamespace());
    }

    /**
     * The legacy {@code EntityThrowable} hitbox was 0.25 x 0.25, which is what vanilla declares beside the
     * size is now part of the {@link EntityType} rather than the constructor.
     */
    @Test
    public void theTomatoEntityCarriesTheLegacyThrowableHitbox() {
        EntityType<EntityTomato> type = RusticEntities.TOMATO.get();
        assertEquals(0.25F, type.getWidth(), 0.0001F, "the legacy EntityThrowable width was 0.25");
        assertEquals(0.25F, type.getHeight(), 0.0001F, "the legacy EntityThrowable height was 0.25");
    }

    /**
     * The client lookup crashes on an entity with no renderer (the P3 NoopRenderer lesson), so the tomato
     * must be an item-carrying thrown projectile - the type vanilla's {@code ThrownItemRenderer} renders.
     * That is pinned here as the class hierarchy ({@link EntityTomato} extends {@code ThrowableItemProjectile},
     * which is the {@code ItemSupplier} the renderer reads); constructing a live entity and its synced default
     * stack needs a ticking {@code Level} and is a live-game check.
     */
    @Test
    public void theEntityTomatoIsAThrownItemProjectileTheVanillaRendererCanDraw() {
        assertTrue(net.minecraft.world.entity.projectile.ThrowableItemProjectile.class
                        .isAssignableFrom(EntityTomato.class),
                "rustic:tomato must extend ThrowableItemProjectile so ThrownItemRenderer (an ItemSupplier "
                        + "renderer) can draw it, the legacy RenderTomato behaviour");
        assertTrue(net.minecraft.world.entity.Entity.class.isAssignableFrom(EntityTomato.class));
    }

    // ------------------------------------------------------------------ the four produce items

    /** The tomato is the sneaks-to-throw subclass; the plain foods stay ordinary {@link Item}s. */
    @Test
    public void theProduceItemsAreThePortedSubclasses() {
        assertSame(ItemTomato.class, RusticItems.TOMATO.get().getClass());
        assertSame(ItemGhostPepper.class, RusticItems.GHOST_PEPPER.get().getClass());
        assertSame(ItemOlives.class, RusticItems.OLIVES.get().getClass());
        assertSame(ItemIronberries.class, RusticItems.IRONBERRIES.get().getClass());
    }

    /**
     * TOMATO food: {@code ItemFoodBase("tomato", 4, 0.4F, false)}, no potion effect (the nausea/glow are on
     * the other fruits), eat time is the vanilla 32 ticks (legacy set no override).
     */
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

    /**
     * GHOST_PEPPER food: {@code (4, 0.7F)} and alwaysEdible per the legacy initFood override; the burn and
     * FIRE_POWER are code in finishUsingItem, so the food record carries no effect.
     */
    @Test
    public void theGhostPepperIsAnAlwaysEdibleFoodWithNoDataEffect() {
        FoodProperties food = foodOf(RusticItems.GHOST_PEPPER.get());
        assertEquals(4, food.nutrition(), "ghost pepper nutrition (1.12.2: 4)");
        assertEquals(FoodConstants.saturationByModifier(4, 0.7F), food.saturation(), 0.0001F, "ghost pepper saturation");
        assertTrue(food.canAlwaysEat(), "the ghost pepper was setAlwaysEdible in 1.12.2");
        assertTrue(food.effects().isEmpty(), "the burn and fire power are code in ItemGhostPepper, not food effects");
    }

    /**
     * OLIVES food: {@code (1, 0.4)}, the legacy {@code setPotionEffect(NAUSEA, 200, 1, false, false, 0.95F)}
     * folded onto FoodProperties as CONFUSION (the 1.20.5 name), a 24-tick eat time (ItemOlives override) and
     * NOT always edible - the legacy initFood set only the potion.
     */
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

    /**
     * IRONBERRIES food: {@code (2, 0.4)}, always edible and eaten in 16 ticks (the legacy override is 1.21's
     * {@code fast()}, eat-seconds 0.8); the stacking FULLMETAL is code, so the food record carries no effect.
     */
    @Test
    public void theIronberriesAreAlwaysEdibleFastFoodsWithNoDataEffect() {
        FoodProperties food = foodOf(RusticItems.IRONBERRIES.get());
        assertEquals(2, food.nutrition(), "ironberries nutrition (1.12.2: 2)");
        assertEquals(FoodConstants.saturationByModifier(2, 0.4F), food.saturation(), 0.0001F, "ironberries saturation");
        assertTrue(food.canAlwaysEat(), "the legacy initFood override set setAlwaysEdible");
        assertEquals(0.8F, food.eatSeconds(), 0.0001F, "the 16-tick eat override is 1.21's fast() (0.8 s)");
        assertTrue(food.effects().isEmpty(), "the stacking fullmetal is code in ItemIronberries, not a food effect");
    }

    // ------------------------------------------------------------------ the farming tab

    /**
     * 1.12.2 led the farming tab with {@code ModItems.OLIVES} as its icon. The stand-in {@code Items.WHEAT}
     * was replaced the moment olives existed, so the icon must be the olives item now.
     */
    @Test
    public void theFarmingTabIconIsTheOlivesItem() {
        CreativeModeTab farming = RusticCreativeTabs.FARMING.get();
        assertSame(RusticItems.OLIVES.get(), farming.getIconItem().getItem(),
                "the farming tab icon is the legacy ModItems.OLIVES, not the Phase 5 wheat stand-in");
    }

    /** Both tree fruits are shown in the farming tab, matching the legacy setCreativeTab(farmingTab). */
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

    // ------------------------------------------------------------------ lang + datagen models

    /** The three new lang keys the slice depends on exist in en_us.json. */
    @Test
    public void theNewLangKeysArePresent() {
        JsonObject lang = json("/assets/rustic/lang/en_us.json");
        assertEquals("Olives", lang.get("item.rustic.olives").getAsString());
        assertEquals("Ironberries", lang.get("item.rustic.ironberries").getAsString());
        assertTrue(lang.has("tooltip.rustic.ghost_pepper"),
                "the ghost pepper tooltip line needs a lang key the 1.12.2 file never carried");
    }

    /** The two tree fruits each get a flat {@code item/generated} sprite on their own (renamed) texture. */
    @Test
    public void datagenProducedATreeFruitSpriteForEach() {
        for (String id : List.of("olives", "ironberries")) {
            JsonObject model = json("/assets/rustic/models/item/" + id + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), id + " item model");
            assertEquals("rustic:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString(),
                    id + " must texture from rustic:item/, not the legacy rustic:items/ folder");
        }
    }

    // ------------------------------------------------------------------ helpers

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
