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

INSERT INTO virtual_human_asset (asset_key, name, description, image_url, accent_color, badge, enabled, sort_order)
VALUES
('default-interviewer', 'AI 面试官', '基础静态面试官形象，资源不可用时保持占位展示。', NULL, '#2563eb', '通用', TRUE, 0),
('stern-panel', '冷静追问官', '节奏紧凑，关注风险、证据和边界条件。', NULL, '#be123c', '压力', TRUE, 10),
('warm-guide', '引导型导师', '语气温和，帮助候选人逐步展开经历和思路。', NULL, '#0f766e', '引导', TRUE, 20),
('tech-architect', '架构深挖官', '聚焦技术原理、系统设计和工程取舍。', NULL, '#7c3aed', '技术', TRUE, 30),
('hr-partner', '综合评估官', '关注表达、协作、复盘和长期匹配度。', NULL, '#b45309', '综合', TRUE, 40);

INSERT INTO posture_threshold_config (event_type, display_name, description, warning_threshold, critical_threshold, enabled, sort_order)
VALUES
('LOW_LIGHT', '画面亮度偏低', '平均亮度低于预警阈值时提示调整光线，低于严重阈值时标记为严重。', 45, 28, TRUE, 10),
('TOO_STILL', '画面长时间静止', '连续低变化帧数达到阈值时提示确认候选人仍在镜头前。', 5, NULL, TRUE, 20),
('FACE_MISSING', '未检测到人脸', '人脸检测不可用或未检测到人脸时上报严重事件。', 90, NULL, TRUE, 30),
('FACE_OFF_CENTER', '人脸偏离中央', '人脸中心点低于边界阈值或高于对称边界时提示调整坐姿。', 28, NULL, TRUE, 40),
('TOO_CLOSE', '距离摄像头过近', '人脸宽度占画面比例高于阈值时提示后移。', 68, NULL, TRUE, 50),
('TOO_FAR', '距离摄像头过远', '人脸宽度占画面比例低于阈值时提示靠近摄像头。', 14, NULL, TRUE, 60),
('CAMERA_UNAVAILABLE', '摄像头不可用', '浏览器不支持或用户拒绝摄像头时记录不可用状态。', 100, NULL, TRUE, 70);
