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
        RecordEnumeration en = null;
        int deleteId = -1;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            en = rs.enumerateRecords(null, null, false);
            while (en.hasNextElement() && deleteId < 0) {
                int id = en.nextRecordId();
                DataInputStream in = new DataInputStream(
                    new ByteArrayInputStream(rs.getRecord(id)));
                in.readUTF();
                String storedUrl = in.readUTF();
                in.close();
                if (url.equals(storedUrl)) deleteId = id;
            }
            en.destroy();
            en = null;
            if (deleteId > 0) {
                rs.deleteRecord(deleteId);
                rs.closeRecordStore();
                rs = null;
                return true;
            }
        } catch (Exception e) {
            if (en != null) {
                try { en.destroy(); } catch (Exception ignored) {}
            }
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
            return false;
        }
        if (rs != null) {
            try { rs.closeRecordStore(); } catch (Exception ignored) {}
        }
        return false;
    }

    public boolean rename(String url, String title) {
        if (url == null || title == null || title.length() == 0) return false;
        RecordStore rs = null;
        RecordEnumeration en = null;
        int updateId = -1;
        String storedUrl = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            en = rs.enumerateRecords(null, null, false);
            while (en.hasNextElement() && updateId < 0) {
                int id = en.nextRecordId();
                DataInputStream in = new DataInputStream(
                    new ByteArrayInputStream(rs.getRecord(id)));
                in.readUTF();
                String candidateUrl = in.readUTF();
                in.close();
                if (url.equals(candidateUrl)) {
                    updateId = id;
                    storedUrl = candidateUrl;
                }
            }
            en.destroy();
            en = null;
            if (updateId > 0) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF(title);
                out.writeUTF(storedUrl);
                out.flush();
                byte[] data = bytes.toByteArray();
                rs.setRecord(updateId, data, 0, data.length);
                rs.closeRecordStore();
                rs = null;
                return true;
            }
        } catch (Exception e) {
            if (en != null) {
                try { en.destroy(); } catch (Exception ignored) {}
            }
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
            return false;
        }
        if (rs != null) {
            try { rs.closeRecordStore(); } catch (Exception ignored) {}
        }
        return false;
    }

    public boolean move(String url, int offset) {
        if (url == null || (offset != -1 && offset != 1)) return false;
        Entry[] entries = all();
        int index = -1;
        for (int i = 0; i < entries.length; i++) {
            if (url.equals(entries[i].url)) { index = i; break; }
        }
        int target = index + offset;
        if (index < 0 || target < 0 || target >= entries.length) return false;
        Entry temp = entries[index];
        entries[index] = entries[target];
        entries[target] = temp;

        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            RecordEnumeration en = rs.enumerateRecords(null, null, false);
            try {
                while (en.hasNextElement()) rs.deleteRecord(en.nextRecordId());
            } finally { en.destroy(); }
            for (int i = 0; i < entries.length; i++) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF(entries[i].title);
                out.writeUTF(entries[i].url);
                out.flush();
                byte[] data = bytes.toByteArray();
                rs.addRecord(data, 0, data.length);
            }
            return true;
        } catch (Exception e) { return false; }
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
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
