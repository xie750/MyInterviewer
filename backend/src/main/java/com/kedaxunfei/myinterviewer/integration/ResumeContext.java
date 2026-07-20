package com.kedaxunfei.myinterviewer.integration;

import java.util.List;

public record ResumeContext(
        String summary,
        List<String> skills,
        List<String> projects,
        List<String> warnings
) {

    public static ResumeContext empty() {
        return new ResumeContext(null, List.of(), List.of(), List.of());
    }

    public boolean present() {
        return hasText(summary) || !skills.isEmpty() || !projects.isEmpty();
    }

    public String skillText() {
        return skills.isEmpty() ? "简历未提取到明确技能关键词" : String.join("、", skills);
    }

    public String projectText() {
        return projects.isEmpty() ? "简历未提取到明确项目经历" : String.join("；", projects);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
