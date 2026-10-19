package rustic.content.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;
import rustic.content.fluids.RusticFluidBlocks;
import rustic.content.fluids.RusticFluids;

/**
 * Item registration, ported from 1.12.2's {@code ModItems} plus the item half of {@code ModBlocks}.
 *
 * <p>The 1.12.2 codebase never had a block item problem to solve: every {@code BlockBase} subclass
 * registered its own {@code ItemBlock} from inside its constructor, using the block's own registry
 * name. That implicit loop is what this class reproduces explicitly - one {@link BlockItem} per
 * Rustic block, same id - which also means a later phase cannot add a block and forget its item.
 *
 * <p>The non-block items of {@code ModItems} arrive phase by phase with the content they belong to:
 * the Phase 5 produce and herbs, then the Phase 6 buckets, bottles, elixir and tree fruits, and finally
 * the Phase 7 prerequisite bee products, dusts and seed items (below). The book is still ahead of them.
 *
 * <p>Ordering note: this class must be touched from the mod constructor, like
 * {@link RusticBlocks}, so that both registers are attached to the bus before the loader fires
 * {@code RegisterEvent}. Reading {@link RusticBlocks} first is what makes the block list complete by
 * the time the items are derived from it.
 */
public final class RusticItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Rustic.MODID);

    // ------------------------------------------------------------------
    // Phase 5 produce, from 1.12.2's ModItems. The hunger/saturation numbers are the legacy
    // ItemFoodBase constructor arguments (nutrition, saturation modifier) read off each field:
    // tomato (4, 0.4), chili pepper (3, 0.4), ghost pepper (4, 0.7). 1.21 folded the old
    // setPotionEffect food effects into FoodProperties; the ones a FoodProperties cannot hold - the
    // tomato's sneak-throw, the ghost pepper's burn damage, the ironberries' stacking fullmetal -
    // arrived with Phase 6 as item subclasses, and the nausea-carrying olives came with them as
    // plain FoodProperties data.
    // ------------------------------------------------------------------
    public static final DeferredItem<ItemTomato> TOMATO = ITEMS.register("tomato",
            () -> new ItemTomato(food(4, 0.4F)));
    public static final DeferredItem<Item> CHILI_PEPPER =
            ITEMS.registerItem("chili_pepper", Item::new, food(3, 0.4F));
    // TODO Phase 5 handoff: the chili's legacy bite (alwaysEdible, 20 s speed, 1-in-24 burn) and the
    // tomato-crop harvest bonus (getCrop's 1-in-42 ghost pepper) were left out when P5 kept the
    // registrations only; they belong to this produce group and are parked with the phase-tail polish.
    public static final DeferredItem<ItemGhostPepper> GHOST_PEPPER = ITEMS.register("ghost_pepper",
            () -> new ItemGhostPepper(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.7F)
                    .alwaysEdible()
                    .build())));

    // The seeds that plant the stake crops. Each carries its crop as a lazy supplier, because the crop
    // block lives in RusticFarmBlocks and neither register can read the other's fields while its own
    // static initialiser is still running.
    public static final DeferredItem<StakeCropSeedItem> TOMATO_SEEDS = ITEMS.register("tomato_seeds",
            () -> new StakeCropSeedItem(new Item.Properties(), () -> RusticFarmBlocks.TOMATO_CROP.get()));
    public static final DeferredItem<StakeCropSeedItem> CHILI_PEPPER_SEEDS = ITEMS.register("chili_pepper_seeds",
            () -> new StakeCropSeedItem(new Item.Properties(), () -> RusticFarmBlocks.CHILI_CROP.get()));

    // ------------------------------------------------------------------
    // Phase 5 herb edibles: the four crop-herbs whose produce is also their own seed (ItemHerbEdible), each
    // registered under the same id as its HerbBlock in RusticFarmBlocks (which deliberately gave them no
    // block item). Nutrition/saturation are the legacy ItemFoodBase arguments - cloudsbluff (2, 0.2), core
    // root (2, 0.3), ginseng (2, 0.3), marsh mallow (3, 0.3). Only cloudsbluff carried a potion effect in
    // 1.12.2 (four seconds of levitation, always edible); 1.21 folds it into FoodProperties, and it is the
    // vanilla MobEffects.LEVITATION - no custom Rustic effect, which is Phase 6.
    // ------------------------------------------------------------------
    public static final DeferredItem<ItemHerbEdible> CLOUDSBLUFF = ITEMS.register("cloudsbluff",
            () -> new ItemHerbEdible(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.2F)
                    .alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.LEVITATION, 400), 1.0F)
                    .build()),
                    () -> RusticFarmBlocks.CLOUDSBLUFF.get()));
    public static final DeferredItem<ItemHerbEdible> CORE_ROOT = ITEMS.register("core_root",
            () -> new ItemHerbEdible(food(2, 0.3F), () -> RusticFarmBlocks.CORE_ROOT.get()));
    public static final DeferredItem<ItemHerbEdible> GINSENG = ITEMS.register("ginseng",
            () -> new ItemHerbEdible(food(2, 0.3F), () -> RusticFarmBlocks.GINSENG.get()));
    public static final DeferredItem<ItemHerbEdible> MARSH_MALLOW = ITEMS.register("marsh_mallow",
            () -> new ItemHerbEdible(food(3, 0.3F), () -> RusticFarmBlocks.MARSH_MALLOW.get()));

    // ------------------------------------------------------------------
    // Phase 5 vinery produce. The legacy ItemFoodBase("grapes", 3, 0.3F, false) overrode getMaxItemUseDuration to
    // 16 ticks - "taking half as long to eat as normal foods" - which is 1.21's FoodProperties.Builder.fast()
    // (eat-seconds 0.8). No potion effect, so it stays a plain food; grape juice/wine are the fluid/alchemy phases.
    // ------------------------------------------------------------------
    public static final DeferredItem<Item> GRAPES = ITEMS.registerItem("grapes", Item::new,
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.3F)
                    .fast()
                    .build()));

    // ------------------------------------------------------------------
    // Phase 5 berry produce. The legacy ItemFoodBase("wildberries", 2, 0.5F, false) also overrode the eat time to
    // 16 ticks ("eaten faster than normal foods") - 1.21's fast() - and is not very filling, so a low nutrition.
    // ------------------------------------------------------------------
    public static final DeferredItem<Item> WILDBERRIES = ITEMS.registerItem("wildberries", Item::new,
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.5F)
                    .fast()
                    .build()));

    // ------------------------------------------------------------------
    // Phase 6 tree fruits, the potion-food half of 1.12.2's ModItems. Both were anonymous
    // ItemFoodBase subclasses; the leaf-drop chances that produce them (1/6 olives, 1/16 ironberries,
    // shrinking with fortune) live in RusticBlockLoot, the trees that bear the leaves stay Phase 8.
    //
    // OLIVES: (1, 0.4), the legacy setPotionEffect(NAUSEA, 200, 1, 0.95F) folds onto FoodProperties as
    // a probabilistic effect (1.20.5 renamed NAUSEA to CONFUSION), and the 24-tick eat override is code
    // in ItemOlives - 1.21.1's FoodProperties.Builder has no eatSeconds setter, only fast() (16 ticks,
    // the ironberries' number). No always-edible - the legacy initFood override only set the potion.
    // Legacy lore: "Raw, unprocessed olives should not be consumed" - the nausea IS that lore, still one
    // of the mod's strangest foods.
    // ------------------------------------------------------------------
    public static final DeferredItem<ItemOlives> OLIVES = ITEMS.register("olives",
            () -> new ItemOlives(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0.4F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 1), 0.95F)
                    .build())));

    // IRONBERRIES: (2, 0.4), always edible per the legacy initFood override, the 16-tick eat override
    // is fast(), and the stacking-fullmetal onFoodEaten is code - see ItemIronberries.
    public static final DeferredItem<ItemIronberries> IRONBERRIES = ITEMS.register("ironberries",
            () -> new ItemIronberries(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.4F)
                    .alwaysEdible()
                    .fast()
                    .build())));

    /** 1.12.2's {@code ItemFoodBase(hunger, saturation)} pair, built into a plain 1.21 food. */
    private static Item.Properties food(int nutrition, float saturationModifier) {
        return new Item.Properties().food(new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturationModifier)
                .build());
    }

    // ------------------------------------------------------------------
    // Phase 6 fluid buckets. 1.12.2 used Forge's single universal bucket for all fluids, but the plan
    // records a deliberate deviation here: nine per-fluid bucket items, one for each of the eight
    // placeable fluids plus vanta oil. The seven boozes stay bucket-less on purpose, preserving the
    // baseline's nine-bucket-able / seven-not split - booze is only ever served in bottles.
    //
    // Each bucket's content is the source fluid, and the same source fluid's Properties point back at
    // its bucket in RusticFluids, which is how NeoForge links the pair. Vanta oil gets a bucket even
    // though it has no flowing block, so the filled bucket can be drunk or used in a barrel but not
    // placed - the same practical behaviour 1.12.2's universal bucket had for it.
    // ------------------------------------------------------------------
    public static final DeferredItem<BucketItem> OLIVE_OIL_BUCKET = bucket("olive_oil_bucket", RusticFluids.OLIVE_OIL);
    public static final DeferredItem<BucketItem> IRONBERRY_JUICE_BUCKET = bucket("ironberry_juice_bucket", RusticFluids.IRONBERRY_JUICE);
    public static final DeferredItem<BucketItem> WILDBERRY_JUICE_BUCKET = bucket("wildberry_juice_bucket", RusticFluids.WILDBERRY_JUICE);
    public static final DeferredItem<BucketItem> GRAPE_JUICE_BUCKET = bucket("grape_juice_bucket", RusticFluids.GRAPE_JUICE);
    public static final DeferredItem<BucketItem> APPLE_JUICE_BUCKET = bucket("apple_juice_bucket", RusticFluids.APPLE_JUICE);
    public static final DeferredItem<BucketItem> ALE_WORT_BUCKET = bucket("ale_wort_bucket", RusticFluids.ALE_WORT);
    public static final DeferredItem<BucketItem> HONEY_BUCKET = bucket("honey_bucket", RusticFluids.HONEY);
    public static final DeferredItem<BucketItem> GOLDEN_APPLE_JUICE_BUCKET = bucket("golden_apple_juice_bucket", RusticFluids.GOLDEN_APPLE_JUICE);
    public static final DeferredItem<BucketItem> VANTA_OIL_BUCKET = bucket("vanta_oil_bucket", RusticFluids.VANTA_OIL);

    // ------------------------------------------------------------------
    // Phase 6 drinkable bottle. One item holds any of the sixteen source fluids via the
    // rustic:fluid_content data component (see ItemFluidBottle); the barrel/filling recipes reach it
    // through the item fluid handler registered below, the 1.21 form of the legacy SwapEmpty capability.
    // ------------------------------------------------------------------
    public static final DeferredItem<ItemFluidBottle> FLUID_BOTTLE = ITEMS.register("fluid_bottle",
            () -> new ItemFluidBottle(new Item.Properties()));

    // ------------------------------------------------------------------
    // Phase 6 elixir. Stacks to 16, drinks in 24 ticks, and carries its effect list in the
    // rustic:elixir_effects data component (see rustic.content.potions.ElixirUtils); the condenser recipes
    // that fill the alchemy tab with real elixirs are Phase 7.
    // ------------------------------------------------------------------
    public static final DeferredItem<ItemElixir> ELIXIR = ITEMS.register("elixir",
            () -> new ItemElixir(new Item.Properties()));

    // ------------------------------------------------------------------
    // Phase 7 prerequisite items. These seven were named in every earlier phase's doc as "still ahead",
    // but the recipe datagen now lists them as real ingredients and outputs, so they register here.
    // None carried behaviour in 1.12.2 beyond being an ingredient: honeycomb/beeswax/tallow are the bee
    // and flesh products (honeycomb smelts to beeswax, rotten flesh smelts to tallow), dust_tiny_iron/
    // dust_gold are the evaporating-basin outputs that then smelt to an iron nugget / gold ingot, and the
    // two seed items are the crushing-tub apple byproduct and the viney's grape seed.
    //
    // Deviation note: legacy apple_seeds was a growable {@code BlockAppleSeeds} crop that matured into an
    // apple sapling, but that whole apple-tree line belongs to Phase 8 worldgen; here it is only the plain
    // byproduct item the tub drops, and the placeable crop is deferred with the trees. The ids match the
    // legacy registry names ({@code dust_tiny_iron}, {@code dust_gold}) so the existing item textures and
    // every downstream reference resolve unchanged.
    // ------------------------------------------------------------------
    public static final DeferredItem<Item> HONEYCOMB = plainItem("honeycomb");
    public static final DeferredItem<Item> BEESWAX = plainItem("beeswax");
    // The bee the legacy beehive dropped alongside its honeycomb (ModItems.BEE, a placeable-to-bee item used
    // by the apiary). The apiary bee-simulation is Phase 4/out of scope here, so only the plain loot item
    // lands; the beehive's loot table (RusticBlockLoot) drops one or two of these exactly as legacy did.
    public static final DeferredItem<Item> BEE = plainItem("bee");
    public static final DeferredItem<Item> TALLOW = plainItem("tallow");
    public static final DeferredItem<Item> IRON_DUST_TINY = plainItem("dust_tiny_iron");
    public static final DeferredItem<Item> GOLD_DUST = plainItem("dust_gold");
    // Phase 14 (§A3): apple_seeds is no longer a plain byproduct - it is the seed of the growable
    // AppleSeedsBlock (rustic:apple_seeds), so it is that block's BlockItem (still under the same id, so the
    // crushing-tub output and the item-model sprite resolve unchanged) and right-clicks onto farmland to plant
    // the crop, exactly as legacy's BlockAppleSeeds ItemBlock did.
    public static final DeferredItem<BlockItem> APPLE_SEEDS = ITEMS.registerSimpleBlockItem(
            "apple_seeds", RusticFarmBlocks.APPLE_SEEDS_CROP);
    public static final DeferredItem<Item> GRAPE_SEEDS = plainItem("grape_seeds");

    /** A stack-of-64 ingredient item with no behaviour, the port of 1.12.2's plain {@code ItemBase}. */
    private static DeferredItem<Item> plainItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<BucketItem> bucket(String name,
                                                   DeferredHolder<net.minecraft.world.level.material.Fluid, ? extends net.minecraft.world.level.material.Fluid> content) {
        return ITEMS.register(name, () -> new BucketItem(content.value(), new Item.Properties().stacksTo(1)));
    }

    static {
        // 1.12.2: new ItemBlock(this).setRegistryName(getRegistryName()) inside BlockBase.register()
        for (DeferredHolder<Block, ? extends Block> block : RusticBlocks.entries()) {
            String path = block.getId().getPath();
            // A door is not a plain BlockItem: 1.12.2's ItemDoor placed both halves, and 1.21 keeps that in
            // DoubleHighBlockItem (there is no DoorItem any more), so the block-item loop special-cases it.
            // The type test is deferred into the supplier, the same moment registerSimpleBlockItem would have
            // resolved block.get() - the block register is populated before the item one runs.
            ITEMS.register(path, () -> blockItemFor(block.get()));
        }
        // Phase 4: the machine blocks live in their own register but need a block item just the same.
        for (DeferredHolder<Block, ? extends Block> block : RusticMachineBlocks.entries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        }
        // Phase 5: only the two placeable poles get a block item; the crops are picked/looted as seeds.
        for (DeferredHolder<Block, ? extends Block> block : RusticFarmBlocks.blockItemEntries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        }
        // Phase 6: the eight fluid blocks get a same-id block item like every other block, so the placed
        // fluid can be given to the player; the bucket items above are the pickup side of the same fluids.
        for (DeferredHolder<Block, ? extends Block> block : RusticFluidBlocks.entries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        }
    }

    /** Attaches the item register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    /**
     * The block item for one Rustic block: a {@link DoubleHighBlockItem} for a {@link DoorBlock} (the two-half
     * placer that replaced 1.12.2's {@code ItemDoor}, since 1.21 has no {@code DoorItem}), a plain
     * {@link BlockItem} otherwise.
     */
    private static BlockItem blockItemFor(Block block) {
        if (block instanceof DoorBlock door) {
            return new DoubleHighBlockItem(door, new Item.Properties());
        }
        return new BlockItem(block, new Item.Properties());
    }

    /**
     * Exposes the fluid bottle's {@link ItemFluidBottle.BottleHandler} through NeoForge's item fluid-handler
     * capability, the 1.21 replacement for the legacy {@code getCapability} override on {@code ItemFluidBottle}.
     * Barrels, filling machines and recipe inputs query it to read/fill/drain a bottle without a cast.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new ItemFluidBottle.BottleHandler(stack), FLUID_BOTTLE.get());
    }

    private RusticItems() {
    }
}
