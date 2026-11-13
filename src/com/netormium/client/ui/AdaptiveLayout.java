package com.netormium.client.ui;

/**
 * Menghitung geometri UI dari ukuran Canvas aktual.
 * Semua nilai dalam pixel; tidak mengasumsikan resolusi tertentu.
 */
public final class AdaptiveLayout {
    private final ScreenMetrics metrics;
    private final int margin;
    private final int addressBarHeight;
    private final int toolbarHeight;
    private final int statusHeight;
    private final int contentX;
    private final int contentY;
    private final int contentWidth;
    private final int contentHeight;

    public AdaptiveLayout(ScreenMetrics metrics) {
        this.metrics = metrics;

        int w = metrics.getWidth();
        int h = metrics.getHeight();
        int shortSide = w < h ? w : h;

        margin = clamp(shortSide / 40, 2, 8);
        addressBarHeight = clamp(h / 11, 16, 30);
        toolbarHeight = clamp(h / 12, 16, 28);
        statusHeight = h >= 180 ? clamp(h / 24, 8, 16) : 0;

        contentX = margin;
        contentY = addressBarHeight + margin;
        contentWidth = positive(w - (margin * 2));
        contentHeight = positive(
            h - contentY - toolbarHeight - statusHeight - margin
        );
    }

    public int getMargin() { return margin; }
    public int getAddressBarHeight() { return addressBarHeight; }
    public int getToolbarHeight() { return toolbarHeight; }
    public int getStatusHeight() { return statusHeight; }
    public int getContentX() { return contentX; }
    public int getContentY() { return contentY; }
    public int getContentWidth() { return contentWidth; }
    public int getContentHeight() { return contentHeight; }
    public int getScreenWidth() { return metrics.getWidth(); }
    public int getScreenHeight() { return metrics.getHeight(); }

    public int getAddressTextX() {
        return contentX + margin;
    }

    public int getAddressTextWidth() {
        return positive(contentWidth - (margin * 2));
    }

    public int getToolbarY() {
        return metrics.getHeight() - toolbarHeight - statusHeight;
    }

    public int getStatusY() {
        return metrics.getHeight() - statusHeight;
    }

    public int getDefaultFontSizeHint() {
        int shortSide = metrics.getWidth() < metrics.getHeight()
            ? metrics.getWidth() : metrics.getHeight();
        if (shortSide < 160) return 0;
        if (shortSide < 240) return 1;
        if (shortSide < 360) return 2;
        return 3;
    }

    private static int positive(int value) {
        return value < 1 ? 1 : value;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
