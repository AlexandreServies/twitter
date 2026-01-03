package com.bark.twitter.credits;

import com.bark.twitter.config.ApiKeyInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing API credits.
 */
@RestController
@RequestMapping("/credits")
@Tag(name = "Credits", description = "API credit management")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @PostMapping
    @Operation(summary = "Add credits", description = "Adds credits to the authenticated API key")
    public AddCreditsResponse addCredits(HttpServletRequest request, @RequestBody AddCreditsRequest body) {
        String apiKey = (String) request.getAttribute(ApiKeyInterceptor.API_KEY_ATTRIBUTE);
        creditService.addCredits(apiKey, body.amount());
        long newBalance = creditService.getCredits(apiKey);
        return new AddCreditsResponse(body.amount(), newBalance);
    }

    public record AddCreditsRequest(long amount) {}

    public record AddCreditsResponse(long added, long balance) {}
}
