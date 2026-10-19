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

            this.dispatcher.renderItem(this.dummy, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}
