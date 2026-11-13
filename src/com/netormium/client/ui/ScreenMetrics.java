package com.netormium.client.ui;

/**
 * Snapshot dimensi area Canvas dan karakteristik dasar layar.
 * MIDP 2.0 / CLDC 1.1 compatible.
 */
public final class ScreenMetrics {
    public static final int PORTRAIT = 1;
    public static final int LANDSCAPE = 2;
    public static final int SQUARE = 3;

    private final int width;
    private final int height;
    private final int orientation;
    private final int densityClass;

    public ScreenMetrics(int width, int height) {
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }

        this.width = width;
        this.height = height;

        if (width > height) {
            orientation = LANDSCAPE;
        } else if (height > width) {
            orientation = PORTRAIT;
        } else {
            orientation = SQUARE;
        }

        int shortSide = width < height ? width : height;
        if (shortSide < 160) {
            densityClass = 0; // sangat kecil
        } else if (shortSide < 240) {
            densityClass = 1; // kecil
        } else if (shortSide < 360) {
            densityClass = 2; // sedang
        } else {
            densityClass = 3; // besar
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getDensityClass() {
        return densityClass;
    }

    public boolean isLandscape() {
        return orientation == LANDSCAPE;
    }
}
