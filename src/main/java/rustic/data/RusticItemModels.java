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

        herbSprite("grape_stem", "item/grape_seeds");
        basicItem(RusticItems.GRAPES.get());

        basicItem(RusticItems.WILDBERRIES.get());

        basicItem(RusticItems.OLIVES.get());
        basicItem(RusticItems.IRONBERRIES.get());

        fluidBucket("olive_oil_bucket", RusticFluids.OLIVE_OIL.get());
        fluidBucket("ironberry_juice_bucket", RusticFluids.IRONBERRY_JUICE.get());
        fluidBucket("wildberry_juice_bucket", RusticFluids.WILDBERRY_JUICE.get());
        fluidBucket("grape_juice_bucket", RusticFluids.GRAPE_JUICE.get());
        fluidBucket("apple_juice_bucket", RusticFluids.APPLE_JUICE.get());
        fluidBucket("ale_wort_bucket", RusticFluids.ALE_WORT.get());
        fluidBucket("honey_bucket", RusticFluids.HONEY.get());
        fluidBucket("golden_apple_juice_bucket", RusticFluids.GOLDEN_APPLE_JUICE.get());
        fluidBucket("vanta_oil_bucket", RusticFluids.VANTA_OIL.get());

        fluidContainer("fluid_bottle", "item/alcohol_bottle", "item/alcohol_overlay", Fluids.EMPTY);

        withExistingParent("elixir", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/elixir_overlay"))
                .texture("layer1", modLoc("item/elixir_bottle"));

        basicItem(RusticItems.HONEYCOMB.get());
        basicItem(RusticItems.BEESWAX.get());

        basicItem(RusticItems.BEE.get());
        basicItem(RusticItems.TALLOW.get());
        basicItem(RusticItems.IRON_DUST_TINY.get());
        basicItem(RusticItems.GOLD_DUST.get());
        basicItem(RusticItems.APPLE_SEEDS.get());
        basicItem(RusticItems.GRAPE_SEEDS.get());
    }

    private void fluidBucket(String name, Fluid fluid) {
        getBuilder(name)
                .texture("base", mcLoc("item/bucket"))
                .texture("fluid", ResourceLocation.fromNamespaceAndPath("neoforge", "item/mask/bucket_fluid"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(fluid)
                .end();
    }

    private void fluidContainer(String name, String baseTexture, String fluidMaskTexture, Fluid defaultFluid) {
        getBuilder(name)
                .texture("base", modLoc(baseTexture))
                .texture("fluid", modLoc(fluidMaskTexture))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                    .fluid(defaultFluid)
                .end();
    }

    private void herbSprite(String name, String texturePath) {
        withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc(texturePath));
    }
}
