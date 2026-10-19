package rustic.dynamictrees;

import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.block.soil.SoilProperties;
import com.dtteam.dynamictrees.data.GatherDataHelper;
import com.dtteam.dynamictrees.registry.NeoForgeRegistryHandler;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.Species;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import rustic.Rustic;

public final class RusticDynamicTreesCompat {

    private RusticDynamicTreesCompat() {
    }

    public static void init(IEventBus modEventBus) {
        NeoForgeRegistryHandler.setup(Rustic.MODID, modEventBus);
        Rustic.LOGGER.info("Dynamic Trees compat: registry handler attached for namespace '{}'", Rustic.MODID);
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        DtGenDebugCommand.register(event.getDispatcher());
    }

    public static void gatherData(GatherDataEvent event) {
        GatherDataHelper.gatherBlockStateAndModelData(Rustic.MODID, event,
                SoilProperties.REGISTRY, Family.REGISTRY, Species.REGISTRY, LeavesProperties.REGISTRY);
        GatherDataHelper.gatherItemModelData(Rustic.MODID, event,
                Family.REGISTRY, Species.REGISTRY, LeavesProperties.REGISTRY);
        GatherDataHelper.gatherLootData(Rustic.MODID, event);
        Rustic.LOGGER.info("Dynamic Trees compat: queued tree-pack data generation for namespace '{}'", Rustic.MODID);
    }
}
