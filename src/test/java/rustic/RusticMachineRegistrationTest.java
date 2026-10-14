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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rustic.content.RusticCreativeTabs;
import rustic.content.blockentity.BarrelBlockEntity;
import rustic.content.blockentity.CabinetBlockEntity;
import rustic.content.blockentity.RusticBlockEntities;
import rustic.content.blockentity.VaseBlockEntity;
import rustic.content.blocks.BarrelBlock;
import rustic.content.blocks.CabinetBlock;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.blocks.VaseBlock;
import rustic.content.menu.RusticMenus;

/**
 * Phase 4 characterisation test for the storage-machine slice, starting with the barrel vertical slice.
 *
 * <p>{@link RusticDecorRegistrationTest} deliberately excludes the machine ids (see its
 * {@code machineIds()} helper), so this is where a machine's four half-registrations are asserted to
 * line up: the block (with a block item and the legacy numbers), its {@link BlockEntityType}, its
 * {@link MenuType} and the datagen that makes it render and drop. A machine phase that only registered the
 * block - the failure mode this guards against - leaves an invisible, unopenable cube that drops nothing.
 *
 * <p>Like the decor test this runs in ModDevGradle's FML unit-test JVM, which is what makes
 * {@code BuiltInRegistries} populated and the creative tab readable. It does not exercise opening a menu or
 * ticking a block entity (that is a live-game check), only that the wiring a live game depends on exists.
 */
public class RusticMachineRegistrationTest {

    /** The barrel was Material.WOOD at hardness 1.5, with no harvest level and no setResistance call. */
    @Test
    public void barrelIsRegisteredAsAMachineBlockWithTheLegacyWoodNumbers() {
        Block barrel = RusticMachineBlocks.BARREL.get();
        assertSame(BarrelBlock.class, barrel.getClass(), "rustic:barrel is not a BarrelBlock");
        assertSame(barrel, BuiltInRegistries.BLOCK.get(RusticMachineBlocks.BARREL.getId()),
                "the block register does not resolve rustic:barrel to the deferred block");

        assertEquals(1.5F, barrel.defaultDestroyTime(), 0.0001F, "barrel hardness (1.12.2: 1.5)");
        assertEquals(0.0F, barrel.getExplosionResistance(barrel.defaultBlockState(), null, null, null),
                0.0001F, "barrel resistance, which 1.12.2 never set");
        assertSame(SoundType.WOOD, barrel.getSoundType(barrel.defaultBlockState(), null, null, null),
                "Material.WOOD sound");
        // No harvest level in 1.12.2, so it always dropped its item even by hand.
        assertFalse(barrel.defaultBlockState().requiresCorrectToolForDrops(),
                "barrel would stop dropping its item when mined by hand");
    }

    /** 1.12.2 gave every block a block item under the block's own id; the machines are no exception. */
    @Test
    public void barrelHasABlockItemUnderTheSameId() {
        Block barrel = RusticMachineBlocks.BARREL.get();
        Item item = barrel.asItem();
        assertTrue(item instanceof BlockItem, "rustic:barrel has no block item");
        assertEquals("barrel", BuiltInRegistries.ITEM.getKey(item).getPath(),
                "the barrel block item drifted from the block id");
        assertSame(barrel, ((BlockItem) item).getBlock());
    }

    /**
     * The two registrations the decor blocks do not have and a storage machine must: a
     * {@link BlockEntityType} valid for the block (so placing it produces a {@link BarrelBlockEntity}), and
     * a {@link MenuType} the {@code MenuProvider} block entity can open (so right-clicking shows the
     * 27-slot inventory rather than nothing).
     */
    @Test
    public void barrelHasABlockEntityTypeAndAMenuType() {
        ResourceLocation id = RusticMachineBlocks.BARREL.getId();

        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
        assertNotNull(type, "no block entity type registered under " + id);
        assertSame(type, RusticBlockEntities.BARREL.get(), "the deferred holder and the registry disagree");
        assertTrue(type.getValidBlocks().contains(RusticMachineBlocks.BARREL.get()),
                "the barrel block entity type is not valid for the barrel block");

        MenuType<?> menu = BuiltInRegistries.MENU.get(RusticMenus.BARREL.getId());
        assertNotNull(menu, "no menu type registered under " + RusticMenus.BARREL.getId());
        assertSame(RusticMenus.BARREL.get(), menu, "the deferred menu holder and the registry disagree");
    }

    /** The barrel block entity is the 27-slot container the legacy {@code TileEntityBarrel} was. */
    @Test
    public void barrelBlockEntityBacksATwentySevenSlotContainer() {
        BarrelBlockEntity entity = new BarrelBlockEntity(net.minecraft.core.BlockPos.ZERO,
                RusticMachineBlocks.BARREL.get().defaultBlockState());
        assertEquals(27, entity.getContainerSize(), "the legacy TileEntityBarrel had 27 slots");
        assertSame(RusticBlockEntities.BARREL.get(), entity.getType(),
                "the block entity reports the registered type, not a vanilla one");
    }

    /**
     * The machine blocks were appended to the decor tab (they were not a separate creative tab in
     * 1.12.2), so the barrel item must be reachable there. Asserting membership rather than an exact list
     * keeps this test independent of the decor baseline, which excludes these ids on purpose.
     */
    @Test
    public void barrelAppearsInTheDecorTab() {
        CreativeModeTab decor = RusticCreativeTabs.DECOR.get();
        decor.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> ids = new ArrayList<>();
        for (net.minecraft.world.item.ItemStack stack : decor.getDisplayItems()) {
            ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        assertTrue(ids.contains("rustic:barrel"), "the barrel is not offered in the DECOR tab: " + ids);
    }

    /**
     * The datagen output that makes the slice render and drop: a blockstate pointing at a generated model,
     * a block model, an item model, a drop-myself loot table and the axe-mineable tag entry. Re-run
     * {@code runData} when this fails; do not hand-write the files.
     */
    @Test
    public void datagenProducedTheBarrelStateModelLootAndTag() {
        JsonObject state = json("/assets/rustic/blockstates/barrel.json");
        assertEquals("rustic:block/barrel", state.getAsJsonObject("variants").getAsJsonObject("")
                .get("model").getAsString(), "the barrel blockstate should map the empty variant to its model");

        JsonObject itemModel = json("/assets/rustic/models/item/barrel.json");
        assertEquals("rustic:block/barrel", itemModel.get("parent").getAsString(),
                "the barrel item model parents the block model");

        JsonObject loot = json("/data/rustic/loot_table/blocks/barrel.json");
        assertEquals("rustic:blocks/barrel", loot.get("random_sequence").getAsString(), "barrel loot seed");
        JsonElement entry = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0);
        assertEquals("rustic:barrel", entry.getAsJsonObject().get("name").getAsString(),
                "the barrel must drop itself, as 1.12.2's default did");

        List<String> axe = tagValues("mineable/axe");
        assertTrue(axe.contains("rustic:barrel"), "Material.WOOD means the barrel is axe-mined");
    }

    /** The cabinet was Material.WOOD at hardness 2.5, with no harvest level and no setResistance call. */
    @Test
    public void cabinetIsRegisteredAsAMachineBlockWithTheLegacyWoodNumbers() {
        Block cabinet = RusticMachineBlocks.CABINET.get();
        assertSame(CabinetBlock.class, cabinet.getClass(), "rustic:cabinet is not a CabinetBlock");
        assertSame(cabinet, BuiltInRegistries.BLOCK.get(RusticMachineBlocks.CABINET.getId()),
                "the block register does not resolve rustic:cabinet to the deferred block");

        assertEquals(2.5F, cabinet.defaultDestroyTime(), 0.0001F, "cabinet hardness (1.12.2: 2.5)");
        assertEquals(0.0F, cabinet.getExplosionResistance(cabinet.defaultBlockState(), null, null, null),
                0.0001F, "cabinet resistance, which 1.12.2 never set");
        assertSame(SoundType.WOOD, cabinet.getSoundType(cabinet.defaultBlockState(), null, null, null),
                "Material.WOOD sound");
        assertFalse(cabinet.defaultBlockState().requiresCorrectToolForDrops(),
                "cabinet would stop dropping its item when mined by hand");
    }

    /** The three state properties that drive the double, and that the deferred door art would have rendered. */
    @Test
    public void cabinetDefaultStateCarriesFacingMirrorAndTop() {
        BlockState state = RusticMachineBlocks.CABINET.get().defaultBlockState();
        assertTrue(state.hasProperty(CabinetBlock.FACING), "the cabinet lost its facing property");
        assertTrue(state.hasProperty(CabinetBlock.MIRROR), "the cabinet lost its mirror property");
        assertTrue(state.hasProperty(CabinetBlock.TOP), "the cabinet lost its top property");
        assertFalse(state.getValue(CabinetBlock.TOP), "a freshly declared cabinet is a single, not a top half");
    }

    /** 1.12.2 gave every block a block item under the block's own id; the cabinet is no exception. */
    @Test
    public void cabinetHasABlockItemUnderTheSameId() {
        Block cabinet = RusticMachineBlocks.CABINET.get();
        Item item = cabinet.asItem();
        assertTrue(item instanceof BlockItem, "rustic:cabinet has no block item");
        assertEquals("cabinet", BuiltInRegistries.ITEM.getKey(item).getPath(),
                "the cabinet block item drifted from the block id");
        assertSame(cabinet, ((BlockItem) item).getBlock());
    }

    /**
     * The cabinet needs all three machine registrations, and uniquely a second menu type: the single
     * {@code cabinet} (27 slots) and the {@code double_cabinet} (54) that its {@code createMenu} opens when a
     * partner is stacked on it. A missing {@code double_cabinet} would throw the moment anyone built a double.
     */
    @Test
    public void cabinetHasABlockEntityTypeAndBothMenuTypes() {
        ResourceLocation id = RusticMachineBlocks.CABINET.getId();

        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
        assertNotNull(type, "no block entity type registered under " + id);
        assertSame(type, RusticBlockEntities.CABINET.get(), "the deferred holder and the registry disagree");
        assertTrue(type.getValidBlocks().contains(RusticMachineBlocks.CABINET.get()),
                "the cabinet block entity type is not valid for the cabinet block");

        assertSame(RusticMenus.CABINET.get(), BuiltInRegistries.MENU.get(RusticMenus.CABINET.getId()),
                "no single-cabinet menu type under " + RusticMenus.CABINET.getId());
        assertSame(RusticMenus.DOUBLE_CABINET.get(), BuiltInRegistries.MENU.get(RusticMenus.DOUBLE_CABINET.getId()),
                "no double-cabinet menu type under " + RusticMenus.DOUBLE_CABINET.getId());
    }

    /** Each cabinet block entity is the 27-slot half the legacy {@code TileEntityCabinet} was. */
    @Test
    public void cabinetBlockEntityBacksATwentySevenSlotContainer() {
        CabinetBlockEntity entity = new CabinetBlockEntity(net.minecraft.core.BlockPos.ZERO,
                RusticMachineBlocks.CABINET.get().defaultBlockState());
        assertEquals(27, entity.getContainerSize(), "a single cabinet half holds 27 slots (a double is two)");
        assertSame(RusticBlockEntities.CABINET.get(), entity.getType(),
                "the block entity reports the registered type, not a vanilla one");
    }

    /** Like the barrel, the cabinet is offered in the decor tab (1.12.2 put the machines there too). */
    @Test
    public void cabinetAppearsInTheDecorTab() {
        CreativeModeTab decor = RusticCreativeTabs.DECOR.get();
        decor.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> ids = new ArrayList<>();
        for (net.minecraft.world.item.ItemStack stack : decor.getDisplayItems()) {
            ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        assertTrue(ids.contains("rustic:cabinet"), "the cabinet is not offered in the DECOR tab: " + ids);
    }

    /**
     * The datagen that makes the cabinet render and drop. Its blockstate enumerates every facing/mirror/top
     * combination but each one resolves to the same static placeholder cube (the doors are Phase 9), so the
     * assertion is "every referenced model is rustic:block/cabinet", not a single empty-variant key like the
     * property-less barrel. Re-run {@code runData} when this fails; do not hand-write the files.
     */
    @Test
    public void datagenProducedTheCabinetStateModelLootAndTag() {
        Set<String> models = blockstateModels("/assets/rustic/blockstates/cabinet.json");
        assertEquals(Set.of("rustic:block/cabinet"), models,
                "every cabinet state should draw the one static cube while the door art is deferred");

        JsonObject itemModel = json("/assets/rustic/models/item/cabinet.json");
        assertEquals("rustic:block/cabinet", itemModel.get("parent").getAsString(),
                "the cabinet item model parents the block model");

        JsonObject loot = json("/data/rustic/loot_table/blocks/cabinet.json");
        JsonElement entry = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0);
        assertEquals("rustic:cabinet", entry.getAsJsonObject().get("name").getAsString(),
                "the cabinet must drop itself, as 1.12.2's default did");

        List<String> axe = tagValues("mineable/axe");
        assertTrue(axe.contains("rustic:cabinet"), "Material.WOOD means the cabinet is axe-mined");
    }

    /** The vase was Material.ROCK at hardness 0.5, with no harvest level and no setResistance call. */
    @Test
    public void vaseIsRegisteredAsAMachineBlockWithTheLegacyStoneNumbers() {
        Block vase = RusticMachineBlocks.VASE.get();
        assertSame(VaseBlock.class, vase.getClass(), "rustic:vase is not a VaseBlock");
        assertSame(vase, BuiltInRegistries.BLOCK.get(RusticMachineBlocks.VASE.getId()),
                "the block register does not resolve rustic:vase to the deferred block");

        assertEquals(0.5F, vase.defaultDestroyTime(), 0.0001F, "vase hardness (1.12.2: 0.5)");
        assertEquals(0.0F, vase.getExplosionResistance(vase.defaultBlockState(), null, null, null),
                0.0001F, "vase resistance, which 1.12.2 never set");
        assertSame(SoundType.STONE, vase.getSoundType(vase.defaultBlockState(), null, null, null),
                "Material.ROCK sound");
        assertFalse(vase.defaultBlockState().requiresCorrectToolForDrops(),
                "vase would stop dropping its item when mined by hand");
    }

    /** The scaffolded variant property, defaulting to 0; the thirteen real styles are Phase 9's. */
    @Test
    public void vaseDefaultStateCarriesVariantZero() {
        BlockState state = RusticMachineBlocks.VASE.get().defaultBlockState();
        assertTrue(state.hasProperty(VaseBlock.VARIANT), "the vase lost its variant property");
        assertEquals(0, (int) state.getValue(VaseBlock.VARIANT), "the default variant is the legacy brown ceramic");
        assertEquals(13, VaseBlock.VARIANT.getPossibleValues().size(), "1.12.2 shipped thirteen vase styles (0..12)");
    }

    /** 1.12.2 gave every block a block item under the block's own id; the vase is no exception. */
    @Test
    public void vaseHasABlockItemUnderTheSameId() {
        Block vase = RusticMachineBlocks.VASE.get();
        Item item = vase.asItem();
        assertTrue(item instanceof BlockItem, "rustic:vase has no block item");
        assertEquals("vase", BuiltInRegistries.ITEM.getKey(item).getPath(),
                "the vase block item drifted from the block id");
        assertSame(vase, ((BlockItem) item).getBlock());
    }

    /** The vase's block entity type and its single 27-slot menu type. */
    @Test
    public void vaseHasABlockEntityTypeAndAMenuType() {
        ResourceLocation id = RusticMachineBlocks.VASE.getId();

        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
        assertNotNull(type, "no block entity type registered under " + id);
        assertSame(type, RusticBlockEntities.VASE.get(), "the deferred holder and the registry disagree");
        assertTrue(type.getValidBlocks().contains(RusticMachineBlocks.VASE.get()),
                "the vase block entity type is not valid for the vase block");

        MenuType<?> menu = BuiltInRegistries.MENU.get(RusticMenus.VASE.getId());
        assertNotNull(menu, "no menu type registered under " + RusticMenus.VASE.getId());
        assertSame(RusticMenus.VASE.get(), menu, "the deferred menu holder and the registry disagree");
    }

    /** The vase block entity is the 27-slot container the legacy {@code TileEntityVase} was. */
    @Test
    public void vaseBlockEntityBacksATwentySevenSlotContainer() {
        VaseBlockEntity entity = new VaseBlockEntity(net.minecraft.core.BlockPos.ZERO,
                RusticMachineBlocks.VASE.get().defaultBlockState());
        assertEquals(27, entity.getContainerSize(), "the legacy TileEntityVase had 27 slots");
        assertSame(RusticBlockEntities.VASE.get(), entity.getType(),
                "the block entity reports the registered type, not a vanilla one");
    }

    /** Like the barrel and cabinet, the vase is offered in the decor tab. */
    @Test
    public void vaseAppearsInTheDecorTab() {
        CreativeModeTab decor = RusticCreativeTabs.DECOR.get();
        decor.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, null));
        List<String> ids = new ArrayList<>();
        for (net.minecraft.world.item.ItemStack stack : decor.getDisplayItems()) {
            ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        assertTrue(ids.contains("rustic:vase"), "the vase is not offered in the DECOR tab: " + ids);
    }

    /**
     * The datagen that makes the vase render and drop. Its blockstate enumerates all thirteen variant states
     * but each resolves to the one placeholder cube (the real styles are Phase 9), so the assertion is
     * "every referenced model is rustic:block/vase". Re-run {@code runData} when this fails.
     */
    @Test
    public void datagenProducedTheVaseStateModelLootAndTag() {
        Set<String> models = blockstateModels("/assets/rustic/blockstates/vase.json");
        assertEquals(Set.of("rustic:block/vase"), models,
                "every vase state should draw the one static cube while the real styles are deferred");

        JsonObject itemModel = json("/assets/rustic/models/item/vase.json");
        assertEquals("rustic:block/vase", itemModel.get("parent").getAsString(),
                "the vase item model parents the block model");

        JsonObject loot = json("/data/rustic/loot_table/blocks/vase.json");
        JsonElement entry = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0);
        assertEquals("rustic:vase", entry.getAsJsonObject().get("name").getAsString(),
                "the vase must drop itself, as 1.12.2's default did");

        List<String> pickaxe = tagValues("mineable/pickaxe");
        assertTrue(pickaxe.contains("rustic:vase"), "Material.ROCK means the vase is pickaxe-mined");
    }

    // ---------------------------------------------------------------- helpers

    /** Every {@code model} string referenced anywhere in a generated blockstate's {@code variants}. */
    private static Set<String> blockstateModels(String classpathResource) {
        JsonObject variants = json(classpathResource).getAsJsonObject("variants");
        Set<String> models = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
            JsonElement value = variant.getValue();
            if (value.isJsonArray()) {
                for (JsonElement model : value.getAsJsonArray()) {
                    models.add(model.getAsJsonObject().get("model").getAsString());
                }
            } else {
                models.add(value.getAsJsonObject().get("model").getAsString());
            }
        }
        return models;
    }

    private static JsonObject json(String classpathResource) {
        try (Reader reader = new InputStreamReader(resourceStream(classpathResource), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            return fail("could not read " + classpathResource, e);
        }
    }

    private static java.io.InputStream resourceStream(String resource) {
        java.io.InputStream in = RusticMachineRegistrationTest.class.getResourceAsStream(resource);
        assertNotNull(in, "missing resource on the classpath: " + resource
                + " - if this block is new, re-run runData rather than writing the file by hand");
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
