-- =====================================================
-- V4: Feed cursor query 성능 개선용 인덱스 추가
--
-- 목적:
--   - consumed 여부 EXISTS 탐색 최적화
--   - diary별 like/comment count 집계 최적화
--   - diary visibility/status 기반 후보 범위 조회 최적화
--   - friend 양방향 lookup 최적화
-- =====================================================

-- =====================================================
-- 1. consumed 여부 판별 인덱스
-- =====================================================
CREATE INDEX idx_feed_click_user_diary
ON feed_click (user_id, diary_id);

CREATE INDEX idx_comments_user_diary_deleted
ON comments (user_id, diary_id, deleted_at);

-- =====================================================
-- 2. like/comment count 집계 인덱스
-- =====================================================
CREATE INDEX idx_likes_diary_deleted
ON likes (diary_id, deleted_at);

CREATE INDEX idx_comments_diary_deleted
ON comments (diary_id, deleted_at);

-- =====================================================
-- 3. diary 후보 필터 인덱스
-- =====================================================
CREATE INDEX idx_diary_status_deleted_created_id
ON diary (status, deleted_at, created_at, id);

CREATE INDEX idx_diary_user_status_deleted_created_id
ON diary (user_id, status, deleted_at, created_at, id);

-- =====================================================
-- 4. friend 양방향 lookup 보완 인덱스
-- =====================================================
CREATE INDEX idx_friend_user2_user1
ON friend (user_id_2, user_id_1);
