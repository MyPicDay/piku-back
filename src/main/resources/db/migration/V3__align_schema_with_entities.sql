-- =====================================================
-- V3: 엔티티 ↔ DB 스키마 동기화
--
-- JPA 엔티티에 정의되어 있지만 Prod DB에 아직
-- 반영되지 않은 스키마 변경을 적용합니다.
-- =====================================================

-- =====================================================
-- 1. Notification: type ENUM에 LIKE 값 추가
--    NotificationType enum: COMMENT, FRIEND_ACCEPT,
--    FRIEND_DIARY, FRIEND_REQUEST, REPLY, LIKE
-- =====================================================
ALTER TABLE notification MODIFY COLUMN type
  ENUM('COMMENT','FRIEND_ACCEPT','FRIEND_DIARY','FRIEND_REQUEST','REPLY','LIKE') DEFAULT NULL;
