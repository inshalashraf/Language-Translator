package com.example.offlinetranslator.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TranslationRequestTest {
    @Test
    void acceptsValidTranslationRequest() {
        var request = new TranslationRequest(Language.AUTO, Language.HI, "Hello");
        assertEquals(Language.AUTO, request.source());
        assertEquals(Language.HI, request.target());
        assertEquals("Hello", request.text());
    }

    @Test
    void rejectsMissingLanguages() {
        assertThrows(NullPointerException.class,
            () -> new TranslationRequest(null, Language.HI, "Hello"));
        assertThrows(NullPointerException.class,
            () -> new TranslationRequest(Language.EN, null, "Hello"));
    }

    @Test
    void rejectsAutomaticTargetLanguage() {
        assertThrows(IllegalArgumentException.class,
            () -> new TranslationRequest(Language.EN, Language.AUTO, "Hello"));
    }

    @Test
    void rejectsNullOrBlankText() {
        assertThrows(IllegalArgumentException.class,
            () -> new TranslationRequest(Language.EN, Language.HI, null));
        assertThrows(IllegalArgumentException.class,
            () -> new TranslationRequest(Language.EN, Language.HI, "  "));
    }
}
