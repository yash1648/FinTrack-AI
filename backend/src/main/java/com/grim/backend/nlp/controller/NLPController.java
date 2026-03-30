package com.grim.backend.nlp.controller;

import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.security.JwtProvider;
import com.grim.backend.nlp.dto.ParseRequest;
import com.grim.backend.nlp.dto.ParseResponse;
import com.grim.backend.nlp.service.NLPService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nlp")
@RequiredArgsConstructor
public class NLPController {

    private final NLPService nlpService;

    private final JwtProvider jwtProvider;

    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<Map<String, Object>>> parse(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ParseRequest body) {

        UUID userId = jwtProvider.extractUserId(token.substring(7));
        ParseResponse result = nlpService.parseNaturalLanguageInput(userId, body.text());

        if (result.isParsed()) {
            Map<String, Object> data = Map.of(
                    "parsed", true,
                    "draft", result.getDraft()
            );
            return ResponseEntity.ok(new ApiResponse<>(true, data));
        }

        Map<String, Object> data = Map.of(
                "parsed", false,
                "message", "Could not extract transaction details."
        );
        return ResponseEntity.ok(new ApiResponse<>(true, data));
    }


}