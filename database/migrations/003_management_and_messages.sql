-- 在已执行 002_profiles_and_images.sql 的数据库中执行一次。
ALTER TABLE pet_lost
    ADD COLUMN visibility_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    ADD COLUMN moderation_reason VARCHAR(255) DEFAULT NULL,
    ADD KEY idx_lost_public (visibility_status, status, id);
ALTER TABLE pet_found
    ADD COLUMN visibility_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    ADD COLUMN moderation_reason VARCHAR(255) DEFAULT NULL,
    ADD KEY idx_found_public (visibility_status, status, id);

CREATE TABLE user_notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    content VARCHAR(300) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notice_recipient (recipient_id, is_read, id),
    CONSTRAINT fk_notice_recipient FOREIGN KEY (recipient_id) REFERENCES sys_user(id),
    CONSTRAINT fk_notice_actor FOREIGN KEY (actor_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE direct_message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_dm_sender_recipient (sender_id, recipient_id, id),
    KEY idx_dm_recipient_read (recipient_id, is_read, id),
    CONSTRAINT fk_dm_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id),
    CONSTRAINT fk_dm_recipient FOREIGN KEY (recipient_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE record_comment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pet_lost_id BIGINT DEFAULT NULL,
    pet_found_id BIGINT DEFAULT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_record_comment_lost (pet_lost_id, id),
    KEY idx_record_comment_found (pet_found_id, id),
    CONSTRAINT chk_record_comment_target CHECK (
        (pet_lost_id IS NOT NULL) + (pet_found_id IS NOT NULL) = 1
    ),
    CONSTRAINT fk_record_comment_lost FOREIGN KEY (pet_lost_id) REFERENCES pet_lost(id),
    CONSTRAINT fk_record_comment_found FOREIGN KEY (pet_found_id) REFERENCES pet_found(id),
    CONSTRAINT fk_record_comment_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
