package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.items.IItemHandler;
import rustic.content.RusticCreativeTabs;
import rustic.content.blockentity.ApiaryBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;
import rustic.content.blocks.ApiaryBlock;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.items.RusticItems;
import rustic.content.menu.RusticMenus;

/**
 * Phase 15 ({@code UNPORTED_BACKLOG} §A1) characterisation test for the apiary vertical slice - the biggest
 * single gap in the port. It checks the same four half-registrations the machine test does (block with the
 * legacy numbers, {@link BlockEntityType}, {@link MenuType}, datagen) plus the two things unique to the apiary:
 * the item-filtered slots and the pure colony-simulation math lifted verbatim from {@code TileEntityApiary.update()}.
 *
 * <p>Runs in ModDevGradle's FML unit-test JVM (so {@code BuiltInRegistries} is populated and the creative tabs
 * are readable). The tick loop itself, GUI rendering and the random-tick crop boost are live-game behaviour and
 * deliberately not exercised here; only the wiring and the arithmetic a live game depends on are asserted.
 */
public class RusticApiaryTest {

    // ------------------------------------------------------------------ registration quartet

    /** The apiary was {@code Material.WOOD} at hardness 1F (read from the legacy constructor, not the backlog's 1.5). */
    @Test
    public void apiaryIsRegisteredWithTheLegacyWoodNumbers() {
        Block apiary = RusticMachineBlocks.APIARY.get();
        assertSame(ApiaryBlock.class, apiary.getClass(), "rustic:apiary is not an ApiaryBlock");
        assertSame(apiary, BuiltInRegistries.BLOCK.get(RusticMachineBlocks.APIARY.getId()),
                "the block register does not resolve rustic:apiary to the deferred block");

        assertEquals(1.0F, apiary.defaultDestroyTime(), 0.0001F, "apiary hardness (1.12.2: setHardness(1F))");
        assertSame(SoundType.WOOD, apiary.getSoundType(apiary.defaultBlockState(), null, null, null),
                "Material.WOOD sound");
        // isOpaqueCube=false -> noOcclusion(), so the state must report it cannot occlude neighbours.
        assertFalse(apiary.defaultBlockState().canOcclude(), "legacy isOpaqueCube=false -> noOcclusion()");
        assertFalse(apiary.defaultBlockState().requiresCorrectToolForDrops(),
                "no harvest level in 1.12.2, so it dropped its item even by hand");
    }

    /** Legacy carried a horizontal {@code FACING} whose default was NORTH; the port keeps that default. */
    @Test
    public void apiaryCarriesAHorizontalFacingDefaultingNorth() {
        Block apiary = RusticMachineBlocks.APIARY.get();
        assertTrue(apiary.defaultBlockState().hasProperty(ApiaryBlock.FACING),
                "the apiary lost its horizontal FACING property");
        assertEquals(Direction.NORTH, apiary.defaultBlockState().getValue(ApiaryBlock.FACING),
                "BlockApiary's default meta was facing=north");
    }

    @Test
    public void apiaryHasABlockItemUnderTheSameId() {
        Block apiary = RusticMachineBlocks.APIARY.get();
        Item item = apiary.asItem();
        assertTrue(item instanceof BlockItem, "rustic:apiary has no block item");
        assertEquals("apiary", BuiltInRegistries.ITEM.getKey(item).getPath(),
                "the apiary block item drifted from the block id");
        assertSame(apiary, ((BlockItem) item).getBlock());
    }

    /** Both a {@link BlockEntityType} valid for the block and a {@link MenuType} the MenuProvider opens. */
    @Test
    public void apiaryHasABlockEntityTypeAndAMenuType() {
        ResourceLocation id = RusticMachineBlocks.APIARY.getId();

        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
        assertNotNull(type, "no block entity type registered under " + id);
        assertSame(type, RusticBlockEntities.APIARY.get(), "the deferred holder and the registry disagree");
        assertTrue(type.getValidBlocks().contains(RusticMachineBlocks.APIARY.get()),
                "the apiary block entity type is not valid for the apiary block");

        MenuType<?> menu = BuiltInRegistries.MENU.get(RusticMenus.APIARY.getId());
        assertNotNull(menu, "no menu type registered under " + RusticMenus.APIARY.getId());
        assertSame(RusticMenus.APIARY.get(), menu, "the deferred menu holder and the registry disagree");
    }

    // ------------------------------------------------------------------ block entity slots

    /** The two single-slot handlers exist and default to empty (a freshly placed box runs no timers). */
    @Test
    public void apiaryBlockEntityHasTwoEmptySingleSlotHandlers() {
        ApiaryBlockEntity apiary = newApiary();
        assertSame(RusticBlockEntities.APIARY.get(), apiary.getType(), "block entity reports a vanilla type");
        assertEquals(1, apiary.getBeeHandler().getSlots(), "the bee handler is a single slot");
        assertEquals(1, apiary.getHoneyCombHandler().getSlots(), "the honeycomb handler is a single slot");
        assertTrue(apiary.getBeeHandler().getStackInSlot(0).isEmpty());
        assertTrue(apiary.getHoneyCombHandler().getStackInSlot(0).isEmpty());
        assertEquals(0, apiary.getBeeCount());
        assertEquals(0, apiary.getHoneyCombCount());
    }

    /**
     * The filtered insert the legacy anonymous {@code insertItem} override gave each slot: the bee handler
     * admits bees and refuses honeycomb, the comb handler the reverse. Hoppers ride the same handler, so this
     * is the whole of the side-insertion contract.
     */
    @Test
    public void eachHandlerAdmitsOnlyItsOneItem() {
        ApiaryBlockEntity apiary = newApiary();
        IItemHandler bees = apiary.getBeeHandler();
        IItemHandler comb = apiary.getHoneyCombHandler();

        ItemStack beeStack = new ItemStack(RusticItems.BEE.get(), 5);
        assertTrue(bees.insertItem(0, beeStack, false).isEmpty(), "the bee handler must accept bees");
        assertEquals(5, apiary.getBeeCount(), "the inserted bee count is what getBeeCount reports");

        ItemStack honeycomb = new ItemStack(RusticItems.HONEYCOMB.get(), 1);
        assertEquals(1, bees.insertItem(0, honeycomb, false).getCount(),
                "the bee handler must refuse honeycomb");
        assertTrue(comb.insertItem(0, new ItemStack(RusticItems.HONEYCOMB.get(), 3), false).isEmpty(),
                "the comb handler must accept honeycomb");
        assertEquals(3, apiary.getHoneyCombCount());
        assertEquals(1, comb.insertItem(0, new ItemStack(RusticItems.BEE.get(), 1), false).getCount(),
                "the comb handler must refuse bees");
    }

    /** A foreign item (a vanilla plank) is refused by both handlers, matching the strict item filter. */
    @Test
    public void handlersRefuseUnrelatedItems() {
        ApiaryBlockEntity apiary = newApiary();
        ItemStack plank = new ItemStack(Items.OAK_PLANKS);
        assertFalse(apiary.getBeeHandler().insertItem(0, plank.copy(), false).isEmpty(),
                "the bee handler admitted a plank");
        assertFalse(apiary.getHoneyCombHandler().insertItem(0, plank.copy(), false).isEmpty(),
                "the comb handler admitted a plank");
    }

    // ------------------------------------------------------------------ pure colony-sim math

    /**
     * Legacy {@code reproductionTime = (int)(mult * (1600F / ((numBees/20F) + 1F)))}. The 1.12.2 field
     * initializer read 1200 but was overwritten every tick, so the divisor the shipped formula reaches is 1600.
     */
    @Test
    public void reproductionTimeMatchesTheLegacyFormula() {
        assertEquals(800, ApiaryBlockEntity.reproductionTimeFor(20, 1.0), "20 bees -> 1600/2");
        assertEquals(380, ApiaryBlockEntity.reproductionTimeFor(64, 1.0), "64 bees -> (int)(1600/4.2)");
        assertEquals(1523, ApiaryBlockEntity.reproductionTimeFor(1, 1.0), "1 bee -> (int)(1600/1.05)");
        assertEquals(1600, ApiaryBlockEntity.reproductionTimeFor(0, 1.0), "no-bee base divisor (unused in sim)");
        assertEquals(1600, ApiaryBlockEntity.reproductionTimeFor(20, 2.0), "larger multiplier = slower");
    }

    /** Legacy {@code productionTime = (int)(mult * (800F / ((numBees/20F) + 1F)))} (600 initializer never reached). */
    @Test
    public void productionTimeMatchesTheLegacyFormula() {
        assertEquals(400, ApiaryBlockEntity.productionTimeFor(20, 1.0), "20 bees -> 800/2");
        assertEquals(190, ApiaryBlockEntity.productionTimeFor(64, 1.0), "64 bees -> (int)(800/4.2)");
        assertEquals(800, ApiaryBlockEntity.productionTimeFor(0, 1.0), "no-bee base divisor");
        assertEquals(200, ApiaryBlockEntity.productionTimeFor(20, 0.5), "smaller multiplier = faster");
    }

    /** Legacy growth roll {@code random.nextInt(ceil(2048F / (numBees * growthMultiplier))) == 0}, mean period. */
    @Test
    public void growthGateDenominatorMatchesTheLegacyRoll() {
        assertEquals(2048, ApiaryBlockEntity.growthGateDenominatorFor(1, 1.0), "1 bee -> ceil(2048/1)");
        assertEquals(103, ApiaryBlockEntity.growthGateDenominatorFor(20, 1.0), "20 bees -> ceil(2048/20)=ceil(102.4)");
        assertEquals(32, ApiaryBlockEntity.growthGateDenominatorFor(64, 1.0), "64 bees -> ceil(2048/64)=32");
        assertEquals(2048, ApiaryBlockEntity.growthGateDenominatorFor(64, 0.0),
                "zero multiplier falls back to the no-bees denominator (the outer guard excludes it)");
    }

    // ------------------------------------------------------------------ creative tab

    /**
     * 1.12.2 put the apiary in {@code farmingTab} only, not the decor tab. The port filters it out of the
     * blanket DECOR machine append and lists it under FARMING, so it must be in one and not the other.
     */
    @Test
    public void apiaryIsInTheFarmingTabAndNotTheDecorTab() {
        List<String> farming = tabIds(RusticCreativeTabs.FARMING.get());
        assertTrue(farming.contains("rustic:apiary"), "the apiary is not offered in the FARMING tab: " + farming);

        List<String> decor = tabIds(RusticCreativeTabs.DECOR.get());
        assertFalse(decor.contains("rustic:apiary"), "the apiary leaked into DECOR (legacy farmingTab only): " + decor);
    }

    /**
     * Phase 15 follow-up: the user could not find the Rustic bee in-game because no tab listed it. Legacy
     * {@code ItemBase} set {@code setCreativeTab(farmingTab)} in its constructor, so the whole plain-material
     * chain - bee, honeycomb, beeswax, tallow and the two basin dusts - belongs in FARMING. Without the bee
     * there the apiary's bee slot is unreachable in creative.
     */
    @Test
    public void beeChainIsInTheFarmingTab() {
        List<String> farming = tabIds(RusticCreativeTabs.FARMING.get());
        assertTrue(farming.contains("rustic:bee"), "FARMING lists the bee: " + farming);
        assertTrue(farming.contains("rustic:honeycomb"), "FARMING lists the honeycomb: " + farming);
        assertTrue(farming.contains("rustic:beeswax"), "FARMING lists the beeswax: " + farming);
        assertTrue(farming.contains("rustic:tallow"), "FARMING lists the tallow: " + farming);
        assertTrue(farming.contains("rustic:dust_tiny_iron"), "FARMING lists the tiny iron dust: " + farming);
        assertTrue(farming.contains("rustic:dust_gold"), "FARMING lists the gold dust: " + farming);
    }

    // ------------------------------------------------------------------ datagen

    /** The four-facing blockstate, the parented item model, the drop-self loot and the axe tag entry. */
    @Test
    public void datagenProducedTheApiaryStateModelLootAndTag() {
        JsonObject variants = json("/assets/rustic/blockstates/apiary.json").getAsJsonObject("variants");
        assertEquals(4, variants.size(), "a horizontal FACING block has four facing states");
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            JsonObject variant = variants.getAsJsonObject("facing=" + facing.getName());
            assertNotNull(variant, "no blockstate variant for facing=" + facing.getName());
            assertEquals("rustic:block/apiary", variant.get("model").getAsString(),
                    "the apiary state should draw the restored apiary model");
        }
        // The authored-as-a-north-wall mapping the beehive uses: north unrotated, east=90, south=180, west=270.
        assertEquals(90, variants.getAsJsonObject("facing=east").get("y").getAsInt(), "east rotation");
        assertEquals(180, variants.getAsJsonObject("facing=south").get("y").getAsInt(), "south rotation");
        assertEquals(270, variants.getAsJsonObject("facing=west").get("y").getAsInt(), "west rotation");

        assertEquals("rustic:block/apiary",
                json("/assets/rustic/models/item/apiary.json").get("parent").getAsString(),
                "the apiary item model parents the block model");

        JsonObject loot = json("/data/rustic/loot_table/blocks/apiary.json");
        JsonElement entry = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0);
        assertEquals("rustic:apiary", entry.getAsJsonObject().get("name").getAsString(),
                "the apiary must drop itself, as 1.12.2's default did");

        assertTrue(tagValues("mineable/axe").contains("rustic:apiary"), "Material.WOOD means the apiary is axe-mined");
    }

    /** The legacy log-frame recipe: six logs around two planks, one result. */
    @Test
    public void datagenProducedTheApiaryRecipe() {
        JsonObject recipe = json("/data/rustic/recipe/apiary.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("rustic:apiary", recipe.getAsJsonObject("result").get("id").getAsString());
        var pattern = recipe.getAsJsonArray("pattern");
        assertEquals("LLL", pattern.get(0).getAsString());
        assertEquals("P P", pattern.get(1).getAsString());
        assertEquals("LLL", pattern.get(2).getAsString());
        assertEquals("minecraft:logs", recipe.getAsJsonObject("key").getAsJsonObject("L").get("tag").getAsString());
        assertEquals("minecraft:planks", recipe.getAsJsonObject("key").getAsJsonObject("P").get("tag").getAsString());
    }

    // ------------------------------------------------------------------ helpers

    private static ApiaryBlockEntity newApiary() {
        return new ApiaryBlockEntity(BlockPos.ZERO, RusticMachineBlocks.APIARY.get().defaultBlockState());
    }

    private static List<String> tabIds(CreativeModeTab tab) {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> ids = new ArrayList<>();
        for (ItemStack stack : tab.getDisplayItems()) {
            ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return ids;
    }

    private static JsonObject json(String classpathResource) {
        try (Reader reader = new InputStreamReader(resourceStream(classpathResource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static java.io.InputStream resourceStream(String resource) {
        java.io.InputStream in = RusticApiaryTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource
                + " - if this is new content, re-run runData rather than writing the file by hand");
        return in;
    }

    private static List<String> tagValues(String name) {
        JsonObject json = json("/data/minecraft/tags/block/" + name + ".json");
        List<String> ids = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("values")) {
            ids.add(element.isJsonObject() ? element.getAsJsonObject().get("id").getAsString() : element.getAsString());
        }
        return ids;
    }
}
