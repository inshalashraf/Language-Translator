package com.example.offlinetranslator.core;

public interface TranslatorLLM extends AutoCloseable {
    TranslationResult translate(TranslationRequest req) throws Exception;
    @Override default void close() throws Exception {}
}
