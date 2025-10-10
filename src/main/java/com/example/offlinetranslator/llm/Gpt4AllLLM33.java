package com.example.offlinetranslator.llm;

import com.example.offlinetranslator.core.TranslationRequest;
import com.example.offlinetranslator.core.TranslationResult;
import com.example.offlinetranslator.core.TranslatorLLM;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

// import com.hexadevlabs.gpt4all.GPT4All; // Commented out to avoid missing dependency

public class Gpt4AllLLM33 implements TranslatorLLM {
    // private final GPT4All model; // Commented out — no GPT4All library used
    private final int maxTokens;
    private final double temperature;

    public Gpt4AllLLM33(Properties props) {
        try {
            var modelPath = props.getProperty("model.path", "models/model.gguf");
            this.maxTokens = Integer.parseInt(props.getProperty("model.maxTokens", "512"));
            this.temperature = Double.parseDouble(props.getProperty("model.temperature", "0.2"));

            // Commented out model existence check
            // if (!Files.exists(Path.of(modelPath))) {
            //     throw new IllegalStateException("Model not found at: " + modelPath);
            // }

            // Commented out GPT4All loading
            // this.model = new GPT4All(modelPath);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize dummy LLM", e);
        }
    }

    @Override
    public TranslationResult translate(TranslationRequest req) {
        try {
            var prompt = "Translate from " + req.source().label
                    + " to " + req.target().label + ": " + req.text();
            var json = "{\"prompt\":\"" + prompt.replace("\"","\\\"") + "\"}";
            var url = new URL("http://127.0.0.1:8080/completion");
            var conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type","application/json");
            conn.setDoOutput(true);
            try(OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }
            try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder(); String line;
                while((line=br.readLine())!=null) sb.append(line);
                return new TranslationResult(sb.toString(), req.source().name());
            }
        } catch(Exception e) {
            return new TranslationResult("[Error contacting llama.cpp] "+e.getMessage(), req.source().name());
        }
    }

    // Commented out full model-based method
    // public TranslationResult translate(TranslationRequest req) throws Exception {
    //     var system = PromptTemplates.system();
    //     var user = PromptTemplates.user(req.source(), req.target(), req.text());
    //     var completion = model.chatCompletion()
    //             .withSystemPrompt(system)
    //             .withPrompt(user)
    //             .withMaxTokens(maxTokens)
    //             .withTemp(temperature)
    //             .generate();
    //     var translated = completion.getChoices().get(0).getMessage().getContent().trim();
    //     String detected = (req.source() != Language.AUTO) ? req.source().name()
    //             : detectLanguageFast(req.text());
    //     return new TranslationResult(translated, detected);
    // }

    // Commented out detectLanguageFast since GPT4All is not present
    // private String detectLanguageFast(String text) {
    //     try {
    //         var prompt = """
    //             Detect the language name of the text below. Reply with only the language name (e.g., English).
    //             Text:
    //             %s
    //             """.formatted(text);
    //         var out = model.generate(prompt, 64, 0.0);
    //         return out.trim().split("\\R")[0];
    //     } catch (Exception e) {
    //         return "Unknown";
    //     }
    // }

    @Override
    public void close() throws Exception {
        // if (model != null) model.close(); // Commented out
    }
}
