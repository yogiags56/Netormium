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
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            boolean exists = false;
            try {
                while (!exists && en.hasNextElement()) {
                    byte[] old = rs.getRecord(en.nextRecordId());
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(old));
                    in.readUTF();
                    exists = url.equals(in.readUTF());
                }
            } finally {
                en.destroy();
            }
            if (exists) return true;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(title == null ? url : title);
            out.writeUTF(url);
            out.flush();
            byte[] data = bytes.toByteArray();
            rs.addRecord(data, 0, data.length);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (rs != null) rs.closeRecordStore();
            } catch (Exception ignored) {}
        }
    }

    public boolean delete(String url) {
        if (url == null) return false;
        RecordStore rs = null;
        int deleteId = -1;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            try {
                while (en.hasNextElement() && deleteId < 0) {
                    int id = en.nextRecordId();
                    byte[] data = rs.getRecord(id);
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
                    in.readUTF();
                    String storedUrl = in.readUTF();
                    if (url.equals(storedUrl)) deleteId = id;
                    in.close();
                }
            } finally {
                en.destroy();
            }
            if (deleteId > 0) {
                rs.deleteRecord(deleteId);
                return true;
            }
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (rs != null) rs.closeRecordStore();
            } catch (Exception ignored) {}
        }
        return false;
    }

    public boolean rename(String url, String title) {
        if (url == null || title == null || title.length() == 0) return false;
        RecordStore rs = null;
        RecordEnumeration en = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            en = rs.enumerateRecords(null, null, false);
            while (en.hasNextElement()) {
                int id = en.nextRecordId();
                byte[] old = rs.getRecord(id);
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(old));
                in.readUTF();
                String storedUrl = in.readUTF();
                in.close();
                if (url.equals(storedUrl)) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(bytes);
                    out.writeUTF(title);
                    out.writeUTF(storedUrl);
                    out.flush();
                    byte[] data = bytes.toByteArray();
                    rs.setRecord(id, data, 0, data.length);
                    en.destroy();
                    en = null;
                    rs.closeRecordStore();
                    rs = null;
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        if (en != null) en.destroy();
        if (rs != null) {
            try {
                rs.closeRecordStore();
            } catch (Exception ignored) {}
        }
        return false;
    }

    public Bookmark[] all() {
        java.util.Vector v = new java.util.Vector();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            try {
                while (en.hasNextElement()) {
                    byte[] data = rs.getRecord(en.nextRecordId());
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
                    v.addElement(new Bookmark(in.readUTF(), in.readUTF()));
                }
            } finally { en.destroy(); }
        } catch (Exception ignored) {}
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
        Bookmark[] out = new Bookmark[v.size()];
        v.copyInto(out);
        return out;
    }
}
