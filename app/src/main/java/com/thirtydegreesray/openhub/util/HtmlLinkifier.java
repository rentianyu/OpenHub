package com.thirtydegreesray.openhub.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts plain-text URLs inside an HTML fragment into clickable links.
 *
 * <p>GitHub's own markdown renderer already turns {@code http(s)://} and {@code www.} text into
 * anchors, but bare domains (e.g. {@code example.com}) stay plain text, and it happily swallows
 * trailing Chinese punctuation into the href. This helper walks the HTML as a plain string and only
 * rewrites text nodes, so tags, attributes, attribute case (e.g. SVG {@code viewBox}), white space
 * and comments are left byte-for-byte untouched.</p>
 *
 * <p>Skipped regions: anything inside {@code <a>}, {@code <code>}, {@code <pre>}, {@code <script>},
 * {@code <style>}, {@code <textarea>}, {@code <tt>}, {@code <kbd>}, {@code <samp>} and
 * {@code <svg>}, plus HTML comments and CDATA sections.</p>
 */
public final class HtmlLinkifier {

    /** Elements whose text content must not be auto-linked. */
    private static final Set<String> SUPPRESSED_TAGS = new HashSet<>(Arrays.asList(
            "a", "code", "pre", "script", "style", "textarea", "tt", "kbd", "samp", "svg"));

    /** Elements that never have a closing tag. */
    private static final Set<String> VOID_TAGS = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link",
            "meta", "param", "source", "track", "wbr"));

    /** Characters allowed inside a URL body. ASCII only on purpose: this stops Chinese
     *  punctuation right after a link from being swallowed into the href. */
    private static final String URL_CHARS = "A-Za-z0-9\\-._~:/?#\\[\\]@!$&()*+,;=%";

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(?:https?://|www\\.)[" + URL_CHARS + "]+");

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "(?i)(?<![A-Za-z0-9@._/\\\\:\\-])"
                    + "((?:[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?\\.)+([A-Za-z]{2,10}))"
                    + "(?:/[" + URL_CHARS + "]*)?"
                    + "(?![A-Za-z0-9\\-])");

    /** Top level domains we are willing to link. Kept as a white list so that file names
     *  such as README.md, build.gradle, index.html, main.java are never turned into links. */
    private static final Set<String> TLDS = new HashSet<>(Arrays.asList(
            "com", "net", "org", "io", "cn", "cc", "co", "me", "ai", "app", "dev", "tv",
            "info", "xyz", "top", "vip", "site", "online", "shop", "cloud", "link", "live",
            "pro", "edu", "gov", "mil", "int", "biz", "name", "mobi", "asia", "ws", "la",
            "ink", "blog", "wiki", "run", "work", "tools", "group", "plus", "fun", "space",
            "store", "host", "press", "email", "social", "team", "design", "art", "chat",
            "news", "tech", "today", "world", "life", "wang", "club", "pub", "cool", "icu",
            "lol", "company", "network", "digital", "studio", "agency", "careers", "capital",
            "finance", "money", "watch", "video", "music", "photo", "pics", "gallery",
            "travel", "hotel", "cafe", "food", "games", "play", "page", "zone", "city"));

    private HtmlLinkifier() {
    }

    /**
     * @param html HTML fragment, may be {@code null}
     * @return the same fragment with plain-text URLs wrapped in {@code <a href="...">}
     */
    public static String linkify(final String html) {
        if (html == null || html.length() < 8) return html;
        final int len = html.length();
        final StringBuilder out = new StringBuilder(len + 64);
        final List<String> stack = new ArrayList<>();
        int i = 0;
        int textStart = 0;
        while (i < len) {
            if (html.charAt(i) != '<') {
                i++;
                continue;
            }
            // comments and CDATA may contain '>' so handle them before the generic tag scan
            if (html.startsWith("<!--", i)) {
                flushText(out, html, textStart, i, isSuppressed(stack));
                int close = html.indexOf("-->", i + 4);
                int end = close < 0 ? len : close + 3;
                out.append(html, i, end);
                i = end;
                textStart = i;
                continue;
            }
            if (html.startsWith("<![CDATA[", i)) {
                flushText(out, html, textStart, i, isSuppressed(stack));
                int close = html.indexOf("]]>", i + 9);
                int end = close < 0 ? len : close + 3;
                out.append(html, i, end);
                i = end;
                textStart = i;
                continue;
            }
            int tagEnd = findTagEnd(html, i);
            if (tagEnd < 0) {
                i++;
                continue;
            }
            flushText(out, html, textStart, i, isSuppressed(stack));
            out.append(html, i, tagEnd + 1);
            updateStack(stack, html.substring(i, tagEnd + 1));
            i = tagEnd + 1;
            textStart = i;
        }
        flushText(out, html, textStart, len, isSuppressed(stack));
        return out.toString();
    }

    /* ---------------------------------------------------------------- text handling */

    private static void flushText(final StringBuilder out, final String html,
                                  final int start, final int end, final boolean suppressed) {
        if (end <= start) return;
        if (suppressed) {
            out.append(html, start, end);
            return;
        }
        appendLinkifiedText(out, html.substring(start, end));
    }

    private static void appendLinkifiedText(final StringBuilder out, final String text) {
        final Matcher urlMatcher = URL_PATTERN.matcher(text);
        final Matcher domainMatcher = DOMAIN_PATTERN.matcher(text);
        int pos = 0;
        while (pos < text.length()) {
            final boolean hasUrl = urlMatcher.find(pos);
            final int urlStart = hasUrl ? urlMatcher.start() : Integer.MAX_VALUE;

            boolean hasDomain = false;
            int domStart = Integer.MAX_VALUE;
            int domEnd = 0;
            int search = pos;
            while (domainMatcher.find(search)) {
                final String tld = domainMatcher.group(2);
                if (tld != null && TLDS.contains(tld.toLowerCase(Locale.ENGLISH))
                        && domainMatcher.group(1).length() > 3) {
                    hasDomain = true;
                    domStart = domainMatcher.start();
                    domEnd = domainMatcher.end();
                    break;
                }
                search = domainMatcher.start() + 1;
            }

            if (!hasUrl && !hasDomain) {
                out.append(text, pos, text.length());
                return;
            }

            final int start;
            final int matchEnd;
            if (urlStart <= domStart) {
                start = urlStart;
                matchEnd = urlMatcher.end();
            } else {
                start = domStart;
                matchEnd = domEnd;
            }

            final String raw = text.substring(start, matchEnd);
            final String url = trimTrailing(raw);
            if (url.length() < 4) {
                out.append(text, pos, Math.min(start + 1, text.length()));
                pos = start + 1;
                continue;
            }

            out.append(text, pos, start);
            final String href = toHref(url);
            out.append("<a href=\"").append(escapeAttribute(href)).append("\" rel=\"nofollow\">")
                    .append(url).append("</a>");
            pos = start + url.length();
        }
    }

    private static String toHref(final String url) {
        final String lower = url.toLowerCase(Locale.ENGLISH);
        if (lower.startsWith("http://") || lower.startsWith("https://")) return url;
        return "https://" + url;
    }

    /** Strips punctuation that belongs to the sentence rather than to the URL. */
    private static String trimTrailing(final String url) {
        int end = url.length();
        boolean changed = true;
        while (changed && end > 0) {
            changed = false;
            // strip a trailing html entity such as &nbsp; or &#39; as a whole
            final char last = url.charAt(end - 1);
            if (last == ';') {
                final int amp = url.lastIndexOf('&', end - 1);
                if (amp >= 0 && end - amp >= 3 && isEntityBody(url, amp + 1, end - 1)) {
                    end = amp;
                    changed = true;
                    continue;
                }
            }
            if (last == '.' || last == ',' || last == ';' || last == ':' || last == '!'
                    || last == '?' || last == '\'' || last == '*' || last == '"') {
                end--;
                changed = true;
                continue;
            }
            if (last == ')') {
                if (countChar(url, end, '(') < countChar(url, end, ')')) {
                    end--;
                    changed = true;
                    continue;
                }
            }
            if (last == ']') {
                if (countChar(url, end, '[') < countChar(url, end, ']')) {
                    end--;
                    changed = true;
                }
            }
        }
        return url.substring(0, end);
    }

    private static int countChar(final String s, final int end, final char c) {
        int count = 0;
        for (int i = 0; i < end; i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }

    private static boolean isEntityBody(final String s, final int start, final int end) {
        if (end <= start) return false;
        if (s.charAt(start) == '#') return end - start > 1;
        for (int i = start; i < end; i++) {
            final char c = s.charAt(i);
            if (!Character.isLetterOrDigit(c)) return false;
        }
        return true;
    }

    /** Escapes a value for an attribute: bare {@code &} becomes {@code &amp;},
     *  existing entities are kept as they are. */
    private static String escapeAttribute(final String value) {
        final StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '&') {
                final int semi = value.indexOf(';', i + 1);
                if (semi > i + 1 && semi - i <= 10 && isEntityBody(value, i + 1, semi)) {
                    sb.append('&');
                } else {
                    sb.append("&amp;");
                }
            } else if (c == '"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /* ---------------------------------------------------------------- tag scanning */

    private static int findTagEnd(final String s, final int start) {
        char quote = 0;
        for (int i = start + 1; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    private static boolean isSuppressed(final List<String> stack) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (SUPPRESSED_TAGS.contains(stack.get(i))) return true;
        }
        return false;
    }

    private static void updateStack(final List<String> stack, final String tag) {
        if (tag.length() < 3) return;
        if (tag.startsWith("<!") || tag.startsWith("<?")) return;
        final boolean closing = tag.charAt(1) == '/';
        int i = closing ? 2 : 1;
        final int start = i;
        while (i < tag.length()
                && (Character.isLetterOrDigit(tag.charAt(i)) || tag.charAt(i) == '-')) {
            i++;
        }
        if (i <= start) return;
        final String name = tag.substring(start, i).toLowerCase(Locale.ENGLISH);
        if (closing) {
            for (int k = stack.size() - 1; k >= 0; k--) {
                if (stack.get(k).equals(name)) {
                    while (stack.size() > k) {
                        stack.remove(stack.size() - 1);
                    }
                    return;
                }
            }
            return;
        }
        if (tag.endsWith("/>") || VOID_TAGS.contains(name)) return;
        stack.add(name);
    }
}
