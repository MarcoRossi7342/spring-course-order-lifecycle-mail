package dev.learningstore.mail.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.learningstore.mail.config.InfraiProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public final class InfraiEmailClient {
    private final InfraiProperties properties;
    private final ObjectMapper json;
    private final HttpClient http;

    public InfraiEmailClient(InfraiProperties properties, ObjectMapper json) {
        this(properties, json, HttpClient.newBuilder().connectTimeout(properties.requestTimeout()).build());
    }

    InfraiEmailClient(InfraiProperties properties, ObjectMapper json, HttpClient http) {
        this.properties = properties;
        this.json = json;
        this.http = http;
    }

    // Copyable REST idiom: POST /v1/email/template/create
    public String createTemplate(String name, String subject, String html, Map<String, String> templateVars,
                                 String idempotencyKey) {
        JsonNode data = post("/v1/email/template/create", Map.of(
                "name", name, "subject", subject, "html", html, "default_vars", templateVars), idempotencyKey);
        return requiredText(data, "id");
    }

    public RenderedMail previewTemplate(String templateId, Map<String, String> templateVars) {
        JsonNode data = post("/v1/email/template/preview/" + templateId,
                Map.of("vars", templateVars), null);
        return new RenderedMail(requiredText(data, "subject"), requiredText(data, "html"));
    }

    public String send(String to, String subject, String html, String idempotencyKey) {
        JsonNode data = post("/v1/email/send", Map.of("to", to, "subject", subject, "html", html), idempotencyKey);
        return requiredText(data, "message_id");
    }

    private JsonNode post(String path, Map<String, ?> body, String idempotencyKey) {
        String payload;
        try {
            payload = json.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot encode email request", e);
        }

        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder request = HttpRequest.newBuilder(resolve(path))
                    .timeout(properties.requestTimeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(payload));
            if (idempotencyKey != null) request.header("Idempotency-Key", idempotencyKey);

            try {
                HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelay(response, attempt).toMillis());
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new InfraiException(error.path("code").asText("request rejected"),
                            response.statusCode(), error);
                }
                if (response.statusCode() >= 500) {
                    throw new IllegalStateException("Email provider returned status " + response.statusCode());
                }
                return envelope.path("data");
            } catch (IOException e) {
                throw new IllegalStateException("Email request could not be completed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Email request interrupted", e);
            }
        }
        throw new IllegalStateException("Email request retry budget exhausted");
    }

    private URI resolve(String path) {
        return URI.create(properties.baseUrl().toString().replaceAll("/$", "") + path);
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(raw -> {
            try { return Duration.ofSeconds(Long.parseLong(raw)); }
            catch (NumberFormatException ignored) { return Duration.ofMillis(250L << attempt); }
        }).orElse(Duration.ofMillis(250L << attempt));
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) throw new IllegalStateException("Response data is missing " + field);
        return value;
    }

    public record RenderedMail(String subject, String html) {}
}
