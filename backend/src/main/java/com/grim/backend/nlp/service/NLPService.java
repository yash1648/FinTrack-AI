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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    public ParseResponse parseNaturalLanguageInput(UUID userId, String text) {

        try {

            List<CategoryDTO> categories =
                    categoryService.getCategories(userId)
                            .stream()
                            .map(c->{
                                return CategoryDTO.builder()
                                        .id(c.getId())
                                        .name(c.getName())
                                        .build();
                            })
                            .toList();


            String categoryList = categories.stream()
                    .map(CategoryDTO::getName)
                    .collect(Collectors.joining(", "));

            String prompt = buildPrompt(text, categoryList);

            String response = chatClient.prompt()
                    .user(prompt)
                    .options(OllamaChatOptions
                            .builder()
                            .disableThinking()
                            .build())
                    .call()
                    .content();

            JsonNode node = objectMapper.readTree(response);
            log.info("LLM RAW RESPONSE: {}", response);
            BigDecimal amount = node.get("amount").decimalValue();
            String type = node.get("type").asText();
            String categoryName = node.get("categoryName").asText();
            String description = node.get("description").asText();

            String rawDate=node.get("date").asText();

            LocalDate date;
            if(rawDate.matches("\\d{8}")){
                date=LocalDate.parse(rawDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            }else{
                date=LocalDate.parse(rawDate);
            }




            Optional<CategoryDTO> matched =
                    categories.stream()
                            .filter(c -> c.getName()
                                    .equalsIgnoreCase(categoryName))
                            .findFirst();

            if (matched.isEmpty()) {

                log.warn("Category not matched: {}", categoryName);

                return new ParseResponse(false, null, "ollama");
            }

            DraftTransactionDTO draft = DraftTransactionDTO.builder()
                    .amount(amount)
                    .type(type)
                    .categoryId(matched.get().getId().toString())
                    .categoryName(matched.get().getName())
                    .description(description)
                    .date(date)
                    .build();

            return new ParseResponse(true, draft, "ollama");

        } catch (Exception e) {

            log.error("NLP parse failed", e);

            return new ParseResponse(false, null, "ollama");
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
  "type": "expense" | "income" | "transfer" | null,
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
- transfer → moving money between accounts

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
}