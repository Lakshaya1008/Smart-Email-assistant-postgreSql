package com.email.writer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Application configuration.
 *
 * Provides a RestTemplate bean with explicit timeouts.
 * RestTemplateBuilder lost its .connectTimeout()/.readTimeout(Duration) methods
 * in Spring Framework 6.1 (Spring Boot 3.2). We configure timeouts directly on
 * SimpleClientHttpRequestFactory instead — same effect, fully compatible.
 *
 * The 30-second read timeout ensures a slow or unresponsive Gemini API call
 * cannot block a server thread forever.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 10 seconds in milliseconds
        factory.setReadTimeout(30_000);      // 30 seconds in milliseconds
        return new RestTemplate(factory);
    }
}