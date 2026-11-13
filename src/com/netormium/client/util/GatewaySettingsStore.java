package com.netormium.client.util;

import javax.microedition.rms.RecordStore;

/** Persistent gateway endpoint override; an empty value means use the JAD property. */
public final class GatewaySettingsStore {
    private static final String STORE = "NetormiumGateway";

    public String load() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, false);
            if (rs.getNumRecords() == 0) return "";
            byte[] data = rs.getRecord(1);
            if (data == null) return "";
            String value = new String(data, "UTF-8");
            return isValid(value) ? trimSlash(value.trim()) : "";
        } catch (Exception ignored) { return ""; }
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
    }

    public boolean save(String value) {
        if (value == null) return false;
        String candidate = trimSlash(value.trim());
        if (!isValid(candidate)) return false;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            byte[] data = candidate.getBytes("UTF-8");
            if (rs.getNumRecords() == 0) rs.addRecord(data, 0, data.length);
            else rs.setRecord(1, data, 0, data.length);
            return true;
        } catch (Exception e) { return false; }
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
    }

    public void clear() {
        RecordStore rs = null;
        try { rs = RecordStore.openRecordStore(STORE, false); rs.closeRecordStore(); rs = null; RecordStore.deleteRecordStore(STORE); }
        catch (Exception ignored) {}
        finally { try { if (rs != null) rs.closeRecordStore(); } catch (Exception ignored) {} }
    }

    private String trimSlash(String value) {
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private boolean isValid(String value) {
        if (value == null || value.length() < 9 || value.length() > 180 ||
            value.indexOf(' ') >= 0 || value.indexOf('?') >= 0 || value.indexOf('#') >= 0) return false;
        String lower = value.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false;
        int start = value.indexOf("://") + 3;
        if (start >= value.length()) return false;
        String authority = value.substring(start);
        int slash = authority.indexOf('/');
        if (slash >= 0) authority = authority.substring(0, slash);
        return authority.length() > 0 && authority.indexOf('@') < 0;
    }
}
