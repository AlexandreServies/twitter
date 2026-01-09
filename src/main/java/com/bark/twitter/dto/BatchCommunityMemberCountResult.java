package com.bark.twitter.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Result of a batch community member count lookup operation.
 * Distinguishes between successful lookups, not-found communities, and errors.
 */
public class BatchCommunityMemberCountResult {

    private final Map<String, Long> found;
    private final Set<String> notFound;
    private final Set<String> errors;

    public BatchCommunityMemberCountResult() {
        this.found = new HashMap<>();
        this.notFound = new HashSet<>();
        this.errors = new HashSet<>();
    }

    public void addFound(String communityId, long memberCount) {
        found.put(communityId, memberCount);
    }

    public void addNotFound(String communityId) {
        notFound.add(communityId);
    }

    public void addError(String communityId) {
        errors.add(communityId);
    }

    public Map<String, Long> getFound() {
        return found;
    }

    public Set<String> getNotFound() {
        return notFound;
    }

    public Set<String> getErrors() {
        return errors;
    }

    /**
     * Merges another result into this one.
     */
    public void merge(BatchCommunityMemberCountResult other) {
        this.found.putAll(other.found);
        this.notFound.addAll(other.notFound);
        this.errors.addAll(other.errors);
    }
}
