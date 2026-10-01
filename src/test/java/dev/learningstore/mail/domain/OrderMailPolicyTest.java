package dev.learningstore.mail.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.learningstore.mail.domain.OrderMailPolicy.OrderEvent;
import dev.learningstore.mail.domain.OrderMailPolicy.Stage;
import org.junit.jupiter.api.Test;

class OrderMailPolicyTest {
    private final OrderMailPolicy policy = new OrderMailPolicy();

    @Test
    void fulfillment_teaches_the_learner_where_to_begin() {
        OrderEvent event = new OrderEvent("evt-42", "ord-42", "learner@example.com", "Mina",
                "Practical Geometry", Stage.FULFILLMENT, "https://learn.example/courses/geometry");

        var selected = policy.select(event);

        assertThat(selected.key()).isEqualTo("learning-access");
        assertThat(selected.html()).contains("Start learning", "{{detail}}");
        assertThat(policy.variables(event)).containsEntry("course_name", "Practical Geometry");
    }

    @Test
    void receipt_keeps_the_paid_total_in_the_receipt_lesson() {
        OrderEvent event = new OrderEvent("evt-43", "ord-43", "learner@example.com", "Mina",
                "Practical Geometry", Stage.RECEIPT, "USD 49.00");

        assertThat(policy.select(event).key()).isEqualTo("order-receipt");
        assertThat(policy.variables(event)).containsEntry("detail", "USD 49.00");
    }
}
