package com.bark.twitter.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
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
            System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][SYNOPTIC][TWEET][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][SYNOPTIC][TWEET][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][ERROR][SYNOPTIC][TWEET][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][ERROR][SYNOPTIC][TWEET][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUsersById(List<String> userIds) {
        long start = System.currentTimeMillis();
        String commaSeparatedUserIds = userIds.stream().reduce((a, b) -> a + "," + b).orElse("");
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("user_ids", commaSeparatedUserIds)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractData(response);
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + commaSeparatedUserIds + "][SYNOPTIC][USER][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + commaSeparatedUserIds + "][SYNOPTIC][USER][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + commaSeparatedUserIds + "][ERROR][SYNOPTIC][USER][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + commaSeparatedUserIds + "][ERROR][SYNOPTIC][USER][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> getUserById(String userId) {
        Optional<JsonNode> usersById = getUsersById(List.of(userId));
        return usersById.flatMap(usersNode -> {
            if (usersNode.isArray() && !usersNode.isEmpty()) {
                return Optional.of(usersNode.get(0));
            } else {
                return Optional.empty();
            }
        });
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
            System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][SYNOPTIC][USER][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][SYNOPTIC][USER][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][ERROR][SYNOPTIC][USER][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][ERROR][SYNOPTIC][USER][" + elapsed + "ms] " + e.getMessage());
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
            System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][SYNOPTIC][COMMUNITY][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            return result;
        } catch (WebClientResponseException.NotFound e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][SYNOPTIC][COMMUNITY][" + elapsed + "ms] Not found");
            return Optional.empty();
        } catch (WebClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][ERROR][SYNOPTIC][COMMUNITY][" + elapsed + "ms] " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][ERROR][SYNOPTIC][COMMUNITY][" + elapsed + "ms] " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetches users by IDs without logging (for batch operations).
     * Returns the data array directly.
     */
    public Optional<JsonNode> getUsersByIdSilent(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Optional.empty();
        }
        String commaSeparatedUserIds = String.join(",", userIds);
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("user_ids", commaSeparatedUserIds)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return extractData(response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Fetches a user by username without logging (for batch operations).
     */
    public Optional<JsonNode> getUserByUsernameSilent(String username) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("screen_name", username)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return extractFirstFromData(response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Fetches a user by username for batch operations.
     * Returns a result that distinguishes between found, not-found, and error.
     */
    public JsonLookupResult fetchUserByUsername(String username) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("screen_name", username)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> userNode = extractFirstFromData(response);
            if (userNode.isPresent()) {
                return JsonLookupResult.found(userNode.get());
            }
            return JsonLookupResult.notFound();
        } catch (Exception e) {
            return JsonLookupResult.error();
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
