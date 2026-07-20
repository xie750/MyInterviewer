CREATE TABLE IF NOT EXISTS job_position (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NULL,
    tech_stack VARCHAR(200) NULL,
    difficulty VARCHAR(40) NULL,
    prompt_template VARCHAR(1000) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_job_position_name UNIQUE (name)
);

INSERT INTO job_position (name, description, tech_stack, difficulty, prompt_template, enabled, sort_order)
SELECT 'Java 后端工程师', '围绕 Spring Boot、数据库、接口设计和工程实践进行面试。', 'Java, Spring Boot, MySQL', '中级', '你是一名严谨的 Java 后端面试官，重点考察候选人的工程落地能力。', TRUE, 10
WHERE NOT EXISTS (SELECT 1 FROM job_position WHERE name = 'Java 后端工程师');

INSERT INTO job_position (name, description, tech_stack, difficulty, prompt_template, enabled, sort_order)
SELECT '前端工程师', '围绕 Vue、TypeScript、组件设计和浏览器基础进行面试。', 'Vue 3, TypeScript, Vite', '中级', '你是一名注重用户体验和代码质量的前端面试官。', TRUE, 20
WHERE NOT EXISTS (SELECT 1 FROM job_position WHERE name = '前端工程师');

INSERT INTO job_position (name, description, tech_stack, difficulty, prompt_template, enabled, sort_order)
SELECT 'AI 应用工程师', '围绕大模型应用、提示词设计、接口编排和稳定性进行面试。', 'LLM, Prompt Engineering, REST API', '进阶', '你是一名关注 AI 应用可靠性和产品闭环的面试官。', TRUE, 30
WHERE NOT EXISTS (SELECT 1 FROM job_position WHERE name = 'AI 应用工程师');
