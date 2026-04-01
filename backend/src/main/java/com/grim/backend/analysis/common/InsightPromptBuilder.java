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
Analyze spending patterns and give insights and recommendations based on the provided data.

Spending summary (Category: Amount):
""");

        totals.forEach((k,v) ->
                prompt.append("- ").append(k).append(": ").append(v).append("\n")
        );

        prompt.append("""

Return ONLY a valid JSON object with the following structure:
{
 "patterns": ["string"],
 "recommendations": ["string"]
}
Do not include any other text or explanation.
""");

        return prompt.toString();
    }
}