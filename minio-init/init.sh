#!/bin/sh
set -e

echo "==> mc alias"
mc alias set myminio "$MINIO_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

echo "==> ensure bucket exists: $STORAGE_BUCKET"
mc mb -p myminio/"$STORAGE_BUCKET" || true

echo "==> set anonymous read on prefix: $PUBLIC_PREFIX"
mc anonymous set download myminio/"$STORAGE_BUCKET"/"$PUBLIC_PREFIX"

if [ -d "$FIXED_CHARACTER_SOURCE_DIR" ]; then
  echo "==> sync fixed characters: $FIXED_CHARACTER_PREFIX"
  mc mirror --overwrite "$FIXED_CHARACTER_SOURCE_DIR" myminio/"$STORAGE_BUCKET"/"$FIXED_CHARACTER_PREFIX"
else
  echo "==> skip fixed characters sync: source directory not found ($FIXED_CHARACTER_SOURCE_DIR)"
fi

echo "==> verify"
mc anonymous list myminio/"$STORAGE_BUCKET" || true

echo "==> done"
