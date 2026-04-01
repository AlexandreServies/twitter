package com.bark.twitter.config;

/**
 * Thread-local context for storing the current request's API key.
 * This allows any code in the request chain to access the API key
 * for logging purposes without passing it through every method.
 */
public class ApiKeyContext {

    private static final ThreadLocal<String> apiKey = new ThreadLocal<>();
    private static final ThreadLocal<String> ipAddress = new ThreadLocal<>();

    public static void set(String key) {
        apiKey.set(key);
    }

    public static void setIpAddress(String ip) {
        ipAddress.set(ip);
    }

    public static String get() {
        return apiKey.get();
    }

    public static String getIpAddress() {
        return ipAddress.get();
    }

    public static void clear() {
        apiKey.remove();
        ipAddress.remove();
    }

    /**
     * Returns first 4 characters of API key + IP address for logging, or "????" if not set.
     */
    public static String getLogPrefix() {
        String key = apiKey.get();
        String keyPrefix = (key == null || key.length() < 4) ? "????" : key.substring(0, 4);
        String ip = ipAddress.get();
        return ip != null ? keyPrefix + "|" + ip : keyPrefix;
    }
}
