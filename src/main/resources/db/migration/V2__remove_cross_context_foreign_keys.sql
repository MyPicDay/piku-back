-- =====================================================
-- V2: 헥사고날 리팩토링 - 크로스 컨텍스트 FK 제거
--
-- 엔티티에서 @ManyToOne → ID 참조(String/Long)로 전환된
-- 크로스 컨텍스트 FK를 안전하게 제거합니다.
--
-- 유지되는 FK:
--   - photos.diary_id → diary (Photo 엔티티가 @ManyToOne 유지)
--   - comments.parent_id → comments (Comment 자기참조 @ManyToOne 유지)
--
-- 아래 프로시저는 FK가 존재할 때만 DROP합니다.
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

-- 1. characters → users (Character 엔티티: String userId)
CALL drop_fk_if_exists('characters', 'FK27yx743bsnnsqplnjhk5yf224');

-- 2. diary → users (Diary 엔티티: String userId)
CALL drop_fk_if_exists('diary', 'FK74rd0bn5raxejw2ukenelbdmt');

-- 3. comments → users (Comment 엔티티: String userId)
CALL drop_fk_if_exists('comments', 'FK8omq0tc18jd43bu5tjh6jvraq');

-- 4. comments → diary (Comment 엔티티: Long diaryId)
CALL drop_fk_if_exists('comments', 'FKjrqn5w090e67v7yrlqjjadxb7');

-- 5. notification → users (Notification 엔티티: String senderId)
CALL drop_fk_if_exists('notification', 'FKnk4ftb5am9ubmkv1661h15ds9');

-- 6. notification → diary (Notification 엔티티: Long diaryId)
CALL drop_fk_if_exists('notification', 'FKey4f6r0yoeuhly3v5hxh1tajd');

-- 7. inquiry → users (Inquiry 엔티티: String userId)
CALL drop_fk_if_exists('inquiry', 'FKray80kmwpjpjb91ime7ogijjr');

-- 프로시저 정리
DROP PROCEDURE IF EXISTS drop_fk_if_exists;
