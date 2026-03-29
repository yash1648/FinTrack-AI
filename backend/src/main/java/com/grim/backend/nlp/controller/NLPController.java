package com.grim.backend.nlp.controller;

import com.grim.backend.nlp.dto.DraftTransactionDTO;
import com.grim.backend.nlp.dto.ParseRequest;
import com.grim.backend.nlp.dto.ParseResponse;
import com.grim.backend.nlp.service.NLPService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nlp")
@RequiredArgsConstructor
public class NLPController {

    private final NLPService nlpService;

    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parse(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ParseRequest body) {

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