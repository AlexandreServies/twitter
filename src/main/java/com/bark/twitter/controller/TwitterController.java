package com.bark.twitter.controller;

import com.bark.twitter.service.TwitterService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TwitterController {

    private final TwitterService twitterService;

    public TwitterController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @GetMapping("/tweet/{id}")
    public JsonNode getTweet(@PathVariable String id) {
        return twitterService.getTweet(id);
    }

    @GetMapping("/user/{id}")
    public JsonNode getUser(@PathVariable String id) {
        return twitterService.getUser(id);
    }

    @GetMapping("/community/{id}")
    public ResponseEntity<Map<String, String>> getCommunity(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "Community lookup not implemented"));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
