package rustic.content.client.hud;

/**
 * Pure row/column arithmetic for the S9.5 HUD meters (extra armor rows + armor toughness), lifted out
 * of the {@code LayeredDraw.Layer} renderers so it can be unit-tested without a screen.
 *
 * <p>Vanilla 1.21 draws the armor bar as exactly ten icons ({@code Gui#renderArmor}), capping the display
 * at 20 armour points and silently clipping anything higher - which matters here because Rustic's
 * iron-skin effect adds flat {@code Attributes.ARMOR} that a player can carry past 20. It also draws no
 * toughness meter at all. This class reproduces the 1.12.2 {@code EventHandlerClient} stacking maths
 * ({@code armorRows/rowHeight} and the {@code j + i*20} per-icon full/half/empty compares) so a
 * companion {@code GuiLayer} can fill in the rows vanilla omits.
 *
 * <p>Every rule here is transcribed from the legacy code paths, not reinvented:
 * {@code onRenderArmorOverlayEvent} lines 504-519 and {@code onRenderArmorToughnessEvent} lines 454-469.
 */
public final class ArmorHudLayout {

    /** Icons drawn across one horizontal row (vanilla draws 10). */
    public static final int ICONS_PER_ROW = 10;

    /** Armour/toughness points each icon represents (a full icon = 2 points). */
    public static final int POINTS_PER_ICON = 2;

    /** The uncompressed row height; the legacy {@code rowHeight} only shrinks past two rows. */
    public static final int BASE_ROW_HEIGHT = 10;

    /** The three fill states an icon can take, matching vanilla's full/half/empty sprites and the legacy u offsets. */
    public enum Icon {
        FULL,
        HALF,
        EMPTY
    }

    /**
     * Number of display rows needed for {@code value} points: {@code ceil(value / (ICONS_PER_ROW *
     * POINTS_PER_ICON))}, i.e. one row per 20 points, and {@code 0} when there is nothing to show.
     * Transcribes legacy {@code MathHelper.ceil(armorLevel / 20.0F)}.
     */
    public static int rows(int value) {
        if (value <= 0) {
            return 0;
        }
        int perRow = ICONS_PER_ROW * POINTS_PER_ICON; // 20
        return (value + perRow - 1) / perRow;
    }

    /**
     * Vertical spacing between rows. Legacy: {@code min(max(10 - (rows - 2), 3), 10)} - stays at the full
     * 10px until more than two rows are in play, then compresses down to a floor of 3 so a pathological
     * stack of rows still fits above the hotbar.
     */
    public static int rowHeight(int rows) {
        return Math.min(Math.max(BASE_ROW_HEIGHT - (rows - 2), 3), BASE_ROW_HEIGHT);
    }

    /**
     * The fill state of the {@code col}-th icon (0-based, left-to-right) of the {@code row}-th row (0-based)
     * for a total of {@code value} points. Mirrors the vanilla {@code k * 2 + 1 <=> value} compare, offset by
     * the row: the icon covers point {@code (row * ICONS_PER_ROW + col) * POINTS_PER_ICON + 1}.
     */
    public static Icon iconFor(int value, int row, int col) {
        int point = (row * ICONS_PER_ROW + col) * POINTS_PER_ICON + 1;
        if (point < value) {
            return Icon.FULL;
        }
        if (point == value) {
            return Icon.HALF;
        }
        return Icon.EMPTY;
    }

    /**
     * The vertical offset (negative = upward) of extra {@code rowIndex} (1-based; row 0 is the vanilla row)
     * relative to the baseline row, using the given {@code rowHeight}. Pure so the test can assert the
     * stacking without touching a live {@code Gui.leftHeight}.
     */
    public static int extraRowOffsetY(int rowIndex, int rowHeight) {
        return -rowIndex * rowHeight;
    }

    /** The legacy texture u-offset into {@code rustic:textures/gui/icons.png} for a toughness icon state. */
    public static int toughnessIconU(Icon icon) {
        return switch (icon) {
            case FULL -> 34;
            case HALF -> 25;
            case EMPTY -> 16;
        };
    }

    /** All toughness icons share v-offset 0 in the legacy sheet. */
    public static final int TOUGHNESS_ICON_V = 0;

    private ArmorHudLayout() {
    }
}
