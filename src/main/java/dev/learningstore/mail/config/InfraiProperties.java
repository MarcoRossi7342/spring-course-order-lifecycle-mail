package dev.learningstore.mail.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("infrai")
public record InfraiProperties(URI baseUrl, String apiKey, String templateNamespace, Duration requestTimeout) {
    public InfraiProperties {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("INFRAI_API_KEY is required");
        }
    }
}
