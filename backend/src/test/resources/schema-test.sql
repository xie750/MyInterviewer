DROP TABLE IF EXISTS interview_report;
DROP TABLE IF EXISTS posture_event;
DROP TABLE IF EXISTS interview_message;
DROP TABLE IF EXISTS interview_session;
DROP TABLE IF EXISTS posture_threshold_config;
DROP TABLE IF EXISTS virtual_human_asset;
DROP TABLE IF EXISTS interviewer_style;
DROP TABLE IF EXISTS job_position;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_position (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(500),
    tech_stack VARCHAR(200),
    difficulty VARCHAR(40),
    prompt_template VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE interviewer_style (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(500),
    prompt_template VARCHAR(1000) NOT NULL,
    scenario VARCHAR(200),
    virtual_human_key VARCHAR(80),
    virtual_human_name VARCHAR(80),
    virtual_human_description VARCHAR(300),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    style_id BIGINT NOT NULL,
    resume_used BOOLEAN NOT NULL DEFAULT FALSE,
    resume_file_name VARCHAR(255),
    resume_summary VARCHAR(1000),
    resume_skills VARCHAR(1000),
    resume_projects VARCHAR(1000),
    resume_warnings VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    question_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE interview_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content CLOB NOT NULL,
    round_no INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE interview_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_score INT NOT NULL,
    technical_score INT NOT NULL,
    communication_score INT NOT NULL,
    logic_score INT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    strengths VARCHAR(1000) NOT NULL,
    improvements VARCHAR(1000) NOT NULL,
    recommendation VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posture_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    detail VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE virtual_human_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    accent_color VARCHAR(20),
    badge VARCHAR(40),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posture_threshold_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    warning_threshold INT,
    critical_threshold INT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
