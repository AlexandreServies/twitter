package com.bark.twitter.util;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Utility class for HTML-related string operations.
 */
public final class HtmlUtils {

    private HtmlUtils() {
        // Utility class
    }

    /**
     * Decodes HTML entities in text (e.g., &amp; -> &, &lt; -> <, &gt; -> >, &quot; -> ", &#39; -> ')
     * Returns null if input is null, empty string if input is empty.
     *
     * @param text The text potentially containing HTML entities
     * @return The text with HTML entities decoded
     */
    public static String decodeHtmlEntities(String text) {
        if (text == null) {
            return null;
        }
        if (text.isEmpty()) {
            return text;
        }
        return StringEscapeUtils.unescapeHtml4(text);
    }
}
