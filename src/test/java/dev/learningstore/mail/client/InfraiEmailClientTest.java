package dev.learningstore.mail.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.learningstore.mail.config.InfraiProperties;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InfraiEmailClientTest {
    @Test
    void templateRequestsUseDiscoveryFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<JsonNode> createBody = new AtomicReference<>();
        AtomicReference<JsonNode> previewBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/email/template/create", exchange -> {
            createBody.set(mapper.readTree(exchange.getRequestBody()));
            byte[] response = "{\"ok\":true,\"data\":{\"id\":\"test-id\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) { body.write(response); }
        });
        server.createContext("/v1/email/template/preview/test-id", exchange -> {
            previewBody.set(mapper.readTree(exchange.getRequestBody()));
            byte[] response = "{\"ok\":true,\"data\":{\"subject\":\"Hello\",\"html\":\"<p>Hello</p>\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) { body.write(response); }
        });
        server.start();
        try {
            var properties = new InfraiProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    "test-key", "test", Duration.ofSeconds(5));
            var client = new InfraiEmailClient(properties, mapper);
            var vars = Map.of("learner_name", "Mina");
            String id = client.createTemplate("test", "Hello", "<p>Hello</p>", vars, "test-key");
            client.previewTemplate(id, vars);

            assertThat(createBody.get().path("default_vars")).isEqualTo(mapper.valueToTree(vars));
            assertThat(createBody.get().has("template_vars")).isFalse();
            assertThat(previewBody.get()).isEqualTo(mapper.valueToTree(Map.of("vars", vars)));
        } finally {
            server.stop(0);
        }
    }
}
