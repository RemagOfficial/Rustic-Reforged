package rustic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import rustic.content.client.hud.ArmorHudLayout;
import rustic.content.client.hud.ArmorHudLayout.Icon;

public class ArmorHudLayoutTest {

    @Test
    void rowsCountOneRowPerTwentyPoints() {

        assertEquals(0, ArmorHudLayout.rows(0), "zero points draws nothing");
        assertEquals(0, ArmorHudLayout.rows(-6), "negative clamps to zero rows");
        assertEquals(1, ArmorHudLayout.rows(1), "a single point still needs a row");
        assertEquals(1, ArmorHudLayout.rows(20), "exactly one full row at 20");
        assertEquals(2, ArmorHudLayout.rows(21), "the 21st point spills onto a second row (vanilla clips this away)");
        assertEquals(2, ArmorHudLayout.rows(40), "two full rows at 40");
        assertEquals(3, ArmorHudLayout.rows(41), "three rows past 40");
    }

    @Test
    void rowHeightStaysTenThenCompressesToFloorThree() {

        assertEquals(10, ArmorHudLayout.rowHeight(1), "a lone row keeps the base height");
        assertEquals(10, ArmorHudLayout.rowHeight(2), "two rows still uncompressed");
        assertEquals(9, ArmorHudLayout.rowHeight(3), "third row squeezes by one");
        assertEquals(8, ArmorHudLayout.rowHeight(4), "fourth row squeezes by two");
        assertEquals(3, ArmorHudLayout.rowHeight(9), "reaches the floor at nine rows");
        assertEquals(3, ArmorHudLayout.rowHeight(10), "never below the floor");
        assertEquals(3, ArmorHudLayout.rowHeight(20), "stays clamped for a pathological stack");
    }

    @Test
    void iconFillMatchesVanillaTwoPointCompare() {

        for (int col = 0; col < ArmorHudLayout.ICONS_PER_ROW; col++) {
            assertEquals(Icon.FULL, ArmorHudLayout.iconFor(20, 0, col), "20 points fills every slot of row 0");
        }

        assertEquals(Icon.FULL, ArmorHudLayout.iconFor(19, 0, 8), "col 8 (point 17) stays full at 19");
        assertEquals(Icon.HALF, ArmorHudLayout.iconFor(19, 0, 9), "col 9 (point 19) is the half at 19");

        assertEquals(Icon.FULL, ArmorHudLayout.iconFor(21, 0, 9), "row 0 stays full once past 20");
        assertEquals(Icon.HALF, ArmorHudLayout.iconFor(21, 1, 0), "row 1 col 0 (point 21) is the half at 21");
        assertEquals(Icon.EMPTY, ArmorHudLayout.iconFor(21, 1, 1), "row 1 col 1 (point 23) is empty at 21");

        assertEquals(Icon.HALF, ArmorHudLayout.iconFor(1, 0, 0), "one point = leading half");
        assertEquals(Icon.EMPTY, ArmorHudLayout.iconFor(1, 0, 1), "everything after it is empty");
    }

    @Test
    void extraRowsStackUpwardFromTheVanillaBaseline() {

        assertEquals(-10, ArmorHudLayout.extraRowOffsetY(1, 10), "first overflow row sits one base height up");
        assertEquals(-18, ArmorHudLayout.extraRowOffsetY(2, 9), "third row stacks two compressed heights up");
        assertEquals(-27, ArmorHudLayout.extraRowOffsetY(3, 9), "fourth row is three heights up");
    }

    @Test
    void toughnessIconOffsetsMatchLegacySheet() {
        assertEquals(34, ArmorHudLayout.toughnessIconU(Icon.FULL), "legacy full toughness u");
        assertEquals(25, ArmorHudLayout.toughnessIconU(Icon.HALF), "legacy half toughness u");
        assertEquals(16, ArmorHudLayout.toughnessIconU(Icon.EMPTY), "legacy empty toughness u");
        assertEquals(0, ArmorHudLayout.TOUGHNESS_ICON_V, "all toughness icons share v 0");
    }
}
