/*
 Navicat Premium Dump SQL

 Source Server         : PawFind-Local
 Source Server Type    : MySQL
 Source Server Version : 80413 (8.4.13)
 Source Host           : localhost:3306
 Source Schema         : pawfind

 Target Server Type    : MySQL
 Target Server Version : 80413 (8.4.13)
 File Encoding         : 65001

 Date: 06/11/2026 15:08:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS user_notification;
DROP TABLE IF EXISTS direct_message;
DROP TABLE IF EXISTS record_comment;

DROP TABLE IF EXISTS `file_attachment`;
DROP TABLE IF EXISTS `pet_profile`;

-- ----------------------------
-- Table structure for admin_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `admin_operation_log`;
CREATE TABLE `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `admin_id` bigint NOT NULL COMMENT '管理员用户ID',
  `target_type` varchar(30) NOT NULL COMMENT '操作对象类型',
  `target_id` bigint NOT NULL COMMENT '操作对象ID',
  `action` varchar(30) NOT NULL COMMENT '操作类型',
  `reason` varchar(255) DEFAULT NULL COMMENT '操作原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_log_admin_created` (`admin_id`,`created_at`),
  KEY `idx_admin_log_target` (`target_type`,`target_id`),
  CONSTRAINT `fk_admin_log_admin` FOREIGN KEY (`admin_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作日志表';

-- ----------------------------
-- Table structure for ai_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `title` varchar(100) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_session_user_updated` (`user_id`,`updated_at`),
  CONSTRAINT `fk_ai_session_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI聊天会话表';

-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '所属会话ID',
  `role` varchar(20) NOT NULL COMMENT '消息角色：USER或ASSISTANT',
  `content` text NOT NULL COMMENT '消息内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_message_session_created` (`session_id`,`created_at`,`id`),
  CONSTRAINT `fk_ai_message_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI聊天消息表';

-- ----------------------------
-- Table structure for community_comment
-- ----------------------------
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` bigint NOT NULL COMMENT '所属帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论者用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `visibility_status` varchar(20) NOT NULL DEFAULT 'VISIBLE' COMMENT '显示状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_comment_user` (`user_id`),
  KEY `idx_comment_post_created` (`post_id`,`created_at`),
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`id`),
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区评论表';

-- ----------------------------
-- Table structure for community_post
-- ----------------------------
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `author_id` bigint NOT NULL COMMENT '发布者ID',
  `title` varchar(120) NOT NULL COMMENT '帖子标题',
  `content` text NOT NULL COMMENT '帖子正文',
  `category` varchar(20) NOT NULL COMMENT '帖子分类',
  `audit_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
  `visibility_status` varchar(20) NOT NULL DEFAULT 'VISIBLE' COMMENT '显示状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `moderation_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_post_public` (`audit_status`,`visibility_status`,`created_at`),
  KEY `idx_post_author` (`author_id`),
  CONSTRAINT `fk_community_post_author` FOREIGN KEY (`author_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社区帖子表';

-- ----------------------------
-- Table structure for pet_clue
-- ----------------------------
DROP TABLE IF EXISTS `pet_clue`;
CREATE TABLE `pet_clue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '线索ID',
  `lost_id` bigint NOT NULL COMMENT '关联的走失记录ID',
  `user_id` bigint NOT NULL COMMENT '线索提交者ID',
  `content` text NOT NULL COMMENT '线索内容',
  `contact_info` varchar(100) DEFAULT NULL,
  `sighting_time` datetime DEFAULT NULL COMMENT '目击时间',
  `sighting_location` varchar(255) DEFAULT NULL COMMENT '目击地点',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '线索状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_clue_lost_id` (`lost_id`),
  KEY `idx_pet_clue_user_id` (`user_id`),
  CONSTRAINT `fk_pet_clue_lost` FOREIGN KEY (`lost_id`) REFERENCES `pet_lost` (`id`),
  CONSTRAINT `fk_pet_clue_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物目击线索表';

-- ----------------------------
-- Table structure for pet_found
-- ----------------------------
DROP TABLE IF EXISTS `pet_found`;
CREATE TABLE `pet_found` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发现记录ID',
  `user_id` bigint NOT NULL COMMENT '发布者用户ID',
  `pet_name` varchar(50) DEFAULT NULL,
  `species` varchar(30) NOT NULL COMMENT '宠物种类',
  `description` text COMMENT '宠物特征描述',
  `found_time` datetime NOT NULL COMMENT '发现时间',
  `found_location` varchar(255) NOT NULL COMMENT '发现地点',
  `status` varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT '记录状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_pet_found_user` (`user_id`),
  KEY `idx_pet_found_status_created` (`status`,`created_at`),
  CONSTRAINT `fk_pet_found_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物发现记录表';

-- ----------------------------
-- Table structure for pet_lost
-- ----------------------------
DROP TABLE IF EXISTS `pet_lost`;
CREATE TABLE `pet_lost` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '走失记录ID',
  `user_id` bigint NOT NULL COMMENT '发布者用户ID',
  `pet_name` varchar(50) DEFAULT NULL COMMENT '宠物名字',
  `species` varchar(30) NOT NULL COMMENT '宠物种类',
  `description` text COMMENT '宠物特征描述',
  `lost_time` datetime NOT NULL COMMENT '走失时间',
  `lost_location` varchar(255) NOT NULL COMMENT '走失地点',
  `status` varchar(20) NOT NULL DEFAULT 'LOOKING' COMMENT '寻找状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_pet_lost_user` (`user_id`),
  KEY `idx_pet_lost_status_created` (`status`,`created_at`),
  CONSTRAINT `fk_pet_lost_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物走失记录表';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
  `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE file_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pet_lost_id BIGINT DEFAULT NULL,
    pet_found_id BIGINT DEFAULT NULL,
    community_post_id BIGINT DEFAULT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_attachment_pet_created (pet_lost_id, created_at, id),
    KEY idx_attachment_found (pet_found_id, id),
    KEY idx_attachment_post (community_post_id, id),
    CONSTRAINT fk_attachment_found FOREIGN KEY (pet_found_id) REFERENCES pet_found(id),
    CONSTRAINT fk_attachment_post FOREIGN KEY (community_post_id) REFERENCES community_post(id),
    CONSTRAINT chk_attachment_owner CHECK (
        (pet_lost_id IS NOT NULL) + (pet_found_id IS NOT NULL) + (community_post_id IS NOT NULL) = 1
    ),
    CONSTRAINT fk_attachment_pet_lost FOREIGN KEY (pet_lost_id) REFERENCES pet_lost(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

SET FOREIGN_KEY_CHECKS = 1;
