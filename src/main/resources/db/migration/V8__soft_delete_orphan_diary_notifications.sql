-- =====================================================
-- V8: 삭제되었거나 존재하지 않는 일기 알림 정리
--
-- notification -> diary FK는 V2에서 제거되었기 때문에, 이미 생성된
-- 알림 행은 일기 삭제 후에도 남아 있을 수 있습니다.
-- =====================================================

UPDATE notification n
LEFT JOIN diary d ON d.id = n.diary_id
SET n.deleted_at = CURRENT_TIMESTAMP(6)
WHERE n.diary_id IS NOT NULL
  AND n.deleted_at IS NULL
  AND (d.id IS NULL OR d.deleted_at IS NOT NULL);
