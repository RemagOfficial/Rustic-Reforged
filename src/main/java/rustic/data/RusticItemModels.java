package rustic.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import rustic.Rustic;
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

        // Phase 6 fluid buckets: the nine per-fluid buckets (a deliberate deviation from the legacy universal
        // bucket). There is no per-fluid bucket art yet - the tinted fluid look and the FluidType texture bindings are
        // Phase 9 - so each model parents on the vanilla bucket, which gives datagen a real existing texture to check
        // and shows a plain bucket in-hand for now. The eight placeable fluids plus vanta oil (which has no block) all
        // get one; the seven boozes stay bucket-less, preserving the observable 9/7 split.
        fluidBucket("olive_oil_bucket");
        fluidBucket("ironberry_juice_bucket");
        fluidBucket("wildberry_juice_bucket");
        fluidBucket("grape_juice_bucket");
        fluidBucket("apple_juice_bucket");
        fluidBucket("ale_wort_bucket");
        fluidBucket("honey_bucket");
        fluidBucket("golden_apple_juice_bucket");
        fluidBucket("vanta_oil_bucket");

        // Phase 6 drinkable bottle: a plain generated-item placeholder. The tinted fluid-sprite FluidBottleModel
        // and its item colour handler are Phase 9, so no layer0 art is wired here yet.
        withExistingParent("fluid_bottle", mcLoc("item/generated"));

        // Phase 6 elixir: the same placeholder deal - the legacy item was tinted per stored effect via
        // ElixirUtils.getColor (IColoredItem); that colour handler is Phase 9, so a plain generated item now.
        withExistingParent("elixir", mcLoc("item/generated"));
    }

    /** A fluid bucket item modelled on the vanilla bucket until Phase 9 supplies the tinted art. */
    private void fluidBucket(String name) {
        withExistingParent(name, mcLoc("item/bucket"));
    }

    /** A flat {@code item/generated} sprite whose single layer is the given Rustic texture path. */
    private void herbSprite(String name, String texturePath) {
        withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc(texturePath));
    }
}
