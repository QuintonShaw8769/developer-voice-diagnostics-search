package dev.infrai.voicesearch.infrai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "infrai")
public record InfraiProperties(
        String apiKey,
        String baseUrl,
        String collection,
        Retry retry
) {
    public record Retry(int maxAttempts, long initialDelayMs) {}
}
