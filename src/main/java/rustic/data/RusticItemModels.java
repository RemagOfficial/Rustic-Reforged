package rustic.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import rustic.Rustic;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.RusticItems;

/**
 * Item models for the Phase 5 standalone items - the produce and the seeds, none of which is a block, so
 * {@link RusticBlockStates} (which only parents an item model on a block model) never emits them. The legacy
 * mod hand-wrote these as {@code item/generated} JSON pointing at {@code rustic:items/<name>}; 1.21 renamed
 * the texture folder to {@code item/}, and {@link ItemModelProvider#basicItem} writes exactly the one-parent-
 * one-texture model with that corrected path, checking the PNG exists at datagen time.
 *
 * <p>The block items (poles, decor, machines) are deliberately absent: their models come from
 * {@link RusticBlockStates}, and generating them twice would collide.
 *
 * <p>The Phase 6 fluid buckets and the drinkable bottle use NeoForge's {@link DynamicFluidContainerModelBuilder}
 * (loader {@code neoforge:fluid_container}) instead of a static sprite: the model composites a {@code base}
 * container shape with a {@code fluid} mask that is filled at render time with the still texture of whichever
 * fluid the stack actually holds (resolved through {@code FluidUtil.getFluidContained}). The fluid still textures
 * are already fully coloured, so no item colour handler is required - the default white tint leaves them as-is.
 */
public class RusticItemModels extends ItemModelProvider {

    public RusticItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Rustic.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(RusticItems.TOMATO.get());
        basicItem(RusticItems.CHILI_PEPPER.get());
        basicItem(RusticItems.GHOST_PEPPER.get());
        basicItem(RusticItems.TOMATO_SEEDS.get());
        basicItem(RusticItems.CHILI_PEPPER_SEEDS.get());

        // Phase 5 herbs: every herb id has an item - nine self-herb block items and four crop-herb edibles -
        // and all thirteen are flat inventory sprites, exactly the legacy item/generated JSONs. They are not
        // parented on the block model: a HerbBlock's world model is a two-stage cross, which would read wrong
        // in an inventory slot. Eleven use their own item/herbs sprite; the two mushrooms have none and reuse
        // their mature block sprite (block/herbs), as the legacy deathstalk_mushroom/mooncap_mushroom item
        // models did. RusticBlockStates deliberately skips simpleBlockWithItem for these blocks, so generating
        // the item here cannot collide.
        herbSprite("aloe_vera", "item/herbs/aloe_vera");
        herbSprite("blood_orchid", "item/herbs/blood_orchid");
        herbSprite("chamomile", "item/herbs/chamomile");
        herbSprite("cloudsbluff", "item/herbs/cloudsbluff");
        herbSprite("cohosh", "item/herbs/cohosh");
        herbSprite("core_root", "item/herbs/core_root");
        herbSprite("deathstalk_mushroom", "block/herbs/deathstalk_mushroom");
        herbSprite("ginseng", "item/herbs/ginseng");
        herbSprite("horsetail", "item/herbs/horsetail");
        herbSprite("marsh_mallow", "item/herbs/marsh_mallow");
        herbSprite("mooncap_mushroom", "block/herbs/mooncap_mushroom");
        herbSprite("vanta_lily", "item/herbs/vanta_lily");
        herbSprite("wind_thistle", "item/herbs/wind_thistle");

        // Phase 5 vinery. The stem is a placeable block, but its block item is the legacy "Grape Seeds" flat
        // sprite (item/grape_seeds), not a draw of the world cross - so RusticBlockStates emits only the blockstate
        // for it and the item model is generated here, exactly as the self-herbs are. The grapes produce is a plain
        // flat sprite on its own item/grapes texture.
        herbSprite("grape_stem", "item/grape_seeds");
        basicItem(RusticItems.GRAPES.get());

        // Phase 5 wildberry bush. The bush's own block item is drawn by RusticBlockStates (its legacy item parented
        // on the block model); only the berry produce needs a flat sprite here, on the existing item/wildberries art.
        basicItem(RusticItems.WILDBERRIES.get());

        // Phase 6 tree fruits. Both were hand-written item/generated JSONs in legacy, on their own item textures
        // (item/olives, item/ironberries); the olives leaf-drop is the farming-tab icon, the ironberries carry the
        // stacking FULLMETAL effect. Plain flat sprites, so basicItem writes each with its corrected texture path.
        basicItem(RusticItems.OLIVES.get());
        basicItem(RusticItems.IRONBERRIES.get());

        // Phase 6 fluid buckets: the nine per-fluid buckets (a deliberate deviation from the legacy universal
        // bucket). Each is a DynamicFluidContainerModel - the vanilla bucket as its base and NeoForge's own
        // bucket_fluid mask as the fluid layer - baked against its own source fluid, so it renders that fluid's
        // still texture inside the bucket. The eight placeables plus vanta oil (which has no block) all get one;
        // the seven boozes stay bucket-less, preserving the observable 9/7 split.
        fluidBucket("olive_oil_bucket", RusticFluids.OLIVE_OIL.get());
        fluidBucket("ironberry_juice_bucket", RusticFluids.IRONBERRY_JUICE.get());
        fluidBucket("wildberry_juice_bucket", RusticFluids.WILDBERRY_JUICE.get());
        fluidBucket("grape_juice_bucket", RusticFluids.GRAPE_JUICE.get());
        fluidBucket("apple_juice_bucket", RusticFluids.APPLE_JUICE.get());
        fluidBucket("ale_wort_bucket", RusticFluids.ALE_WORT.get());
        fluidBucket("honey_bucket", RusticFluids.HONEY.get());
        fluidBucket("golden_apple_juice_bucket", RusticFluids.GOLDEN_APPLE_JUICE.get());
        fluidBucket("vanta_oil_bucket", RusticFluids.VANTA_OIL.get());

        // Phase 6 drinkable bottle: the legacy three-layer bottle reduced to base + fluid. The glass is our own
        // alcohol_bottle art; the alcohol_overlay texture is the liquid mask that the contained fluid's still
        // texture is poured into at render time (resolved through the bottle's registered fluid handler). Baked
        // against Fluids.EMPTY so an empty bottle shows only the glass, and a filled one re-skins to its contents.
        fluidContainer("fluid_bottle", "item/alcohol_bottle", "item/alcohol_overlay", Fluids.EMPTY);

        // Phase 6 elixir, tint restored in Phase 17 S17.4: the legacy two-layer bottle. layer0 is the
        // elixir_overlay mask (tint index 0) coloured per stored effect by the item colour handler in RusticClient
        // (ElixirUtils.getColor - the first effect's colour, else the magenta NO_EFFECTS fallback); layer1 is the
        // glass bottle (tint index 1), which that handler leaves white. Matches the legacy item/elixir.json layer
        // order exactly (legacy: layer0 items/elixir_overlay, layer1 items/elixir_bottle).
        withExistingParent("elixir", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/elixir_overlay"))
                .texture("layer1", modLoc("item/elixir_bottle"));

        // Phase 7 prerequisite items: the two bee products, the flesh tallow, the two smelting dusts and the
        // two crushing-tub/viney seed byproducts. All seven are flat inventory sprites on their own existing
        // item textures (item/honeycomb, item/beeswax, item/tallow, item/dust_tiny_iron, item/dust_gold,
        // item/apple_seeds, item/grape_seeds), so basicItem writes each one-parent/one-texture model and
        // validates the PNG at datagen time. The grape_seeds sprite is shared with the grape_stem block item
        // above, but the two models live under different item ids and cannot collide.
        basicItem(RusticItems.HONEYCOMB.get());
        basicItem(RusticItems.BEESWAX.get());
        // Phase 8: the beehive's bee drop, a flat sprite on the existing item/bee art (legacy ModItems.BEE).
        basicItem(RusticItems.BEE.get());
        basicItem(RusticItems.TALLOW.get());
        basicItem(RusticItems.IRON_DUST_TINY.get());
        basicItem(RusticItems.GOLD_DUST.get());
        basicItem(RusticItems.APPLE_SEEDS.get());
        basicItem(RusticItems.GRAPE_SEEDS.get());
    }

    /**
     * A per-fluid bucket: NeoForge's dynamic fluid-container model with the vanilla bucket as the base and the
     * engine's own {@code bucket_fluid} mask as the fluid layer, baked against {@code fluid}. Because the base
     * model already carries the correct fluid, the bucket renders it even before the runtime contained-fluid
     * override resolves, and the pre-coloured still texture means no item colour handler is needed.
     */
    private void fluidBucket(String name, Fluid fluid) {
        getBuilder(name)
                .texture("base", mcLoc("item/bucket"))
                .texture("fluid", ResourceLocation.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(fluid)
                .end();
    }

    /**
     * A dynamic fluid container (the drinkable bottle) with a Rustic base and a Rustic fluid mask, baked against
     * {@code defaultFluid}. {@link Fluids#EMPTY} makes the resting model show only the container; the runtime
     * contained-fluid override then pours whichever fluid the stack holds into the mask.
     */
    private void fluidContainer(String name, String baseTexture, String fluidMaskTexture, Fluid defaultFluid) {
        getBuilder(name)
                .texture("base", modLoc(baseTexture))
                .texture("fluid", modLoc(fluidMaskTexture))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(defaultFluid)
                .end();
    }

    /** A flat {@code item/generated} sprite whose single layer is the given Rustic texture path. */
    private void herbSprite(String name, String texturePath) {
        withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc(texturePath));
    }
}
