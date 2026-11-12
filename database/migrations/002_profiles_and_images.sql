-- 在已执行 001_page_api.sql 的数据库中执行一次。
ALTER TABLE file_attachment
    MODIFY COLUMN pet_lost_id BIGINT DEFAULT NULL,
    ADD COLUMN pet_found_id BIGINT DEFAULT NULL AFTER pet_lost_id,
    ADD COLUMN community_post_id BIGINT DEFAULT NULL AFTER pet_found_id,
    ADD KEY idx_attachment_found (pet_found_id, id),
    ADD KEY idx_attachment_post (community_post_id, id),
    ADD CONSTRAINT fk_attachment_found FOREIGN KEY (pet_found_id) REFERENCES pet_found(id),
    ADD CONSTRAINT fk_attachment_post FOREIGN KEY (community_post_id) REFERENCES community_post(id),
    ADD CONSTRAINT chk_attachment_owner CHECK (
        (pet_lost_id IS NOT NULL) + (pet_found_id IS NOT NULL) + (community_post_id IS NOT NULL) = 1
    );

CREATE TABLE pet_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    species VARCHAR(30) NOT NULL,
    breed VARCHAR(50) DEFAULT NULL,
    birth_date DATE DEFAULT NULL,
    description VARCHAR(2000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_profile_user (user_id, id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
