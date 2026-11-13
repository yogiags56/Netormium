package com.netormium.client.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.rms.RecordStore;

/**
 * Persists tab navigation state only. Remote Chromium session IDs are transient
 * and must never be treated as durable across MIDlet or Gateway restarts.
 */
public final class BrowserStateStore {
    private static final String STORE = "NetormiumState";
    private static final int VERSION = 2;
    private static final int MAX_RECORD_BYTES = 16384;

    public void save(TabManager tabs) {
        RecordStore rs = null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(VERSION);
            out.writeInt(tabs.size());
            out.writeInt(tabs.activeIndex());
            for (int i = 0; i < tabs.size(); i++) {
                TabManager.Tab tab = tabs.get(i);
                out.writeUTF(limit(tab.url, 2048));
                out.writeUTF(limit(tab.title, 256));
                out.writeInt(Math.max(0, tab.scrollY));
            }
            out.flush();
            byte[] data = bytes.toByteArray();
            if (data.length > MAX_RECORD_BYTES) return;

            rs = RecordStore.openRecordStore(STORE, true);
            if (rs.getNumRecords() == 0) rs.addRecord(data, 0, data.length);
            else rs.setRecord(1, data, 0, data.length);
        } catch (Exception ignored) {
            // Storage may be full or unavailable; keep the current browsing session alive.
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {}
        }
    }

    public void load(TabManager tabs) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            if (rs.getNumRecords() == 0) return;
            byte[] data = rs.getRecord(1);
            if (data == null || data.length < 12 || data.length > MAX_RECORD_BYTES) return;

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            int version = in.readInt();
            if (version != 1 && version != VERSION) return;
            int count = in.readInt();
            int active = in.readInt();
            if (count < 0 || count > TabManager.MAX_TABS) return;

            String[] urls = new String[count];
            String[] titles = new String[count];
            String[] sessions = new String[count];
            int[] scroll = new int[count];

            for (int i = 0; i < count; i++) {
                urls[i] = in.readUTF();
                titles[i] = in.readUTF();
                // Version 1 stored server-side session IDs. Consume but discard them.
                if (version == 1) in.readUTF();
                sessions[i] = "";
                scroll[i] = Math.max(0, in.readInt());
            }
            if (in.available() != 0) return;
            tabs.restore(urls, titles, sessions, scroll, active);
        } catch (Exception ignored) {
            // Ignore corrupt/old RMS records and retain the default tab.
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {}
        }
    }

    private String limit(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }
}
