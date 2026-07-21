package com.kedaxunfei.myinterviewer.integration;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.kedaxunfei.myinterviewer.config.AppProperties;
import com.kedaxunfei.myinterviewer.domain.InterviewMessage;
import com.kedaxunfei.myinterviewer.domain.InterviewerStyle;
import com.kedaxunfei.myinterviewer.domain.JobPosition;
import com.kedaxunfei.myinterviewer.domain.MessageRole;

import jakarta.annotation.PostConstruct;

public class LlmInterviewAiService implements InterviewAiService {

    private final AppProperties appProperties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private WebClient webClient;

    public LlmInterviewAiService(AppProperties appProperties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        AppProperties.Ai ai = appProperties.ai();
        if (ai.apiBaseUrl() == null || ai.apiBaseUrl().isBlank()) {
            throw new IllegalStateException(
                    "AI_PROVIDER 已设置为非 local，但 AI_API_BASE_URL 未配置，请在 .env 中填写"
            );
        }
        this.webClient = WebClient.builder()
                .baseUrl(ai.apiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + ai.apiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generateOpeningQuestion(JobPosition position, InterviewerStyle style) {
        return generateOpeningQuestion(position, style, ResumeContext.empty());
    }

    @Override
    public String generateOpeningQuestion(JobPosition position, InterviewerStyle style, ResumeContext resume) {
        String systemPrompt = buildSystemPrompt(position, style, resume);
        String userPrompt = buildOpeningUserPrompt(position, style, resume);
        return callLlm(systemPrompt, userPrompt);
    }

    @Override
    public String generateFollowUpQuestion(
            JobPosition position,
            InterviewerStyle style,
            ResumeContext resume,
            List<InterviewMessage> history,
            String answer,
            int nextQuestionNo
    ) {
        String systemPrompt = buildSystemPrompt(position, style, resume);
        StringBuilder context = new StringBuilder();
        for (InterviewMessage msg : history) {
            String role = msg.getRole() == MessageRole.USER ? "候选人" : "面试官";
            context.append(role).append("：").append(msg.getContent()).append("\n\n");
        }
        String userPrompt = "以上是本次面试的完整对话历史。现在候选人刚刚回答了第 "
                + (nextQuestionNo - 1) + " 题。\n\n"
                + "候选人的回答：\n" + answer + "\n\n"
                + "请基于上下文，作为「" + style.getName() + "」风格，追问第 " + nextQuestionNo
                + " 题。要求：\n"
                + "1. 问题要具体、有深度，不要泛泛而谈；\n"
                + "2. 与候选人上一轮回答有关联性；\n"
                + "3. 保持「" + style.getName() + "」的提问风格；\n"
                + "4. 直接输出问题本身，不要加「第X题」等前缀，不要解释。";
        return callLlm(systemPrompt, context + userPrompt);
    }

    @Override
    public InterviewAiReport generateReport(JobPosition position, InterviewerStyle style, List<InterviewMessage> history) {
        return generateReport(position, style, ResumeContext.empty(), history);
    }

    @Override
    public InterviewAiReport generateReport(
            JobPosition position,
            InterviewerStyle style,
            ResumeContext resume,
            List<InterviewMessage> history
    ) {
        List<String> answers = history.stream()
                .filter(message -> message.getRole() == MessageRole.USER)
                .map(InterviewMessage::getContent)
                .toList();
        int answerCount = answers.size();
        int totalLength = answers.stream().mapToInt(String::length).sum();

        String systemPrompt = "你是一位专业的面试评估专家。请基于以下面试对话，输出一份 JSON 格式的评估报告。"
                + "JSON 格式如下（不要输出任何其他内容）：\n"
                + "{\n"
                + "  \"totalScore\": 整数 0-100,\n"
                + "  \"technicalScore\": 整数 0-100,\n"
                + "  \"communicationScore\": 整数 0-100,\n"
                + "  \"logicScore\": 整数 0-100,\n"
                + "  \"summary\": \"整体表现总结，2-3 句话\",\n"
                + "  \"strengths\": \"亮点，2-3 句话\",\n"
                + "  \"improvements\": \"待改进点，2-3 句话\",\n"
                + "  \"recommendation\": \"下一步建议，2-3 句话\"\n"
                + "}";

        StringBuilder dialogue = new StringBuilder();
        for (InterviewMessage msg : history) {
            String role = msg.getRole() == MessageRole.USER ? "候选人" : "面试官";
            dialogue.append(role).append("：").append(msg.getContent()).append("\n\n");
        }
        String resumeHint = resume != null && resume.present()
                ? "\n简历摘要：" + resume.summary() + "\n技能：" + String.join("、", resume.skills())
                : "";
        String userPrompt = "岗位：" + position.getName() + "\n"
                + "面试官风格：" + style.getName() + "\n"
                + "候选人共回答 " + answerCount + " 轮，总字数 " + totalLength + "\n"
                + resumeHint + "\n\n"
                + "=== 面试对话 ===\n" + dialogue + "\n"
                + "请输出 JSON 评估报告。";

        String json = callLlm(systemPrompt, userPrompt);
        return parseReportFromJson(json, answerCount, totalLength, resume);
    }

    private String buildSystemPrompt(JobPosition position, InterviewerStyle style, ResumeContext resume) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是「").append(style.getName()).append("」风格的面试官。");
        prompt.append("正在面试「").append(position.getName()).append("」岗位。\n");
        if (position.getTechStack() != null && !position.getTechStack().isBlank()) {
            prompt.append("岗位技术栈：").append(position.getTechStack()).append("\n");
        }
        if (position.getPromptTemplate() != null && !position.getPromptTemplate().isBlank()) {
            prompt.append("面试要求：").append(position.getPromptTemplate()).append("\n");
        }
        prompt.append("面试官风格描述：").append(style.getDescription() != null ? style.getDescription() : "专业、有条理").append("\n");
        if (style.getPromptTemplate() != null && !style.getPromptTemplate().isBlank()) {
            prompt.append("风格指引：").append(style.getPromptTemplate()).append("\n");
        }
        if (resume != null && resume.present()) {
            prompt.append("\n候选人简历摘要：\n").append(resume.summary()).append("\n");
            if (!resume.skills().isEmpty()) {
                prompt.append("技能：").append(String.join("、", resume.skills())).append("\n");
            }
        }
        return prompt.toString();
    }

    private String buildOpeningUserPrompt(JobPosition position, InterviewerStyle style, ResumeContext resume) {
        if (resume != null && resume.present()) {
            return "请基于候选人的简历，提出第一个面试问题。要求：\n"
                    + "1. 问题要具体、有深度，可以结合简历中的技能或项目经历；\n"
                    + "2. 保持「" + style.getName() + "」的提问风格；\n"
                    + "3. 只输出问题本身，不要加「第一题」等前缀，不要解释。";
        }
        return "请针对「" + position.getName() + "」岗位，提出第一个面试问题。要求：\n"
                + "1. 问题要具体、有深度；\n"
                + "2. 保持「" + style.getName() + "」的提问风格；\n"
                + "3. 只输出问题本身，不要加「第一题」等前缀，不要解释。";
    }

    private String callLlm(String systemPrompt, String userPrompt) {
        AppProperties.Ai ai = appProperties.ai();
        int timeoutSeconds = ai.timeoutSeconds() > 0 ? ai.timeoutSeconds() : 30;
        int maxRetries = ai.maxRetries() > 0 ? ai.maxRetries() : 2;

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );

        LlmRequest request = new LlmRequest(
                ai.model().isEmpty() ? "gpt-4o-mini" : ai.model(),
                messages
        );

        ClientResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .exchange()
                .block();

        if (response == null || !response.statusCode().equals(HttpStatus.OK)) {
            throw new AiServiceException("LLM 调用失败，HTTP 状态码："
                    + (response != null ? response.statusCode() : "null"));
        }

        String body = response.bodyToMono(String.class).block();
        if (body == null || body.isBlank()) {
            throw new AiServiceException("LLM 返回空响应");
        }

        return extractContent(body)
                .orElseThrow(() -> new AiServiceException("LLM 返回内容为空: " + body));
    }

    private java.util.Optional<String> extractContent(String jsonBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(jsonBody);
            com.fasterxml.jackson.databind.JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    com.fasterxml.jackson.databind.JsonNode content = message.get("content");
                    if (content != null && !content.asText().isBlank()) {
                        return java.util.Optional.of(content.asText().trim());
                    }
                }
            }
            return java.util.Optional.empty();
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }

    private InterviewAiReport parseReportFromJson(String json, int answerCount, int totalLength, ResumeContext resume) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            int totalScore = root.path("totalScore").asInt(70);
            int technicalScore = root.path("technicalScore").asInt(70);
            int communicationScore = root.path("communicationScore").asInt(70);
            int logicScore = root.path("logicScore").asInt(70);
            String summary = root.path("summary").asText("本次面试共完成 " + answerCount + " 轮回答。");
            String strengths = root.path("strengths").asText("回答能覆盖核心问题，具备一定工程意识。");
            String improvements = root.path("improvements").asText("建议进一步补充量化结果和技术取舍分析。");
            String recommendation = root.path("recommendation").asText("建议按 STAR 结构整理项目经历，并准备可追问的技术细节。");

            return new InterviewAiReport(
                    clamp(totalScore), clamp(technicalScore), clamp(communicationScore), clamp(logicScore),
                    summary, strengths, improvements, recommendation
            );
        } catch (Exception ex) {
            return fallbackReport(answerCount, totalLength, resume);
        }
    }

    private InterviewAiReport fallbackReport(int answerCount, int totalLength, ResumeContext resume) {
        int totalScore = clamp(58 + answerCount * 5 + Math.min(totalLength / 120, 12));
        int technicalScore = clamp(58 + answerCount * 5 + Math.min(totalLength / 120, 12));
        int communicationScore = clamp(60 + answerCount * 4 + Math.min(totalLength / 160, 10));
        int logicScore = clamp(58 + answerCount * 4 + Math.min(totalLength / 180, 12));
        String resumeSummary = resume != null && resume.present()
                ? "，并参考了简历摘要中的技能和项目经历" : "";
        String summary = "本次模拟面试共完成 " + answerCount + " 轮回答，整体表现为" + level(totalScore) + "。"
                + "系统基于文字回答的完整度和表达结构" + resumeSummary + "生成报告。";
        String strengths = "能够围绕问题给出基本说明，已经具备继续展开项目经历的基础。";
        String improvements = "建议进一步强化指标化表达，并在关键技术选择上补充替代方案比较。";
        String recommendation = resume != null && resume.present()
                ? "下一步建议围绕简历中的技能准备可量化的项目证据。" : "建议按 STAR 结构整理 2-3 个代表项目。";
        return new InterviewAiReport(
                totalScore, technicalScore, communicationScore, logicScore,
                summary, strengths, improvements, recommendation
        );
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String level(int score) {
        if (score >= 85) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "基本达标";
        return "需要加强";
    }

    // ---- DTOs for OpenAI-compatible API ----

    private record ChatMessage(String role, String content) {
    }

    private record LlmRequest(String model, List<ChatMessage> messages) {
    }

    public static class AiServiceException extends RuntimeException {
        public AiServiceException(String message) {
            super(message);
        }
    }
}
