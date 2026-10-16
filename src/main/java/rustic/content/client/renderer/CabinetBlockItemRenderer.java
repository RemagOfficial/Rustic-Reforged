package rustic.content.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import rustic.content.blockentity.CabinetBlockEntity;
import rustic.content.blocks.CabinetBlock;
import rustic.content.blocks.RusticMachineBlocks;

/**
 * The 1.21 replacement for the legacy {@code CabinetRenderer.CabinetTEISR.renderByItem}: a
 * {@link BlockEntityWithoutLevelRenderer} that draws the cabinet block item in the inventory / hand / ground as the
 * same {@code CabinetRenderer} body the world uses, closed and facing south. It reuses the registered BER through
 * {@link BlockEntityRenderDispatcher#renderItem} against a lone dummy block entity (null level, default state) so the
 * inventory and the world render from one code path.
 *
 * <p>The legacy item read a {@code material} tag and tinted the model; the ported cabinet stores no material, so the
 * item always renders the plain (white) cabinet body - the {@code useColoredTexture} branch was the only live one.
 */
@OnlyIn(Dist.CLIENT)
public final class CabinetBlockItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final BlockEntityRenderDispatcher dispatcher;
    private final CabinetBlockEntity dummy;

    public CabinetBlockItemRenderer() {
        this(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private CabinetBlockItemRenderer(BlockEntityRenderDispatcher dispatcher,
                                     net.minecraft.client.model.geom.EntityModelSet models) {
        super(dispatcher, models);
        this.dispatcher = dispatcher;
        this.dummy = new CabinetBlockEntity(BlockPos.ZERO,
                RusticMachineBlocks.CABINET.get().defaultBlockState());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof CabinetBlock) {
            // The dummy inherits the caller's item transforms from builtin/entity, so only the closed south-facing
            // body needs drawing; a null level makes the BER fall back to single/untinted/closed.
            this.dispatcher.renderItem(this.dummy, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}
