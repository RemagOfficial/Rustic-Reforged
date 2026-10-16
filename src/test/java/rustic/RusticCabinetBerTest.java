package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import org.junit.jupiter.api.Test;
import rustic.content.blockentity.CabinetBlockEntity;
import rustic.content.client.model.CabinetModel;
import rustic.content.client.renderer.CabinetRenderer;

/**
 * Headless guard for Phase 9 slice S9.2 (the cabinet door BER + animated model + item renderer). The door swing is a
 * GL-only visual, so this asserts the pieces that are verifiable without a render context:
 *
 * <ol>
 *   <li>the {@code CabinetRenderer} is a {@link BlockEntityRenderer} bound to {@link CabinetBlockEntity}, and that
 *       block entity now satisfies {@link LidBlockEntity} (the interface the renderer reads openness through);</li>
 *   <li>each body layer bakes the six named parts with the door hinged on the correct vertical edge and a handle child
 *       that rides under the door;</li>
 *   <li>the extracted {@code doorAngle} cubic ramp reproduces the legacy eased swing (shut at {@code openness=0}, a
 *       quarter turn open at {@code openness=1}, mirrored to the opposite sign).</li>
 * </ol>
 */
public class RusticCabinetBerTest {

    private static final float EPS = 1.0E-4F;

    @Test
    public void cabinetBerBindsItsBlockEntity() {
        for (Type iface : CabinetRenderer.class.getGenericInterfaces()) {
            if (iface instanceof ParameterizedType parameterized
                    && parameterized.getRawType() == BlockEntityRenderer.class) {
                assertEquals(CabinetBlockEntity.class, parameterized.getActualTypeArguments()[0],
                        "CabinetRenderer must render CabinetBlockEntity");
                return;
            }
        }
        fail("CabinetRenderer does not directly implement BlockEntityRenderer<CabinetBlockEntity>");
    }

    @Test
    public void cabinetBlockEntityIsALidBlock() {
        assertTrue(LidBlockEntity.class.isAssignableFrom(CabinetBlockEntity.class),
                "The lid openness read by the renderer needs CabinetBlockEntity to implement LidBlockEntity");
    }

    @Test
    public void singleBodyLayerHingesDoorWithHandleChild() {
        ModelPart single = CabinetModel.createBodyLayer(false, false).bakeRoot();
        assertSixParts(single);
        ModelPart door = single.getChild("door");
        // Round-16 faithful rebuild: the door is the legacy ModelCabinet rotationPoint for the non-mirror hinge,
        // x = -7, y = 16 (single), z = -6.5, expressed as a PartPose offset.
        assertEquals(-7.0F, door.x, EPS);
        assertEquals(16.0F, door.y, EPS);
        assertEquals(-6.5F, door.z, EPS, "Door hinge must sit at the legacy z = -6.5 rotation point");
        assertTrue(door.hasChild("handle"), "The handle must ride under the door so it swings with it");
    }

    @Test
    public void mirrorBodyLayerFlipsDoorHingeToOppositeEdge() {
        ModelPart mirror = CabinetModel.createBodyLayer(true, false).bakeRoot();
        assertSixParts(mirror);
        // Legacy mirror variant moves the hinge rotationPoint to x = +7.
        assertEquals(7.0F, mirror.getChild("door").x, EPS, "Mirrored door hinges on the opposite vertical edge");
    }

    @Test
    public void doubleBodyLayerTallsTheDoorUpToTheUpperBlock() {
        ModelPart dbl = CabinetModel.createBodyLayer(false, true).bakeRoot();
        assertSixParts(dbl);
        // Legacy ModelCabinetDouble door rotationPoint y = 8 (the double door sits higher than the single's 16).
        assertEquals(8.0F, dbl.getChild("door").y, EPS);
    }

    @Test
    public void doorAngleSwingsWithLegacySign() {
        float quarter = (float) (Math.PI / 2.0);
        // Shut: eased = 1-(1-0)^3 = 0 -> no swing.
        assertEquals(0.0F, CabinetModel.doorAngle(0.0F, false), EPS);
        // Fully open, non-mirror: the legacy renderer uses +(f*PI/2). No compensating flip is needed because the BER
        // now applies the real scale(1,-1,-1) and ModelPart.yRot shares ModelRenderer.rotateAngleY's sign convention.
        assertEquals(quarter, CabinetModel.doorAngle(1.0F, false), EPS);
        // The ramp is cubic: half-open lands at 1-(0.5)^3 = 0.875 of a quarter turn.
        assertEquals(0.875F * quarter, CabinetModel.doorAngle(0.5F, false), EPS);
        // Mirror swings the opposite (negative) way, per legacy (mirror ? -(f*PI/2) : (f*PI/2)).
        assertEquals(-quarter, CabinetModel.doorAngle(1.0F, true), EPS);
    }

    @Test
    public void sidesUseLegacyRotationPointsAndNoLipHack() {
        // Round-16: the model now reproduces legacy ModelCabinet verbatim, so the side panels sit at their original
        // centred-space rotation points (right x=-8, left x=7, both z=-8). The round-15 left_lip part is gone - it only
        // existed to paper over a corner-space transform that no longer matched the geometry.
        ModelPart single = CabinetModel.createBodyLayer(false, false).bakeRoot();
        assertFalse(single.hasChild("left_lip"), "The round-15 lip hack must be removed in the faithful rebuild");
        ModelPart right = single.getChild("right");
        ModelPart left = single.getChild("left");
        assertEquals(-8.0F, right.x, EPS);
        assertEquals(-8.0F, right.z, EPS);
        assertEquals(7.0F, left.x, EPS);
        assertEquals(-8.0F, left.z, EPS);
    }

    private static void assertSixParts(ModelPart root) {
        for (String part : new String[] {"bottom", "top", "back", "right", "left", "door"}) {
            assertTrue(root.hasChild(part), "Cabinet body layer is missing the '" + part + "' part");
        }
        assertFalse(root.getChild("door") == null);
    }
}
