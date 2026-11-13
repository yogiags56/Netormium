package com.netormium.client.util;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordEnumeration;

/** Independent persistent Speed Dial collection; separate from ordinary bookmarks. */
public final class SpeedDialStore {
    private static final String STORE = "NetormiumSpeedDial";
    public static final int MAX_ITEMS = 12;

    public static final class Entry {
        public String title;
        public String url;
        Entry(String title, String url) { this.title = title; this.url = url; }
    }

    public boolean add(String title, String url) {
        if (url == null || url.length() == 0) return false;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            boolean exists = false;
            int count = 0;
            try {
                while (en.hasNextElement()) {
                    byte[] data = rs.getRecord(en.nextRecordId());
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
                    in.readUTF();
                    String oldUrl = in.readUTF();
                    count++;
                    if (url.equals(oldUrl)) exists = true;
                }
            } finally { en.destroy(); }
            if (exists) return true;
            if (count >= MAX_ITEMS) return false;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(title == null || title.length() == 0 ? url : title);
            out.writeUTF(url);
            out.flush();
            byte[] data = bytes.toByteArray();
            rs.addRecord(data, 0, data.length);
            return true;
        } catch (Exception e) { return false; }
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
    }

    public boolean delete(String url) {
        if (url == null) return false;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            try {
                while (en.hasNextElement()) {
                    int id = en.nextRecordId();
                    DataInputStream in = new DataInputStream(
                        new ByteArrayInputStream(rs.getRecord(id)));
                    in.readUTF();
                    if (url.equals(in.readUTF())) {
                        rs.deleteRecord(id);
                        return true;
                    }
                }
            } finally { en.destroy(); }
        } catch (Exception e) { return false; }
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
        return false;
    }

    public boolean rename(String url, String title) {
        if (url == null || title == null || title.length() == 0) return false;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            try {
                while (en.hasNextElement()) {
                    int id = en.nextRecordId();
                    DataInputStream in = new DataInputStream(
                        new ByteArrayInputStream(rs.getRecord(id)));
                    in.readUTF();
                    String storedUrl = in.readUTF();
                    if (url.equals(storedUrl)) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(bytes);
                        out.writeUTF(title);
                        out.writeUTF(storedUrl);
                        out.flush();
                        byte[] data = bytes.toByteArray();
                        rs.setRecord(id, data, 0, data.length);
                        return true;
                    }
                }
            } finally { en.destroy(); }
        } catch (Exception e) { return false; }
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
        return false;
    }

    public Entry[] all() {
        java.util.Vector values = new java.util.Vector();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            try {
                while (en.hasNextElement()) {
                    DataInputStream in = new DataInputStream(
                        new ByteArrayInputStream(rs.getRecord(en.nextRecordId())));
                    values.addElement(new Entry(in.readUTF(), in.readUTF()));
                }
            } finally { en.destroy(); }
        } catch (Exception ignored) {}
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
        Entry[] result = new Entry[values.size()];
        values.copyInto(result);
        return result;
    }
}
