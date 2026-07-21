package com.kedaxunfei.myinterviewer;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class MyInterviewerApplication {

    private static final Logger log = LoggerFactory.getLogger(MyInterviewerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MyInterviewerApplication.class, args);
    }

    @Bean
    public ApplicationRunner aiConfigDiagnostic() {
        return args -> {
            String provider = System.getenv("AI_PROVIDER");
            String apiBaseUrl = System.getenv("AI_API_BASE_URL");
            String apiKey = System.getenv("AI_API_KEY");
            String model = System.getenv("AI_MODEL");

            log.info("============================================================");
            log.info("  ENV DIAGNOSTIC (raw System.getenv):");
            log.info("  AI_PROVIDER    = {}", provider == null ? "<NOT FOUND>" : provider);
            log.info("  AI_API_BASE_URL= {}", apiBaseUrl == null ? "<NOT FOUND>" : apiBaseUrl);
            log.info("  AI_API_KEY     = {}", apiKey == null ? "<NOT FOUND>" : (apiKey.isEmpty() ? "<EMPTY>" : "***" + apiKey.substring(apiKey.length() - 4)));
            log.info("  AI_MODEL       = {}", model == null ? "<NOT FOUND>" : model);
            log.info("============================================================");
        };
    }
}
