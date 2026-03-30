package com.grim.backend.analysis.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grim.backend.analysis.dto.AiInsightResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class OllamaAnalysisClient {

    private final ChatClient chatClient;
    private final ObjectMapper mapper;

    public AiInsightResult analyze(String prompt) {

        String result = chatClient
                .prompt(prompt)
                .call()
                .content();

        try {
            JsonNode root = mapper.readTree(result);
            List<String> patterns = parseStringList(root.get("patterns"));
            List<String> recommendations = parseStringList(root.get("recommendations"));
            return new AiInsightResult(patterns, recommendations);
        }
        catch (Exception e) {
            return new AiInsightResult(Collections.emptyList(), Collections.emptyList());
        }
    }

    private List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }

        final int maxItems = 20;
        final int maxLen = 300;

        return StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(s -> s.length() > maxLen ? s.substring(0, maxLen) : s)
                .limit(maxItems)
                .toList();
    }
}