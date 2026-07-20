package com.kedaxunfei.myinterviewer.dto;

import java.util.List;

public record ResumeParseResponse(
        String fileName,
        String summary,
        List<String> skills,
        List<String> projects,
        List<String> warnings,
        Integer extractedTextLength
) {

    public ResumeContextRequest toContextRequest() {
        return new ResumeContextRequest(summary, skills, projects, warnings);
    }
}
