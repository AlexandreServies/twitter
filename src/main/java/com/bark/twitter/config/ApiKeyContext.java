package com.bark.twitter.config;

/**
 * Thread-local context for storing the current request's API key.
 * This allows any code in the request chain to access the API key
 * for logging purposes without passing it through every method.
 */
public class ApiKeyContext {

    private static final ThreadLocal<String> apiKey = new ThreadLocal<>();

    public static void set(String key) {
        apiKey.set(key);
    }

    public static String get() {
        return apiKey.get();
    }

    public static void clear() {
        apiKey.remove();
    }

    /**
     * Returns first 4 characters of API key for logging, or "????" if not set.
     */
    public static String getLogPrefix() {
        String key = apiKey.get();
        if (key == null || key.length() < 4) {
            return "????";
        }
        return key.substring(0, 4);
    }
}
