package com.kedaxunfei.myinterviewer.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kedaxunfei.myinterviewer.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI service conditional assembly:
 *  - app.ai.provider=local or missing => LocalInterviewAiService (fallback mode)
 *  - app.ai.provider=openai/qwen/deepseek/custom => LlmInterviewAiService (real LLM)
 */
@Configuration
public class InterviewAiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(InterviewAiAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "local", matchIfMissing = true)
    public InterviewAiService localInterviewAiService(AppProperties appProperties) {
        log.warn("============================================================");
        log.warn("  AI Service: LOCAL fallback mode (LocalInterviewAiService)");
        log.warn("  provider={} | aiLocalFeature={}", appProperties.ai().provider(), appProperties.feature().aiLocal());
        log.warn("  To use real LLM: set AI_PROVIDER=deepseek in .env and fill AI_API_BASE_URL + AI_API_KEY");
        log.warn("============================================================");
        return new LocalInterviewAiService(appProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai")
    public InterviewAiService openAiService(AppProperties appProperties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("=== AI Service: OpenAI (LlmInterviewAiService) provider={} model={} ===",
            appProperties.ai().provider(), appProperties.ai().model());
        return new LlmInterviewAiService(appProperties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "qwen")
    public InterviewAiService qwenService(AppProperties appProperties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("=== AI Service: Qwen (LlmInterviewAiService) provider={} model={} ===",
            appProperties.ai().provider(), appProperties.ai().model());
        return new LlmInterviewAiService(appProperties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "deepseek")
    public InterviewAiService deepSeekService(AppProperties appProperties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("=== AI Service: DeepSeek (LlmInterviewAiService) provider={} model={} baseUrl={} ===",
            appProperties.ai().provider(), appProperties.ai().model(), appProperties.ai().apiBaseUrl());
        return new LlmInterviewAiService(appProperties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "custom")
    public InterviewAiService customService(AppProperties appProperties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        log.info("=== AI Service: Custom (LlmInterviewAiService) provider={} model={} baseUrl={} ===",
            appProperties.ai().provider(), appProperties.ai().model(), appProperties.ai().apiBaseUrl());
        return new LlmInterviewAiService(appProperties, objectMapper);
    }
}
