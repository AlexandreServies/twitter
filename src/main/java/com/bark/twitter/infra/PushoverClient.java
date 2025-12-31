package com.bark.twitter.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Simple Pushover client for sending alert notifications.
 */
@Component
public class PushoverClient {

    private static final String PUSHOVER_URL = "https://api.pushover.net/1/messages.json";

    private final WebClient webClient;
    private final String apiKey;
    private final String userKey;
    private final boolean enabled;

    public PushoverClient(
            @Value("${pushover.api-key:}") String apiKey,
            @Value("${pushover.user-key:}") String userKey) {
        this.webClient = WebClient.builder().build();
        this.apiKey = apiKey;
        this.userKey = userKey;
        this.enabled = !apiKey.isEmpty() && !userKey.isEmpty();

        if (enabled) {
            System.out.println("[" + System.currentTimeMillis() + "][PUSHOVER] Client initialized");
        } else {
            System.out.println("[" + System.currentTimeMillis() + "][PUSHOVER] Client disabled (missing api-key or user-key)");
        }
    }

    /**
     * Sends a high-priority notification.
     *
     * @param title   The notification title
     * @param message The notification message
     */
    public void sendHighPriority(String title, String message) {
        send(title, message, 1);
    }

    /**
     * Sends a notification with the specified priority.
     *
     * @param title    The notification title
     * @param message  The notification message
     * @param priority 0 = normal, 1 = high
     */
    public void send(String title, String message, int priority) {
        if (!enabled) {
            System.out.println("[" + System.currentTimeMillis() + "][PUSHOVER] Notification skipped (disabled): " + title);
            return;
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("token", apiKey);
            formData.add("user", userKey);
            formData.add("title", title);
            formData.add("message", message);
            formData.add("priority", String.valueOf(priority));

            webClient.post()
                    .uri(PUSHOVER_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> System.out.println("[" + System.currentTimeMillis() + "][PUSHOVER] Notification sent: " + title),
                            error -> System.err.println("[" + System.currentTimeMillis() + "][PUSHOVER] Failed to send notification: " + error.getMessage())
                    );
        } catch (Exception e) {
            System.err.println("[" + System.currentTimeMillis() + "][PUSHOVER] Error sending notification: " + e.getMessage());
        }
    }
}
