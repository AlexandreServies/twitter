package com.bark.twitter.service;

import com.bark.twitter.dto.axion.AxionExtendedEntitiesDto;
import com.bark.twitter.dto.axion.AxionMediaDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Warms CloudFront cache for Twitter video URLs before returning responses.
 * This ensures cached responses are available when clients request the videos.
 * All operations are async and fire-and-forget with zero latency impact.
 * Each URL is only warmed once (tracked in memory).
 */
@Service
public class VideoCacheWarmingService {

    private static final String PROXY_HOST = "twproxy.twproxy.workers.dev";
    private static final int MAX_TRACKED_URLS = 10000;

    private final HttpClient httpClient;
    private final Executor executor;
    private final Set<String> warmedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public VideoCacheWarmingService() {
        this.executor = Executors.newCachedThreadPool();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(executor)
                .build();
    }

    /**
     * Warms cache for all video URLs in a tweet (including nested tweets).
     * Fire-and-forget - never blocks, never throws.
     */
    public void warmCacheAsync(AxionTweetDto tweet) {
        if (tweet == null) {
            return;
        }
        executor.execute(() -> {
            try {
                List<String> videoUrls = extractProxyVideoUrls(tweet);
                for (String url : videoUrls) {
                    warmSingleUrl(url);
                }
            } catch (Exception e) {
                // Silently ignore - cache warming is best-effort
            }
        });
    }

    private List<String> extractProxyVideoUrls(AxionTweetDto tweet) {
        List<String> urls = new ArrayList<>();
        extractFromTweet(tweet, urls);

        // Also check nested tweets
        if (tweet.quotedTweet() != null) {
            extractFromTweet(tweet.quotedTweet(), urls);
        }
        if (tweet.retweetedTweet() != null) {
            extractFromTweet(tweet.retweetedTweet(), urls);
        }
        if (tweet.replyTweet() != null) {
            extractFromTweet(tweet.replyTweet(), urls);
        }

        return urls;
    }

    private void extractFromTweet(AxionTweetDto tweet, List<String> urls) {
        AxionExtendedEntitiesDto extendedEntities = tweet.extendedEntities();
        if (extendedEntities == null || extendedEntities.media() == null) {
            return;
        }

        for (AxionMediaDto media : extendedEntities.media()) {
            String videoUrl = media.videoUrl();
            if (videoUrl != null && videoUrl.contains(PROXY_HOST)) {
                urls.add(videoUrl);
            }
        }
    }

    private void warmSingleUrl(String url) {
        // Skip if already warmed
        if (!warmedUrls.add(url)) {
            return;
        }

        // Prevent unbounded memory growth
        if (warmedUrls.size() > MAX_TRACKED_URLS) {
            warmedUrls.clear();
        }

        try {
            // Use GET to ensure CloudFront caches the full response body
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response ->
                        System.out.println("[" + System.currentTimeMillis() + "][CACHE_WARM] Success: " + url + " (status=" + response.statusCode() + ")"))
                    .exceptionally(e -> {
                        System.err.println("[" + System.currentTimeMillis() + "][CACHE_WARM] Failed: " + url + " - " + e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            System.err.println("[" + System.currentTimeMillis() + "][CACHE_WARM] Error: " + url + " - " + e.getMessage());
        }
    }
}
