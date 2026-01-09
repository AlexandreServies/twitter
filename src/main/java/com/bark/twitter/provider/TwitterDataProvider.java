package com.bark.twitter.provider;

import com.bark.twitter.dto.BatchCommunityMemberCountResult;
import com.bark.twitter.dto.BatchUserResult;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;

import java.util.List;

/**
 * Interface for Twitter data providers.
 * Allows switching between different data sources (Synoptic, TwitterAPI.io, etc.)
 */
public interface TwitterDataProvider {

    /**
     * Gets a tweet by ID.
     *
     * @param tweetId the tweet ID
     * @return the tweet data in Axion format
     * @throws com.bark.twitter.exception.NotFoundException if tweet not found
     */
    AxionTweetDto getTweet(String tweetId);

    /**
     * Gets a user by numeric ID.
     *
     * @param userId the numeric user ID
     * @return the user data in Axion format
     * @throws com.bark.twitter.exception.NotFoundException if user not found
     */
    AxionUserInfoDto getUser(String userId);

    /**
     * Gets a user by username/handle.
     *
     * @param username the username (without @)
     * @return the user data in Axion format
     * @throws com.bark.twitter.exception.NotFoundException if user not found
     */
    AxionUserInfoDto getUserByUsername(String username);

    /**
     * Gets a community by ID.
     *
     * @param communityId the community ID
     * @return the community data in Axion format
     * @throws com.bark.twitter.exception.NotFoundException if community not found
     */
    AxionCommunityDto getCommunity(String communityId);

    /**
     * Returns the name of this provider for logging purposes.
     */
    String getProviderName();

    /**
     * Batch fetches users by their numeric IDs.
     * Returns found users, not-found IDs, and errored IDs separately.
     *
     * @param userIds list of user IDs to fetch
     * @return batch result with found users (keyed by userId), not-found, and errors
     */
    BatchUserResult getUsersByIds(List<String> userIds);

    /**
     * Batch fetches users by their usernames (lowercase).
     * Returns found users, not-found usernames, and errored usernames separately.
     *
     * @param usernames list of usernames to fetch (lowercase)
     * @return batch result with found users (keyed by lowercase username), not-found, and errors
     */
    BatchUserResult getUsersByUsernames(List<String> usernames);

    /**
     * Batch fetches community member counts by their IDs.
     * Returns found member counts, not-found IDs, and errored IDs separately.
     *
     * @param communityIds list of community IDs to fetch
     * @return batch result with found member counts (keyed by communityId), not-found, and errors
     */
    BatchCommunityMemberCountResult getCommunityMemberCounts(List<String> communityIds);
}
