package com.kedaxunfei.myinterviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Feature feature,
        Ai ai,
        Speech speech,
        Cors cors
) {

    public record Jwt(String issuer, String secret, long expirationMinutes) {
    }

    public record Feature(
            boolean resume,
            boolean aiLocal,
            boolean speech,
            boolean posture,
            boolean pdfReport
    ) {
    }

    public record Ai(
            String provider,
            String apiBaseUrl,
            String apiKey,
            String model,
            int timeoutSeconds,
            int maxRetries
    ) {
    }

    public record Speech(
            String asrLang,
            String ttsLang,
            double ttsRate
    ) {
    }

    public record Cors(
            String allowedOrigins,
            String allowedMethods,
            String allowedHeaders,
            String exposedHeaders,
            boolean allowCredentials,
            int maxAge
    ) {
    }
}
