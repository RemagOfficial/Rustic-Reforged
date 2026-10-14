package rustic.content.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
 * <p>The non-block items of {@code ModItems} (olives, tomato, seeds, buckets, bottle, book, ...) are
 * not here yet; they belong to the phases that port the items themselves.
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
    // setPotionEffect food effects into FoodProperties, but none of these three had one, so they are
    // plain foods. The potion-carrying edibles (nausea, fullmetal, fire-power) are Phase 6 and are
    // deliberately not here.
    // ------------------------------------------------------------------
    public static final DeferredItem<Item> TOMATO =
            ITEMS.registerItem("tomato", Item::new, food(4, 0.4F));
    public static final DeferredItem<Item> CHILI_PEPPER =
            ITEMS.registerItem("chili_pepper", Item::new, food(3, 0.4F));
    public static final DeferredItem<Item> GHOST_PEPPER =
            ITEMS.registerItem("ghost_pepper", Item::new, food(4, 0.7F));

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

    private static DeferredItem<BucketItem> bucket(String name,
                                                   DeferredHolder<net.minecraft.world.level.material.Fluid, ? extends net.minecraft.world.level.material.Fluid> content) {
        return ITEMS.register(name, () -> new BucketItem(content.value(), new Item.Properties().stacksTo(1)));
    }

    static {
        // 1.12.2: new ItemBlock(this).setRegistryName(getRegistryName()) inside BlockBase.register()
        for (DeferredHolder<Block, ? extends Block> block : RusticBlocks.entries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
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
