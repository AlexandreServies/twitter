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
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][TWEET][" + id + "] GET /tweet/" + id);
        JsonNode response = twitterService.getTweet(id);
        System.out.println("[" + System.currentTimeMillis() + "][TWEET][" + id + "] " + response);
        return response;
    }

    @GetMapping("/user/{id}")
    public JsonNode getUser(@PathVariable String id) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][USER][" + id + "] GET /user/" + id);
        JsonNode response = twitterService.getUser(id);
        System.out.println("[" + System.currentTimeMillis() + "][USER][" + id + "] " + response);
        return response;
    }

    @GetMapping("/community/{id}")
    public ResponseEntity<Map<String, String>> getCommunity(@PathVariable String id) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][COMMUNITY][" + id + "] GET /community/" + id);
        Map<String, String> response = Map.of("error", "Community lookup not implemented");
        System.out.println("[" + System.currentTimeMillis() + "][COMMUNITY][" + id + "] " + response);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
