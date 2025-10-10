package com.example.offlinetranslator;

import com.example.offlinetranslator.core.TranslationRequest;
import com.example.offlinetranslator.core.TranslationResult;
import com.example.offlinetranslator.llm.OnlineTranslator;

import java.io.InputStream;
import java.util.Properties;

public class TranslateCli {
    public static void main(String[] args) throws Exception {
        var props = new Properties();
        try (InputStream in = TranslateCli.class.getResourceAsStream("/app.properties")) {
            if (in != null) props.load(in);
        }

        try (var t = new OnlineTranslator(props)) {
            var req = new TranslationRequest(
                    com.example.offlinetranslator.core.Language.EN,
                    com.example.offlinetranslator.core.Language.ES,
                    "Hello world"
            );
            TranslationResult res = t.translate(req);
            System.out.println("Translated: " + res.translated());
        }
    }
}
