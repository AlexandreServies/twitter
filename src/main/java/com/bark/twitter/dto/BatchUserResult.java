package com.bark.twitter.dto;

import com.bark.twitter.dto.axion.AxionUserInfoDto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Result of a batch user lookup operation.
 * Distinguishes between successful lookups, not-found users, and errors.
 */
public class BatchUserResult {

    private final Map<String, AxionUserInfoDto> found;
    private final Set<String> notFound;
    private final Set<String> errors;

    public BatchUserResult() {
        this.found = new HashMap<>();
        this.notFound = new HashSet<>();
        this.errors = new HashSet<>();
    }

    public void addFound(String key, AxionUserInfoDto user) {
        found.put(key, user);
    }

    public void addNotFound(String key) {
        notFound.add(key);
    }

    public void addError(String key) {
        errors.add(key);
    }

    public Map<String, AxionUserInfoDto> getFound() {
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
    public void merge(BatchUserResult other) {
        this.found.putAll(other.found);
        this.notFound.addAll(other.notFound);
        this.errors.addAll(other.errors);
    }
}
