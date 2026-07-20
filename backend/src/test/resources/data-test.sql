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

INSERT INTO posture_event (session_id, user_id, event_type, severity, score, detail, occurred_at)
VALUES
(100, 2, 'LOW_LIGHT', 'WARNING', 62, '摄像头画面亮度偏低，建议调整光线', CURRENT_TIMESTAMP),
(100, 2, 'FACE_OFF_CENTER', 'WARNING', 70, '人脸偏离画面中央，建议调整坐姿', CURRENT_TIMESTAMP);

INSERT INTO virtual_human_asset (asset_key, name, description, image_url, accent_color, badge, enabled, sort_order)
VALUES
('default-interviewer', 'AI 面试官', '基础静态面试官形象，资源不可用时保持占位展示。', NULL, '#2563eb', '通用', TRUE, 0),
('stern-panel', '冷静追问官', '节奏紧凑，关注风险、证据和边界条件。', NULL, '#be123c', '压力', TRUE, 10),
('warm-guide', '引导型导师', '语气温和，帮助候选人逐步展开经历和思路。', NULL, '#0f766e', '引导', TRUE, 20);

INSERT INTO posture_threshold_config (event_type, display_name, description, warning_threshold, critical_threshold, enabled, sort_order)
VALUES
('LOW_LIGHT', '画面亮度偏低', '平均亮度低于预警阈值时提示调整光线，低于严重阈值时标记为严重。', 45, 28, TRUE, 10),
('TOO_STILL', '画面长时间静止', '连续低变化帧数达到阈值时提示确认候选人仍在镜头前。', 5, NULL, TRUE, 20),
('FACE_MISSING', '未检测到人脸', '人脸检测不可用或未检测到人脸时上报严重事件。', 90, NULL, TRUE, 30),
('FACE_OFF_CENTER', '人脸偏离中央', '人脸中心点低于边界阈值或高于对称边界时提示调整坐姿。', 28, NULL, TRUE, 40),
('TOO_CLOSE', '距离摄像头过近', '人脸宽度占画面比例高于阈值时提示后移。', 68, NULL, TRUE, 50),
('TOO_FAR', '距离摄像头过远', '人脸宽度占画面比例低于阈值时提示靠近摄像头。', 14, NULL, TRUE, 60),
('CAMERA_UNAVAILABLE', '摄像头不可用', '浏览器不支持或用户拒绝摄像头时记录不可用状态。', 100, NULL, TRUE, 70);
