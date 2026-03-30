package com.grim.backend.nlp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grim.backend.category.dto.CategoryDTO;
import com.grim.backend.category.service.CategoryService;
import com.grim.backend.nlp.dto.DraftTransactionDTO;
import com.grim.backend.nlp.dto.ParseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NLPService {

    private final ChatClient chatClient;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    @Value("${nlp.prefilter.min-text-length:3}")
    private int minTextLength;

    @Value("${nlp.prefilter.max-text-length:500}")
    private int maxTextLength;

    @Value("${nlp.fallback.enabled:true}")
    private boolean fallbackEnabled;

    private static final Pattern MONEY_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");

    public ParseResponse parseNaturalLanguageInput(UUID userId, String text) {

        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.length() < minTextLength || trimmed.length() > maxTextLength) {
            return new ParseResponse(false, null, "prefilter");
        }

        try {
            List<CategoryDTO> categories = categoryService.getCategories(userId).stream()
                    .map(c -> CategoryDTO.builder().id(c.getId()).name(c.getName()).build())
                    .toList();

            Map<String, CategoryDTO> categoriesByLowerName = categories.stream()
                    .collect(Collectors.toMap(
                            c -> c.getName().toLowerCase(Locale.ROOT),
                            c -> c,
                            (a, b) -> a
                    ));

            String categoryList = categories.stream()
                    .map(CategoryDTO::getName)
                    .collect(Collectors.joining(", "));

            String prompt = buildPrompt(trimmed, categoryList);

            String response = chatClient.prompt()
                    .user(prompt)
                    .options(OllamaChatOptions.builder().disableThinking().build())
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return tryFallback(userId, trimmed)
                        .map(d -> new ParseResponse(true, d, "validated-fallback"))
                        .orElseGet(() -> new ParseResponse(false, null, "ollama"));
            }

            JsonNode node = objectMapper.readTree(response);
            DraftTransactionDTO draft = extractDraftFromAi(node, categoriesByLowerName);
            if (draft != null) {
                return new ParseResponse(true, draft, "validated-ollama");
            }

            return tryFallback(userId, trimmed)
                    .map(d -> new ParseResponse(true, d, "validated-fallback"))
                    .orElseGet(() -> new ParseResponse(false, null, "ollama"));

        } catch (Exception e) {
            log.warn("NLP parse failed safely: {}", e.getMessage());
            return tryFallback(userId, trimmed)
                    .map(d -> new ParseResponse(true, d, "validated-fallback"))
                    .orElseGet(() -> new ParseResponse(false, null, "ollama"));
        }
    }

    private String buildPrompt(String text, String categories) {

        LocalDate today = LocalDate.now();

        return """
You are a strict financial transaction parser.

Your job is to extract structured transaction data from user input.

TODAY_DATE = %s

ALLOWED_CATEGORIES = [%s]

OUTPUT REQUIREMENTS (CRITICAL):

Return ONLY valid JSON.
Do NOT include explanations.
Do NOT include thinking.
Do NOT include markdown.
Do NOT include extra text.

The response MUST match this schema exactly:

{
  "amount": number | null,
  "type": "expense" | "income" | null,
  "categoryName": string | null,
  "description": string | null,
  "date": "YYYY-MM-DD" | null
}

FIELD RULES:

amount
- numeric value only
- do not include currency symbols

type
- expense → money leaving the user
- income → money received by the user

categoryName
- MUST match one of ALLOWED_CATEGORIES
- if no suitable category exists return null
- do NOT invent new categories

description
- short human readable description
- if missing infer from input

date
- must be ISO format: YYYY-MM-DD
- if user gives relative dates convert using TODAY_DATE

RELATIVE DATE RULES:

today = %s
yesterday = today - 1 day
tomorrow = today + 1 day

If no date is mentioned use TODAY_DATE.

INVALID INPUT RULES:

If the input is not a financial transaction return:

{
  "amount": null,
  "type": null,
  "categoryName": null,
  "description": null,
  "date": null
}

USER_INPUT:
%s
""".formatted(today, categories, today, text);
    }

    private DraftTransactionDTO extractDraftFromAi(JsonNode node, Map<String, CategoryDTO> categoriesByLowerName) {
        if (node == null || node.isNull()) {
            return null;
        }

        BigDecimal amount = parsePositiveAmount(node.get("amount"));
        if (amount == null) {
            return null;
        }

        String type = textOrNull(node.get("type"));
        if (type == null) {
            return null;
        }
        String normalizedType = type.toLowerCase(Locale.ROOT);
        if (!normalizedType.equals("expense") && !normalizedType.equals("income")) {
            return null;
        }

        String categoryName = textOrNull(node.get("categoryName"));
        if (categoryName == null) {
            return null;
        }
        CategoryDTO matched = categoriesByLowerName.get(categoryName.toLowerCase(Locale.ROOT));
        if (matched == null) {
            return null;
        }

        String description = textOrNull(node.get("description"));
        if (description != null) {
            if (description.length() > 255) {
                return null;
            }
        }

        LocalDate date = parseDate(node.get("date"));
        if (date == null || date.isAfter(LocalDate.now())) {
            return null;
        }

        return DraftTransactionDTO.builder()
                .amount(amount)
                .type(normalizedType)
                .categoryId(matched.getId().toString())
                .categoryName(matched.getName())
                .description(description)
                .date(date)
                .build();
    }

    private BigDecimal parsePositiveAmount(JsonNode amountNode) {
        if (amountNode == null || amountNode.isNull()) {
            return null;
        }

        try {
            BigDecimal amount;
            if (amountNode.isNumber()) {
                amount = amountNode.decimalValue();
            } else {
                String raw = amountNode.asText();
                if (raw == null) return null;
                Matcher m = MONEY_PATTERN.matcher(raw);
                if (!m.find()) return null;
                amount = new BigDecimal(m.group(1));
            }

            return amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String s = node.asText();
        if (s == null) return null;
        s = s.trim();
        return s.isBlank() ? null : s;
    }

    private LocalDate parseDate(JsonNode dateNode) {
        if (dateNode == null || dateNode.isNull()) {
            return null;
        }
        String raw = dateNode.asText();
        if (raw == null) return null;
        raw = raw.trim();

        try {
            if (raw.matches("\\d{8}")) {
                return LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            return LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private Optional<DraftTransactionDTO> tryFallback(UUID userId, String text) {
        if (!fallbackEnabled) {
            return Optional.empty();
        }

        String lower = text.toLowerCase(Locale.ROOT);

        String normalizedType = null;
        if (lower.contains("spent") || lower.contains("expense") || lower.contains("paid") || lower.contains("bought")) {
            normalizedType = "expense";
        } else if (lower.contains("received") || lower.contains("earned") || lower.contains("income") || lower.contains("salary")) {
            normalizedType = "income";
        }
        if (normalizedType == null) {
            return Optional.empty();
        }

        Matcher m = MONEY_PATTERN.matcher(lower);
        if (!m.find()) {
            return Optional.empty();
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(m.group(1));
        } catch (Exception e) {
            return Optional.empty();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        List<com.grim.backend.category.entity.Category> categories = categoryService.getCategories(userId);
        com.grim.backend.category.entity.Category matched = categories.stream()
                .filter(c -> c.getName() != null && lower.contains(c.getName().toLowerCase(Locale.ROOT)))
                .max((a, b) -> Integer.compare(safeLen(a.getName()), safeLen(b.getName())))
                .orElse(null);
        if (matched == null) {
            return Optional.empty();
        }

        // Extract date from text (simple fallback)
        LocalDate date = extractDateFromText(lower);
        if (date == null) {
            date = LocalDate.now();
        }

        String description = text.trim();
        if (description.length() > 255) {
            description = description.substring(0, 255);
        }

        return Optional.of(DraftTransactionDTO.builder()
                .amount(amount)
                .type(normalizedType)
                .categoryId(matched.getId().toString())
                .categoryName(matched.getName())
                .description(description)
                .date(date)
                .build());
    }

    private LocalDate extractDateFromText(String text) {
        // Simple regex for common date patterns in fallback
        // This is a basic implementation - could be enhanced with more sophisticated parsing
        Pattern datePattern = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})");
        Matcher matcher = datePattern.matcher(text);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (year < 100) {
                    year += 2000; // Assume 2000+ for 2-digit years
                }
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Invalid date, fall back to today
            }
        }
        return null;
    }

    private int safeLen(String s) {
        return s == null ? 0 : s.length();
    }
}