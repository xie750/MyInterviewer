ALTER TABLE interview_session
    ADD COLUMN resume_used BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN resume_summary VARCHAR(1000) NULL,
    ADD COLUMN resume_skills VARCHAR(1000) NULL,
    ADD COLUMN resume_projects VARCHAR(1000) NULL,
    ADD COLUMN resume_warnings VARCHAR(1000) NULL;
