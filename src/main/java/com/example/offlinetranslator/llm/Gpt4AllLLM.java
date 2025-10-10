package com.example.offlinetranslator.llm;

import com.example.offlinetranslator.core.TranslationRequest;
import com.example.offlinetranslator.core.TranslationResult;
import com.example.offlinetranslator.core.TranslatorLLM;
import java.util.Properties;

/**
 * Demo implementation — does NOT call any real model or server.
 * Simply echoes the input with a dummy "[Translated]" prefix.
 */
public class Gpt4AllLLM implements TranslatorLLM {
    private final int maxTokens;
    private final double temperature;

    public Gpt4AllLLM(Properties props) {
        this.maxTokens = Integer.parseInt(props.getProperty("model.maxTokens", "512"));
        this.temperature = Double.parseDouble(props.getProperty("model.temperature", "0.2"));
    }

    @Override
    public TranslationResult translate(TranslationRequest req) {
        // Simple demo: return fake translated text
        String fakeOutput = "[Demo Translation] (" +
                req.source().name() + " → " + req.target().name() + ") " +
                req.text();

        // Return instantly without hitting any model
        return new TranslationResult(fakeOutput, req.source().name());
    }

    @Override
    public void close() {
        // Nothing to close — no model resources used
    }
}
