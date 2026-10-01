package dev.learningstore.mail.domain;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public final class OrderMailPolicy {
    public MailLesson select(OrderEvent event) {
        return switch (event.stage()) {
            case CHECKOUT -> new MailLesson("checkout-confirmed", "Your course order {{order_id}} is confirmed",
                    "<h1>Enrollment reserved</h1><p>Hi {{learner_name}}, we received order {{order_id}} for {{course_name}}.</p>");
            case FULFILLMENT -> new MailLesson("learning-access", "Your {{course_name}} access is ready",
                    "<h1>Start learning</h1><p>Hi {{learner_name}}, your course is ready: <a href=\"{{detail}}\">open the classroom</a>.</p>");
            case RECEIPT -> new MailLesson("order-receipt", "Receipt for order {{order_id}}",
                    "<h1>Receipt</h1><p>{{course_name}}</p><p>Total: {{detail}}</p>");
            case ORDER_UPDATE -> new MailLesson("order-update", "An update to order {{order_id}}",
                    "<h1>Order update</h1><p>Hi {{learner_name}}, {{detail}}</p>");
        };
    }

    public Map<String, String> variables(OrderEvent event) {
        return Map.of("order_id", event.orderId(), "learner_name", event.learnerName(),
                "course_name", event.courseName(), "detail", event.detail());
    }

    public enum Stage { CHECKOUT, FULFILLMENT, RECEIPT, ORDER_UPDATE }
    public record OrderEvent(String eventId, String orderId, String learnerEmail, String learnerName,
                             String courseName, Stage stage, String detail) {}
    public record MailLesson(String key, String subject, String html) {}
}
