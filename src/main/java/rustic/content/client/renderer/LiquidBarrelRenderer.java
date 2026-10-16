package rustic.content.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import rustic.Rustic;
import rustic.content.blockentity.LiquidBarrelBlockEntity;

/**
 * 1.21 form of the legacy {@code rustic.client.renderer.LiquidBarrelRenderer}. Draws the stored fluid as a top
 * surface quad inside the barrel - the only pass the legacy renderer had (the liquid barrel holds no items).
 *
 * <p>Faithful numbers from the legacy renderer: the quad spans {@code 0.1875..0.8125} in x/z at
 * {@code y = 0.125 + 0.8125 * amount/capacity} (a tall, narrow fill inside the barrel body) with a {@code 0.1875}
 * sprite UV inset, drawn through {@link FluidTankRenderUtil}.
 */
public class LiquidBarrelRenderer implements BlockEntityRenderer<LiquidBarrelBlockEntity> {

    /** Round-5 diagnostic, see {@link CrushingTubRenderer#FLUID_DIAG_LOGGED}; remove once the Phase 9 verdict is green. */
    private static final AtomicBoolean FLUID_DIAG_LOGGED = new AtomicBoolean();

    private final ItemRenderer itemRenderer;

    public LiquidBarrelRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LiquidBarrelBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // ENTITYBLOCK_ANIMATED: draw the open-topped barrel body first (via the block's own baked model through the
        // item renderer) so the fluid surface below reads as held inside it. renderSingleBlock is unusable and
        // renderStatic(NONE) leaves the model at full scale but translated by (-0.5,-0.5,-0.5), so a +0.5
        // pre-translate recentres it in the block's 0..1 space - see CrushingTubRenderer.
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        this.itemRenderer.renderStatic(new ItemStack(be.getBlockState().getBlock()), ItemDisplayContext.NONE,
                packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), (int) be.getBlockPos().asLong());
        poseStack.popPose();
        FluidStack fluid = be.getTank().getFluid();
        if (FLUID_DIAG_LOGGED.compareAndSet(false, true)) {
            Rustic.LOGGER.info("Phase 9 diag: liquid barrel BER saw fluid='{}' amount={}mb capacity={}mb at {}",
                    fluid.isEmpty() ? "<empty>" : fluid.getFluid().getFluidType(), fluid.getAmount(),
                    LiquidBarrelBlockEntity.CAPACITY, be.getBlockPos());
        }
        FluidTankRenderUtil.renderFluidSurface(fluid, LiquidBarrelBlockEntity.CAPACITY,
                0.125F, 0.8125F, 0.1875F, 0.8125F, 0.1875F, 0.8125F, 0.1875F,
                poseStack, bufferSource, packedLight);
    }
}
