CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_sys_user_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_sys_user_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

INSERT INTO sys_user (username, password_hash, display_name, role, status)
SELECT 'admin', '$2a$10$dB3VIurDWUPjGrweGqrgjOizpz.4vyZAU0iSFAcJO4PuLpE8hX/OC', '管理员', 'ADMIN', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user (username, password_hash, display_name, role, status)
SELECT 'user', '$2a$10$dB3VIurDWUPjGrweGqrgjOizpz.4vyZAU0iSFAcJO4PuLpE8hX/OC', '普通用户', 'USER', 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'user');
