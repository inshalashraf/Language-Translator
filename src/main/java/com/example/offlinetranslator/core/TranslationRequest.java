package com.example.offlinetranslator.core;

import java.util.Objects;

/**
 * Immutable input for a translation operation.
 */
public record TranslationRequest(Language source, Language target, String text) {
    public TranslationRequest {
        Objects.requireNonNull(source, "Source language is required");
        Objects.requireNonNull(target, "Target language is required");
        if (target == Language.AUTO) {
            throw new IllegalArgumentException("Target language cannot be AUTO");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to translate cannot be blank");
        }
    }
}
