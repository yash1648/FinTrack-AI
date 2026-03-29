package com.grim.backend.nlp.controller;

import com.grim.backend.nlp.dto.DraftTransactionDTO;
import com.grim.backend.nlp.dto.ParseRequest;
import com.grim.backend.nlp.dto.ParseResponse;
import com.grim.backend.nlp.service.NLPService;
import com.grim.backend.ratelimiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nlp")
@RequiredArgsConstructor
public class NLPController {

    private final NLPService nlpService;
    private final RateLimiterService rateLimiterService;
    @PostMapping("/parse")
    public ResponseEntity<?> parse(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ParseRequest body,
            HttpServletRequest httpRequest) {

        String ip = httpRequest.getRemoteAddr();
        if (!rateLimiterService.allowRequest(ip, 20, Duration.ofMinutes(1))) {
            return ResponseEntity.status(429).body("Too many requests");
        }


        ParseResponse result =
                nlpService.parseNaturalLanguageInput(userId, body.text());

        if (result.isParsed()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "parsed", true,
                            "draft", result.getDraft()
                    )
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "parsed", false,
                        "message", "Could not extract transaction details."
                )
        ));
    }


}