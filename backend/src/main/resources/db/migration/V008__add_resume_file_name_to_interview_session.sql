ALTER TABLE interview_session
    ADD COLUMN resume_file_name VARCHAR(255) NULL AFTER resume_used;
