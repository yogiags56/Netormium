package com.netormium.client.net;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/** NTM/2 persistent browser session client for MIDP 2.0 / CLDC 1.1. */
public final class InteractiveGateway {
    private String baseUrl = "";
    private String apiToken = "";
    private static final int MAX_META = 60000;
    private static final int MAX_PNG = 180000;
    private volatile String lastTransportError = "";
    private volatile String activeOpenId = "";
    private int openSequence = 0;

    public static final class Element {
        public int index, x, y, width, height;
        public String kind, label, href, inputType;
        public boolean focused;
    }

    public static final class Page {
        public String sessionId = "";
        public String url = "";
        public String title = "";
        public String text = "";
        public Element[] elements = new Element[0];
        public String error = "";
        public boolean dataSaver = false;
    }

    public void configureBaseUrl(String value, String token) {
        apiToken = token == null ? "" : token.trim();
        if (value == null) return;
        String candidate = value.trim();
        while (candidate.endsWith("/")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }
        if (isValidBaseUrl(candidate)) {
            baseUrl = candidate;
        }
    }

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

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getLastTransportError() {
        return lastTransportError;
    }

    public boolean isSessionExpired(String error) {
        if (error == null) return false;
        String value = error.toLowerCase();
        return value.indexOf("http 404") >= 0 ||
            value.indexOf("session expired") >= 0 ||
            value.indexOf("session not found") >= 0;
    }

    public String healthCheck() {
        byte[] data = requestBytes("/health", 1024);
        if (data == null) return lastTransportError.length() == 0 ? "Unavailable" : lastTransportError;
        String value = new String(data).trim();
        return value.length() == 0 ? "Connected (empty response)" : value;
    }

    public Page open(String url, int width, int height) {
        return open(url, width, height, true);
    }

    public Page open(String url, int width, int height, boolean dataSaver) {
        String requestId;
        synchronized (this) {
            openSequence++;
            requestId = Long.toString(System.currentTimeMillis()) + "-" + openSequence;
            activeOpenId = requestId;
        }
        try {
            return requestPage("/v1/session/open?url=" + encode(url) +
                "&w=" + width + "&h=" + height + "&ds=" + (dataSaver ? "1" : "0") +
                "&rid=" + encode(requestId));
        } finally {
            synchronized (this) {
                if (requestId.equals(activeOpenId)) activeOpenId = "";
            }
        }
    }

    /** Requests cancellation of the current in-flight session creation. */
    public void cancelCurrentOpen() {
        final String requestId = activeOpenId;
        if (requestId.length() == 0) return;
        new Thread(new Runnable() {
            public void run() {
                requestBytes("/v1/session/cancel?rid=" + encode(requestId), 1024);
            }
        }).start();
    }

    public Page tap(String sessionId, int x, int y) {
        return requestPage("/v1/session/tap?id=" + encode(sessionId) +
            "&x=" + x + "&y=" + y);
    }

    public Page type(String sessionId, String value) {
        return requestPage("/v1/session/type?id=" + encode(sessionId) +
            "&text=" + encode(value));
    }

    public Page key(String sessionId, String key) {
        return requestPage("/v1/session/key?id=" + encode(sessionId) +
            "&key=" + encode(key));
    }

    public Page inspect(String sessionId) {
        return requestPage("/v1/session/metadata?id=" + encode(sessionId));
    }

    public byte[] snapshot(String sessionId, int scrollY) {
        byte[] png = requestBytes("/v1/session/snapshot?id=" + encode(sessionId) +
            "&y=" + scrollY, MAX_PNG);
        if (png != null && !hasPngSignature(png)) {
            lastTransportError = "Gateway returned invalid PNG data";
            return null;
        }
        return png;
    }

    private boolean hasPngSignature(byte[] data) {
        if (data == null || data.length < 24 ||
            (data[0] & 255) != 137 || data[1] != 80 ||
            data[2] != 78 || data[3] != 71 ||
            data[4] != 13 || data[5] != 10 ||
            data[6] != 26 || data[7] != 10 ||
            data[12] != 73 || data[13] != 72 ||
            data[14] != 68 || data[15] != 82) {
            return false;
        }

        long width = readPngUnsignedInt(data, 16);
        long height = readPngUnsignedInt(data, 20);
        if (width < 1 || width > 640 || height < 1 || height > 960) {
            return false;
        }
        return width * height <= 614400L;
    }

    private long readPngUnsignedInt(byte[] data, int offset) {
        return ((long)(data[offset] & 255) << 24) |
            ((long)(data[offset + 1] & 255) << 16) |
            ((long)(data[offset + 2] & 255) << 8) |
            (long)(data[offset + 3] & 255);
    }

    public void close(final String sessionId) {
        if (sessionId == null || sessionId.length() == 0) return;
        new Thread(new Runnable() {
            public void run() {
                requestBytes("/v1/session/close?id=" + encode(sessionId), 1024);
            }
        }).start();
    }

    private Page requestPage(String path) {
        Page page = new Page();
        byte[] data = requestBytes(path, MAX_META);
        if (data == null) {
            page.error = lastTransportError.length() == 0 ? "Gateway unavailable. Check connection and gateway address." : lastTransportError;
            return page;
        }
        try {
            parse(new String(data, "UTF-8"), page);
        } catch (Exception e) {
            page.error = e.toString();
        }
        return page;
    }

    private byte[] requestBytes(String path, int maxBytes) {
        boolean retrySafe = path.startsWith("/health") ||
            path.indexOf("/v1/session/metadata") == 0 ||
            path.indexOf("/v1/session/snapshot") == 0;
        int attempts = retrySafe ? 2 : 1;
        byte[] result = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            result = requestBytesOnce(path, maxBytes);
            if (result != null) return result;
            if (attempt + 1 < attempts &&
                (lastTransportError.indexOf("Network error") == 0 ||
                 lastTransportError.indexOf("Gateway HTTP 5") == 0)) {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            } else break;
        }
        return result;
    }

    private byte[] requestBytesOnce(String path, int maxBytes) {
        HttpConnection connection = null;
        InputStream input = null;
        if (baseUrl.length() == 0) {
            lastTransportError = "Gateway URL is not configured. Set Netormium-Gateway-URL in the JAD.";
            return null;
        }
        try {
            connection = (HttpConnection) Connector.open(baseUrl + path, Connector.READ, true);
            connection.setRequestMethod(HttpConnection.GET);
            connection.setRequestProperty("User-Agent", "Netormium/1.0-rc21 (Java ME)");
            if (apiToken.length() > 0) connection.setRequestProperty("Authorization", "Bearer " + apiToken);
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                lastTransportError = code == 404
                    ? "Gateway HTTP 404 - session expired or not found"
                    : "Gateway HTTP " + code;
                return null;
            }
            long declaredLength = connection.getLength();
            if (declaredLength > maxBytes) {
                lastTransportError = "Gateway response exceeds client limit";
                return null;
            }
            input = connection.openInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buffer = new byte[1024];
            int count, total = 0;
            while ((count = input.read(buffer)) != -1) {
                if (count == 0) {
                    int single = input.read();
                    if (single < 0) break;
                    total++;
                    if (total > maxBytes) {
                        lastTransportError = "Gateway response exceeds client limit";
                        return null;
                    }
                    out.write(single);
                    continue;
                }
                total += count;
                if (total > maxBytes) {
                    lastTransportError = "Gateway response exceeds client limit";
                    return null;
                }
                out.write(buffer, 0, count);
            }
            lastTransportError = "";
            return out.toByteArray();
        } catch (Exception e) {
            lastTransportError = "Network error: " + e.toString();
            return null;
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
        }
    }

    private void parse(String raw, Page page) throws Exception {
        java.util.Vector elements = new java.util.Vector();
        int start = 0;
        boolean header = false;
        while (start < raw.length()) {
            int end = raw.indexOf('\n', start);
            if (end < 0) end = raw.length();
            String line = raw.substring(start, end);
            if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
            start = end + 1;
            if (!header) {
                if (!"NTM/2".equals(line)) {
                    page.error = "Gateway returned an invalid response. Check gateway version.";
                    return;
                }
                header = true;
                continue;
            }
            if ("END".equals(line)) break;
            String[] f = split(line);
            if (f.length < 2) continue;
            if ("ERROR".equals(f[0])) page.error = decode(f[1]);
            else if ("SESSION".equals(f[0])) page.sessionId = f[1];
            else if ("DATASAVER".equals(f[0])) page.dataSaver = "1".equals(f[1]);
            else if ("URL".equals(f[0])) page.url = decode(f[1]);
            else if ("TITLE".equals(f[0])) page.title = decode(f[1]);
            else if ("TEXT".equals(f[0])) page.text = decode(f[1]);
            else if ("ELEMENT".equals(f[0]) && f.length >= 9) {
                Element e = new Element();
                e.index = number(f[1]); e.kind = f[2];
                e.x = number(f[3]); e.y = number(f[4]);
                e.width = number(f[5]); e.height = number(f[6]);
                e.label = decode(f[7]); e.href = decode(f[8]);
                e.inputType = f.length > 9 ? decode(f[9]) : "";
                e.focused = f.length > 10 && "1".equals(f[10]);
                elements.addElement(e);
            }
        }
        page.elements = new Element[elements.size()];
        elements.copyInto(page.elements);
        if (!header) page.error = "Empty gateway response";
        else if (page.error.length() == 0 && page.sessionId.length() == 0 && page.url.length() == 0)
            page.error = "Incomplete gateway response";
    }

    private int number(String value) {
        try { return Integer.parseInt(value); } catch (Exception e) { return 0; }
    }

    private String[] split(String line) {
        java.util.Vector v = new java.util.Vector();
        int start = 0, at;
        while ((at = line.indexOf('\t', start)) >= 0) {
            v.addElement(line.substring(start, at)); start = at + 1;
        }
        v.addElement(line.substring(start));
        String[] out = new String[v.size()]; v.copyInto(out); return out;
    }

    private String encode(String value) {
        try {
            byte[] bytes = value.getBytes("UTF-8");
            StringBuffer out = new StringBuffer();
            String hex = "0123456789ABCDEF";
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i] & 255;
                if ((b >= 65 && b <= 90) || (b >= 97 && b <= 122) ||
                    (b >= 48 && b <= 57) || b == 45 || b == 95 || b == 46 || b == 126) {
                    out.append((char)b);
                } else {
                    out.append('%').append(hex.charAt(b >> 4)).append(hex.charAt(b & 15));
                }
            }
            return out.toString();
        } catch (Exception e) { return ""; }
    }

    private String decode(String value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < value.length();) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length()) {
                int a = Character.digit(value.charAt(i + 1), 16);
                int b = Character.digit(value.charAt(i + 2), 16);
                if (a >= 0 && b >= 0) { out.write((a << 4) | b); i += 3; continue; }
            }
            byte[] raw = String.valueOf(c).getBytes("UTF-8");
            out.write(raw); i++;
        }
        return new String(out.toByteArray(), "UTF-8");
    }
}
