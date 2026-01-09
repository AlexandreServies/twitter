package com.bark.twitter.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;

@Component
public class SynopticClient {

    private final WebClient webClient;
    private final RateLimiter tweetRateLimiter;
    private final RateLimiter usersByIdRateLimiter;
    private final RateLimiter userByUsernameRateLimiter;
    private final RateLimiter communityRateLimiter;

    public SynopticClient(WebClient synopticWebClient,
                          @Qualifier("synopticTweetRateLimiter") RateLimiter tweetRateLimiter,
                          @Qualifier("synopticUsersByIdRateLimiter") RateLimiter usersByIdRateLimiter,
                          @Qualifier("synopticUserByUsernameRateLimiter") RateLimiter userByUsernameRateLimiter,
                          @Qualifier("synopticCommunityRateLimiter") RateLimiter communityRateLimiter) {
        this.webClient = synopticWebClient;
        this.tweetRateLimiter = tweetRateLimiter;
        this.usersByIdRateLimiter = usersByIdRateLimiter;
        this.userByUsernameRateLimiter = userByUsernameRateLimiter;
        this.communityRateLimiter = communityRateLimiter;
    }

    /**
     * Fetches a tweet by ID.
     * @param tweetId the tweet ID
     * @param silent if true, suppresses logging (for batch operations)
     */
    public JsonLookupResult getTweet(String tweetId, boolean silent) {
        RateLimiter.waitForPermission(tweetRateLimiter);
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
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][SYNOPTIC][TWEET][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            }
            return result.map(JsonLookupResult::found).orElse(JsonLookupResult.notFound());
        } catch (WebClientResponseException.NotFound e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][SYNOPTIC][TWEET][" + elapsed + "ms] Not found");
            }
            return JsonLookupResult.notFound();
        } catch (Exception e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + tweetId + "][ERROR][SYNOPTIC][TWEET][" + elapsed + "ms] " + e.getMessage());
            }
            return JsonLookupResult.error();
        }
    }

    /**
     * Fetches a tweet by ID with logging enabled.
     */
    public Optional<JsonNode> getTweet(String tweetId) {
        return getTweet(tweetId, false).toOptional();
    }

    /**
     * Fetches users by IDs.
     * @param userIds list of user IDs
     * @param silent if true, suppresses logging (for batch operations)
     * @return JsonLookupResult containing the data array (not individual users)
     */
    public JsonLookupResult getUsersById(List<String> userIds, boolean silent) {
        if (userIds == null || userIds.isEmpty()) {
            return JsonLookupResult.notFound();
        }
        RateLimiter.waitForPermission(usersByIdRateLimiter);
        long start = System.currentTimeMillis();
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

            Optional<JsonNode> result = extractData(response);
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + commaSeparatedUserIds + "][SYNOPTIC][USER][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            }
            return result.map(JsonLookupResult::found).orElse(JsonLookupResult.notFound());
        } catch (WebClientResponseException.NotFound e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + commaSeparatedUserIds + "][SYNOPTIC][USER][" + elapsed + "ms] Not found");
            }
            return JsonLookupResult.notFound();
        } catch (Exception e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + commaSeparatedUserIds + "][ERROR][SYNOPTIC][USER][" + elapsed + "ms] " + e.getMessage());
            }
            return JsonLookupResult.error();
        }
    }

    /**
     * Fetches users by IDs with logging enabled.
     */
    public Optional<JsonNode> getUsersById(List<String> userIds) {
        return getUsersById(userIds, false).toOptional();
    }

    /**
     * Fetches a single user by ID with logging enabled.
     */
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

    /**
     * Fetches a user by username.
     * @param username the username (without @)
     * @param silent if true, suppresses logging (for batch operations)
     */
    public JsonLookupResult getUserByUsername(String username, boolean silent) {
        RateLimiter.waitForPermission(userByUsernameRateLimiter);
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
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][SYNOPTIC][USER][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            }
            return result.map(JsonLookupResult::found).orElse(JsonLookupResult.notFound());
        } catch (WebClientResponseException.NotFound e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][SYNOPTIC][USER][" + elapsed + "ms] Not found");
            }
            return JsonLookupResult.notFound();
        } catch (Exception e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][@" + username + "][ERROR][SYNOPTIC][USER][" + elapsed + "ms] " + e.getMessage());
            }
            return JsonLookupResult.error();
        }
    }

    /**
     * Fetches a user by username with logging enabled.
     */
    public Optional<JsonNode> getUserByUsername(String username) {
        return getUserByUsername(username, false).toOptional();
    }

    /**
     * Fetches a community by ID.
     * @param communityId the community ID
     * @param silent if true, suppresses logging (for batch operations)
     */
    public JsonLookupResult getCommunity(String communityId, boolean silent) {
        RateLimiter.waitForPermission(communityRateLimiter);
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
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][SYNOPTIC][COMMUNITY][" + elapsed + "ms] " + (result.isPresent() ? result.get() : "Not found"));
            }
            return result.map(JsonLookupResult::found).orElse(JsonLookupResult.notFound());
        } catch (WebClientResponseException.NotFound e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][SYNOPTIC][COMMUNITY][" + elapsed + "ms] Not found");
            }
            return JsonLookupResult.notFound();
        } catch (Exception e) {
            if (!silent) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("[" + System.currentTimeMillis() + "][" + communityId + "][ERROR][SYNOPTIC][COMMUNITY][" + elapsed + "ms] " + e.getMessage());
            }
            return JsonLookupResult.error();
        }
    }

    /**
     * Fetches a community by ID with logging enabled.
     */
    public Optional<JsonNode> getCommunity(String communityId) {
        return getCommunity(communityId, false).toOptional();
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
