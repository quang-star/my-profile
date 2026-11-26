package dev.quang.portfolio.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class HtmlSanitizerService {

    private static final String BASE_URI = "https://portfolio.invalid/";

    private static final Safelist RICH_SAFELIST = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "strike", "code", "span", "mark", "a", "ul", "ol", "li")
            .addAttributes("a", "href", "target", "rel")
            .addAttributes(":all", "style", "class", "data-color", "data-tag")
            .addProtocols("a", "href", "http", "https", "mailto", "tel")
            .preserveRelativeLinks(true);

    private static final Set<String> ALLOWED_TAG_COLORS = Set.of(
            "blue", "emerald", "indigo", "amber", "sky", "rose", "slate"
    );

    private static final Set<String> BASIC_COLOR_KEYWORDS = Set.of(
            "transparent", "inherit", "currentcolor",
            "black", "white", "red", "green", "blue", "yellow", "orange", "purple", "pink",
            "gray", "grey", "silver", "maroon", "olive", "lime", "aqua", "cyan", "teal",
            "navy", "fuchsia", "magenta", "brown", "gold", "coral", "indigo", "violet",
            "crimson", "salmon", "khaki", "plum", "orchid", "tan", "beige", "ivory",
            "linen", "wheat", "azure", "lavender", "turquoise"
    );

    private static final Pattern HEX_PATTERN = Pattern.compile("^#(?:[0-9a-f]{3}|[0-9a-f]{4}|[0-9a-f]{6}|[0-9a-f]{8})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RGB_PATTERN = Pattern.compile("^rgb\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RGBA_PATTERN = Pattern.compile("^rgba\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(0|1|0?\\.\\d+|1\\.0+)\\s*\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAFE_HREF_PATTERN = Pattern.compile("^(https?://|mailto:|tel:|#|/(?!/))", Pattern.CASE_INSENSITIVE);

    public String sanitize(String rawHtml) {
        if (rawHtml == null || rawHtml.isEmpty()) {
            return "";
        }

        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(rawHtml, BASE_URI, RICH_SAFELIST, outputSettings);
        Document doc = Jsoup.parseBodyFragment(cleaned, BASE_URI);
        doc.outputSettings(outputSettings);

        for (Element el : doc.body().getAllElements()) {
            if (el == doc.body()) {
                continue;
            }

            // 1. Style: keep ONLY safe color & background-color
            if (el.hasAttr("style")) {
                String safeStyle = sanitizeStyle(el.attr("style"));
                if (safeStyle != null) {
                    el.attr("style", safeStyle);
                } else {
                    el.removeAttr("style");
                }
            }

            // 2. Class: allow ONLY inline-tag
            if (el.hasAttr("class")) {
                String[] parts = el.attr("class").trim().split("\\s+");
                boolean hasInlineTag = false;
                for (String part : parts) {
                    if ("inline-tag".equals(part)) {
                        hasInlineTag = true;
                        break;
                    }
                }
                if (hasInlineTag) {
                    el.attr("class", "inline-tag");
                } else {
                    el.removeAttr("class");
                }
            }

            // 3. data-color: allow ONLY 6 safe values
            if (el.hasAttr("data-color")) {
                String color = el.attr("data-color").trim();
                if (!ALLOWED_TAG_COLORS.contains(color)) {
                    el.removeAttr("data-color");
                }
            }

            // 4. Anchor attributes
            if ("a".equals(el.tagName())) {
                String href = el.attr("href").trim();
                if (!isSafeHref(href)) {
                    el.removeAttr("href");
                } else {
                    el.attr("href", href);
                }
                String target = el.attr("target").trim();
                if ("_blank".equals(target)) {
                    el.attr("rel", "noopener noreferrer");
                } else if ("_self".equals(target)) {
                    el.removeAttr("rel");
                } else {
                    el.removeAttr("target");
                    el.removeAttr("rel");
                }
            } else {
                el.removeAttr("href");
                el.removeAttr("target");
                el.removeAttr("rel");
            }
        }

        return doc.body().html();
    }

    private String sanitizeStyle(String rawStyle) {
        Map<String, String> declarations = new LinkedHashMap<>();
        for (String part : rawStyle.split(";")) {
            int colonIdx = part.indexOf(':');
            if (colonIdx <= 0) continue;
            String prop = part.substring(0, colonIdx).trim().toLowerCase(java.util.Locale.ROOT);
            String val = part.substring(colonIdx + 1).trim();
            if (("color".equals(prop) || "background-color".equals(prop)) && isSafeColorValue(val)) {
                declarations.put(prop, val);
            }
        }
        if (declarations.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        declarations.forEach((prop, val) -> {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(prop).append(": ").append(val);
        });
        return sb.toString();
    }

    private boolean isSafeColorValue(String rawValue) {
        String val = rawValue.trim().toLowerCase(java.util.Locale.ROOT);
        if (val.isEmpty()) return false;
        if (BASIC_COLOR_KEYWORDS.contains(val)) return true;
        if (HEX_PATTERN.matcher(val).matches()) return true;

        Matcher rgb = RGB_PATTERN.matcher(val);
        if (rgb.matches()) {
            return Integer.parseInt(rgb.group(1)) <= 255
                    && Integer.parseInt(rgb.group(2)) <= 255
                    && Integer.parseInt(rgb.group(3)) <= 255;
        }

        Matcher rgba = RGBA_PATTERN.matcher(val);
        if (rgba.matches()) {
            return Integer.parseInt(rgba.group(1)) <= 255
                    && Integer.parseInt(rgba.group(2)) <= 255
                    && Integer.parseInt(rgba.group(3)) <= 255;
        }

        return false;
    }

    private boolean isSafeHref(String href) {
        if (href.isEmpty()) return false;
        for (int i = 0; i < href.length(); i++) {
            char c = href.charAt(i);
            if (c < 0x20 || c == 0x7F) return false;
        }
        return SAFE_HREF_PATTERN.matcher(href).find();
    }
}
