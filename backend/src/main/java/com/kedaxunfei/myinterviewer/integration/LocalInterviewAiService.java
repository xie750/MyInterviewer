package com.kedaxunfei.myinterviewer.integration;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kedaxunfei.myinterviewer.domain.InterviewMessage;
import com.kedaxunfei.myinterviewer.domain.InterviewerStyle;
import com.kedaxunfei.myinterviewer.domain.JobPosition;
import com.kedaxunfei.myinterviewer.domain.MessageRole;

@Service
public class LocalInterviewAiService implements InterviewAiService {

    @Override
    public String generateOpeningQuestion(JobPosition position, InterviewerStyle style) {
        return generateOpeningQuestion(position, style, ResumeContext.empty());
    }

    @Override
    public String generateOpeningQuestion(JobPosition position, InterviewerStyle style, ResumeContext resume) {
        if (resume != null && resume.present()) {
            return "你正在面试" + position.getName() + "。我看到你的简历中提到"
                    + resume.skillText() + "，并有" + resume.projectText()
                    + "。请先选择其中一个最能代表你能力的项目，说明你的职责、关键设计、遇到的难点和最终结果。";
        }
        return "你正在面试" + position.getName() + "。请结合一个真实项目，介绍你在"
                + safe(position.getTechStack(), "核心技术栈")
                + "方面承担的职责、关键设计和最终结果。";
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
        String focus = chooseFocus(answer, nextQuestionNo);
        String styleHint = style.getName().contains("HR") ? "请从协作和复盘角度" : "请从技术取舍和落地细节角度";
        String resumeHint = resume != null && resume.present()
                ? "，并对照简历中的" + resume.skillText() + "说明真实参与深度"
                : "";
        return "第 " + nextQuestionNo + " 题，" + styleHint + "继续说明：" + focus + resumeHint
                + "。请尽量给出背景、你的动作、遇到的问题和可验证结果。";
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
        int keywordHits = countKeywordHits(answers);

        int technicalScore = clamp(58 + answerCount * 5 + keywordHits * 3 + Math.min(totalLength / 120, 12));
        int communicationScore = clamp(60 + answerCount * 4 + Math.min(totalLength / 160, 10));
        int logicScore = clamp(58 + keywordHits * 2 + answerCount * 4 + Math.min(totalLength / 180, 12));
        int totalScore = Math.round((technicalScore + communicationScore + logicScore) / 3.0f);

        String resumeSummary = resume != null && resume.present()
                ? "，并参考了简历摘要中的技能和项目经历"
                : "";
        String summary = "本次" + position.getName() + "模拟面试共完成 " + answerCount
                + " 轮回答，整体表现为" + level(totalScore) + "。系统基于文字回答的完整度、技术要点覆盖和表达结构"
                + resumeSummary + "生成报告。";
        String strengths = keywordHits > 2
                ? "回答能覆盖需求、架构、性能、测试或风险等关键工程要素，具备一定落地意识。"
                : "能够围绕问题给出基本说明，已经具备继续展开项目经历的基础。";
        String improvements = totalLength < 240
                ? "回答偏简略，建议补充具体场景、约束条件、权衡依据、失败处理和量化结果。"
                : "建议进一步强化指标化表达，并在关键技术选择上补充替代方案比较。";
        String recommendation = resume != null && resume.present()
                ? "下一步建议围绕简历中的 " + resume.skillText()
                + " 准备可量化的项目证据，并补充每个项目的职责边界、技术取舍和结果指标。"
                : "下一步可按 STAR 结构整理 2-3 个代表项目，并针对 "
                + safe(position.getTechStack(), position.getName()) + " 准备可追问的技术细节。";
        return new InterviewAiReport(
                totalScore,
                technicalScore,
                communicationScore,
                logicScore,
                summary,
                strengths,
                improvements,
                recommendation
        );
    }

    private String chooseFocus(String answer, int nextQuestionNo) {
        String lower = answer.toLowerCase();
        if (lower.contains("性能") || lower.contains("缓存") || lower.contains("并发")) {
            return "你提到的性能或并发处理具体如何验证，瓶颈出现时有哪些备选方案";
        }
        if (lower.contains("测试") || lower.contains("质量") || lower.contains("上线")) {
            return "你如何保障方案上线质量，出现异常时如何回滚和定位";
        }
        if (lower.contains("鉴权") || lower.contains("权限") || lower.contains("安全")) {
            return "权限边界如何设计，普通用户越权访问时系统如何防护";
        }
        return switch (nextQuestionNo % 3) {
            case 0 -> "这个方案最大的风险是什么，你当时如何降低风险";
            case 1 -> "如果访问量或数据量扩大 10 倍，你会优先调整哪里";
            default -> "你在这个项目中做过哪些取舍，为什么没有选择其他方案";
        };
    }

    private int countKeywordHits(List<String> answers) {
        String text = String.join(" ", answers);
        String[] keywords = {"架构", "数据库", "缓存", "性能", "测试", "权限", "异常", "监控", "指标", "复盘"};
        int hits = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                hits++;
            }
        }
        return hits;
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String level(int score) {
        if (score >= 85) {
            return "优秀";
        }
        if (score >= 75) {
            return "良好";
        }
        if (score >= 60) {
            return "基本达标";
        }
        return "需要加强";
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
