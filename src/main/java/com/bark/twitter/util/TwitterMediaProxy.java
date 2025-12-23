package com.bark.twitter.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility for proxying Twitter video URLs through CloudFront.
 * Twitter blocks direct embedding of video.twimg.com URLs with 403 errors.
 * This proxy adds the required Referer header to make videos accessible.
 */
public class TwitterMediaProxy {

    private static final String PROXY_BASE_URL = "https://d7nmnehv38vh6.cloudfront.net/";

    private static final String VIDEO_HOST = "video.twimg.com";

    /**
     * Wraps a Twitter video URL with the proxy if needed.
     * Only proxies video.twimg.com URLs; other URLs are returned unchanged.
     *
     * @param url The original URL
     * @return The proxied URL if applicable, otherwise the original URL
     */
    public static String proxyVideoUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        if (!url.contains(VIDEO_HOST)) {
            return url;
        }

        return PROXY_BASE_URL + "?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
    }
}
