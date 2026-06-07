-- =====================================================
-- V1: Baseline Schema
-- Flyway baseline-on-migrate: true 설정으로
-- 기존 DB에는 실행되지 않고, 새 DB에서만 실행됩니다.
-- =====================================================

-- =====================================================
-- 1. 독립 테이블 (FK 의존성 없음)
-- =====================================================

-- Users
CREATE TABLE users (
  id varchar(36) NOT NULL,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  avatar varchar(255) DEFAULT NULL,
  email varchar(255) NOT NULL,
  nickname varchar(255) NOT NULL,
  password varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK6dotkott2kjsp8vw4d0m25fb7 (email),
  UNIQUE KEY UK2ty1xmrrgtn89xt7kyxx6ta7h (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Allowed Email
CREATE TABLE allowed_email (
  id bigint NOT NULL AUTO_INCREMENT,
  domain varchar(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UKntr751a2j1hauaw2vqndmcw1m (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Verified Email
CREATE TABLE verified_email (
  id bigint NOT NULL AUTO_INCREMENT,
  email varchar(255) NOT NULL,
  type enum('PASSWORD_RESET','SIGN_UP') NOT NULL,
  used bit(1) NOT NULL,
  verified_at datetime(6) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Verification
CREATE TABLE verification (
  id bigint NOT NULL AUTO_INCREMENT,
  code varchar(255) NOT NULL,
  email varchar(255) NOT NULL,
  expires_at datetime(6) NOT NULL,
  type enum('PASSWORD_RESET','SIGN_UP') NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Refresh Tokens
CREATE TABLE refresh_tokens (
  refresh_key varchar(255) NOT NULL,
  refresh_token varchar(255) DEFAULT NULL,
  user_id varchar(255) DEFAULT NULL,
  PRIMARY KEY (refresh_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Diary Image Generation
CREATE TABLE diary_image_generation (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  diary_id bigint DEFAULT NULL,
  file_path varchar(255) DEFAULT NULL,
  prompt text NOT NULL,
  user_id varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Feed Click
CREATE TABLE feed_click (
  id bigint NOT NULL AUTO_INCREMENT,
  diary_id bigint DEFAULT NULL,
  user_id varchar(36) NOT NULL,
  view_duration_seconds int DEFAULT NULL,
  clicked_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Friend
CREATE TABLE friend (
  user_id_1 varchar(255) NOT NULL,
  user_id_2 varchar(255) NOT NULL,
  created_at varchar(255) DEFAULT NULL,
  PRIMARY KEY (user_id_1,user_id_2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Friend Request
CREATE TABLE friend_request (
  from_user_id varchar(255) NOT NULL,
  to_user_id varchar(255) NOT NULL,
  PRIMARY KEY (from_user_id,to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Likes
CREATE TABLE likes (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  diary_id bigint NOT NULL,
  user_id varchar(36) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_diary (user_id,diary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- FCM
CREATE TABLE fcm (
  id bigint NOT NULL AUTO_INCREMENT,
  device_id varchar(255) DEFAULT NULL,
  token varchar(255) DEFAULT NULL,
  user_id varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Diary Metadata
CREATE TABLE diary_metadata (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  analyzed_at datetime(6) DEFAULT NULL,
  diary_id bigint NOT NULL,
  primary_topic varchar(50) DEFAULT NULL,
  quality_score double DEFAULT NULL,
  topics text,
  PRIMARY KEY (id),
  UNIQUE KEY UKs2hfjq3khj3n0hs41fb92qw90 (diary_id),
  KEY idx_diary_metadata_diary (diary_id),
  KEY idx_diary_metadata_topic (primary_topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- User Preference
CREATE TABLE user_preference (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  last_updated_at datetime(6) DEFAULT NULL,
  topic_affinities text,
  user_id varchar(36) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UKs5oeayykfc7bpkpdwyrffwcqx (user_id),
  KEY idx_user_preference_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- 2. Users 참조 테이블
-- =====================================================

-- Diary
CREATE TABLE diary (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  content varchar(500) DEFAULT NULL,
  date date DEFAULT NULL,
  status enum('ANONYMOUS','FRIENDS','PRIVATE','PUBLIC') DEFAULT NULL,
  user_id varchar(36) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FK74rd0bn5raxejw2ukenelbdmt (user_id),
  CONSTRAINT FK74rd0bn5raxejw2ukenelbdmt FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Characters
CREATE TABLE characters (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  image_url varchar(255) DEFAULT NULL,
  type enum('AI_GENERATED','FIXED') DEFAULT NULL,
  user_id varchar(36) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FK27yx743bsnnsqplnjhk5yf224 (user_id),
  CONSTRAINT FK27yx743bsnnsqplnjhk5yf224 FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Inquiry
CREATE TABLE inquiry (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  content varchar(1000) NOT NULL,
  image_url varchar(255) DEFAULT NULL,
  user_id varchar(36) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FKray80kmwpjpjb91ime7ogijjr (user_id),
  CONSTRAINT FKray80kmwpjpjb91ime7ogijjr FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- 3. Diary + Users 참조 테이블
-- =====================================================

-- Photos (diary FK 유지: Photo 엔티티가 @ManyToOne 사용)
CREATE TABLE photos (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  photo_order int DEFAULT NULL,
  represent bit(1) DEFAULT NULL,
  url varchar(255) DEFAULT NULL,
  diary_id bigint DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FKfubwq82xm313a946bcxee24ip (diary_id),
  CONSTRAINT FKfubwq82xm313a946bcxee24ip FOREIGN KEY (diary_id) REFERENCES diary (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Notification
CREATE TABLE notification (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  is_read bit(1) DEFAULT NULL,
  receiver_id varchar(255) DEFAULT NULL,
  type enum('COMMENT','FRIEND_ACCEPT','FRIEND_DIARY','FRIEND_REQUEST','REPLY') DEFAULT NULL,
  diary_id bigint DEFAULT NULL,
  user_id varchar(36) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FKey4f6r0yoeuhly3v5hxh1tajd (diary_id),
  KEY FKnk4ftb5am9ubmkv1661h15ds9 (user_id),
  CONSTRAINT FKey4f6r0yoeuhly3v5hxh1tajd FOREIGN KEY (diary_id) REFERENCES diary (id),
  CONSTRAINT FKnk4ftb5am9ubmkv1661h15ds9 FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Comments (parent_id 자기참조 FK 유지: Comment 엔티티가 @ManyToOne 사용)
CREATE TABLE comments (
  id bigint NOT NULL AUTO_INCREMENT,
  created_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  content varchar(255) DEFAULT NULL,
  diary_id bigint DEFAULT NULL,
  parent_id bigint DEFAULT NULL,
  user_id varchar(36) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY FKjrqn5w090e67v7yrlqjjadxb7 (diary_id),
  KEY FKlri30okf66phtcgbe5pok7cc0 (parent_id),
  KEY FK8omq0tc18jd43bu5tjh6jvraq (user_id),
  CONSTRAINT FK8omq0tc18jd43bu5tjh6jvraq FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT FKjrqn5w090e67v7yrlqjjadxb7 FOREIGN KEY (diary_id) REFERENCES diary (id),
  CONSTRAINT FKlri30okf66phtcgbe5pok7cc0 FOREIGN KEY (parent_id) REFERENCES comments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
