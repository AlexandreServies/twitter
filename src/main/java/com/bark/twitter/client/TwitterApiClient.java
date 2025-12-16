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

    public Optional<JsonNode> getCommunity(String communityId) {
        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/community/info")
                            .queryParam("id", communityId)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            Optional<JsonNode> result = extractCommunityInfo(response);
            System.out.println("[" + System.currentTimeMillis() + "][TWITTERAPI][" + communityId + "] " + result.orElse(null));
            return result;
        } catch (WebClientResponseException e) {
            System.out.println("[ERROR] Error fetching community " + communityId + ": " + e.getStatusCode() + " " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            System.out.println("[ERROR] Error fetching community " + communityId + ": " + e.getMessage());
            return Optional.empty();
        }
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
