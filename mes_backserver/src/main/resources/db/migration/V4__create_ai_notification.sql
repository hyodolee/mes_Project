CREATE TABLE IF NOT EXISTS MES_AI_NOTIFICATION (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200)  NOT NULL,
    message       TEXT          NOT NULL,
    severity      VARCHAR(20)   NOT NULL DEFAULT 'WARNING',
    source_ref    VARCHAR(100),
    is_read       TINYINT(1)    NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_notification_read (is_read),
    INDEX idx_ai_notification_created (created_at DESC)
);
