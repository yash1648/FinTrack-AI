package com.grim.backend.analysis.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightResult {

    private List<String> patterns;

    private List<String> recommendations;

}