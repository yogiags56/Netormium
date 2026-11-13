package com.netormium.client.net;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/** Downloads a PNG viewport snapshot; PNG decoding is mandatory in MIDP 2.0. */
public final class SnapshotLoader {
    private String baseUrl = ""; 

    public void configureBaseUrl(String value) {
        if (value == null) return;
        String candidate = value.trim();
        while (candidate.endsWith("/")) candidate = candidate.substring(0, candidate.length() - 1);
        if (isValidBaseUrl(candidate)) baseUrl = candidate;
    }
    private static final int MAX_IMAGE_BYTES = 180000;

    private boolean isValidBaseUrl(String value) {
        if (value == null || value.length() == 0 || value.indexOf(' ') >= 0) return false;
        String lower = value.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false;
        int schemeEnd = value.indexOf("://") + 3;
        if (schemeEnd >= value.length()) return false;
        String host = value.substring(schemeEnd);
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        return host.length() > 0 && host.indexOf('@') < 0;
    }

    public byte[] load(String targetUrl, int width, int height, int scrollY) {
        HttpConnection connection = null;
        InputStream input = null;
        try {
            if (baseUrl.length() == 0) return null;
            String request = baseUrl + "/v1/snapshot?url=" + encode(targetUrl) +
                "&w=" + width + "&h=" + height + "&y=" + scrollY;
            connection = (HttpConnection) Connector.open(request, Connector.READ, true);
            connection.setRequestMethod(HttpConnection.GET);
            connection.setRequestProperty("User-Agent", "Netormium/1.0.0-rc21 (Java ME)");
            connection.setRequestProperty("Accept", "image/png");
            if (connection.getResponseCode() != 200) return null;
            long declaredLength = connection.getLength();
            if (declaredLength > MAX_IMAGE_BYTES) return null;
            input = connection.openInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(4096);
            byte[] buffer = new byte[1024];
            int count, total = 0;
            while ((count = input.read(buffer)) != -1) {
                if (count == 0) {
                    int single = input.read();
                    if (single < 0) break;
                    total++;
                    if (total > MAX_IMAGE_BYTES) return null;
                    bytes.write(single);
                    continue;
                }
                total += count;
                if (total > MAX_IMAGE_BYTES) return null;
                bytes.write(buffer, 0, count);
            }
            return bytes.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
        }
    }

    private String encode(String value) throws Exception {
        byte[] bytes = value.getBytes("UTF-8");
        StringBuffer out = new StringBuffer();
        String hex = "0123456789ABCDEF";
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 255;
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') ||
                (b >= '0' && b <= '9') || b == '-' || b == '_' || b == '.' || b == '~') {
                out.append((char)b);
            } else {
                out.append('%').append(hex.charAt((b >> 4) & 15)).append(hex.charAt(b & 15));
            }
        }
        return out.toString();
    }
}
