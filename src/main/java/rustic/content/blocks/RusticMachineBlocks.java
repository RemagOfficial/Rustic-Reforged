package rustic.content.blocks;

import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

import java.util.Collection;

public final class RusticMachineBlocks {

    private static final DeferredRegister.Blocks MACHINE_BLOCKS = DeferredRegister.createBlocks(Rustic.MODID);

    public static final DeferredBlock<BarrelBlock> BARREL =
            MACHINE_BLOCKS.registerBlock("barrel", BarrelBlock::new, RusticBlockProperties.wood(1.5F).noOcclusion());

    public static final DeferredBlock<LiquidBarrelBlock> LIQUID_BARREL =
            MACHINE_BLOCKS.registerBlock("liquid_barrel", LiquidBarrelBlock::new, RusticBlockProperties.wood(1.5F));

    public static final DeferredBlock<CabinetBlock> CABINET =
            MACHINE_BLOCKS.registerBlock("cabinet", CabinetBlock::new, RusticBlockProperties.wood(2.5F));

    public static final DeferredBlock<VaseBlock> VASE =
            MACHINE_BLOCKS.registerBlock("vase", VaseBlock::new, RusticBlockProperties.stone(0.5F));

    public static final DeferredBlock<CrushingTubBlock> CRUSHING_TUB =
            MACHINE_BLOCKS.registerBlock("crushing_tub", CrushingTubBlock::new, RusticBlockProperties.wood(1.5F).noOcclusion());

    public static final DeferredBlock<EvaporatingBasinBlock> EVAPORATING_BASIN =
            MACHINE_BLOCKS.registerBlock("evaporating_basin", EvaporatingBasinBlock::new, RusticBlockProperties.stone(1.5F).noOcclusion());

    public static final DeferredBlock<BrewingBarrelBlock> BREWING_BARREL =
            MACHINE_BLOCKS.registerBlock("brewing_barrel", BrewingBarrelBlock::new, RusticBlockProperties.wood(1.5F).noOcclusion());

    public static final DeferredBlock<CondenserBlock> CONDENSER =
            MACHINE_BLOCKS.registerBlock("condenser", CondenserBlock::new, RusticBlockProperties.stone(2.0F));

    public static final DeferredBlock<CondenserAdvancedBlock> CONDENSER_ADVANCED =
            MACHINE_BLOCKS.registerBlock("condenser_advanced", CondenserAdvancedBlock::new, RusticBlockProperties.stone(2.0F));

    public static final DeferredBlock<RetortBlock> RETORT =
            MACHINE_BLOCKS.registerBlock("retort", RetortBlock::new, RusticBlockProperties.stone(2.0F).noOcclusion());

    public static final DeferredBlock<RetortBlock> RETORT_ADVANCED =
            MACHINE_BLOCKS.registerBlock("retort_advanced", RetortBlock::new, RusticBlockProperties.stone(2.0F).noOcclusion());

    public static final DeferredBlock<ApiaryBlock> APIARY =
            MACHINE_BLOCKS.registerBlock("apiary", ApiaryBlock::new, RusticBlockProperties.wood(1.0F).noOcclusion());

    public static Collection<DeferredHolder<Block, ? extends Block>> entries() {
        return MACHINE_BLOCKS.getEntries();
    }

    public static void register(IEventBus modEventBus) {
        MACHINE_BLOCKS.register(modEventBus);
    }

    private RusticMachineBlocks() {
    }
}
