package com.grim.backend.analysis.controller;

import com.grim.backend.analysis.dto.AnomalyResponse;
import com.grim.backend.analysis.dto.InsightData;
import com.grim.backend.analysis.dto.ProjectionResponse;
import com.grim.backend.analysis.service.AnalysisService;
import com.grim.backend.auth.dto.ApiResponse;
import com.grim.backend.auth.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<InsightData>> insights(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(new ApiResponse<>(true, analysisService.getInsights(userDetails.getId())));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<ApiResponse<List<AnomalyResponse>>> anomalies(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(new ApiResponse<>(true, analysisService.getAnomalies(userDetails.getId())));
    }

    @GetMapping("/projection")
    public ResponseEntity<ApiResponse<ProjectionResponse>> projection(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(new ApiResponse<>(true, analysisService.getProjection(userDetails.getId())));
    }
}