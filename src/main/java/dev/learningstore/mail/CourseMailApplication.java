package dev.learningstore.mail;

import dev.learningstore.mail.config.InfraiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(InfraiProperties.class)
public class CourseMailApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourseMailApplication.class, args);
    }
}
