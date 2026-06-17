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
