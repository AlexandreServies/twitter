package com.bark.twitter.controller;

import com.bark.twitter.dto.CommunityResponse;
import com.bark.twitter.dto.ErrorResponse;
import com.bark.twitter.dto.twitterapi.AuthorDto;
import com.bark.twitter.dto.twitterapi.TweetDto;
import com.bark.twitter.service.TwitterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Twitter API", description = "Twitter data relay service")
@SecurityRequirement(name = "apiKey")
public class TwitterController {

    private final TwitterService twitterService;
    private final ObjectMapper objectMapper;

    public TwitterController(TwitterService twitterService, ObjectMapper objectMapper) {
        this.twitterService = twitterService;
        this.objectMapper = objectMapper;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    @GetMapping("/tweet/{id}")
    @Operation(summary = "Get tweet by ID", description = "Fetches a tweet by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tweet found",
                    content = @Content(schema = @Schema(implementation = TweetDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Missing x-api-key header\"}"))),
            @ApiResponse(responseCode = "403", description = "Invalid API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Invalid API key\"}"))),
            @ApiResponse(responseCode = "404", description = "Tweet not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Tweet not found: 123456789\"}")))
    })
    public TweetDto getTweet(@Parameter(description = "Tweet ID") @PathVariable String id) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][TWEET][" + id + "] GET /tweet/" + id);
        TweetDto response = twitterService.getTweet(id);
        System.out.println("[" + System.currentTimeMillis() + "][TWEET][" + id + "] " + toJson(response));
        return response;
    }

    @GetMapping("/user/{id}")
    @Operation(summary = "Get user by ID", description = "Fetches a Twitter user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Missing x-api-key header\"}"))),
            @ApiResponse(responseCode = "403", description = "Invalid API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Invalid API key\"}"))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"User not found: 123456789\"}")))
    })
    public AuthorDto getUser(@Parameter(description = "User ID") @PathVariable String id) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][USER][" + id + "] GET /user/" + id);
        AuthorDto response = twitterService.getUser(id);
        System.out.println("[" + System.currentTimeMillis() + "][USER][" + id + "] " + toJson(response));
        return response;
    }

    @GetMapping("/community/{id}")
    @Operation(summary = "Get community by ID", description = "Fetches a Twitter community by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Community found",
                    content = @Content(schema = @Schema(implementation = CommunityResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Missing x-api-key header\"}"))),
            @ApiResponse(responseCode = "403", description = "Invalid API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Invalid API key\"}"))),
            @ApiResponse(responseCode = "404", description = "Community not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Community not found: 123456789\"}")))
    })
    public JsonNode getCommunity(@Parameter(description = "Community ID") @PathVariable String id) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][COMMUNITY][" + id + "] GET /community/" + id);
        JsonNode response = twitterService.getCommunity(id);
        System.out.println("[" + System.currentTimeMillis() + "][COMMUNITY][" + id + "] " + toJson(response));
        return response;
    }

    @GetMapping("/health")
    @Hidden
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
