package com.netormium.client.net;

/**
 * Parser fallback sangat ringan: mengubah HTML menjadi teks.
 * Bukan parser HTML5 dan tidak mengeksekusi JavaScript/CSS.
 */
public final class HtmlTextParser {
    private HtmlTextParser() {
    }

    public static String extractTitle(String html) {
        String lower = html.toLowerCase();
        int start = lower.indexOf("<title");
        if (start < 0) return "";
        start = lower.indexOf('>', start);
        if (start < 0) return "";
        int end = lower.indexOf("</title", start + 1);
        if (end < 0) return "";
        return decodeEntities(stripTags(html.substring(start + 1, end))).trim();
    }

    public static String toText(String html) {
        StringBuffer out = new StringBuffer();
        StringBuffer tag = new StringBuffer();
        boolean inTag = false;
        boolean inScript = false;
        boolean inStyle = false;
        String lower = html.toLowerCase();
        int i = 0;

        while (i < html.length()) {
            char c = html.charAt(i);
            if (c == '<') {
                int end = html.indexOf('>', i);
                if (end < 0) break;
                String token = lower.substring(i, end + 1);
                if (token.startsWith("<script")) inScript = true;
                if (token.startsWith("</script")) inScript = false;
                if (token.startsWith("<style")) inStyle = true;
                if (token.startsWith("</style")) inStyle = false;

                if (!inScript && !inStyle &&
                    (token.startsWith("<br") || token.startsWith("</p") ||
                     token.startsWith("</div") || token.startsWith("</h") ||
                     token.startsWith("</li") || token.startsWith("</tr") ||
                     token.startsWith("<hr"))) {
                    appendSpaceOrNewline(out, '\n');
                }
                i = end + 1;
            } else {
                if (!inScript && !inStyle) out.append(c);
                i++;
            }
        }

        return normalizeWhitespace(decodeEntities(out.toString()));
    }

    private static String stripTags(String value) {
        StringBuffer out = new StringBuffer();
        boolean tag = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<') tag = true;
            else if (c == '>') tag = false;
            else if (!tag) out.append(c);
        }
        return out.toString();
    }

    private static void appendSpaceOrNewline(StringBuffer out, char c) {
        int n = out.length();
        if (n > 0 && out.charAt(n - 1) != '\n') out.append(c);
    }

    private static String normalizeWhitespace(String value) {
        StringBuffer out = new StringBuffer();
        boolean pendingSpace = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\t' || c == ' ') {
                pendingSpace = true;
            } else if (c == '\n') {
                while (out.length() > 0 && out.charAt(out.length() - 1) == ' ') {
                    out.setLength(out.length() - 1);
                }
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
                pendingSpace = false;
            } else {
                if (pendingSpace && out.length() > 0 &&
                    out.charAt(out.length() - 1) != '\n') out.append(' ');
                out.append(c);
                pendingSpace = false;
            }
        }
        return out.toString().trim();
    }

    private static String decodeEntities(String value) {
        return replace(value, "&nbsp;", " ")
            .replace('&', '&'); // Named entities beyond nbsp are retained in this MVP.
    }

    private static String replace(String source, String target, String replacement) {
        StringBuffer out = new StringBuffer();
        int from = 0;
        int at;
        while ((at = source.indexOf(target, from)) >= 0) {
            out.append(source.substring(from, at));
            out.append(replacement);
            from = at + target.length();
        }
        out.append(source.substring(from));
        return out.toString();
    }
}
