package com.netormium.client.net;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/** Loader HTTP sederhana; selalu jalankan dari worker thread. */
public final class HttpPageLoader {
    private static final int MAX_BYTES = 131072;

    public static final class Result {
        public String finalUrl;
        public String title;
        public String text;
        public int status;
        public String error;
    }

    public Result load(String url) {
        Result result = new Result();
        HttpConnection connection = null;
        InputStream input = null;

        try {
            connection = (HttpConnection) Connector.open(url, Connector.READ, true);
            connection.setRequestMethod(HttpConnection.GET);
            connection.setRequestProperty("User-Agent", "Netormium/1.0-rc20 (Java ME)");
            connection.setRequestProperty("Accept", "text/html,text/plain,*/*");
            connection.setRequestProperty("Accept-Encoding", "identity");

            result.status = connection.getResponseCode();
            result.finalUrl = connection.getURL();

            if (result.status < 200 || result.status >= 400) {
                result.error = "HTTP " + result.status;
                return result;
            }

            input = connection.openInputStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            int total = 0;

            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_BYTES) {
                    result.error = "Page too large for MVP (limit 128 KB)";
                    return result;
                }
                bytes.write(buffer, 0, count);
            }

            String body = new String(bytes.toByteArray(), "UTF-8");
            result.title = HtmlTextParser.extractTitle(body);
            result.text = HtmlTextParser.toText(body);
        } catch (Exception e) {
            result.error = e.toString();
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            try { if (connection != null) connection.close(); } catch (Exception ignored) {}
        }
        return result;
    }
}
