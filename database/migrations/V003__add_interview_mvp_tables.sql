CREATE TABLE IF NOT EXISTS interviewer_style (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NULL,
    prompt_template VARCHAR(1000) NOT NULL,
    scenario VARCHAR(200) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_interviewer_style_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    style_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    question_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_interview_session_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    INDEX idx_interview_session_user_id (user_id),
    INDEX idx_interview_session_status (status)
);

CREATE TABLE IF NOT EXISTS interview_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    round_no INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_interview_message_role CHECK (role IN ('ASSISTANT', 'USER')),
    INDEX idx_interview_message_session_id (session_id)
);

CREATE TABLE IF NOT EXISTS interview_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total_score INT NOT NULL,
    technical_score INT NOT NULL,
    communication_score INT NOT NULL,
    logic_score INT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    strengths VARCHAR(1000) NOT NULL,
    improvements VARCHAR(1000) NOT NULL,
    recommendation VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_interview_report_session_id UNIQUE (session_id),
    INDEX idx_interview_report_user_id (user_id)
);

INSERT INTO interviewer_style (name, description, prompt_template, scenario, enabled, sort_order)
SELECT '严厉压力面', '节奏紧凑，持续追问方案边界、失败场景和取舍依据。', '保持专业但直接，优先追问候选人回答中的模糊点、风险点和落地证据。', '用于检验抗压能力和问题拆解深度', TRUE, 10
WHERE NOT EXISTS (SELECT 1 FROM interviewer_style WHERE name = '严厉压力面');

INSERT INTO interviewer_style (name, description, prompt_template, scenario, enabled, sort_order)
SELECT '温和引导面', '通过提示和澄清帮助候选人逐步展开思路。', '语气温和，先肯定有效信息，再用开放问题引导候选人补充上下文、过程和结果。', '用于基础能力评估和新人面试', TRUE, 20
WHERE NOT EXISTS (SELECT 1 FROM interviewer_style WHERE name = '温和引导面');

INSERT INTO interviewer_style (name, description, prompt_template, scenario, enabled, sort_order)
SELECT '技术深挖面', '围绕核心技术细节、架构选择和性能可靠性连续深入。', '聚焦技术原理、工程实践和权衡，请持续要求候选人解释为什么这样设计。', '用于技术岗位复试', TRUE, 30
WHERE NOT EXISTS (SELECT 1 FROM interviewer_style WHERE name = '技术深挖面');

INSERT INTO interviewer_style (name, description, prompt_template, scenario, enabled, sort_order)
SELECT 'HR 综合面', '关注沟通表达、团队协作、动机匹配和职业规划。', '从经历、协作、复盘和稳定性角度提问，避免深入到过窄技术细节。', '用于综合素质评估', TRUE, 40
WHERE NOT EXISTS (SELECT 1 FROM interviewer_style WHERE name = 'HR 综合面');
