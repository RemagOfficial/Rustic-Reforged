package rustic.content.client.hud;

public final class ArmorHudLayout {

    public static final int ICONS_PER_ROW = 10;

    public static final int POINTS_PER_ICON = 2;

    public static final int BASE_ROW_HEIGHT = 10;

    public enum Icon {
        FULL,
        HALF,
        EMPTY
    }

    public static int rows(int value) {
        if (value <= 0) {
            return 0;
        }
        int perRow = ICONS_PER_ROW * POINTS_PER_ICON;

        return (value + perRow - 1) / perRow;
    }

    public static int rowHeight(int rows) {
        return Math.min(Math.max(BASE_ROW_HEIGHT - (rows - 2), 3), BASE_ROW_HEIGHT);
    }

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

    public static int extraRowOffsetY(int rowIndex, int rowHeight) {
        return -rowIndex * rowHeight;
    }

    public static int toughnessIconU(Icon icon) {
        return switch (icon) {
            case FULL -> 34;
            case HALF -> 25;
            case EMPTY -> 16;
        };
    }

    public static final int TOUGHNESS_ICON_V = 0;

    private ArmorHudLayout() {
    }
}
