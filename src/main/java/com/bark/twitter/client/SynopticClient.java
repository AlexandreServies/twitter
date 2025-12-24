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
        long start = System.currentTimeMillis();
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
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][TWEET][" + tweetId + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][TWEET][" + tweetId + "][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][TWEET][" + tweetId + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][TWEET][" + tweetId + "][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUser(String userId) {
        long start = System.currentTimeMillis();
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
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][USER][" + userId + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][USER][" + userId + "][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][USER][" + userId + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][USER][" + userId + "][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUserByUsername(String username) {
        long start = System.currentTimeMillis();
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
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][USER][@" + username + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][USER][@" + username + "][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][USER][@" + username + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][USER][@" + username + "][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getCommunity(String communityId) {
        long start = System.currentTimeMillis();
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/communities/" + communityId)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractData(response);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][COMMUNITY][" + communityId + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[SYNOPTIC][COMMUNITY][" + communityId + "][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][COMMUNITY][" + communityId + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[ERROR][SYNOPTIC][COMMUNITY][" + communityId + "][" + elapsed + "ms] " + e.getMessage());
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

    private Optional<JsonNode> extractData(JsonNode response) {
        if (response == null) {
            return Optional.empty();
        }

        JsonNode data = response.get("data");
        if (data == null || data.isNull()) {
            return Optional.empty();
        }

        return Optional.of(data);
    }
}
