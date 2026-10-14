package rustic.content.fluids;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * The flowing half of one of the eight placeable Rustic fluids. It carries no drink logic (only the source
 * is ever handed to a bottle to be drunk), so it is a plain {@link BaseFlowingFluid.Flowing}; the class exists
 * to give the flowing variants a distinct, discoverable type the way vanilla separates {@code Water} from
 * {@code Water.Flowing} through {@link BaseFlowingFluid.Source} and {@link BaseFlowingFluid.Flowing}.
 */
public class RusticFlowingFluid extends BaseFlowingFluid.Flowing {

    public RusticFlowingFluid(Properties properties) {
        super(properties);
    }
}
