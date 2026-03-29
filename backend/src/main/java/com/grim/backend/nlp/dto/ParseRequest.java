package com.grim.backend.nlp.dto;

import jakarta.validation.constraints.NotBlank;

public record ParseRequest(@NotBlank String text) {

}