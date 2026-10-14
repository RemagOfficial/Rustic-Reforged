package rustic.content.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.blocks.RusticBlocks;
import rustic.content.blocks.RusticFarmBlocks;
import rustic.content.blocks.RusticMachineBlocks;

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
    }

    /** Attaches the item register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private RusticItems() {
    }
}
