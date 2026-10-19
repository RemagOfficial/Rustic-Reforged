package rustic.content.blockentity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;
import rustic.content.blocks.RusticMachineBlocks;

public final class RusticBlockEntities {

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rustic.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL =
            BLOCK_ENTITIES.register("barrel", () -> BlockEntityType.Builder
                    .of(BarrelBlockEntity::new, RusticMachineBlocks.BARREL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidBarrelBlockEntity>> LIQUID_BARREL =
            BLOCK_ENTITIES.register("liquid_barrel", () -> BlockEntityType.Builder
                    .of(LiquidBarrelBlockEntity::new, RusticMachineBlocks.LIQUID_BARREL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CabinetBlockEntity>> CABINET =
            BLOCK_ENTITIES.register("cabinet", () -> BlockEntityType.Builder
                    .of(CabinetBlockEntity::new, RusticMachineBlocks.CABINET.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VaseBlockEntity>> VASE =
            BLOCK_ENTITIES.register("vase", () -> BlockEntityType.Builder
                    .of(VaseBlockEntity::new, RusticMachineBlocks.VASE.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrushingTubBlockEntity>> CRUSHING_TUB =
            BLOCK_ENTITIES.register("crushing_tub", () -> BlockEntityType.Builder
                    .of(CrushingTubBlockEntity::new, RusticMachineBlocks.CRUSHING_TUB.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EvaporatingBasinBlockEntity>> EVAPORATING_BASIN =
            BLOCK_ENTITIES.register("evaporating_basin", () -> BlockEntityType.Builder
                    .of(EvaporatingBasinBlockEntity::new, RusticMachineBlocks.EVAPORATING_BASIN.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrewingBarrelBlockEntity>> BREWING_BARREL =
            BLOCK_ENTITIES.register("brewing_barrel", () -> BlockEntityType.Builder
                    .of(BrewingBarrelBlockEntity::new, RusticMachineBlocks.BREWING_BARREL.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER =
            BLOCK_ENTITIES.register("condenser", () -> BlockEntityType.Builder
                    .of(CondenserBlockEntity::new, RusticMachineBlocks.CONDENSER.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserAdvancedBlockEntity>> CONDENSER_ADVANCED =
            BLOCK_ENTITIES.register("condenser_advanced", () -> BlockEntityType.Builder
                    .of(CondenserAdvancedBlockEntity::new, RusticMachineBlocks.CONDENSER_ADVANCED.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ApiaryBlockEntity>> APIARY =
            BLOCK_ENTITIES.register("apiary", () -> BlockEntityType.Builder
                    .of(ApiaryBlockEntity::new, RusticMachineBlocks.APIARY.get())
                    .build(null));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BARREL.get(),
                (barrel, side) -> new InvWrapper(barrel));

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                LIQUID_BARREL.get(),
                (liquidBarrel, side) -> liquidBarrel.getTank());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CABINET.get(),
                (cabinet, side) -> new InvWrapper(cabinet));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                VASE.get(),
                (vase, side) -> new InvWrapper(vase));

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CRUSHING_TUB.get(),
                (tub, side) -> tub.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CRUSHING_TUB.get(),
                (tub, side) -> tub.getDrainView());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                EVAPORATING_BASIN.get(),
                (basin, side) -> basin.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                EVAPORATING_BASIN.get(),
                (basin, side) -> basin.getTank());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BREWING_BARREL.get(),
                (barrel, side) -> barrel.getItemHandler());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CONDENSER.get(),
                (condenser, side) -> condenser.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CONDENSER.get(),
                (condenser, side) -> condenser.getTank());
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CONDENSER_ADVANCED.get(),
                (condenser, side) -> condenser.getItemHandler());
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CONDENSER_ADVANCED.get(),
                (condenser, side) -> condenser.getTank());

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                APIARY.get(),
                (apiary, side) -> side == net.minecraft.core.Direction.DOWN
                        ? apiary.getHoneyCombHandler() : apiary.getBeeHandler());
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private RusticBlockEntities() {
    }
}
