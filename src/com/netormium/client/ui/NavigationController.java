package com.netormium.client.ui;

import javax.microedition.lcdui.Canvas;

/**
 * Normalisasi input keypad. Aksi dikembalikan ke pemanggil agar
 * aplikasi dapat menghubungkannya ke browser/controller.
 */
public final class NavigationController {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_DOWN = 2;
    public static final int ACTION_LEFT = 3;
    public static final int ACTION_RIGHT = 4;
    public static final int ACTION_SELECT = 5;
    public static final int ACTION_BACK = 6;
    public static final int ACTION_MENU = 7;

    private int backKey;
    private int menuKey;

    public NavigationController() {
        backKey = -1;
        menuKey = -1;
    }

    public void setSoftkeyKeyCodes(int backKeyCode, int menuKeyCode) {
        backKey = backKeyCode;
        menuKey = menuKeyCode;
    }

    public int mapKey(Canvas canvas, int keyCode) {
        if (backKey != -1 && keyCode == backKey) return ACTION_BACK;
        if (menuKey != -1 && keyCode == menuKey) return ACTION_MENU;

        int gameAction = canvas.getGameAction(keyCode);
        switch (gameAction) {
            case Canvas.UP: return ACTION_UP;
            case Canvas.DOWN: return ACTION_DOWN;
            case Canvas.LEFT: return ACTION_LEFT;
            case Canvas.RIGHT: return ACTION_RIGHT;
            case Canvas.FIRE: return ACTION_SELECT;
            default: return ACTION_NONE;
        }
    }
}
