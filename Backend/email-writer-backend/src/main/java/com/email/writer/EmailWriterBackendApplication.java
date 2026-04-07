package com.email.writer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Smart Email Assistant Backend Application.
 *
 * @EnableJpaRepositories removed — Spring Boot auto-configuration already
 * scans and registers all JPA repositories in the same package tree.
 * The annotation was redundant and adds misleading specificity.
 */
@SpringBootApplication
public class EmailWriterBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailWriterBackendApplication.class, args);
    }
}