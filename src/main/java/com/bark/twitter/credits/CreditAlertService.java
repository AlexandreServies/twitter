package com.bark.twitter.credits;

import com.bark.twitter.infra.PushoverClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically checks the credit balance of the monitored API keys (the Axiom keys) and sends a
 * Pushover alert when one drops below the configured threshold.
 *
 * <p>The alert is <b>edge-triggered</b> and de-duplicated in-memory <b>per instance</b>: each key
 * fires at most once per crossing below the threshold, and only re-arms once its balance recovers
 * back above the threshold (e.g. after a top-up). State is per-instance and not shared via DynamoDB,
 * so with N running instances a single low-balance event can produce up to N pushes — intentionally,
 * so the alert still fires if any one instance is down.
 */
@Service
public class CreditAlertService {

    private static final long CHECK_INTERVAL_MS = 60_000;

    private final CreditService creditService;
    private final PushoverClient pushoverClient;
    private final CreditAlertProperties properties;

    /** Keys already alerted on this instance; cleared when a key's balance recovers above threshold. */
    private final Set<String> alerted = ConcurrentHashMap.newKeySet();

    public CreditAlertService(CreditService creditService,
                              PushoverClient pushoverClient,
                              CreditAlertProperties properties) {
        this.creditService = creditService;
        this.pushoverClient = pushoverClient;
        this.properties = properties;
    }

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void checkLowBalances() {
        long threshold = properties.getThreshold();
        for (Map.Entry<String, String> entry : properties.getKeys().entrySet()) {
            String label = entry.getKey();
            String apiKey = entry.getValue();
            if (apiKey == null || apiKey.isBlank()) {
                continue;
            }

            long credits;
            try {
                credits = creditService.getCredits(apiKey);
            } catch (Exception e) {
                System.err.println("[" + System.currentTimeMillis() + "][CREDIT_ALERT] Failed to read credits for "
                        + label + ": " + e.getMessage());
                continue;
            }

            if (credits < threshold) {
                // add() returns true only the first time -> at most one alert per instance per crossing.
                if (alerted.add(apiKey)) {
                    String title = "⚠️ Twitter credits low: " + label;
                    String message = label + " has " + String.format("%,d", credits)
                            + " credits remaining (below " + String.format("%,d", threshold)
                            + "). Top up via POST /credits.";
                    pushoverClient.sendHighPriority(title, message);
                    System.out.println("[" + System.currentTimeMillis() + "][CREDIT_ALERT] Low-balance alert sent for "
                            + label + " (" + credits + " remaining)");
                }
            } else if (alerted.remove(apiKey)) {
                // Recovered above threshold -> re-arm so the next drop alerts again.
                System.out.println("[" + System.currentTimeMillis() + "][CREDIT_ALERT] " + label
                        + " recovered above threshold (" + credits + " remaining) — re-armed");
            }
        }
    }
}
