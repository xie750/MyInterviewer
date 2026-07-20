INSERT INTO sys_user (username, password_hash, display_name, role, status)
VALUES
('user', '$2a$10$dB3VIurDWUPjGrweGqrgjOizpz.4vyZAU0iSFAcJO4PuLpE8hX/OC', '普通用户', 'USER', 'ENABLED'),
('admin', '$2a$10$dB3VIurDWUPjGrweGqrgjOizpz.4vyZAU0iSFAcJO4PuLpE8hX/OC', '管理员', 'ADMIN', 'ENABLED');

INSERT INTO job_position (name, description, tech_stack, difficulty, prompt_template, enabled, sort_order)
VALUES
('Java 后端工程师', '围绕 Spring Boot、数据库、接口设计和工程实践进行面试。', 'Java, Spring Boot, MySQL', '中级', '你是一名严谨的 Java 后端面试官，重点考察候选人的工程落地能力。', TRUE, 10),
('前端工程师', '围绕 Vue、TypeScript、组件设计和浏览器基础进行面试。', 'Vue 3, TypeScript, Vite', '中级', '你是一名注重用户体验和代码质量的前端面试官。', TRUE, 20),
('AI 应用工程师', '围绕大模型应用、提示词设计、接口编排和稳定性进行面试。', 'LLM, Prompt Engineering, REST API', '进阶', '你是一名关注 AI 应用可靠性和产品闭环的面试官。', TRUE, 30);

INSERT INTO interviewer_style (name, description, prompt_template, scenario, enabled, sort_order)
VALUES
('严厉压力面', '节奏紧凑，持续追问方案边界、失败场景和取舍依据。', '保持专业但直接，优先追问候选人回答中的模糊点、风险点和落地证据。', '用于检验抗压能力和问题拆解深度', TRUE, 10),
('温和引导面', '通过提示和澄清帮助候选人逐步展开思路。', '语气温和，先肯定有效信息，再用开放问题引导候选人补充上下文、过程和结果。', '用于基础能力评估和新人面试', TRUE, 20),
('技术深挖面', '围绕核心技术细节、架构选择和性能可靠性连续深入。', '聚焦技术原理、工程实践和权衡，请持续要求候选人解释为什么这样设计。', '用于技术岗位复试', TRUE, 30),
('HR 综合面', '关注沟通表达、团队协作、动机匹配和职业规划。', '从经历、协作、复盘和稳定性角度提问，避免深入到过窄技术细节。', '用于综合素质评估', TRUE, 40);
