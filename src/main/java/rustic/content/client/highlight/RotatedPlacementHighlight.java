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

/**
 * S9.7: the rotated-placement marker. 1.12.2 drew it in {@code EventHandlerClient.onBlockHighlight} (a
 * {@code DrawBlockHighlightEvent}) as an extra pair of diagonal lines on the top/bottom face of a hovered block whose
 * item implements {@link AdvancedRotationPlacement}, so clicking a Y face still tells the player which way the block
 * will turn. It did not cancel the vanilla selection box - the marker was painted on top of it - and this port keeps
 * that: the event is never cancelled, only extended.
 *
 * <p>The modern hook is {@link RenderHighlightEvent.Block}. Its {@link PoseStack} is camera-relative but NOT yet
 * translated by the camera (vanilla asserts it clear right before firing the event and applies the {@code -camera}
 * shift itself inside {@code renderHitOutline}), so this handler mirrors that: push, translate by
 * {@code -camera.position()}, draw at absolute world coordinates, pop. {@link RotatedPlacementHighlightMath} carries
 * the pure coordinate rules.
 */
@OnlyIn(Dist.CLIENT)
public final class RotatedPlacementHighlight {

    /** The legacy {@code AABB.grow(0.002)} outset, so the marker sits a hair outside the selection box. */
    private static final double LINE_INFLATE = 0.002D;
    /** The legacy line colour: black at the {@code 0.4} alpha the {@code drawRotablePlacementBlockHighlight} used. */
    private static final float LINE_ALPHA = 0.4F;

    /** Entry point wired on the game bus from {@code RusticClient}. */
    public static void render(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }

        // Only when the block the player is holding places an AdvancedRotationPlacement; matches the legacy
        // "held item resolves to a rotatable block" guard, main hand first then off hand.
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

    /**
     * The two crossing diagonals of the inset square, reproducing the legacy {@code drawRotablePlacementBlockHighlight}
     * strip: one line from the {@code (min,min)} corner to the {@code (max,max)} corner and a second crossing from
     * {@code (min,max)} to {@code (max,min)}, on the clicked face.
     */
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

    /**
     * The legacy hand-selection: prefer a {@link BlockItem} in the main hand, fall back to the off hand only when the
     * main hand is empty or holds a non-block item, and yield an empty stack when neither hand has one.
     */
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
