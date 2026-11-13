package com.netormium.client.net;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import com.netormium.client.util.UrlUtil;

/**
 * Client NTM/1. Reads the gateway's line protocol.
 * Configure GATEWAY_BASE_URL for the deployment.
 */
public final class GatewayPageLoader {
    private String baseUrl = "";

    public void configureBaseUrl(String value) {
        if (value == null) return;
        String candidate = value.trim();
        while (candidate.endsWith("/")) candidate = candidate.substring(0, candidate.length() - 1);
        if (isValidBaseUrl(candidate)) baseUrl = candidate;
    }
    private static final int MAX_RESPONSE_BYTES = 65536;

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

    public static final class Link {
        public int index;
        public String text;
        public String href;
    }

    public static final class Page {
        public int status;
        public String url = "";
        public String title = "";
        public String text = "";
        public String error = "";
        public Link[] links = new Link[0];
    }

    public Page load(String targetUrl) {
        Page page = new Page();
        HttpConnection connection = null;
        InputStream input = null;
        try {
            if (baseUrl.length() == 0) {
                page.error = "Gateway URL is not configured. Set Netormium-Gateway-URL in the JAD.";
                return page;
            }
            String requestUrl = baseUrl + "/v1/page?url=" + encode(targetUrl);
            connection = (HttpConnection) Connector.open(requestUrl, Connector.READ, true);
            connection.setRequestMethod(HttpConnection.GET);
            connection.setRequestProperty("User-Agent", "Netormium/1.0-rc21 (Java ME)");
            connection.setRequestProperty("Accept", "text/plain");
            connection.setRequestProperty("Accept-Encoding", "identity");

            int httpStatus = connection.getResponseCode();
            if (httpStatus < 200 || httpStatus >= 300) {
                page.error = "Gateway HTTP " + httpStatus;
                return page;
            }

            input = connection.openInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_RESPONSE_BYTES) {
                    page.error = "Gateway response exceeds 64 KB";
                    return page;
                }
                bytes.write(buffer, 0, count);
            }
            parse(new String(bytes.toByteArray(), "UTF-8"), page);
        } catch (Exception e) {
            page.error = e.toString();
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
        }
        return page;
    }

    private void parse(String response, Page page) throws Exception {
        java.util.Vector links = new java.util.Vector();
        int start = 0;
        boolean headerSeen = false;

        while (start < response.length()) {
            int end = response.indexOf('\n', start);
            if (end < 0) end = response.length();
            String line = response.substring(start, end);
            if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
            start = end + 1;

            if (!headerSeen) {
                if (!"NTM/1".equals(line)) {
                    page.error = "Unsupported gateway protocol";
                    return;
                }
                headerSeen = true;
                continue;
            }
            if ("END".equals(line)) break;

            String[] fields = splitTabs(line);
            if (fields.length < 2) continue;
            if ("STATUS".equals(fields[0])) {
                try { page.status = Integer.parseInt(fields[1]); } catch (Exception ignored) {}
            } else if ("URL".equals(fields[0])) {
                page.url = decode(fields[1]);
            } else if ("TITLE".equals(fields[0])) {
                page.title = decode(fields[1]);
            } else if ("TEXT".equals(fields[0])) {
                page.text = decode(fields[1]);
            } else if ("ERROR".equals(fields[0])) {
                page.error = decode(fields[1]);
            } else if ("LINK".equals(fields[0]) && fields.length >= 4) {
                Link link = new Link();
                try { link.index = Integer.parseInt(fields[1]); } catch (Exception ignored) {}
                link.text = decode(fields[2]);
                link.href = decode(fields[3]);
                links.addElement(link);
            }
        }

        page.links = new Link[links.size()];
        links.copyInto(page.links);
        if (!headerSeen) page.error = "Empty gateway response";
    }

    private String[] splitTabs(String line) {
        java.util.Vector parts = new java.util.Vector();
        int start = 0;
        int at;
        while ((at = line.indexOf('\t', start)) >= 0) {
            parts.addElement(line.substring(start, at));
            start = at + 1;
        }
        parts.addElement(line.substring(start));
        String[] result = new String[parts.size()];
        parts.copyInto(result);
        return result;
    }

    private String encode(String value) throws Exception {
        byte[] bytes = value.getBytes("UTF-8");
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 255;
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') ||
                (b >= '0' && b <= '9') || b == '-' || b == '_' || b == '.' || b == '~') {
                out.append((char)b);
            } else {
                out.append('%');
                String hex = "0123456789ABCDEF";
                out.append(hex.charAt((b >> 4) & 15));
                out.append(hex.charAt(b & 15));
            }
        }
        return out.toString();
    }

    private String decode(String value) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < value.length();) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length()) {
                int hi = Character.digit(value.charAt(i + 1), 16);
                int lo = Character.digit(value.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    out.write((hi << 4) | lo);
                    i += 3;
                    continue;
                }
            }
            if (c == '+') {
                out.write(' ');
            } else {
                byte[] raw = String.valueOf(c).getBytes("UTF-8");
                out.write(raw);
            }
            i++;
        }
        return new String(out.toByteArray(), "UTF-8");
    }
}
