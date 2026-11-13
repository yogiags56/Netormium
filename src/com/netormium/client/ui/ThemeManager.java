package com.netormium.client.ui;

import javax.microedition.lcdui.Font;

/** Palet dan pemilihan font dasar untuk UI Netormium. */
public final class ThemeManager {
    public static final int BACKGROUND = 0xFFFFFF;
    public static final int FOREGROUND = 0x202020;
    public static final int HEADER_BACKGROUND = 0xE8F0F8;
    public static final int HEADER_FOREGROUND = 0x102840;
    public static final int ACCENT = 0x1769AA;
    public static final int BORDER = 0xA0A0A0;
    public static final int MUTED = 0x666666;

    private ThemeManager() {
    }

    public static Font getBodyFont(ScreenMetrics metrics) {
        int shortSide = metrics.getWidth() < metrics.getHeight()
            ? metrics.getWidth() : metrics.getHeight();

        if (shortSide < 160) {
            return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        }
        if (shortSide < 240) {
            return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        }
        return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    }

    public static Font getTitleFont() {
        return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    }
}
