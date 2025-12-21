package com.bark.twitter.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Component
public class SynopticClient {

    private final WebClient webClient;

    public SynopticClient(WebClient synopticWebClient) {
        this.webClient = synopticWebClient;
    }

    public Optional<JsonNode> getTweet(String tweetId) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tweets/lookup")
                            .queryParam("tweet_ids", tweetId)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractFirstFromData(response);
            System.out.println("[" + System.currentTimeMillis() + "][SYNOPTIC][" + tweetId + "] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException e) {
            System.out.println("[ERROR] Error fetching tweet " + tweetId + ": " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            System.out.println("[ERROR] Error fetching tweet " + tweetId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUser(String userId) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("user_ids", userId)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractFirstFromData(response);
            System.out.println("[" + System.currentTimeMillis() + "][SYNOPTIC][" + userId + "] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException e) {
            System.out.println("[ERROR] Error fetching user " + userId + ": " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            System.out.println("[ERROR] Error fetching user " + userId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUserByUsername(String username) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("screen_name", username)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractFirstFromData(response);
            System.out.println("[" + System.currentTimeMillis() + "][SYNOPTIC][@" + username + "] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException e) {
            System.out.println("[ERROR] Error fetching user @" + username + ": " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            System.out.println("[ERROR] Error fetching user @" + username + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<JsonNode> extractFirstFromData(JsonNode response) {
        if (response == null) {
            return Optional.empty();
        }

        JsonNode data = response.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(data.get(0));
    }
}
