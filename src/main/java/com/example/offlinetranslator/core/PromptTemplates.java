package com.example.offlinetranslator.core;

public final class PromptTemplates {
    private PromptTemplates() {}

    public static String system() {
        return """
        You are an expert translator. Always translate faithfully, keep meaning, tone, and names.
        For code or URLs, keep them unchanged. Do not add explanations unless asked.
        """;
    }

    public static String user(Language src, Language tgt, String text) {
        var srcPart = (src == Language.AUTO) ? "Detect the language automatically" : ("Source language: " + src.label);
        return """
        %s
        Target language: %s
        Text:
        ---
        %s
        ---
        Reply with ONLY the translated text.""".formatted(srcPart, tgt.label, text);
    }
}
