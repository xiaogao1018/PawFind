-- 对旧版 pawfind.sql 初始化的数据库执行一次。保留现有记录。
ALTER TABLE pet_found ADD COLUMN pet_name VARCHAR(50) DEFAULT NULL AFTER user_id;
ALTER TABLE pet_clue ADD COLUMN contact_info VARCHAR(100) DEFAULT NULL AFTER content;
ALTER TABLE community_post
    MODIFY COLUMN title VARCHAR(120) NOT NULL COMMENT '帖子标题',
    ADD COLUMN moderation_reason VARCHAR(255) DEFAULT NULL AFTER visibility_status;

CREATE TABLE IF NOT EXISTS file_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pet_lost_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_attachment_pet_created (pet_lost_id, created_at, id),
    CONSTRAINT fk_attachment_pet_lost FOREIGN KEY (pet_lost_id) REFERENCES pet_lost(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
