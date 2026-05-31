-- Move fixed character image references from backend-local paths to MinIO object keys.
-- Bucket name and public URL are intentionally not stored in DB.

UPDATE characters
SET image_url = CONCAT('public/', image_url)
WHERE type = 'FIXED'
  AND image_url IS NOT NULL
  AND image_url <> ''
  AND image_url LIKE 'characters/fixed/%';

UPDATE characters
SET image_url = CONCAT('public/characters/fixed/', image_url)
WHERE type = 'FIXED'
  AND image_url IS NOT NULL
  AND image_url <> ''
  AND image_url NOT LIKE 'public/%'
  AND image_url NOT LIKE '%/%';

UPDATE users
SET avatar = CONCAT('public/', avatar)
WHERE avatar IS NOT NULL
  AND avatar <> ''
  AND avatar LIKE 'characters/fixed/%';
