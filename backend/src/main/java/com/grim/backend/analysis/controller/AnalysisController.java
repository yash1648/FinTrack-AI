package com.grim.backend.analysis.controller;

import com.grim.backend.analysis.dto.AnomalyResponse;
import com.grim.backend.analysis.dto.InsightData;
import com.grim.backend.analysis.dto.ProjectionResponse;
import com.grim.backend.analysis.service.AnalysisService;
import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final JwtProvider jwtProvider;

    @GetMapping("/insights")
    public ApiResponse<InsightData> insights(
            @RequestHeader("Authorization") String token) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        return new ApiResponse(true, analysisService.getInsights(userId));
    }

    @GetMapping("/anomalies")
    public ApiResponse<List<AnomalyResponse>> anomalies(
            @RequestHeader("Authorization") String token) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        return new ApiResponse(true, analysisService.getAnomalies(userId));
    }

    @GetMapping("/projection")
    public ApiResponse<ProjectionResponse> projection(
            @RequestHeader("Authorization") String token) {
        UUID userId = jwtProvider.extractUserId(token.substring(7));
        return new ApiResponse(true, analysisService.getProjection(userId));
    }
}