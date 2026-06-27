CREATE INDEX idx_users_created_deleted
ON users (created_at, deleted_at);

CREATE INDEX idx_users_deleted_created
ON users (deleted_at, created_at);

CREATE INDEX idx_diary_created
ON diary (created_at);

CREATE INDEX idx_diary_image_generation_created
ON diary_image_generation (created_at);

CREATE INDEX idx_admin_statistics_events_type_date_user
ON admin_statistics_events (event_type, event_date, user_id);
