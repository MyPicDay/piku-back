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
