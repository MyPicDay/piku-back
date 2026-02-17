-- =====================================================
-- V1: Baseline Schema
-- 현재 prod DB 스키마의 기준점 (Flyway baseline)
-- Flyway baseline-on-migrate: true 설정으로
-- 기존 prod DB에는 실행되지 않고, 새 DB에서만 실행됩니다.
-- =====================================================

-- Users
CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(36) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  nickname VARCHAR(20) NOT NULL,
  avatar VARCHAR(255),
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verified Email
CREATE TABLE IF NOT EXISTS verified_email (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL UNIQUE,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verification
CREATE TABLE IF NOT EXISTS verification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  code VARCHAR(255) NOT NULL,
  type VARCHAR(20) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Allowed Email
CREATE TABLE IF NOT EXISTS allowed_email (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL UNIQUE,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  token VARCHAR(512) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Characters
CREATE TABLE IF NOT EXISTS characters (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  name VARCHAR(255),
  description TEXT,
  image_url VARCHAR(255),
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Diary
CREATE TABLE IF NOT EXISTS diary (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  content TEXT,
  status VARCHAR(20) NOT NULL,
  date DATE NOT NULL,
  ai_photo_url VARCHAR(255),
  character_id BIGINT,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Photos
CREATE TABLE IF NOT EXISTS photos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  diary_id BIGINT,
  url VARCHAR(255),
  represent BOOLEAN,
  photo_order INT,
  PRIMARY KEY (id),
  CONSTRAINT fk_photos_diary FOREIGN KEY (diary_id) REFERENCES diary(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Diary Image Generation
CREATE TABLE IF NOT EXISTS diary_image_generation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  diary_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  prompt_text TEXT,
  generated_image_url VARCHAR(255),
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Feed Click
CREATE TABLE IF NOT EXISTS feed_click (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  diary_id BIGINT NOT NULL,
  clicked_at DATETIME(6) NOT NULL,
  view_duration_seconds INT,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comments
CREATE TABLE IF NOT EXISTS comments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  content TEXT,
  user_id VARCHAR(36) NOT NULL,
  diary_id BIGINT NOT NULL,
  parent_id BIGINT,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Likes
CREATE TABLE IF NOT EXISTS likes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  diary_id BIGINT NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_likes_user_diary (user_id, diary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Friend
CREATE TABLE IF NOT EXISTS friend (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  friend_id VARCHAR(36) NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Friend Request
CREATE TABLE IF NOT EXISTS friend_request (
  id BIGINT NOT NULL AUTO_INCREMENT,
  sender_id VARCHAR(36) NOT NULL,
  receiver_id VARCHAR(36) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notification
CREATE TABLE IF NOT EXISTS notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  receiver_id VARCHAR(255),
  user_id VARCHAR(36),
  type VARCHAR(30),
  diary_id BIGINT,
  is_read BOOLEAN DEFAULT FALSE,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- FCM Token
CREATE TABLE IF NOT EXISTS fcm (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  token VARCHAR(255) NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inquiry
CREATE TABLE IF NOT EXISTS inquiry (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME(6),
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Diary Metadata (Recommendation)
CREATE TABLE IF NOT EXISTS diary_metadata (
  id BIGINT NOT NULL AUTO_INCREMENT,
  diary_id BIGINT NOT NULL UNIQUE,
  primary_topic VARCHAR(50),
  mood VARCHAR(30),
  keywords VARCHAR(500),
  analyzed_at DATETIME(6),
  PRIMARY KEY (id),
  INDEX idx_diary_metadata_diary_id (diary_id),
  INDEX idx_diary_metadata_topic (primary_topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User Preference (Recommendation)
CREATE TABLE IF NOT EXISTS user_preference (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,
  topic VARCHAR(50) NOT NULL,
  score DOUBLE NOT NULL DEFAULT 0.0,
  interaction_count INT NOT NULL DEFAULT 0,
  last_interaction_at DATETIME(6),
  PRIMARY KEY (id),
  INDEX idx_user_preference_user_id (user_id),
  UNIQUE KEY uk_user_preference (user_id, topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
