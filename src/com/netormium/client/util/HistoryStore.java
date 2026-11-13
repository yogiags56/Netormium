package com.netormium.client.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.rms.RecordStore;

/** Bounded persistent history for MIDP RMS. */
public final class HistoryStore {
    private static final String STORE = "NetormiumHistory";
    private static final int VERSION = 1;
    private static final int MAX_PERSISTED_ENTRIES = 24;
    private static final int MAX_RECORD_BYTES = 49152;

    public void save(BrowserHistory history) {
        RecordStore rs = null;
        try {
            String[] all = history.entries();
            int start = Math.max(0, all.length - MAX_PERSISTED_ENTRIES);
            int count = all.length - start;
            int selected = history.index() - start;

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(VERSION);
            out.writeInt(count);
            out.writeInt(selected);
            for (int i = start; i < all.length; i++) {
                String url = all[i] == null ? "" : all[i];
                if (url.length() > 512) url = url.substring(0, 512);
                out.writeUTF(url);
            }
            out.flush();
            byte[] data = bytes.toByteArray();
            if (data.length > MAX_RECORD_BYTES) return;

            rs = RecordStore.openRecordStore(STORE, true);
            if (rs.getNumRecords() == 0) rs.addRecord(data, 0, data.length);
            else rs.setRecord(1, data, 0, data.length);
        } catch (Exception ignored) {
            // A full RMS store must not interrupt page navigation.
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {}
        }
    }

    public void load(BrowserHistory history) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            if (rs.getNumRecords() == 0) return;
            byte[] data = rs.getRecord(1);
            if (data == null || data.length < 12 || data.length > MAX_RECORD_BYTES) return;

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            if (in.readInt() != VERSION) return;
            int count = in.readInt();
            int selected = in.readInt();
            if (count < 0 || count > MAX_PERSISTED_ENTRIES) return;
            String[] urls = new String[count];
            for (int i = 0; i < count; i++) urls[i] = in.readUTF();
            if (in.available() != 0) return;
            history.restore(urls, selected);
        } catch (Exception ignored) {
            // Ignore corrupt RMS history and start with an empty history.
        } finally {
            try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {}
        }
    }
}
