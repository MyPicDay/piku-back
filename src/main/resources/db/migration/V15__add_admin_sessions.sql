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
