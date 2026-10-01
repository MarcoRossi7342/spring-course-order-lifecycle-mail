package dev.learningstore.mail.service;

import dev.learningstore.mail.client.InfraiEmailClient;
import dev.learningstore.mail.config.InfraiProperties;
import dev.learningstore.mail.domain.OrderMailPolicy;
import dev.learningstore.mail.domain.OrderMailPolicy.MailLesson;
import dev.learningstore.mail.domain.OrderMailPolicy.OrderEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public final class CourseOrderMailService {
    private final InfraiEmailClient emailClient;
    private final OrderMailPolicy policy;
    private final InfraiProperties properties;
    private final String registrationBatch = UUID.randomUUID().toString().substring(0, 8);
    private final Map<String, String> templateIds = new ConcurrentHashMap<>();

    public CourseOrderMailService(InfraiEmailClient emailClient, OrderMailPolicy policy, InfraiProperties properties) {
        this.emailClient = emailClient;
        this.policy = policy;
        this.properties = properties;
    }

    public DeliveryResult notifyLearner(OrderEvent event) {
        MailLesson lesson = policy.select(event);
        Map<String, String> variables = policy.variables(event);
        String templateId = templateIds.computeIfAbsent(lesson.key(), key -> register(lesson, variables));
        InfraiEmailClient.RenderedMail rendered = emailClient.previewTemplate(templateId, variables);
        String messageId = emailClient.send(event.learnerEmail(), rendered.subject(), rendered.html(), event.eventId());
        return new DeliveryResult(event.orderId(), lesson.key(), messageId);
    }

    private String register(MailLesson lesson, Map<String, String> exampleVariables) {
        String name = properties.templateNamespace() + "-" + lesson.key() + "-" + registrationBatch;
        return emailClient.createTemplate(name, lesson.subject(), lesson.html(), exampleVariables, "register-" + name);
    }

    public record DeliveryResult(String orderId, String template, String messageId) {}
}
