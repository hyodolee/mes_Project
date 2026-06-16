CREATE TABLE IF NOT EXISTS MES_AI_RAG_DOCUMENT (
    document_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_file_name  VARCHAR(255) NOT NULL,
    stored_file_name    VARCHAR(255) NOT NULL,
    stored_file_path    VARCHAR(500) NOT NULL,
    content_type        VARCHAR(120),
    document_type       VARCHAR(50)  NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    chunk_count         INT          NOT NULL DEFAULT 0,
    error_message       TEXT,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_rag_document_status (status),
    INDEX idx_ai_rag_document_type (document_type),
    INDEX idx_ai_rag_document_created (created_at DESC)
);
