package com.bark.twitter.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Component
public class TwitterApiClient {

    private final WebClient webClient;

    public TwitterApiClient(@Value("${twitterapi.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.twitterapi.io/twitter")
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    public Optional<JsonNode> getTweet(String tweetId) {
        long start = System.currentTimeMillis();
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tweets")
                            .queryParam("tweet_ids", tweetId)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractFirstFromTweets(response);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][TWEET][" + tweetId + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][TWEET][" + tweetId + "][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][TWEET][" + tweetId + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][TWEET][" + tweetId + "][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUser(String userId) {
        long start = System.currentTimeMillis();
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/user/info")
                            .queryParam("userId", userId)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractUserData(response);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][USER][" + userId + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][USER][" + userId + "][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][USER][" + userId + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][USER][" + userId + "][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUserByUsername(String username) {
        long start = System.currentTimeMillis();
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/user/info")
                            .queryParam("userName", username)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractUserData(response);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][USER][@" + username + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][USER][@" + username + "][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][USER][@" + username + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][USER][@" + username + "][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getCommunity(String communityId) {
        long start = System.currentTimeMillis();
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/community/info")
                            .queryParam("community_id", communityId)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractCommunityInfo(response);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][COMMUNITY][" + communityId + "][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][COMMUNITY][" + communityId + "][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][ERROR][TWITTERAPI][COMMUNITY][" + communityId + "][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<JsonNode> extractFirstFromTweets(JsonNode response) {
        if (response == null) {
            return Optional.empty();
        }

        JsonNode tweets = response.get("tweets");
        if (tweets == null || !tweets.isArray() || tweets.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(tweets.get(0));
    }

    private Optional<JsonNode> extractUserData(JsonNode response) {
        if (response == null) {
            return Optional.empty();
        }

        JsonNode data = response.get("data");
        if (data == null || data.isNull()) {
            return Optional.empty();
        }

        // Check if user was found (id is not null)
        JsonNode idNode = data.get("id");
        if (idNode == null || idNode.isNull()) {
            return Optional.empty();
        }

        return Optional.of(data);
    }

    private Optional<JsonNode> extractCommunityInfo(JsonNode response) {
        if (response == null) {
            return Optional.empty();
        }

        JsonNode communityInfo = response.get("community_info");
        if (communityInfo == null || communityInfo.isNull()) {
            return Optional.empty();
        }

        // Check if community was found (id is not null)
        JsonNode idNode = communityInfo.get("id");
        if (idNode == null || idNode.isNull()) {
            return Optional.empty();
        }

        return Optional.of(communityInfo);
    }
}
