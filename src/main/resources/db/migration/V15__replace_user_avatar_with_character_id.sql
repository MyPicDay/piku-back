-- Replace the User-owned avatar path with a Character identifier reference.
-- Cross-context foreign keys are intentionally not introduced.

DROP PROCEDURE IF EXISTS validate_user_avatar_character_migration;

DELIMITER //
CREATE PROCEDURE validate_user_avatar_character_migration()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM users u
    WHERE u.avatar IS NULL
       OR TRIM(u.avatar) = ''
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'users.avatar contains null or blank values';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM users u
    WHERE NOT EXISTS (
      SELECT 1
      FROM characters c
      WHERE c.type = 'FIXED'
        AND BINARY c.image_url = BINARY u.avatar
    )
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'users.avatar has no exact fixed character match';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM users u
    WHERE (
      SELECT COUNT(*)
      FROM characters c
      WHERE c.type = 'FIXED'
        AND BINARY c.image_url = BINARY u.avatar
    ) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'users.avatar has multiple exact fixed character matches';
  END IF;
END //
DELIMITER ;

CALL validate_user_avatar_character_migration();
DROP PROCEDURE validate_user_avatar_character_migration;

ALTER TABLE users
  ADD COLUMN character_id BIGINT NULL;

UPDATE users u
JOIN characters c
  ON c.type = 'FIXED'
 AND BINARY c.image_url = BINARY u.avatar
SET u.character_id = c.id;

ALTER TABLE users
  MODIFY COLUMN character_id BIGINT NOT NULL;

CREATE INDEX idx_users_character_id
  ON users (character_id);

ALTER TABLE users
  DROP COLUMN avatar;
