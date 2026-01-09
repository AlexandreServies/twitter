package com.bark.twitter.provider;

import com.bark.twitter.cache.UsernameCacheService;
import com.bark.twitter.client.JsonLookupResult;
import com.bark.twitter.client.SynopticClient;
import com.bark.twitter.dto.BatchCommunityMemberCountResult;
import com.bark.twitter.dto.BatchUserResult;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.NotFoundException;
import com.bark.twitter.mapper.SynopticToAxiomMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Twitter data provider that uses Synoptic API as the data source.
 */
@Component
public class SynopticDataProvider implements TwitterDataProvider {

    private final SynopticClient synopticClient;
    private final SynopticToAxiomMapper axiomMapper;
    private final UsernameCacheService usernameCacheService;
    private final ExecutorService batchExecutor;

    public SynopticDataProvider(SynopticClient synopticClient,
                                SynopticToAxiomMapper axiomMapper,
                                UsernameCacheService usernameCacheService,
                                @Qualifier("synopticBatchExecutor") ExecutorService batchExecutor) {
        this.synopticClient = synopticClient;
        this.axiomMapper = axiomMapper;
        this.usernameCacheService = usernameCacheService;
        this.batchExecutor = batchExecutor;
    }

    @Override
    public AxionTweetDto getTweet(String tweetId) {
        JsonNode synopticTweet = synopticClient.getTweet(tweetId)
                .orElseThrow(() -> new NotFoundException("Tweet not found: " + tweetId));

        // Cache author username -> userId mapping
        usernameCacheService.cacheFromSynopticUser(synopticTweet);
        // Also cache from user_profile if present
        JsonNode userProfile = synopticTweet.path("user_profile");
        if (!userProfile.isMissingNode() && !userProfile.isNull()) {
            usernameCacheService.cacheFromSynopticUser(userProfile);
        }

        JsonNode transformed = transformMedia(synopticTweet);
        JsonNode enriched = enrichReplyData(transformed);

        return axiomMapper.mapTweet(enriched);
    }

    @Override
    public AxionUserInfoDto getUser(String userId) {
        JsonNode synopticUser = synopticClient.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        // Cache username -> userId mapping
        usernameCacheService.cacheFromSynopticUser(synopticUser);

        return axiomMapper.mapUser(synopticUser);
    }

    @Override
    public AxionUserInfoDto getUserByUsername(String username) {
        JsonNode synopticUser = synopticClient.getUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: @" + username));

        // Cache username -> userId mapping
        usernameCacheService.cacheFromSynopticUser(synopticUser);

        return axiomMapper.mapUser(synopticUser);
    }

    @Override
    public AxionCommunityDto getCommunity(String communityId) {
        // First call: get community data
        JsonNode communityData = synopticClient.getCommunity(communityId)
                .orElseThrow(() -> new NotFoundException("Community not found: " + communityId));

        // Extract creator user ID from community data and cache it
        JsonNode creatorNode = communityData.get("creator");
        String creatorUserId = null;
        if (creatorNode != null && !creatorNode.isNull()) {
            // Cache creator username -> userId mapping
            usernameCacheService.cacheFromSynopticUser(creatorNode);

            JsonNode userIdNode = creatorNode.get("user_id");
            if (userIdNode != null && !userIdNode.isNull()) {
                creatorUserId = userIdNode.asText();
            }
        }

        // Second call: get creator user data
        JsonNode creatorData = null;
        if (creatorUserId != null) {
            creatorData = synopticClient.getUserById(creatorUserId).orElse(null);
            if (creatorData != null) {
                // Cache creator full user data
                usernameCacheService.cacheFromSynopticUser(creatorData);
            }
        }

        return axiomMapper.mapCommunity(communityData, creatorData);
    }

    @Override
    public String getProviderName() {
        return "SYNOPTIC";
    }

    @Override
    public BatchUserResult getUsersByIds(List<String> userIds) {
        BatchUserResult result = new BatchUserResult();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        // Split into balanced chunks (max 100 per call)
        List<List<String>> chunks = partitionBalanced(userIds, 100);

        // Execute all chunks in parallel (rate limiting handled by SynopticClient)
        List<CompletableFuture<BatchUserResult>> futures = new ArrayList<>();
        for (List<String> chunk : chunks) {
            futures.add(CompletableFuture.supplyAsync(() -> fetchUsersByIdChunk(chunk), batchExecutor));
        }

        // Wait for all and merge results
        for (CompletableFuture<BatchUserResult> future : futures) {
            try {
                result.merge(future.join());
            } catch (Exception e) {
                // Mark all IDs in failed chunks as errors (we don't know which chunk failed)
            }
        }
        return result;
    }

    @Override
    public BatchUserResult getUsersByUsernames(List<String> usernames) {
        BatchUserResult result = new BatchUserResult();
        if (usernames == null || usernames.isEmpty()) {
            return result;
        }

        // Execute all username lookups in parallel (rate limiting handled by SynopticClient)
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String username : usernames) {
            futures.add(CompletableFuture.runAsync(() -> {
                JsonLookupResult lookupResult = synopticClient.getUserByUsername(username, true);

                synchronized (result) {
                    if (lookupResult.isFound()) {
                        // Cache username -> userId mapping
                        usernameCacheService.cacheFromSynopticUser(lookupResult.data());
                        AxionUserInfoDto user = axiomMapper.mapUser(lookupResult.data());
                        result.addFound(username, user);
                    } else if (lookupResult.isNotFound()) {
                        result.addNotFound(username);
                    } else {
                        result.addError(username);
                    }
                }
            }, batchExecutor));
        }

        // Wait for all to complete
        for (CompletableFuture<Void> future : futures) {
            try {
                future.join();
            } catch (Exception e) {
                // Individual failures already tracked in the async block
            }
        }
        return result;
    }

    @Override
    public BatchCommunityMemberCountResult getCommunityMemberCounts(List<String> communityIds) {
        BatchCommunityMemberCountResult result = new BatchCommunityMemberCountResult();
        if (communityIds == null || communityIds.isEmpty()) {
            return result;
        }

        // Execute all community lookups in parallel (rate limiting handled by SynopticClient)
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String communityId : communityIds) {
            futures.add(CompletableFuture.runAsync(() -> {
                JsonLookupResult lookupResult = synopticClient.getCommunity(communityId, true);

                synchronized (result) {
                    if (lookupResult.isFound()) {
                        // Extract member_count from community data
                        long memberCount = lookupResult.data().path("member_count").asLong(0);
                        result.addFound(communityId, memberCount);
                    } else if (lookupResult.isNotFound()) {
                        result.addNotFound(communityId);
                    } else {
                        result.addError(communityId);
                    }
                }
            }, batchExecutor));
        }

        // Wait for all to complete
        for (CompletableFuture<Void> future : futures) {
            try {
                future.join();
            } catch (Exception e) {
                // Individual failures already tracked in the async block
            }
        }
        return result;
    }

    /**
     * Fetches a chunk of users by ID (max 100).
     * Tracks which IDs were found vs not found.
     */
    private BatchUserResult fetchUsersByIdChunk(List<String> userIds) {
        BatchUserResult result = new BatchUserResult();
        Set<String> foundIds = new HashSet<>();

        try {
            Optional<JsonNode> usersOpt = synopticClient.getUsersById(userIds, true).toOptional();
            if (usersOpt.isPresent()) {
                JsonNode usersArray = usersOpt.get();
                if (usersArray.isArray()) {
                    for (JsonNode userNode : usersArray) {
                        // Cache username -> userId mapping
                        usernameCacheService.cacheFromSynopticUser(userNode);

                        String userId = userNode.path("user_id").asText("");
                        if (!userId.isEmpty()) {
                            result.addFound(userId, axiomMapper.mapUser(userNode));
                            foundIds.add(userId);
                        }
                    }
                }
            }

            // Mark unfound IDs as not found
            for (String userId : userIds) {
                if (!foundIds.contains(userId)) {
                    result.addNotFound(userId);
                }
            }
        } catch (Exception e) {
            // Mark all IDs in this chunk as errors
            for (String userId : userIds) {
                result.addError(userId);
            }
        }
        return result;
    }

    /**
     * Splits a list into balanced chunks.
     * For 220 items with max 100: returns 3 chunks of ~73-74 items each.
     */
    private <T> List<List<T>> partitionBalanced(List<T> list, int maxChunkSize) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        int totalSize = list.size();
        int numChunks = (int) Math.ceil((double) totalSize / maxChunkSize);
        int baseSize = totalSize / numChunks;
        int remainder = totalSize % numChunks;

        List<List<T>> chunks = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < numChunks; i++) {
            int chunkSize = baseSize + (i < remainder ? 1 : 0);
            chunks.add(list.subList(offset, offset + chunkSize));
            offset += chunkSize;
        }
        return chunks;
    }

    private JsonNode transformMedia(JsonNode tweet) {
        if (!(tweet instanceof ObjectNode node)) {
            return tweet;
        }
        node.remove("media");
        JsonNode mediaV2 = node.remove("mediaV2");
        if (mediaV2 != null && !mediaV2.isNull()) {
            node.set("media", mediaV2);
        }
        return node;
    }

    private JsonNode enrichReplyData(JsonNode tweet) {
        JsonNode replyNode = tweet.get("reply");
        if (replyNode == null || replyNode.isNull()) {
            return tweet;
        }

        JsonNode replyToStatusId = replyNode.get("reply_to_status_id");
        if (replyToStatusId == null || replyToStatusId.isNull()) {
            return tweet;
        }

        String repliedToTweetId = replyToStatusId.asText();
        JsonNode repliedToTweet = fetchRawTweet(repliedToTweetId);

        if (repliedToTweet != null) {
            ((ObjectNode) tweet).set("reply", repliedToTweet);
        }

        return tweet;
    }

    private JsonNode fetchRawTweet(String tweetId) {
        JsonNode tweet = synopticClient.getTweet(tweetId).orElse(null);
        if (tweet != null) {
            tweet = transformMedia(tweet);
        }
        return tweet;
    }
}
