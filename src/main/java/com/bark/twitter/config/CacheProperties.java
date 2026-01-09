package com.bark.twitter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twitter.cache")
public record CacheProperties(
        EndpointCacheConfig tweets,
        EndpointCacheConfig users,
        EndpointCacheConfig communities,
        EndpointCacheConfig follows,
        EndpointCacheConfig communityMemberCounts
) {
    public record EndpointCacheConfig(int ttlMinutes, int billingPeriodMinutes) {
        public long ttlMs() {
            return ttlMinutes * 60 * 1000L;
        }

        public long billingPeriodMs() {
            return billingPeriodMinutes * 60 * 1000L;
        }

        public boolean billEveryRequest() {
            return billingPeriodMinutes == 0;
        }
    }
}
