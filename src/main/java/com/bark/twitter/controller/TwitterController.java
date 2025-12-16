package com.bark.twitter.controller;

import com.bark.twitter.service.TwitterService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TwitterController {

    private static final Logger log = LoggerFactory.getLogger(TwitterController.class);

    private final TwitterService twitterService;

    public TwitterController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @GetMapping("/tweet/{id}")
    public JsonNode getTweet(@PathVariable String id) {
        long start = System.currentTimeMillis();
        log.info("[{}][TWEET][{}] GET /tweet/{}", start, id, id);
        JsonNode response = twitterService.getTweet(id);
        log.info("[{}][TWEET][{}] {}", System.currentTimeMillis(), id, response);
        return response;
    }

    @GetMapping("/user/{id}")
    public JsonNode getUser(@PathVariable String id) {
        long start = System.currentTimeMillis();
        log.info("[{}][USER][{}] GET /user/{}", start, id, id);
        JsonNode response = twitterService.getUser(id);
        log.info("[{}][USER][{}] {}", System.currentTimeMillis(), id, response);
        return response;
    }

    @GetMapping("/community/{id}")
    public ResponseEntity<Map<String, String>> getCommunity(@PathVariable String id) {
        long start = System.currentTimeMillis();
        log.info("[{}][COMMUNITY][{}] GET /community/{}", start, id, id);
        Map<String, String> response = Map.of("error", "Community lookup not implemented");
        log.info("[{}][COMMUNITY][{}] {}", System.currentTimeMillis(), id, response);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
