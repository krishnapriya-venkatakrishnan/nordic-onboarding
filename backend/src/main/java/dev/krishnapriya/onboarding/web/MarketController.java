package dev.krishnapriya.onboarding.web;

import dev.krishnapriya.onboarding.domain.ApplicationRequest;
import dev.krishnapriya.onboarding.domain.ApplicationResult;
import dev.krishnapriya.onboarding.domain.MarketConfig;
import dev.krishnapriya.onboarding.domain.OnboardingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/markets")
public class MarketController {

    private final OnboardingService onboarding;

    public MarketController(OnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @GetMapping
    public List<MarketConfig.Summary> markets() {
        return onboarding.available();
    }

    @GetMapping("/{code}/onboarding")
    public MarketConfig config(@PathVariable String code) {
        return onboarding.config(code);
    }

    /** Simulated BankID / MitID login. */
    @PostMapping("/{code}/identity/sessions")
    public Map<String, String> startIdentitySession(@PathVariable String code) {
        return Map.of("sessionId", onboarding.startIdentitySession(code));
    }

    @PostMapping("/{code}/national-id/check")
    public Map<String, Object> checkNationalId(@PathVariable String code, @RequestBody Map<String, String> body) {
        return onboarding.checkNationalId(code, body.get("value"))
                .<Map<String, Object>>map(e -> Map.of("valid", false, "message", e.message()))
                .orElseGet(() -> Map.of("valid", true));
    }

    @PostMapping("/{code}/applications")
    public ResponseEntity<ApplicationResult> submit(@PathVariable String code, @RequestBody ApplicationRequest request) {
        ApplicationResult result = onboarding.submit(code, request);
        return ResponseEntity.status(result.accepted() ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_CONTENT)
                .body(result);
    }
}
