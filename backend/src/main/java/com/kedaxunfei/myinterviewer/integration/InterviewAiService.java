package com.kedaxunfei.myinterviewer.integration;

import java.util.List;

import com.kedaxunfei.myinterviewer.domain.InterviewMessage;
import com.kedaxunfei.myinterviewer.domain.InterviewerStyle;
import com.kedaxunfei.myinterviewer.domain.JobPosition;

public interface InterviewAiService {

    String generateOpeningQuestion(JobPosition position, InterviewerStyle style);

    String generateOpeningQuestion(JobPosition position, InterviewerStyle style, ResumeContext resume);

    String generateFollowUpQuestion(
            JobPosition position,
            InterviewerStyle style,
            ResumeContext resume,
            List<InterviewMessage> history,
            String answer,
            int nextQuestionNo
    );

    InterviewAiReport generateReport(JobPosition position, InterviewerStyle style, List<InterviewMessage> history);

    InterviewAiReport generateReport(
            JobPosition position,
            InterviewerStyle style,
            ResumeContext resume,
            List<InterviewMessage> history
    );
}
