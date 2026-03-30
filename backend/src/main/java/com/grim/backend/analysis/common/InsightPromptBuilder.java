package com.grim.backend.analysis.common;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class InsightPromptBuilder {

    public String build(Map<String, BigDecimal> totals) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
You are a financial analysis assistant.

Analyze spending patterns and give insights and recommendations.

Spending summary:
""");

        totals.forEach((k,v) ->
                prompt.append(k).append(": ").append(v).append("\n")
        );

        prompt.append("""
Return JSON:
{
 "patterns": [],
 "recommendations": []
}
""");

        return prompt.toString();
    }
}