CREATE TABLE posture_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    score INT NOT NULL,
    detail VARCHAR(500),
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_posture_event_session_time (session_id, occurred_at),
    INDEX idx_posture_event_user_time (user_id, occurred_at)
);
