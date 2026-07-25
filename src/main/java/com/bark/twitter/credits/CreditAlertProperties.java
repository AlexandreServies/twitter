package com.bark.twitter.credits;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for the low-credit-balance Pushover alert.
 * <p>
 * Bound from {@code credits.low-balance-alert} in {@code application.yml}.
 */
@Configuration
@ConfigurationProperties(prefix = "credits.low-balance-alert")
public class CreditAlertProperties {

    /** Alert when a monitored key's balance drops below this many credits. */
    private long threshold = 1_000_000;

    /** Label -&gt; API key for the keys to monitor (e.g. {@code AXIOM}, {@code AXIOM_PULSE}). */
    private Map<String, String> keys = new LinkedHashMap<>();

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }
}
