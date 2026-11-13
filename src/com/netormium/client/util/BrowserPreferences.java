package com.netormium.client.util;

import javax.microedition.rms.RecordStore;

/** Persistent MIDlet preferences. Version 2 extends the original Data Saver record. */
public final class BrowserPreferences {
    private static final String STORE = "NetormiumPrefs";
    private static final int VERSION = 2;
    private static final int DEFAULT_FONT_SIZE = 1;

    private byte[] read() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            if (rs.getNumRecords() < 1) return null;
            return rs.getRecord(1);
        } catch (Exception ignored) { return null; }
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
    }

    private void write(byte[] data) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            if (rs.getNumRecords() < 1) rs.addRecord(data, 0, data.length);
            else rs.setRecord(1, data, 0, data.length);
        } catch (Exception ignored) {}
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
    }

    private byte[] normalized() {
        byte[] old = read();
        byte saver = 1;
        byte font = DEFAULT_FONT_SIZE;
        byte full = 0;
        if (old != null && old.length >= 2) {
            saver = old[1];
            if (old.length >= 4 && (old[0] & 255) >= 2) {
                font = old[2];
                full = old[3];
            }
        }
        if (font < 0 || font > 2) font = DEFAULT_FONT_SIZE;
        return new byte[] {(byte) VERSION, saver, font, full};
    }

    public boolean loadDataSaver(boolean fallback) {
        byte[] d = read();
        if (d == null || d.length < 2) return fallback;
        return d[1] != 0;
    }

    public void saveDataSaver(boolean enabled) {
        byte[] d = normalized();
        d[1] = (byte) (enabled ? 1 : 0);
        write(d);
    }

    public int loadFontSize() {
        byte[] d = normalized();
        return d[2];
    }

    public void saveFontSize(int size) {
        if (size < 0 || size > 2) return;
        byte[] d = normalized();
        d[2] = (byte) size;
        write(d);
    }

    public boolean loadFullScreen() {
        return normalized()[3] != 0;
    }

    public void saveFullScreen(boolean enabled) {
        byte[] d = normalized();
        d[3] = (byte) (enabled ? 1 : 0);
        write(d);
    }
}
