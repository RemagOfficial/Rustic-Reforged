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

        assertEquals(-7.0F, door.x, EPS);
        assertEquals(16.0F, door.y, EPS);
        assertEquals(-6.5F, door.z, EPS, "Door hinge must sit at the legacy z = -6.5 rotation point");
        assertTrue(door.hasChild("handle"), "The handle must ride under the door so it swings with it");
    }

    @Test
    public void mirrorBodyLayerFlipsDoorHingeToOppositeEdge() {
        ModelPart mirror = CabinetModel.createBodyLayer(true, false).bakeRoot();
        assertSixParts(mirror);

        assertEquals(7.0F, mirror.getChild("door").x, EPS, "Mirrored door hinges on the opposite vertical edge");
    }

    @Test
    public void doubleBodyLayerTallsTheDoorUpToTheUpperBlock() {
        ModelPart dbl = CabinetModel.createBodyLayer(false, true).bakeRoot();
        assertSixParts(dbl);

        assertEquals(8.0F, dbl.getChild("door").y, EPS);
    }

    @Test
    public void doorAngleSwingsWithLegacySign() {
        float quarter = (float) (Math.PI / 2.0);

        assertEquals(0.0F, CabinetModel.doorAngle(0.0F, false), EPS);

        assertEquals(quarter, CabinetModel.doorAngle(1.0F, false), EPS);

        assertEquals(0.875F * quarter, CabinetModel.doorAngle(0.5F, false), EPS);

        assertEquals(-quarter, CabinetModel.doorAngle(1.0F, true), EPS);
    }

    @Test
    public void sidesUseLegacyRotationPointsAndNoLipHack() {

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
