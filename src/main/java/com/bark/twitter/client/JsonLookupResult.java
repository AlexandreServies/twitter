package com.bark.twitter.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Result of a JSON lookup - either found, not found, or error.
 */
public record JsonLookupResult(
        Status status,
        JsonNode data
) {
    public enum Status {
        FOUND,
        NOT_FOUND,
        ERROR
    }

    public static JsonLookupResult found(JsonNode data) {
        return new JsonLookupResult(Status.FOUND, data);
    }

    public static JsonLookupResult notFound() {
        return new JsonLookupResult(Status.NOT_FOUND, null);
    }

    public static JsonLookupResult error() {
        return new JsonLookupResult(Status.ERROR, null);
    }

    public boolean isFound() {
        return status == Status.FOUND;
    }

    public boolean isNotFound() {
        return status == Status.NOT_FOUND;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }
}
