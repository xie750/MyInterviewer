INSERT INTO sys_user (username, password_hash, display_name, role, status)
VALUES
('user', '$2a$10$dB3VIurDWUPjGrweGqrgjOizpz.4vyZAU0iSFAcJO4PuLpE8hX/OC', '普通用户', 'USER', 'ENABLED'),
('admin', '$2a$10$dB3VIurDWUPjGrweGqrgjOizpz.4vyZAU0iSFAcJO4PuLpE8hX/OC', '管理员', 'ADMIN', 'ENABLED'),
('disabled', '$2a$10$dB3VIurDWUPjGrweGqrgjOizpz.4vyZAU0iSFAcJO4PuLpE8hX/OC', '禁用用户', 'USER', 'DISABLED');

INSERT INTO job_position (name, description, tech_stack, difficulty, prompt_template, enabled, sort_order)
VALUES
('Java 后端工程师', 'Spring Boot 和数据库基础面试', 'Java, Spring Boot, MySQL', '中级', '考察 Java 后端基础和工程实践', TRUE, 10),
('前端工程师', 'Vue 和 TypeScript 基础面试', 'Vue 3, TypeScript, Vite', '中级', '考察前端工程能力', TRUE, 20),
('停用岗位', '管理员可见，普通用户不可见', 'Legacy', '初级', '停用岗位不进入用户选择列表', FALSE, 30);

INSERT INTO interviewer_style (name, description, prompt_template, scenario, virtual_human_key, virtual_human_name, virtual_human_description, enabled, sort_order)
VALUES
('严厉压力面', '持续追问边界和风险', '直接追问模糊点和落地证据', '抗压评估', 'stern-panel', '冷静追问官', '节奏紧凑，关注风险、证据和边界条件。', TRUE, 10),
('温和引导面', '逐步引导候选人展开', '先肯定再引导补充上下文', '基础评估', 'warm-guide', '引导型导师', '语气温和，帮助候选人逐步展开经历和思路。', TRUE, 20),
('停用风格', '普通用户不可见', '停用风格不进入选择列表', '测试', 'default-interviewer', 'AI 面试官', '停用风格不进入普通用户选择列表。', FALSE, 30);

INSERT INTO interview_session (id, user_id, position_id, style_id, status, question_count, started_at, ended_at)
VALUES (100, 2, 1, 1, 'COMPLETED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO interview_message (session_id, role, content, round_no)
VALUES
(100, 'ASSISTANT', '请介绍一次你设计 Spring Boot 接口的经历。', 1),
(100, 'USER', '我会从接口契约、鉴权、异常处理和测试几个方面说明。', 1);

INSERT INTO interview_report (session_id, user_id, total_score, technical_score, communication_score, logic_score, summary, strengths, improvements, recommendation)
VALUES (100, 2, 78, 80, 76, 78, '回答覆盖接口设计关键点。', '能提到鉴权和测试。', '可以补充更多性能和异常场景。', '建议继续加强复杂业务建模练习。');
