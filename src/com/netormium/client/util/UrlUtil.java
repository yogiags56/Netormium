package com.netormium.client.util;

public final class UrlUtil {
    private UrlUtil() {
    }

    public static String normalize(String input) {
        if (input == null) return null;
        String value = input.trim();
        if (value.length() == 0) return null;

        if (value.indexOf("://") < 0) {
            value = "http://" + value;
        }

        int schemeEnd = value.indexOf("://");
        String scheme = value.substring(0, schemeEnd).toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return null;
        }

        return value;
    }
}
