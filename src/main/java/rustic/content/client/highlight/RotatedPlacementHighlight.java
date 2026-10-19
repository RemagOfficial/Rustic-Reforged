package rustic.content.client.highlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import rustic.content.blocks.AdvancedRotationPlacement;

@OnlyIn(Dist.CLIENT)
public final class RotatedPlacementHighlight {

    private static final double LINE_INFLATE = 0.002D;

    private static final float LINE_ALPHA = 0.4F;

    public static void render(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        ItemStack stack = heldBlockStack(player);
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof AdvancedRotationPlacement)) {
            return;
        }

        BlockHitResult hit = event.getTarget();
        if (hit.getDirection().getAxis() != Direction.Axis.Y) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir() || !player.level().getWorldBorder().isWithinBounds(pos)) {
            return;
        }

        AABB box = state.getShape(player.level(), pos).bounds();
        if (!RotatedPlacementHighlightMath.isCenteredXZ(box)) {
            return;
        }

        AABB grown = box.inflate(LINE_INFLATE, LINE_INFLATE, LINE_INFLATE);
        double minXZ = RotatedPlacementHighlightMath.insetMinXZ(grown);
        double maxXZ = RotatedPlacementHighlightMath.insetMaxXZ(grown);
        double faceY = RotatedPlacementHighlightMath.faceY(grown, hit.getDirection() == Direction.UP) + pos.getY();

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        drawRotationMarker(buffer, poseStack.last(), pos.getX(), pos.getZ(), minXZ, maxXZ, faceY);
        poseStack.popPose();
    }

    private static void drawRotationMarker(VertexConsumer buffer, PoseStack.Pose pose, int px, int pz,
                                           double minXZ, double maxXZ, double y) {
        addLine(buffer, pose, minXZ + px, y, minXZ + pz, maxXZ + px, y, maxXZ + pz);
        addLine(buffer, pose, minXZ + px, y, maxXZ + pz, maxXZ + px, y, minXZ + pz);
    }

    private static void addLine(VertexConsumer buffer, PoseStack.Pose pose,
                                double x1, double y1, double z1, double x2, double y2, double z2) {
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(0.0F, 0.0F, 0.0F, LINE_ALPHA)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(0.0F, 0.0F, 0.0F, LINE_ALPHA)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static ItemStack heldBlockStack(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mainIsBlock = !main.isEmpty() && main.getItem() instanceof BlockItem;
        if (!mainIsBlock && !off.isEmpty()) {
            return off;
        }
        return main;
    }

    private RotatedPlacementHighlight() {
    }
}
