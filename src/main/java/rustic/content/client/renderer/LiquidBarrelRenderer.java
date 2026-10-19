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

public class LiquidBarrelRenderer implements BlockEntityRenderer<LiquidBarrelBlockEntity> {

    private static final AtomicBoolean FLUID_DIAG_LOGGED = new AtomicBoolean();

    private final ItemRenderer itemRenderer;

    public LiquidBarrelRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LiquidBarrelBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

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
