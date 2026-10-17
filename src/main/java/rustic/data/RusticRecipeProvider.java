package rustic.data;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticBlocks.Furniture;
import rustic.content.blocks.RusticBlocks.WoodFamily;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.crafting.BrewingBarrelRecipe;
import rustic.content.crafting.CondenserRecipe;
import rustic.content.crafting.CrushingTubRecipe;
import rustic.content.crafting.EvaporatingBasinRecipe;
import rustic.content.crafting.RecipeNonIngredientReturn;
import rustic.content.crafting.RecipeOliveOil;
import rustic.content.crafting.RecipeVantaOil;
import rustic.content.fluids.RusticFluids;
import rustic.content.items.RusticItems;
import rustic.content.potions.ElixirUtils;
import rustic.content.potions.RusticMobEffects;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Recipe datagen: the 1.21 replacement for the vanilla-crafting and smelting half of 1.12.2's
 * {@code rustic.common.crafting.Recipes} ({@code addCraftingRecipes} / {@code addSmeltingRecipes} /
 * {@code addSilverCraftingRecipes}).
 *
 * <p>Legacy built these recipes in code at {@code FMLPreInitializationEvent} with {@code GameRegistry}
 * and {@code ShapedOreRecipe}; here every one of them is emitted as JSON under
 * {@code data/rustic/recipe/}. The ore-dictionary ingredient names ({@code plankWood}, {@code dyeRed},
 * {@code wax}, {@code ingotSilver}) become the item tags that {@link RusticItemTags} and NeoForge's
 * conventional tags already populate, so a recipe asking for "any plank" asks for {@code #minecraft:planks}.
 *
 * <p><b>Config gating.</b> The legacy {@code enable_*} flags wrapped each group at recipe-registration
 * time. 1.21 recipes are baked JSON, and {@code ModConfigSpec} answers are not readable during
 * {@code runData} (the config is not loaded yet), so - exactly as {@link RusticBlockTags} and the creative
 * tabs already do - the datagen emits the shipped-config set, where every one of these flags defaults
 * true. Turning a group off stays a runtime/creative-tab concern rather than a per-user recipe removal.
 * The one external-material gate the legacy code could not resolve statically is the silver set, which
 * 1.12.2 also required {@code ingotSilver} to exist for: those recipes are still always emitted, but
 * wrapped in a {@code neoforge:not}/{@code neoforge:tag_empty} condition on {@code c:ingots/silver}, so
 * they drop out of an install that provides no silver, reproducing the legacy {@code OreDictionary} check.
 *
 * <p><b>Also here (P7.3):</b> the four special crafting-table types (cabinet, non-ingredient return, olive oil,
 * vanta oil) are now emitted too - the three dynamic pattern recipes as one-line {@code Unit}-codec stubs, the
 * non-ingredient-return family (ale wort, bottle/barrel emptying) as data-driven shapeless rows. The
 * {@code dynamictrees} seed conversions remain Phase 10 compat, so this class emits shaped + smelting JSON, the
 * four machine recipe types (crushing tub, evaporating basin, brewing barrel, condenser) built straight through
 * their Phase-4b serializers, and the four custom crafting-table types.</p>
 *
 * <p><b>The JSON half.</b> 1.12.2 kept its recipes in two places: {@code Recipes.init()} and 43 data-driven files
 * under {@code assets/rustic/recipes/}. Porting only the first of those is what left the iron and gold mounts, the
 * Rustic wood slabs/stairs/fences, the workstations, the rope, the vase and the gargoyle with no recipe at all -
 * every one of those files whose result this build registers is now emitted here too. The five whose results do not
 * exist yet ({@code apiary}, {@code retort}, {@code retort_advanced} and the two door rows) wait for the phases
 * that add those blocks. The one recipe that was still hand-written in {@code src/main/resources}, the Patchouli
 * almanac, is emitted here too - see {@link #addAlmanac(RecipeOutput)}.
 */
public class RusticRecipeProvider extends RecipeProvider {

    /** The conventional silver-ingot tag the legacy {@code ingotSilver} ore name maps onto. */
    private static final TagKey<Item> SILVER_INGOTS = cTag("ingots/silver");
    /** The beeswax/tallow {@code wax} ore name, now a c: tag. */
    private static final TagKey<Item> WAX = cTag("wax");
    /** The legacy {@code dustRedstone} ore name. */
    private static final TagKey<Item> DUST_REDSTONE = cTag("dusts/redstone");

    public RusticRecipeProvider(PackOutput output, CompletableFuture<net.minecraft.core.HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        addPillars(recipeOutput);
        addSlate(recipeOutput);
        addClayWalls(recipeOutput);
        addFurniture(recipeOutput);
        addIronLattice(recipeOutput);
        addPaintedWood(recipeOutput);
        addWoodVariants(recipeOutput);
        addMetalMounts(recipeOutput);
        addMachines(recipeOutput);
        addMiscDecor(recipeOutput);
        addSilver(recipeOutput);
        addSmelting(recipeOutput);
        addCrushingTub(recipeOutput);
        addEvaporatingBasin(recipeOutput);
        addBrewingBarrel(recipeOutput);
        addCondenser(recipeOutput);
        addCraftingTableRecipes(recipeOutput);
        addAlmanac(recipeOutput);
    }

    // ------------------------------------------------------------------
    // Stone pillars (Recipes.addCraftingRecipes, the pillar block). Each pillar is three rows of two
    // stone of the matching variety; the slate pillar was additionally nested under the slate flag there.
    // ------------------------------------------------------------------
    private void addPillars(RecipeOutput out) {
        pillar(out, RusticBlocks.STONE_PILLAR.get(), Blocks.STONE, "pillar_stone");
        pillar(out, RusticBlocks.ANDESITE_PILLAR.get(), Blocks.ANDESITE, "pillar_andesite");
        pillar(out, RusticBlocks.DIORITE_PILLAR.get(), Blocks.DIORITE, "pillar_diorite");
        pillar(out, RusticBlocks.GRANITE_PILLAR.get(), Blocks.GRANITE, "pillar_granite");
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_PILLAR.get(), 6)
                .define('S', RusticBlocks.SLATE.get())
                .pattern("SS").pattern("SS").pattern("SS")
                .unlockedBy(getHasName(RusticBlocks.SLATE.get()), has(RusticBlocks.SLATE.get()))
                .save(out, rl("pillar_slate"));
    }

    private void pillar(RecipeOutput out, ItemLike result, ItemLike stone, String id) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 6)
                .define('S', stone)
                .pattern("SS ").pattern("SS ").pattern("SS ")
                .unlockedBy(getHasName(stone), has(stone))
                .save(out, rl(id));
    }

    // ------------------------------------------------------------------
    // The slate family (ENABLE_SLATE): roof, its stairs/slab, brick stairs/slab, tile (from slate or
    // brick), brick (from tile) and chiseled (from brick). Numbers and patterns are the legacy calls.
    // ------------------------------------------------------------------
    private void addSlate(RecipeOutput out) {
        shaped(RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_ROOF.get(), 4, 'S', RusticBlocks.SLATE.get())
                .pattern("SS").pattern("SS")
                .unlockedBy(getHasName(RusticBlocks.SLATE.get()), has(RusticBlocks.SLATE.get()))
                .save(out, rl("slate_roof"));
        stairs(RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_ROOF_STAIRS.get(), RusticBlocks.SLATE_ROOF.get())
                .save(out, rl("slate_roof_stairs"));
        slab(out, RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_ROOF_SLAB.get(), RusticBlocks.SLATE_ROOF.get(),
                "slate_roof_slab");
        stairs(RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_BRICK_STAIRS.get(), RusticBlocks.SLATE_BRICK.get())
                .save(out, rl("slate_brick_stairs"));
        slab(out, RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_BRICK_SLAB.get(), RusticBlocks.SLATE_BRICK.get(),
                "slate_brick_slab");
        // Tile from slate, and tile again from brick - two distinct ids because the result item is the same.
        single(out, RusticBlocks.SLATE_TILE.get(), RusticBlocks.SLATE.get(), "slate_tile");
        single(out, RusticBlocks.SLATE_TILE.get(), RusticBlocks.SLATE_BRICK.get(), "slate_tile_1");
        shaped(RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_BRICK.get(), 4, 'S', RusticBlocks.SLATE_TILE.get())
                .pattern("SS").pattern("SS")
                .unlockedBy(getHasName(RusticBlocks.SLATE_TILE.get()), has(RusticBlocks.SLATE_TILE.get()))
                .save(out, rl("slate_brick"));
        shaped(RecipeCategory.BUILDING_BLOCKS, RusticBlocks.SLATE_CHISELED.get(), 4, 'S', RusticBlocks.SLATE_BRICK.get())
                .pattern("SS").pattern("SS")
                .unlockedBy(getHasName(RusticBlocks.SLATE_BRICK.get()), has(RusticBlocks.SLATE_BRICK.get()))
                .save(out, rl("slate_chiseled"));
    }

    // ------------------------------------------------------------------
    // Clay walls (ENABLE_CLAY_WALLS): planks frame a clay or clay-wall core. plankWood -> #minecraft:planks.
    // ------------------------------------------------------------------
    private void addClayWalls(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.CLAY_WALL.get(), 8)
                .define('P', ItemTags.PLANKS)
                .define('C', Blocks.CLAY)
                .pattern(" P ").pattern("PCP").pattern(" P ")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("clay_wall"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.CLAY_WALL_CROSS.get())
                .define('P', ItemTags.PLANKS)
                .define('C', RusticBlocks.CLAY_WALL.get())
                .pattern("P P").pattern(" C ").pattern("P P")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("clay_wall_cross"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.CLAY_WALL_DIAG.get())
                .define('P', ItemTags.PLANKS)
                .define('C', RusticBlocks.CLAY_WALL.get())
                .pattern("P  ").pattern(" C ").pattern("  P")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("clay_wall_diag"));
    }

    // ------------------------------------------------------------------
    // Chairs (ENABLE_CHAIRS, 4 per craft) and tables (ENABLE_TABLES, 2 per craft), eight woods each. The
    // six vanilla woods use their planks block; olive and ironwood use the Rustic wood-family planks.
    // ------------------------------------------------------------------
    private void addFurniture(RecipeOutput out) {
        for (Furniture furniture : RusticBlocks.FURNITURE) {
            ItemLike planks = planksFor(furniture.name());
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, furniture.chair().get(), 4)
                    .define('P', planks)
                    .define('S', Items.STICK)
                    .pattern("P  ").pattern("PPP").pattern("S S")
                    .group("chair")
                    .unlockedBy(getHasName(planks), has(planks))
                    .save(out, rl(furniture.name() + "_chair"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, furniture.table().get(), 2)
                    .define('P', planks)
                    .define('S', Items.STICK)
                    .pattern("PPP").pattern("S S")
                    .group("table")
                    .unlockedBy(getHasName(planks), has(planks))
                    .save(out, rl(furniture.name() + "_table"));
        }
    }

    /** The planks a furniture recipe uses: a vanilla plank block, or the matching Rustic wood-family planks. */
    private static ItemLike planksFor(String wood) {
        return switch (wood) {
            case "oak" -> Blocks.OAK_PLANKS;
            case "big_oak" -> Blocks.DARK_OAK_PLANKS; // 1.12.2's "big_oak" is 1.21 dark oak
            case "birch" -> Blocks.BIRCH_PLANKS;
            case "spruce" -> Blocks.SPRUCE_PLANKS;
            case "acacia" -> Blocks.ACACIA_PLANKS;
            case "jungle" -> Blocks.JUNGLE_PLANKS;
            default -> {
                for (WoodFamily family : RusticBlocks.WOOD_FAMILIES) {
                    if (family.name().equals(wood)) {
                        yield family.planks().get();
                    }
                }
                throw new IllegalArgumentException("No planks for furniture wood " + wood);
            }
        };
    }

    // ------------------------------------------------------------------
    // Iron lattice (ENABLE_LATTICE): cross of five iron ingots -> 16. ingotIron again becomes NeoForge's
    // c:ingots/iron, which vanilla fills, so this crafts in a Rustic-only install.
    // ------------------------------------------------------------------
    private void addIronLattice(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.IRON_LATTICE.get(), 16)
                .define('I', Tags.Items.INGOTS_IRON)
                .pattern(" I ").pattern("III").pattern(" I ")
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(out, rl("iron_lattice"));
    }

    // ------------------------------------------------------------------
    // Painted wood (ENABLE_PAINTED_WOOD): eight planks ringed around a dye, one per colour. The result id
    // keeps the legacy spelling (light gray is still painted_wood_silver). dye<Name> -> c:dyes/<name>.
    // ------------------------------------------------------------------
    private void addPaintedWood(RecipeOutput out) {
        for (DyeColor color : DyeColor.values()) {
            TagKey<Item> dye = cTag("dyes/" + color.getName());
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.PAINTED_WOOD.get(color).get(), 8)
                    .define('P', ItemTags.PLANKS)
                    .define('D', dye)
                    .pattern("PPP").pattern("PDP").pattern("PPP")
                    .group("painted_wood")
                    .unlockedBy("has_planks", has(ItemTags.PLANKS))
                    .unlockedBy("has_dye", has(dye))
                    .save(out, rl(RusticBlocks.paintedWoodPath(color)));
        }
    }

    // ------------------------------------------------------------------
    // The Rustic woods' own plank parts (olive_planks.json, olive_slab.json, ironwood_stairs.json and the
    // rest). One log squares to four planks, and the plank then makes the slab, stairs, fence and gate in
    // vanilla's own patterns - which is what 1.12.2 shipped, its groups being the vanilla ones. The recipe
    // ids stay the legacy file names (olive_planks, not planks_olive) so the two sets can be compared.
    // ------------------------------------------------------------------
    private void addWoodVariants(RecipeOutput out) {
        for (WoodFamily family : RusticBlocks.WOOD_FAMILIES) {
            String wood = family.name();
            ItemLike planks = family.planks().get();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, planks, 4)
                    .requires(family.log().get())
                    .group("planks")
                    .unlockedBy(getHasName(family.log().get()), has(family.log().get()))
                    .save(out, rl(wood + "_planks"));
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, family.slab().get(), 6)
                    .define('P', planks)
                    .pattern("PPP")
                    .group("wooden_slab")
                    .unlockedBy(getHasName(planks), has(planks))
                    .save(out, rl(wood + "_slab"));
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, family.stairs().get(), 4)
                    .define('P', planks)
                    .pattern("P  ").pattern("PP ").pattern("PPP")
                    .group("wooden_stairs")
                    .unlockedBy(getHasName(planks), has(planks))
                    .save(out, rl(wood + "_stairs"));
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, family.fence().get(), 3)
                    .define('P', planks).define('S', Items.STICK)
                    .pattern("PSP").pattern("PSP")
                    .group("wooden_fence")
                    .unlockedBy(getHasName(planks), has(planks))
                    .save(out, rl(wood + "_fence"));
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, family.fenceGate().get())
                    .define('P', planks).define('S', Items.STICK)
                    .pattern("SPS").pattern("SPS")
                    .unlockedBy(getHasName(planks), has(planks))
                    .save(out, rl(wood + "_fence_gate"));
        }
    }

    // ------------------------------------------------------------------
    // The metal mounts (chain.json, candle_gold.json, iron_lantern.json, iron_torch.json and the rest of
    // 1.12.2's JSON set). Iron and gold were plain JSON and so always existed; only the silver six were
    // built in code behind the ingotSilver test - see addSilver. The ore-dict names become NeoForge's
    // conventional tags, which NeoForge itself fills from vanilla (c:ingots/iron, c:ingots/gold,
    // c:nuggets/iron), so a Rustic-only install can craft all of them.
    // ------------------------------------------------------------------
    private void addMetalMounts(RecipeOutput out) {
        mounts(out, "", RusticBlocks.CHAIN.get(), RusticBlocks.CANDLE.get(), RusticBlocks.CANDLE_DOUBLE.get(),
                RusticBlocks.CANDLE_LEVER.get(), RusticBlocks.CHANDELIER.get(), RusticBlocks.IRON_LANTERN.get(),
                "iron_lantern", Tags.Items.INGOTS_IRON);
        mounts(out, "_gold", RusticBlocks.CHAIN_GOLD.get(), RusticBlocks.CANDLE_GOLD.get(),
                RusticBlocks.CANDLE_DOUBLE_GOLD.get(), RusticBlocks.CANDLE_LEVER_GOLD.get(),
                RusticBlocks.CHANDELIER_GOLD.get(), RusticBlocks.GOLDEN_LANTERN.get(),
                "golden_lantern", Tags.Items.INGOTS_GOLD);
        // The iron torch is the mount of the group that costs nuggets rather than ingots.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.IRON_TORCH.get(), 4)
                .define('I', Tags.Items.NUGGETS_IRON)
                .define('C', Ingredient.of(Items.COAL, Items.CHARCOAL))
                .define('S', Items.STICK)
                .pattern("I").pattern("C").pattern("S")
                .unlockedBy("has_iron_nugget", has(Tags.Items.NUGGETS_IRON))
                .save(out, rl("iron_torch"));
    }

    /**
     * The six mount recipes for one metal, in the patterns 1.12.2 used identically for iron, gold and silver.
     * {@code suffix} is the legacy id tail ({@code ""} for iron, {@code "_gold"}, {@code "_silver"}); the lantern
     * id is spelled out because it is the one that does not follow it (iron_lantern, golden_lantern,
     * silver_lantern).
     */
    private void mounts(RecipeOutput out, String suffix, ItemLike chain, ItemLike candle, ItemLike candleDouble,
                        ItemLike candleLever, ItemLike chandelier, ItemLike lantern, String lanternId,
                        TagKey<Item> ingots) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, chain, 12)
                .define('I', ingots)
                .pattern("I").pattern("I").pattern("I")
                .unlockedBy("has_ingot", has(ingots))
                .save(out, rl("chain" + suffix));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, candle, 4)
                .define('S', Items.STRING).define('W', WAX).define('I', ingots)
                .pattern("S").pattern("W").pattern("I")
                .unlockedBy("has_ingot", has(ingots))
                .save(out, rl("candle" + suffix));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, candleDouble, 4)
                .define('S', Items.STRING).define('W', WAX).define('I', ingots)
                .pattern("S S").pattern("W W").pattern(" I ")
                .unlockedBy("has_ingot", has(ingots))
                .save(out, rl("candle_double" + suffix));
        // The lever is that candle with a redstone switch wired under it, so it unlocks off the candle.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, candleLever)
                .define('C', candle).define('R', DUST_REDSTONE)
                .pattern("C").pattern("R")
                .unlockedBy("has_candle", has(candle))
                .save(out, rl("candle_lever" + suffix));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, chandelier, 2)
                .define('I', ingots).define('C', chain)
                .pattern(" I ").pattern("C C").pattern("III")
                .unlockedBy("has_chain", has(chain))
                .save(out, rl("chandelier" + suffix));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, lantern, 4)
                .define('I', ingots)
                .define('C', Ingredient.of(Items.COAL, Items.CHARCOAL))
                .pattern("I").pattern("C").pattern("I")
                .unlockedBy("has_ingot", has(ingots))
                .save(out, rl(lanternId));
    }

    // ------------------------------------------------------------------
    // The workstations (barrel.json, crushing_tub.json, condenser.json and friends). The three banded wood
    // cases are plank + wooden slab + iron; slabWood became the vanilla wooden-slab item tag, which
    // RusticItemTags now also puts the two Rustic slabs in, so those still count. 1.12's hardened_clay is
    // today's terracotta, and its stained_hardened_clay meta 0 - the condenser's clay dish - is white
    // terracotta.
    // ------------------------------------------------------------------
    private void addMachines(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.BARREL.get(), 2)
                .define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('I', Tags.Items.INGOTS_IRON)
                .pattern("PSP").pattern("I I").pattern("PSP")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("barrel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.LIQUID_BARREL.get(), 2)
                .define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('I', Tags.Items.INGOTS_IRON)
                .pattern("P P").pattern("I I").pattern("PSP")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("liquid_barrel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.BREWING_BARREL.get())
                .define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('I', Tags.Items.INGOTS_IRON)
                .pattern("PIP").pattern("S S").pattern("PIP")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("brewing_barrel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.CRUSHING_TUB.get())
                .define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('I', Tags.Items.INGOTS_IRON)
                .pattern("P P").pattern("I I").pattern("SSS")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("crushing_tub"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.EVAPORATING_BASIN.get())
                .define('#', Blocks.TERRACOTTA)
                .pattern("# #").pattern(" # ")
                .unlockedBy("has_terracotta", has(Blocks.TERRACOTTA))
                .save(out, rl("evaporating_basin"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.CONDENSER.get())
                .define('B', Items.BRICK).define('E', Items.BUCKET).define('C', Blocks.WHITE_TERRACOTTA)
                .pattern(" B ").pattern("BEB").pattern("BCB")
                .group("rustic_condenser")
                .unlockedBy("has_brick", has(Items.BRICK))
                .save(out, rl("condenser"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.CONDENSER_ADVANCED.get())
                .define('B', Items.NETHER_BRICK).define('E', Items.BUCKET).define('I', Blocks.IRON_BLOCK)
                .pattern(" B ").pattern("BEB").pattern("BIB")
                .group("rustic_condenser")
                .unlockedBy("has_nether_brick", has(Items.NETHER_BRICK))
                .save(out, rl("condenser_advanced"));
    }

    // ------------------------------------------------------------------
    // The remaining decor JSON rows: rope, the grape stake, the vase, the wooden lantern, the gargoyle,
    // fertile soil and the grape stem. Legacy called the last one grape_seeds.json while its result was the
    // plantable grape_stem block item, so the id here names what actually comes out.
    // ------------------------------------------------------------------
    private void addMiscDecor(RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.ROPE.get(), 12)
                .define('S', Items.STRING)
                .pattern("S").pattern("S").pattern("S")
                .unlockedBy("has_string", has(Items.STRING))
                .save(out, rl("rope"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticFarmBlocks.CROP_STAKE.get(), 3)
                .define('P', ItemTags.PLANKS)
                .pattern("P").pattern("P").pattern("P")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .save(out, rl("crop_stake"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.VASE.get(), 6)
                .define('#', Blocks.TERRACOTTA)
                .pattern(" # ").pattern("# #").pattern("###")
                .unlockedBy("has_terracotta", has(Blocks.TERRACOTTA))
                .save(out, rl("vase"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.LANTERN_WOOD.get())
                .define('S', Items.STICK).define('P', ItemTags.PLANKS)
                .define('T', Blocks.TORCH).define('G', Blocks.GLASS_PANE)
                .pattern("SPS").pattern("GTG").pattern("SPS")
                .unlockedBy("has_torch", has(Blocks.TORCH))
                .save(out, rl("lantern_wood"));
        // The gargoyle's stone_slab meta 0 was the smooth-stone slab, which is what stone_slab means today.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticBlocks.GARGOYLE.get(), 2)
                .define('P', Blocks.STONE_PRESSURE_PLATE).define('R', Blocks.STONE).define('S', Blocks.STONE_SLAB)
                .pattern("PRP").pattern("SSS")
                .unlockedBy("has_pressure_plate", has(Blocks.STONE_PRESSURE_PLATE))
                .save(out, rl("gargoyle"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, RusticBlocks.FERTILE_SOIL.get())
                .requires(Items.BONE_MEAL).requires(Blocks.DIRT)
                .unlockedBy("has_dirt", has(Blocks.DIRT))
                .save(out, rl("fertile_soil"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, RusticFarmBlocks.GRAPE_STEM.get())
                .requires(RusticItems.GRAPES.get())
                .unlockedBy(getHasName(RusticItems.GRAPES.get()), has(RusticItems.GRAPES.get()))
                .save(out, rl("grape_stem"));
    }

    // ------------------------------------------------------------------
    // The silver set (addSilverCraftingRecipes). Emitted only when the silver blocks registered at all
    // (RusticBlocks.silverDecorEnabled()); every recipe then carries a not(tag_empty(c:ingots/silver))
    // condition so it drops out when no mod provides silver, reproducing the legacy OreDictionary check.
    // Unlike the iron and gold mounts above this group has always needed another mod: 1.12.2 shipped no
    // silver ingot of its own, and neither does this port.
    // ------------------------------------------------------------------
    private void addSilver(RecipeOutput out) {
        RusticBlocks.SilverDecor silver = RusticBlocks.SILVER_DECOR;
        if (silver == null) {
            return;
        }
        RecipeOutput gated = out.withConditions(new NotCondition(new TagEmptyCondition(SILVER_INGOTS.location())));
        mounts(gated, "_silver", silver.chain().get(), silver.candle().get(), silver.candleDouble().get(),
                silver.candleLever().get(), silver.chandelier().get(), silver.lantern().get(),
                "silver_lantern", SILVER_INGOTS);
    }

    // ------------------------------------------------------------------
    // Smelting (addSmeltingRecipes). The legacy olive/ironwood logs each smelt to charcoal; the tiny/gold
    // dusts (P7.0 items) smelt to an iron nugget / gold ingot; honeycomb to beeswax; and rotten flesh to
    // tallow behind FLESH_SMELTING. Legacy cook time was GameRegistry's default 200 ticks.
    // ------------------------------------------------------------------
    private void addSmelting(RecipeOutput out) {
        smelt(out, Ingredient.of(RusticItems.HONEYCOMB.get()), RusticItems.HONEYCOMB.get(), RecipeCategory.FOOD,
                RusticItems.BEESWAX.get(), 0.3F, "beeswax_from_smelting");
        for (WoodFamily family : RusticBlocks.WOOD_FAMILIES) {
            smelt(out, Ingredient.of(family.log().get()), family.log().get(), RecipeCategory.FOOD, Items.CHARCOAL,
                    0.15F, "charcoal_from_" + family.name() + "_log");
        }
        smelt(out, Ingredient.of(RusticItems.IRON_DUST_TINY.get()), RusticItems.IRON_DUST_TINY.get(),
                RecipeCategory.MISC, Items.IRON_NUGGET, 0.15F, "iron_nugget_from_smelting");
        smelt(out, Ingredient.of(RusticItems.GOLD_DUST.get()), RusticItems.GOLD_DUST.get(), RecipeCategory.MISC,
                Items.GOLD_INGOT, 0.5F, "gold_ingot_from_smelting");
        smelt(out, Ingredient.of(Items.ROTTEN_FLESH), Items.ROTTEN_FLESH, RecipeCategory.MISC,
                RusticItems.TALLOW.get(), 0.3F, "tallow_from_smelting");
    }

    private void smelt(RecipeOutput out, Ingredient ingredient, ItemLike unlock, RecipeCategory category,
                       ItemLike result, float experience, String id) {
        SimpleCookingRecipeBuilder.smelting(ingredient, category, result, experience, 200)
                .unlockedBy(getHasName(unlock), has(unlock))
                .save(out, rl(id));
    }

    // ------------------------------------------------------------------
    // Machine recipes (P7.2). The four Phase-4b custom Recipe types, emitted straight through each
    // serializer's codec by handing the constructed Recipe to RecipeOutput.accept (no advancement, no
    // condition). The rows are 1.12.2's Recipes.addCrushingTubRecipes / addEvaporatingRecipes /
    // addBrewingRecipes / addCondenserRecipes verbatim: ore-dict ingredients become the concrete items the
    // machines match, the crushing-tub fluid amounts (250 mb, golden apple 100/1000) and the evaporating
    // input amounts (500/100 - which also drive the basin's getAmount-tick countdown) are preserved, the
    // brewing quality is left to the barrel block entity at runtime (amount is matching-irrelevant, so the
    // legacy 1 mb placeholder is kept), and every condenser result is a rustic:elixir carrying one
    // MobEffectInstance (ElixirUtils.addEffect) exactly as the legacy Basic/Advanced constructors built it.
    // The dynamictrees apple-seed and the for.honey compat rows are skipped (Phase 10).
    // ------------------------------------------------------------------
    private void addCrushingTub(RecipeOutput out) {
        crush(out, "olive_oil", fluid(RusticFluids.OLIVE_OIL.get(), 250), ing(RusticItems.OLIVES.get()), ItemStack.EMPTY);
        crush(out, "ironberry_juice", fluid(RusticFluids.IRONBERRY_JUICE.get(), 250), ing(RusticItems.IRONBERRIES.get()), ItemStack.EMPTY);
        crush(out, "sugar", fluid(Fluids.WATER, 250), ing(Items.SUGAR_CANE), new ItemStack(Items.SUGAR, 2));
        crush(out, "wildberry_juice", fluid(RusticFluids.WILDBERRY_JUICE.get(), 250), ing(RusticItems.WILDBERRIES.get()), ItemStack.EMPTY);
        crush(out, "grape_juice", fluid(RusticFluids.GRAPE_JUICE.get(), 250), ing(RusticItems.GRAPES.get()), ItemStack.EMPTY);
        crush(out, "apple_juice", fluid(RusticFluids.APPLE_JUICE.get(), 250), ing(Items.APPLE), new ItemStack(RusticItems.APPLE_SEEDS.get()));
        crush(out, "honey", fluid(RusticFluids.HONEY.get(), 250), ing(RusticItems.HONEYCOMB.get()), ItemStack.EMPTY);
        crush(out, "golden_apple_juice", fluid(RusticFluids.GOLDEN_APPLE_JUICE.get(), 100), ing(Items.GOLDEN_APPLE), new ItemStack(RusticItems.APPLE_SEEDS.get()));
        crush(out, "golden_apple_juice_enchanted", fluid(RusticFluids.GOLDEN_APPLE_JUICE.get(), 1000), ing(Items.ENCHANTED_GOLDEN_APPLE), new ItemStack(RusticItems.APPLE_SEEDS.get()));
        crush(out, "vanta_oil", fluid(RusticFluids.VANTA_OIL.get(), 250), ing(RusticFarmBlocks.VANTA_LILY.get()), ItemStack.EMPTY);
    }

    private void addEvaporatingBasin(RecipeOutput out) {
        evaporate(out, "iron_dust_tiny", fluid(RusticFluids.IRONBERRY_JUICE.get(), 500), new ItemStack(RusticItems.IRON_DUST_TINY.get()));
        evaporate(out, "gold_dust", fluid(RusticFluids.GOLDEN_APPLE_JUICE.get(), 100), new ItemStack(RusticItems.GOLD_DUST.get()));
    }

    private void addBrewingBarrel(RecipeOutput out) {
        brew(out, "ale", fluid(RusticFluids.ALE_WORT.get(), 1), fluid(RusticFluids.ALE.get(), 1));
        brew(out, "cider", fluid(RusticFluids.APPLE_JUICE.get(), 1), fluid(RusticFluids.CIDER.get(), 1));
        brew(out, "iron_wine", fluid(RusticFluids.IRONBERRY_JUICE.get(), 1), fluid(RusticFluids.IRON_WINE.get(), 1));
        brew(out, "mead", fluid(RusticFluids.HONEY.get(), 1), fluid(RusticFluids.MEAD.get(), 1));
        brew(out, "wildberry_wine", fluid(RusticFluids.WILDBERRY_JUICE.get(), 1), fluid(RusticFluids.WILDBERRY_WINE.get(), 1));
        brew(out, "wine", fluid(RusticFluids.GRAPE_JUICE.get(), 1), fluid(RusticFluids.WINE.get(), 1));
        brew(out, "ambrosia", fluid(RusticFluids.GOLDEN_APPLE_JUICE.get(), 1), fluid(RusticFluids.AMBROSIA.get(), 1));
    }

    // ------------------------------------------------------------------
    // Condenser (Recipes.addCondenserRecipes). Basic rows (two ingredients, empty modifier) run 400 ticks;
    // advanced rows (a modifier herb - sometimes ItemStack.EMPTY, but always the 3-ingredient advanced
    // shape) run 300 ticks, matching the legacy Basic/Advanced getTime() overrides. The modifier gate rides
    // on Ingredient.EMPTY for basic, exactly as the ported CondenserRecipe.matches reads it.
    // ------------------------------------------------------------------
    private void addCondenser(RecipeOutput out) {
        Ingredient chamomile = ing(RusticFarmBlocks.CHAMOMILE.get());
        Ingredient marshMallow = ing(RusticItems.MARSH_MALLOW.get());
        Ingredient cohosh = ing(RusticFarmBlocks.COHOSH.get());
        Ingredient horsetail = ing(RusticFarmBlocks.HORSETAIL.get());
        Ingredient deathstalk = ing(RusticFarmBlocks.DEATHSTALK_MUSHROOM.get());
        Ingredient mooncap = ing(RusticFarmBlocks.MOONCAP_MUSHROOM.get());
        Ingredient windThistle = ing(RusticFarmBlocks.WIND_THISTLE.get());
        Ingredient aloeVera = ing(RusticFarmBlocks.ALOE_VERA.get());
        Ingredient bloodOrchid = ing(RusticFarmBlocks.BLOOD_ORCHID.get());
        Ingredient coreRoot = ing(RusticItems.CORE_ROOT.get());
        Ingredient ginseng = ing(RusticItems.GINSENG.get());
        Ingredient cloudsbluff = ing(RusticItems.CLOUDSBLUFF.get());
        Ingredient beef = ing(Items.BEEF);
        Ingredient honeycomb = ing(RusticItems.HONEYCOMB.get());
        Ingredient soulSand = ing(Blocks.SOUL_SAND);
        Ingredient spiderEye = ing(Items.SPIDER_EYE);
        Ingredient sugar = ing(Items.SUGAR);
        Ingredient brick = ing(Items.BRICK);
        Ingredient coal = ing(Items.COAL);
        Ingredient rottenFlesh = ing(Items.ROTTEN_FLESH);
        Ingredient redstone = ing(Items.REDSTONE);
        Ingredient ironNugget = ing(Items.IRON_NUGGET);
        Ingredient leather = ing(Items.LEATHER);
        Ingredient clayBall = ing(Items.CLAY_BALL);
        Ingredient bone = ing(Items.BONE);
        Ingredient gunpowder = ing(Items.GUNPOWDER);
        Ingredient featherItem = ing(Items.FEATHER);
        Ingredient paper = ing(Items.PAPER);
        Ingredient ironberries = ing(RusticItems.IRONBERRIES.get());
        Ingredient chiliPepper = ing(RusticItems.CHILI_PEPPER.get());
        Ingredient blazePowder = ing(Items.BLAZE_POWDER);
        Ingredient netherrack = ing(Blocks.NETHERRACK);
        Ingredient empty = Ingredient.EMPTY;

        // Healing (instant health).
        condense(out, "heal_basic", false, empty, List.of(chamomile, beef), new MobEffectInstance(MobEffects.HEAL, 1, 0));
        condense(out, "heal_advanced", true, marshMallow, List.of(chamomile, beef), new MobEffectInstance(MobEffects.HEAL, 1, 1));
        // Regeneration.
        condense(out, "regeneration_basic", false, empty, List.of(cohosh, honeycomb), new MobEffectInstance(MobEffects.REGENERATION, 900, 0));
        condense(out, "regeneration_extended", true, horsetail, List.of(cohosh, honeycomb), new MobEffectInstance(MobEffects.REGENERATION, 1800, 0));
        condense(out, "regeneration_strong", true, marshMallow, List.of(cohosh, honeycomb), new MobEffectInstance(MobEffects.REGENERATION, 450, 1));
        // Wither.
        condense(out, "wither_basic", false, empty, List.of(deathstalk, soulSand), new MobEffectInstance(MobEffects.WITHER, 900, 0));
        condense(out, "wither_extended", true, horsetail, List.of(deathstalk, soulSand), new MobEffectInstance(MobEffects.WITHER, 1800, 0));
        condense(out, "wither_strong", true, marshMallow, List.of(deathstalk, soulSand), new MobEffectInstance(MobEffects.WITHER, 450, 1));
        // Night vision.
        condense(out, "night_vision_basic", false, empty, List.of(mooncap, spiderEye), new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0));
        condense(out, "night_vision_extended", true, horsetail, List.of(mooncap, spiderEye), new MobEffectInstance(MobEffects.NIGHT_VISION, 9600, 0));
        // Speed.
        condense(out, "speed_basic", false, empty, List.of(windThistle, sugar), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 0));
        condense(out, "speed_extended", true, horsetail, List.of(windThistle, sugar), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9600, 0));
        condense(out, "speed_strong", true, marshMallow, List.of(windThistle, sugar), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 1));
        // Fire resistance (advanced, no modifier herb on the shorter one).
        condense(out, "fire_resistance_basic", true, empty, List.of(aloeVera, brick, coal), new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0));
        condense(out, "fire_resistance_extended", true, horsetail, List.of(aloeVera, brick, coal), new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 9600, 0));
        // Health boost.
        condense(out, "health_boost_basic", true, empty, List.of(bloodOrchid, rottenFlesh, redstone), new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 0));
        condense(out, "health_boost_extended", true, horsetail, List.of(bloodOrchid, rottenFlesh, redstone), new MobEffectInstance(MobEffects.HEALTH_BOOST, 9600, 0));
        condense(out, "health_boost_strong", true, marshMallow, List.of(bloodOrchid, rottenFlesh, redstone), new MobEffectInstance(MobEffects.HEALTH_BOOST, 1800, 1));
        // Haste.
        condense(out, "haste_basic", true, empty, List.of(coreRoot, ironNugget, redstone), new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0));
        condense(out, "haste_extended", true, horsetail, List.of(coreRoot, ironNugget, redstone), new MobEffectInstance(MobEffects.DIG_SPEED, 9600, 0));
        condense(out, "haste_strong", true, marshMallow, List.of(coreRoot, ironNugget, redstone), new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1));
        // Strength.
        condense(out, "strength_basic", true, empty, List.of(ginseng, bone, gunpowder), new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 0));
        condense(out, "strength_extended", true, horsetail, List.of(ginseng, bone, gunpowder), new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 0));
        condense(out, "strength_strong", true, marshMallow, List.of(ginseng, bone, gunpowder), new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 1));
        // Iron skin (custom effect).
        condense(out, "iron_skin_basic", true, empty, List.of(ironberries, leather, clayBall), new MobEffectInstance(RusticMobEffects.IRON_SKIN, 3600, 0));
        condense(out, "iron_skin_extended", true, horsetail, List.of(ironberries, leather, clayBall), new MobEffectInstance(RusticMobEffects.IRON_SKIN, 9600, 0));
        condense(out, "iron_skin_strong", true, marshMallow, List.of(ironberries, leather, clayBall), new MobEffectInstance(RusticMobEffects.IRON_SKIN, 1800, 1));
        // Feather (custom effect; the vanilla feather is an ingredient, not the effect).
        condense(out, "feather_basic", true, empty, List.of(cloudsbluff, featherItem, paper), new MobEffectInstance(RusticMobEffects.FEATHER, 3600, 0));
        condense(out, "feather_extended", true, horsetail, List.of(cloudsbluff, featherItem, paper), new MobEffectInstance(RusticMobEffects.FEATHER, 9600, 0));
        // Blazing trail (custom effect).
        condense(out, "blazing_trail_basic", true, empty, List.of(chiliPepper, blazePowder, netherrack), new MobEffectInstance(RusticMobEffects.BLAZING_TRAIL, 3600, 0));
        condense(out, "blazing_trail_extended", true, horsetail, List.of(chiliPepper, blazePowder, netherrack), new MobEffectInstance(RusticMobEffects.BLAZING_TRAIL, 9600, 0));
    }

    private void condense(RecipeOutput out, String id, boolean advanced, Ingredient modifier,
                          List<Ingredient> ingredients, MobEffectInstance effect) {
        ItemStack result = new ItemStack(RusticItems.ELIXIR.get());
        ElixirUtils.addEffect(effect, result);
        out.accept(rl("condenser/" + id), new CondenserRecipe(advanced, modifier, ingredients,
                new FluidStack(Fluids.WATER, 125), new ItemStack(Items.GLASS_BOTTLE), result,
                advanced ? 300 : 400), null);
    }

    private void crush(RecipeOutput out, String id, FluidStack result, Ingredient input, ItemStack byproduct) {
        out.accept(rl("crushing_tub/" + id), new CrushingTubRecipe(input, result, byproduct), null);
    }

    private void evaporate(RecipeOutput out, String id, FluidStack input, ItemStack result) {
        out.accept(rl("evaporating_basin/" + id), new EvaporatingBasinRecipe(input, result), null);
    }

    private void brew(RecipeOutput out, String id, FluidStack input, FluidStack result) {
        out.accept(rl("brewing_barrel/" + id), new BrewingBarrelRecipe(input, result), null);
    }

    private static FluidStack fluid(Fluid fluid, int amount) {
        return new FluidStack(fluid, amount);
    }

    private static Ingredient ing(ItemLike item) {
        return Ingredient.of(item);
    }

    // ------------------------------------------------------------------
    // Small builder helpers.
    // ------------------------------------------------------------------
    private static ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count, char key,
                                              ItemLike ingredient) {
        return ShapedRecipeBuilder.shaped(category, result, count).define(key, ingredient);
    }

    private ShapedRecipeBuilder stairs(RecipeCategory category, ItemLike result, ItemLike base) {
        return ShapedRecipeBuilder.shaped(category, result, 4)
                .define('S', base)
                .pattern("S  ").pattern("SS ").pattern("SSS")
                .unlockedBy(getHasName(base), has(base));
    }

    private void slab(RecipeOutput out, RecipeCategory category, ItemLike result, ItemLike base, String id) {
        ShapedRecipeBuilder.shaped(category, result, 6)
                .define('S', base)
                .pattern("SSS")
                .unlockedBy(getHasName(base), has(base))
                .save(out, rl(id));
    }

    // ------------------------------------------------------------------
    // Phase 7.3 crafting-table recipes. The cabinet is an ordinary shaped crafting-table recipe, so it is emitted as
    // vanilla {@code minecraft:crafting_shaped} JSON - that gives it recipe-book support and JEI's normal Crafting tab
    // instead of inventing a type. The remaining three genuinely cannot be plain JSON: olive/vanta oiling copy the
    // matched food/weapon and merge effects, and the non-ingredient-return rows must suppress a container remainder
    // (the fluid bottle declares {@code craftRemainder(GLASS_BOTTLE)}, so a vanilla shapeless would double it). Those
    // stay code-driven {@code CraftingRecipe}s under the vanilla CRAFTING type, exactly like the game's own
    // {@code crafting_special_*} recipes, and are {@code isSpecial()} so they never clutter the recipe book.
    // Like the rest of this provider every row is emitted unconditionally (config is not loaded at datagen time);
    // RecipeOliveOil checks ENABLE_OLIVE_OILING in its matches(), so a runtime toggle works without regenerating JSON.
    // ------------------------------------------------------------------
    private void addCraftingTableRecipes(RecipeOutput out) {
        out.accept(rl("ale_wort"), new RecipeNonIngredientReturn(
                new ItemStack(RusticItems.ALE_WORT_BUCKET.get()),
                List.of(Ingredient.of(Items.BREAD), Ingredient.of(Items.SUGAR), Ingredient.of(Items.WATER_BUCKET))),
                null);
        out.accept(rl("bottle_emptying"), new RecipeNonIngredientReturn(
                new ItemStack(Items.GLASS_BOTTLE),
                List.of(Ingredient.of(RusticItems.FLUID_BOTTLE.get()))),
                null);
        out.accept(rl("barrel_emptying"), new RecipeNonIngredientReturn(
                new ItemStack(RusticMachineBlocks.LIQUID_BARREL.get()),
                List.of(Ingredient.of(RusticMachineBlocks.LIQUID_BARREL.get()))),
                null);
        // Legacy RecipeCabinet: full plank rows above and below an empty-centre middle row of one plank and one
        // wooden trapdoor.
        cabinetShape(out, rl("cabinet"));
        out.accept(rl("olive_oiling"), new RecipeOliveOil(), null);
        out.accept(rl("vanta_oiling"), new RecipeVantaOil(), null);
    }

    /** The cabinet pattern: full plank rows above and below a middle row of one plank and one trapdoor. */
    private void cabinetShape(RecipeOutput out, ResourceLocation id) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, RusticMachineBlocks.CABINET.get())
                .define('P', ItemTags.PLANKS)
                .define('T', ItemTags.WOODEN_TRAPDOORS)
                .pattern("PPP").pattern("P T").pattern("PPP")
                .group("rustic_cabinet")
                .unlockedBy("has_planks", has(ItemTags.PLANKS))
                .unlockedBy("has_trapdoor", has(ItemTags.WOODEN_TRAPDOORS))
                .save(out, id);
    }

    // ------------------------------------------------------------------
    // The almanac (S11, replacing the last hand-written file under src/main/resources, recipe/almanac.json).
    // The result is Patchouli's own guide_book item carrying the patchouli:book component pointed at
    // data/rustic/patchouli_books/book, exactly as the manual JSON spelled it. Patchouli is a localRuntime-only
    // dependency and is deliberately not on the compile classpath, so neither its item nor its component type
    // can be named here; both are resolved out of the default registries instead, which are populated because
    // runData boots with Patchouli loaded. The neoforge:mod_loaded condition the JSON carried is kept through
    // out.withConditions, so a Patchouli-free install still drops the recipe (and its advancement) at load.
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private void addAlmanac(RecipeOutput out) {
        Item guideBook = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book"));
        DataComponentType<ResourceLocation> bookComponent =
                (DataComponentType<ResourceLocation>) (DataComponentType<?>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                        ResourceLocation.fromNamespaceAndPath("patchouli", "book"));
        if (guideBook == Items.AIR || bookComponent == null) {
            throw new IllegalStateException("almanac datagen requires Patchouli's guide_book item and book "
                    + "component on the runData classpath (Patchouli is wired through localRuntime)");
        }
        ItemStack result = new ItemStack(guideBook);
        result.set(bookComponent, rl("book"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, result)
                .define('O', RusticItems.OLIVES.get())
                .define('I', cTag("nuggets/iron"))
                .define('B', Items.BOOK)
                .pattern(" O ").pattern("IBI").pattern(" I ")
                .unlockedBy(getHasName(RusticItems.OLIVES.get()), has(RusticItems.OLIVES.get()))
                .save(out.withConditions(new ModLoadedCondition("patchouli")), rl("almanac"));
    }

    private void single(RecipeOutput out, ItemLike result, ItemLike base, String id) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, 1)
                .define('S', base)
                .pattern("S")
                .unlockedBy(getHasName(base), has(base))
                .save(out, rl(id));
    }

    /** A Rustic-namespaced {@link ResourceLocation}, the recipe/advancement id used in {@code data/rustic/recipe}. */
    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Rustic.MODID, path);
    }

    /** A conventional-namespaced ({@code c:}) item tag key, matching the tags {@link RusticItemTags} writes. */
    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
