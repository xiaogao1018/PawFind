-- 在已执行 003_management_and_messages.sql 的数据库中执行一次。
-- 使用 IF NOT EXISTS，兼容旧版 pawfind.sql 已经创建 AI 表的情况。
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL DEFAULT '新对话',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ai_session_user_updated (user_id, updated_at),
    CONSTRAINT fk_ai_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ai_message_session_created (session_id, created_at, id),
    CONSTRAINT fk_ai_message_session FOREIGN KEY (session_id) REFERENCES ai_chat_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
