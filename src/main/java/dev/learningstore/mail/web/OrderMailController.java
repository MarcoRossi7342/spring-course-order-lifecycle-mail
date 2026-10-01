package dev.learningstore.mail.web;

import dev.learningstore.mail.client.InfraiException;
import dev.learningstore.mail.domain.OrderMailPolicy.OrderEvent;
import dev.learningstore.mail.service.CourseOrderMailService;
import dev.learningstore.mail.service.CourseOrderMailService.DeliveryResult;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-mails")
public final class OrderMailController {
    private final CourseOrderMailService service;

    public OrderMailController(CourseOrderMailService service) { this.service = service; }

    @PostMapping
    public DeliveryResult send(@RequestBody OrderEvent event) { return service.notifyLearner(event); }

    @ExceptionHandler(InfraiException.class)
    ResponseEntity<Map<String, Object>> rejected(InfraiException error) {
        int status = error.status() >= 400 && error.status() < 500 ? error.status() : 502;
        return ResponseEntity.status(status).body(Map.of("error", error.detail()));
    }
}
