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

public final class RusticItems {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Rustic.MODID);

    public static final DeferredItem<ItemTomato> TOMATO = ITEMS.register("tomato",
            () -> new ItemTomato(food(4, 0.4F)));
    public static final DeferredItem<Item> CHILI_PEPPER =
            ITEMS.registerItem("chili_pepper", Item::new, food(3, 0.4F));

    public static final DeferredItem<ItemGhostPepper> GHOST_PEPPER = ITEMS.register("ghost_pepper",
            () -> new ItemGhostPepper(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.7F)
                    .alwaysEdible()
                    .build())));

    public static final DeferredItem<StakeCropSeedItem> TOMATO_SEEDS = ITEMS.register("tomato_seeds",
            () -> new StakeCropSeedItem(new Item.Properties(), () -> RusticFarmBlocks.TOMATO_CROP.get()));
    public static final DeferredItem<StakeCropSeedItem> CHILI_PEPPER_SEEDS = ITEMS.register("chili_pepper_seeds",
            () -> new StakeCropSeedItem(new Item.Properties(), () -> RusticFarmBlocks.CHILI_CROP.get()));

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

    public static final DeferredItem<Item> GRAPES = ITEMS.registerItem("grapes", Item::new,
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(3)
                    .saturationModifier(0.3F)
                    .fast()
                    .build()));

    public static final DeferredItem<Item> WILDBERRIES = ITEMS.registerItem("wildberries", Item::new,
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.5F)
                    .fast()
                    .build()));

    public static final DeferredItem<ItemOlives> OLIVES = ITEMS.register("olives",
            () -> new ItemOlives(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(1)
                    .saturationModifier(0.4F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 1), 0.95F)
                    .build())));

    public static final DeferredItem<ItemIronberries> IRONBERRIES = ITEMS.register("ironberries",
            () -> new ItemIronberries(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2)
                    .saturationModifier(0.4F)
                    .alwaysEdible()
                    .fast()
                    .build())));

    private static Item.Properties food(int nutrition, float saturationModifier) {
        return new Item.Properties().food(new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturationModifier)
                .build());
    }

    public static final DeferredItem<BucketItem> OLIVE_OIL_BUCKET = bucket("olive_oil_bucket", RusticFluids.OLIVE_OIL);
    public static final DeferredItem<BucketItem> IRONBERRY_JUICE_BUCKET = bucket("ironberry_juice_bucket", RusticFluids.IRONBERRY_JUICE);
    public static final DeferredItem<BucketItem> WILDBERRY_JUICE_BUCKET = bucket("wildberry_juice_bucket", RusticFluids.WILDBERRY_JUICE);
    public static final DeferredItem<BucketItem> GRAPE_JUICE_BUCKET = bucket("grape_juice_bucket", RusticFluids.GRAPE_JUICE);
    public static final DeferredItem<BucketItem> APPLE_JUICE_BUCKET = bucket("apple_juice_bucket", RusticFluids.APPLE_JUICE);
    public static final DeferredItem<BucketItem> ALE_WORT_BUCKET = bucket("ale_wort_bucket", RusticFluids.ALE_WORT);
    public static final DeferredItem<BucketItem> HONEY_BUCKET = bucket("honey_bucket", RusticFluids.HONEY);
    public static final DeferredItem<BucketItem> GOLDEN_APPLE_JUICE_BUCKET = bucket("golden_apple_juice_bucket", RusticFluids.GOLDEN_APPLE_JUICE);
    public static final DeferredItem<BucketItem> VANTA_OIL_BUCKET = bucket("vanta_oil_bucket", RusticFluids.VANTA_OIL);

    public static final DeferredItem<ItemFluidBottle> FLUID_BOTTLE = ITEMS.register("fluid_bottle",
            () -> new ItemFluidBottle(new Item.Properties()));

    public static final DeferredItem<ItemElixir> ELIXIR = ITEMS.register("elixir",
            () -> new ItemElixir(new Item.Properties()));

    public static final DeferredItem<Item> HONEYCOMB = plainItem("honeycomb");
    public static final DeferredItem<Item> BEESWAX = plainItem("beeswax");

    public static final DeferredItem<Item> BEE = plainItem("bee");
    public static final DeferredItem<Item> TALLOW = plainItem("tallow");
    public static final DeferredItem<Item> IRON_DUST_TINY = plainItem("dust_tiny_iron");
    public static final DeferredItem<Item> GOLD_DUST = plainItem("dust_gold");

    public static final DeferredItem<BlockItem> APPLE_SEEDS = ITEMS.registerSimpleBlockItem(
            "apple_seeds", RusticFarmBlocks.APPLE_SEEDS_CROP);
    public static final DeferredItem<Item> GRAPE_SEEDS = plainItem("grape_seeds");

    private static DeferredItem<Item> plainItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<BucketItem> bucket(String name,
                                                   DeferredHolder<net.minecraft.world.level.material.Fluid, ? extends net.minecraft.world.level.material.Fluid> content) {
        return ITEMS.register(name, () -> new BucketItem(content.value(), new Item.Properties().stacksTo(1)));
    }

    static {

        for (DeferredHolder<Block, ? extends Block> block : RusticBlocks.entries()) {
            String path = block.getId().getPath();

            ITEMS.register(path, () -> blockItemFor(block.get()));
        }

        for (DeferredHolder<Block, ? extends Block> block : RusticMachineBlocks.entries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        }

        for (DeferredHolder<Block, ? extends Block> block : RusticFarmBlocks.blockItemEntries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        }

        for (DeferredHolder<Block, ? extends Block> block : RusticFluidBlocks.entries()) {
            ITEMS.registerSimpleBlockItem(block.getId().getPath(), block);
        }
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private static BlockItem blockItemFor(Block block) {
        if (block instanceof DoorBlock door) {
            return new DoubleHighBlockItem(door, new Item.Properties());
        }
        return new BlockItem(block, new Item.Properties());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new ItemFluidBottle.BottleHandler(stack), FLUID_BOTTLE.get());
    }

    private RusticItems() {
    }
}
