-- Add WebP optimization metadata to diary photos.
-- photos.url remains the original object key and optimized_url stores the WebP object key.

ALTER TABLE photos
  ADD COLUMN optimized_url varchar(255) DEFAULT NULL,
  ADD COLUMN optimization_status varchar(32) NOT NULL DEFAULT 'SKIPPED',
  ADD COLUMN optimized_at datetime(6) DEFAULT NULL,
  ADD COLUMN optimization_attempt_count int NOT NULL DEFAULT 0,
  ADD COLUMN optimization_last_attempt_at datetime(6) DEFAULT NULL;

UPDATE photos
SET optimization_status = 'PENDING'
WHERE url IS NOT NULL
  AND url <> ''
  AND url LIKE '%.%'
  AND LOWER(SUBSTRING_INDEX(url, '.', -1)) IN ('jpg', 'jpeg', 'png', 'bmp');

UPDATE photos
SET optimized_url = url,
    optimization_status = 'SUCCEEDED',
    optimized_at = COALESCE(updated_at, created_at, NOW(6))
WHERE url IS NOT NULL
  AND url <> ''
  AND url LIKE '%.%'
  AND LOWER(SUBSTRING_INDEX(url, '.', -1)) = 'webp';

CREATE INDEX idx_photos_optimization_status_attempt
ON photos (optimization_status, optimization_last_attempt_at, id);
