package com.netormium.client.util;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordEnumeration;

/** Persistent bookmarks stored in MIDP RMS. */
public final class BookmarkStore {
    private static final String STORE = "NetormiumBookmarks";

    public static final class Bookmark {
        public String title, url;
        Bookmark(String t, String u) { title = t; url = u; }
    }

    public boolean add(String title, String url) {
        if (url == null || url.length() == 0) return false;
        RecordStore rs = null;
        RecordEnumeration en = null;
        boolean added = false;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            en = rs.enumerateRecords(null, null, false);
            boolean exists = false;
            while (en.hasNextElement()) {
                byte[] old = rs.getRecord(en.nextRecordId());
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(old));
                in.readUTF();
                if (url.equals(in.readUTF())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF(title == null ? url : title);
                out.writeUTF(url);
                out.flush();
                byte[] data = bytes.toByteArray();
                rs.addRecord(data, 0, data.length);
            }
            added = true;
        } catch (Exception e) {
            added = false;
        } finally {
            if (en != null) {
                try { en.destroy(); } catch (Exception ignored) {}
            }
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
        }
        return added;
    }

    public Bookmark[] all() {
        java.util.Vector v = new java.util.Vector();
        RecordStore rs = null;
        RecordEnumeration en = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            en = rs.enumerateRecords(null, null, false);
            while (en.hasNextElement()) {
                byte[] data = rs.getRecord(en.nextRecordId());
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
                v.addElement(new Bookmark(in.readUTF(), in.readUTF()));
            }
        } catch (Exception ignored) {
        } finally {
            if (en != null) {
                try { en.destroy(); } catch (Exception ignored) {}
            }
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
        }
        Bookmark[] out = new Bookmark[v.size()];
        v.copyInto(out);
        return out;
    }
}
