package com.bark.twitter.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility for proxying Twitter video URLs through CloudFront.
 * Twitter blocks direct embedding of video.twimg.com URLs with 403 errors.
 * This proxy adds the required Referer header to make videos accessible.
 */
public class TwitterMediaProxy {

    private static final String PROXY_BASE_URL = "https://twproxy.twproxy.workers.dev/";

    /**
     * Wraps a Twitter video URL with the proxy if needed.
     * Proxies video.twimg.com and video-s.twimg.com URLs; other URLs are returned unchanged.
     *
     * @param url The original URL
     * @return The proxied URL if applicable, otherwise the original URL
     */
    public static String proxyVideoUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        if (!needsProxy(url)) {
            return url;
        }

        return PROXY_BASE_URL + "?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    private static boolean needsProxy(String url) {
        return url.contains("video.twimg.com") || url.contains("video-s.twimg.com");
    }
}
