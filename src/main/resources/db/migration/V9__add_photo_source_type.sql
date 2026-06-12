-- Store whether a diary photo came from user upload or AI generation.
-- Existing rows are inferred from diary_image_generation.file_path when possible.

ALTER TABLE photos
  ADD COLUMN source_type varchar(32) NOT NULL DEFAULT 'USER_IMAGE';

UPDATE photos p
JOIN diary_image_generation g ON g.file_path = p.url
SET p.source_type = 'AI_IMAGE';

CREATE INDEX idx_photos_source_type
ON photos (source_type);
