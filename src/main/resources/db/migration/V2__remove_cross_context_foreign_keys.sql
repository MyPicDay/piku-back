-- =====================================================
-- V2: 헥사고날 리팩토링 - 크로스 컨텍스트 FK 제거
--
-- @ManyToOne → ID 참조 전환에 따라 prod DB에 남아있는
-- 불필요한 FK 제약조건을 안전하게 제거합니다.
--
-- 아래 프로시저는 FK가 존재할 때만 DROP합니다.
-- (prod에 FK가 이미 없더라도 오류 없이 통과)
-- =====================================================

-- FK 안전 삭제 프로시저
DELIMITER //
CREATE PROCEDURE drop_fk_if_exists(
  IN tableName VARCHAR(64),
  IN constraintName VARCHAR(64)
)
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = tableName
      AND CONSTRAINT_NAME = constraintName
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', tableName, '` DROP FOREIGN KEY `', constraintName, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

-- 1. characters → users (Character.@ManyToOne User → String userId)
CALL drop_fk_if_exists('characters', 'FKp79a1qd81l5rq08axdeejp5mn');
CALL drop_fk_if_exists('characters', 'fk_characters_users');

-- 2. diary → users (Diary.@ManyToOne User → String userId)
CALL drop_fk_if_exists('diary', 'FK5dcshfkm3wrv5n7l8flj8w42r');
CALL drop_fk_if_exists('diary', 'fk_diary_users');

-- 3. comments → users (Comment.@ManyToOne User → String userId)
CALL drop_fk_if_exists('comments', 'FK8omq0tc18jd43bu5tjh6jvraq');
CALL drop_fk_if_exists('comments', 'fk_comments_users');

-- 4. comments → diary (Comment.@ManyToOne Diary → Long diaryId)
CALL drop_fk_if_exists('comments', 'FKqy1xfxwbqfj65eld4r22q5iak');
CALL drop_fk_if_exists('comments', 'fk_comments_diary');

-- 5. notification → users (Notification.@ManyToOne User sender)
CALL drop_fk_if_exists('notification', 'FKb0yvoep4h4k92isnp8bir7ecd');
CALL drop_fk_if_exists('notification', 'fk_notification_users');

-- 6. notification → diary (Notification.@ManyToOne Diary)
CALL drop_fk_if_exists('notification', 'FK1jxqhnccaxmd2jl2u8gf5ytqe');
CALL drop_fk_if_exists('notification', 'fk_notification_diary');

-- 7. inquiry → users (Inquiry.@ManyToOne User → String userId)
CALL drop_fk_if_exists('inquiry', 'FKl0sbpwv2rjqj0bqy7jqx87a8i');
CALL drop_fk_if_exists('inquiry', 'fk_inquiry_users');

-- 프로시저 정리
DROP PROCEDURE IF EXISTS drop_fk_if_exists;
