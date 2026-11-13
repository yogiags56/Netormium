package com.netormium.client;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;
import com.netormium.client.ui.BrowserCanvas;

public final class NetormiumMIDlet extends MIDlet {
    private BrowserCanvas browser;

    public void startApp() {
        if (browser == null) {
            browser = new BrowserCanvas(this);
        }
        Display.getDisplay(this).setCurrent(browser);
    }

    public void pauseApp() {
        if (browser != null) browser.persistOnPause();
    }

    public void destroyApp(boolean unconditional) {
        if (browser != null) {
            browser.persistOnPause();
            browser.stopLoading();
        }
    }

    public void exit() {
        destroyApp(true);
        notifyDestroyed();
    }
}
