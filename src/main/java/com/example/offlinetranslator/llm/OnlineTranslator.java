package com.example.offlinetranslator.llm;

import com.example.offlinetranslator.core.TranslationRequest;
import com.example.offlinetranslator.core.TranslationResult;
import com.example.offlinetranslator.core.TranslatorLLM;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Simple online translator that calls a REST translation API.
 * Default endpoint: LibreTranslate public instance at https://libretranslate.de/translate
 * Configurable via properties:
 * - translator.endpoint (full URL)
 * - translator.apikey (optional)
 */
public class OnlineTranslator implements TranslatorLLM {
    private final HttpClient http;
    private final String endpoint;
    private final String apiKey;
    private final String backend;

    public OnlineTranslator(Properties props) {
    this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        this.endpoint = props.getProperty("translator.endpoint", "https://translate.argosopentech.com/translate");
        this.apiKey = props.getProperty("translator.apikey", "");
        this.backend = props.getProperty("translator.backend", "google").trim().toLowerCase();
    }

    @Override
    public TranslationResult translate(TranslationRequest req) throws Exception {
        HttpRequest request;
    if ("mymemory".equals(backend)) {
            // MyMemory free API: GET /get?q=...&langpair=source|target
            String q = URLEncoder.encode(req.text(), StandardCharsets.UTF_8);
            String langpair = URLEncoder.encode(req.source().name().toLowerCase() + "|" + req.target().name().toLowerCase(), StandardCharsets.UTF_8);
            String uri = "https://api.mymemory.translated.net/get?q=" + q + "&langpair=" + langpair;
            request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .GET()
                    .build();
    } else if ("google".equals(backend)) {
        // Unofficial Google Translate web endpoint (no API key required):
        // https://translate.googleapis.com/translate_a/single?client=gtx&sl=...&tl=...&dt=t&q=...
        String q = URLEncoder.encode(req.text(), StandardCharsets.UTF_8);
        String uri = String.format(
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=%s&tl=%s&dt=t&q=%s",
            URLEncoder.encode(req.source().name().toLowerCase(), StandardCharsets.UTF_8),
            URLEncoder.encode(req.target().name().toLowerCase(), StandardCharsets.UTF_8),
            q
        );
        request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("User-Agent", "Java/OnlineTranslator")
            .GET()
            .build();
    } else {
            // default: LibreTranslate-style POST (endpoint configurable)
            var form = new StringBuilder();
            form.append("q=").append(URLEncoder.encode(req.text(), StandardCharsets.UTF_8));
            form.append("&source=").append(URLEncoder.encode(req.source().name().toLowerCase(), StandardCharsets.UTF_8));
            form.append("&target=").append(URLEncoder.encode(req.target().name().toLowerCase(), StandardCharsets.UTF_8));
            form.append("&format=text");
            if (!apiKey.isBlank()) {
                form.append("&api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
            }

            request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .build();
        }

        HttpResponse<String> resp;
        try {
            resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IOException("Failed to call translation endpoint", e);
        }

        if (resp.statusCode() / 100 != 2) {
            throw new IOException("Translation API returned status " + resp.statusCode() + ": " + resp.body());
        }

        String body = resp.body();
        String translated = extractTranslatedText(body);
        return new TranslationResult(translated, req.source().name());
    }

    private String extractTranslatedText(String jsonBody) {
        // Very small and forgiving JSON extraction to avoid adding dependencies.
        // Try common shapes:
        // 1) {"translatedText":"..."}
        // 2) {"responseData": {"translatedText": "..."}, ...}
        String key1 = "\"translatedText\"";
        int idx = jsonBody.indexOf(key1);
        if (idx != -1) {
            int colon = jsonBody.indexOf(':', idx + key1.length());
            if (colon != -1) {
                int start = jsonBody.indexOf('"', colon);
                if (start != -1) {
                    start++; // after opening quote
                    StringBuilder sb = new StringBuilder();
                    boolean esc = false;
                    for (int i = start; i < jsonBody.length(); i++) {
                        char c = jsonBody.charAt(i);
                        if (esc) {
                            sb.append(c);
                            esc = false;
                        } else if (c == '\\') {
                            esc = true;
                        } else if (c == '"') {
                            break;
                        } else {
                            sb.append(c);
                        }
                    }
                    return sb.toString();
                }
            }
        }

        // Try MyMemory style: "responseData": { "translatedText": "..." }
        String respKey = "\"responseData\"";
        int rIdx = jsonBody.indexOf(respKey);
        if (rIdx != -1) {
            int tdIdx = jsonBody.indexOf(key1, rIdx);
            if (tdIdx != -1) {
                int colon = jsonBody.indexOf(':', tdIdx + key1.length());
                if (colon != -1) {
                    int start = jsonBody.indexOf('"', colon);
                    if (start != -1) {
                        start++;
                        StringBuilder sb = new StringBuilder();
                        boolean esc = false;
                        for (int i = start; i < jsonBody.length(); i++) {
                            char c = jsonBody.charAt(i);
                            if (esc) {
                                sb.append(c);
                                esc = false;
                            } else if (c == '\\') {
                                esc = true;
                            } else if (c == '"') {
                                break;
                            } else {
                                sb.append(c);
                            }
                        }
                        return sb.toString();
                    }
                }
            }
        }

        // Try Google translate array response like [[["Hola mundo","Hello world",...],...],...]
        // We'll extract the first string token inside the first array element.
    int gIdx = jsonBody.indexOf("[[");
        if (gIdx != -1) {
            // find first occurrence of "..." after [[
            int firstQuote = jsonBody.indexOf('"', gIdx);
            if (firstQuote != -1) {
                int start = firstQuote + 1;
                StringBuilder sb = new StringBuilder();
                boolean esc = false;
                for (int i = start; i < jsonBody.length(); i++) {
                    char c = jsonBody.charAt(i);
                    if (esc) {
                        sb.append(c);
                        esc = false;
                    } else if (c == '\\') {
                        esc = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
        }

        // fallback: return whole body
        return jsonBody;
    }

    @Override
    public void close() {
        // HttpClient does not require explicit close
    }
}
