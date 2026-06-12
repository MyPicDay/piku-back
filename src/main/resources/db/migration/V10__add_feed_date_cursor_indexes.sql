-- Add date-based indexes for latest feed cursor queries.

CREATE INDEX idx_diary_status_deleted_date_id
ON diary (status, deleted_at, date, id);
