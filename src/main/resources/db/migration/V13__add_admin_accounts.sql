CREATE TABLE admins (
  id CHAR(36) NOT NULL,
  email VARCHAR(255) NOT NULL,
  login_id VARCHAR(15) NULL,
  nickname VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  temporary_password_hash VARCHAR(255) NULL,
  temporary_password_issued_at DATETIME(6) NULL,
  temporary_credential_expires_at DATETIME(6) NULL,
  password_hash VARCHAR(255) NULL,
  password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
  otp_registered BOOLEAN NOT NULL DEFAULT FALSE,
  otp_registration_required BOOLEAN NOT NULL DEFAULT TRUE,
  pending_otp_secret TEXT NULL,
  otp_secret TEXT NULL,
  login_failure_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME(6) NULL,
  otp_failure_count INT NOT NULL DEFAULT 0,
  otp_blocked_until DATETIME(6) NULL,
  last_login_at DATETIME(6) NULL,
  created_at DATETIME(6) NULL,
  updated_at DATETIME(6) NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admins_email (email),
  UNIQUE KEY uk_admins_login_id (login_id),
  KEY idx_admins_status_created_at (status, created_at),
  KEY idx_admins_temporary_credential_expires_at (temporary_credential_expires_at)
);

CREATE TABLE admin_roles (
  admin_id CHAR(36) NOT NULL,
  role VARCHAR(30) NOT NULL,
  PRIMARY KEY (admin_id),
  KEY idx_admin_roles_role (role),
  CONSTRAINT fk_admin_roles_admin_id
    FOREIGN KEY (admin_id)
    REFERENCES admins (id)
);

CREATE TABLE admin_sessions (
  id varchar(36) NOT NULL,
  admin_id varchar(36) NOT NULL,
  status varchar(30) NOT NULL,
  current_refresh_token_hash varchar(64) DEFAULT NULL,
  absolute_expires_at datetime(6) NOT NULL,
  idle_expires_at datetime(6) NOT NULL,
  last_rotated_at datetime(6) NOT NULL,
  revoked_at datetime(6) DEFAULT NULL,
  reuse_detected_at datetime(6) DEFAULT NULL,
  created_at datetime(6) DEFAULT NULL,
  updated_at datetime(6) DEFAULT NULL,
  deleted_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_admin_sessions_admin_status (admin_id, status),
  KEY idx_admin_sessions_current_refresh_token_hash (current_refresh_token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_refresh_tokens (
  token_hash varchar(64) NOT NULL,
  session_id varchar(36) NOT NULL,
  admin_id varchar(36) NOT NULL,
  status varchar(30) NOT NULL,
  issued_at datetime(6) NOT NULL,
  expires_at datetime(6) NOT NULL,
  rotated_at datetime(6) DEFAULT NULL,
  revoked_at datetime(6) DEFAULT NULL,
  reused_at datetime(6) DEFAULT NULL,
  PRIMARY KEY (token_hash),
  KEY idx_admin_refresh_tokens_session_status (session_id, status),
  KEY idx_admin_refresh_tokens_admin_status (admin_id, status),
  CONSTRAINT fk_admin_refresh_tokens_session
    FOREIGN KEY (session_id) REFERENCES admin_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_audit_logs (
  id bigint NOT NULL AUTO_INCREMENT,
  actor_admin_id varchar(36) NOT NULL,
  target_admin_id varchar(36) NOT NULL,
  action varchar(50) NOT NULL,
  reason varchar(500) DEFAULT NULL,
  detail text,
  occurred_at datetime(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_admin_audit_logs_occurred_at (occurred_at),
  KEY idx_admin_audit_logs_actor_occurred_at (actor_admin_id, occurred_at),
  KEY idx_admin_audit_logs_target_occurred_at (target_admin_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_statistics_events (
  id bigint NOT NULL AUTO_INCREMENT,
  event_type varchar(40) NOT NULL,
  event_date date NOT NULL,
  occurred_at datetime(6) NOT NULL,
  user_id varchar(36) DEFAULT NULL,
  visitor_key varchar(64) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_admin_statistics_events_type_date (event_type, event_date),
  KEY idx_admin_statistics_events_date_visitor (event_date, visitor_key),
  KEY idx_admin_statistics_events_date_user (event_date, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_daily_statistics (
  metric_date date NOT NULL,
  daily_unique_visitors bigint NOT NULL,
  total_visits bigint NOT NULL,
  dau bigint NOT NULL,
  diary_creations bigint NOT NULL,
  ai_photo_requests bigint NOT NULL,
  ai_photo_successes bigint NOT NULL,
  ai_photo_failures bigint NOT NULL,
  signup_members bigint NOT NULL,
  aggregated_at datetime(6) NOT NULL,
  PRIMARY KEY (metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
