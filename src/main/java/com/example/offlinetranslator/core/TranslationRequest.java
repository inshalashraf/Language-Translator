package com.example.offlinetranslator.core;

public record TranslationRequest(Language source, Language target, String text) {}
