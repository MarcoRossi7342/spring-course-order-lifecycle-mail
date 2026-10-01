package dev.learningstore.mail.client;

import com.fasterxml.jackson.databind.JsonNode;

public final class InfraiException extends RuntimeException {
    private final int status;
    private final JsonNode detail;

    public InfraiException(String code, int status, JsonNode detail) {
        super(code + ": " + detail.path("message").asText("request rejected"));
        this.status = status;
        this.detail = detail;
    }

    public int status() { return status; }
    public JsonNode detail() { return detail; }
}
