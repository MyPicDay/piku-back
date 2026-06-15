-- Convert fixed character image references from PNG to WebP.
-- Storage objects must be uploaded separately before this migration is applied.

UPDATE characters
SET image_url = CONCAT(SUBSTRING(image_url, 1, CHAR_LENGTH(image_url) - 4), '.webp')
WHERE type = 'FIXED'
  AND image_url IS NOT NULL
  AND image_url <> ''
  AND LOWER(image_url) LIKE '%.png'
  AND (
    image_url LIKE 'public/characters/fixed/%'
    OR image_url LIKE 'characters/fixed/%'
    OR image_url NOT LIKE '%/%'
  );

UPDATE users
SET avatar = CONCAT(SUBSTRING(avatar, 1, CHAR_LENGTH(avatar) - 4), '.webp')
WHERE avatar IS NOT NULL
  AND avatar <> ''
  AND LOWER(avatar) LIKE '%.png'
  AND (
    avatar LIKE 'public/characters/fixed/%'
    OR avatar LIKE 'characters/fixed/%'
    OR avatar NOT LIKE '%/%'
  );
