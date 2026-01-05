package com.bark.twitter.controller;

import com.bark.twitter.config.ApiKeyInterceptor;
import com.bark.twitter.dto.ErrorResponse;
import com.bark.twitter.dto.FollowsResponseDto;
import com.bark.twitter.dto.axion.AxionCommunityDto;
import com.bark.twitter.dto.axion.AxionTweetDto;
import com.bark.twitter.dto.axion.AxionUserInfoDto;
import com.bark.twitter.exception.BadRequestException;
import com.bark.twitter.infra.PushoverClient;
import com.bark.twitter.service.TwitterService;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Twitter API", description = "Twitter data relay service")
@SecurityRequirement(name = "apiKey")
public class TwitterController {

    private final TwitterService twitterService;
    private final ObjectMapper objectMapper;
    private final PushoverClient pushoverClient;

    public TwitterController(TwitterService twitterService, ObjectMapper objectMapper, PushoverClient pushoverClient) {
        this.twitterService = twitterService;
        this.objectMapper = objectMapper;
        this.pushoverClient = pushoverClient;
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
                    content = @Content(schema = @Schema(implementation = AxionTweetDto.class))),
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
    public AxionTweetDto getTweet(@Parameter(description = "Tweet ID") @PathVariable String id) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][" + id + "][REQUEST][TWEET] GET /tweet/" + id);
        AxionTweetDto response = twitterService.getTweet(id);
        long duration = System.currentTimeMillis() - start;
        System.out.println("[" + System.currentTimeMillis() + "][" + id + "][RESPONSE][TWEET][" + duration + "ms] " + toJson(response));
        return response;
    }

    @GetMapping("/user/{idOrHandle}")
    @Operation(summary = "Get user by ID or handle", description = "Fetches a Twitter user by their numeric ID or @handle.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = AxionUserInfoDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Invalid user ID format. Use numeric ID or @handle\"}"))),
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
    public AxionUserInfoDto getUser(@Parameter(description = "Numeric user ID or @handle") @PathVariable String idOrHandle) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][" + idOrHandle + "][REQUEST][USER] GET /user/" + idOrHandle);

        AxionUserInfoDto response;
        if (idOrHandle.startsWith("@")) {
            String username = idOrHandle.substring(1);
            response = twitterService.getUserByUsername(username);
        } else {
            if (!isNumeric(idOrHandle)) {
                throw new BadRequestException("Invalid user ID format. Use numeric ID or @handle");
            }
            response = twitterService.getUser(idOrHandle);
        }

        long duration = System.currentTimeMillis() - start;
        System.out.println("[" + System.currentTimeMillis() + "][" + idOrHandle + "][RESPONSE][USER][" + duration + "ms] " + toJson(response));
        return response;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    @GetMapping("/community/{id}")
    @Operation(summary = "Get community by ID", description = "Fetches a Twitter community by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Community found",
                    content = @Content(schema = @Schema(implementation = AxionCommunityDto.class))),
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
    public AxionCommunityDto getCommunity(@Parameter(description = "Community ID") @PathVariable String id) {
        long start = System.currentTimeMillis();
        System.out.println("[" + start + "][" + id + "][REQUEST][COMMUNITY] GET /community/" + id);
        AxionCommunityDto response = twitterService.getCommunity(id);
        long duration = System.currentTimeMillis() - start;
        System.out.println("[" + System.currentTimeMillis() + "][" + id + "][RESPONSE][COMMUNITY][" + duration + "ms] " + toJson(response));
        return response;
    }

    @GetMapping("/follows")
    @Operation(summary = "Get followers/following counts for multiple users",
            description = "Fetches follower and following counts for a batch of Twitter users by their handles. " +
                    "Charges 1 credit per handle regardless of whether found or cached.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Followers data returned",
                    content = @Content(schema = @Schema(implementation = FollowsResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No usernames provided",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"No usernames provided\"}"))),
            @ApiResponse(responseCode = "401", description = "Missing API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Missing x-api-key header\"}"))),
            @ApiResponse(responseCode = "403", description = "Invalid API key or insufficient credits",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Insufficient credits for 100 handles\"}")))
    })
    public FollowsResponseDto getFollows(
            @Parameter(description = "Comma-separated list of Twitter handles (without @)")
            @RequestParam("user_handles") String userHandles,
            HttpServletRequest request) {
        long start = System.currentTimeMillis();
        String apiKey = (String) request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);

        List<String> usernames = Arrays.stream(userHandles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (usernames.isEmpty()) {
            throw new BadRequestException("No usernames provided");
        }

        System.out.println("[" + start + "][" + apiKey.substring(0, 8) + "][REQUEST][FOLLOWS] GET /follows?user_handles=" + userHandles);
        FollowsResponseDto response = twitterService.getFollowsByUsernames(usernames, apiKey);
        long duration = System.currentTimeMillis() - start;
        System.out.println("[" + System.currentTimeMillis() + "][" + apiKey.substring(0, 8) + "][RESPONSE][FOLLOWS][" + duration + "ms] " + toJson(response));
        return response;
    }

    @GetMapping("/health")
    @Hidden
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/emergency-alert")
    @Hidden
    public Map<String, String> sendEmergencyAlert() {
        System.out.println("[" + System.currentTimeMillis() + "][EMERGENCY] Emergency alert requested");
        pushoverClient.sendHighPriority("EMERGENCY TWITTER ALERT", "Emergency alert requested");
        return Map.of("status", "sent");
    }
}
