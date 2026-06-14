-- Support profile monthly diary count aggregation over a user's full diary history.
CREATE INDEX idx_diary_user_status_deleted_date
ON diary (user_id, status, deleted_at, date);
