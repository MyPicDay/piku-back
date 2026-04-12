#!/usr/bin/env bash
set -euo pipefail

ROOT_DOCS="docs"
declare -a REQUIRED_DIRS=(
  "architecture"
  "standards"
  "product-specs"
  "handoffs"
  "runbooks"
  "domain-models"
  "references"
  "generated"
  "archive"
  "indexes"
)

declare -a REQUIRED_INDEXES=(
  "architecture-index.md"
  "standards-index.md"
  "product-specs-index.md"
  "handoffs-index.md"
  "runbooks-index.md"
  "domain-models-index.md"
  "references-index.md"
)

fail() {
  echo "docs-lint: $1" >&2
  exit 1
}

[[ -f "${ROOT_DOCS}/README.md" ]] || fail "missing docs/README.md"

for dir in "${REQUIRED_DIRS[@]}"; do
  [[ -d "${ROOT_DOCS}/${dir}" ]] || fail "missing docs/${dir}"
done

for index_file in "${REQUIRED_INDEXES[@]}"; do
  [[ -f "${ROOT_DOCS}/indexes/${index_file}" ]] || fail "missing docs/indexes/${index_file}"
done

while IFS= read -r file; do
  base="$(basename "$file")"
  [[ "$base" == "README.md" ]] && continue
  fail "stray root docs file: $file"
done < <(find "${ROOT_DOCS}" -maxdepth 1 -type f | sort)

for file in docs/README.md $(find docs/indexes docs/architecture docs/standards docs/product-specs docs/handoffs docs/runbooks docs/domain-models docs/references docs/archive -type f | sort); do
  [[ -f "$file" ]] || continue
  [[ "$(basename "$file")" == ".gitkeep" ]] && continue
  grep -q "Status:" "$file" || fail "missing Status metadata: $file"
  grep -q "Audience:" "$file" || fail "missing Audience metadata: $file"
  grep -q "Source of Truth:" "$file" || fail "missing Source of Truth metadata: $file"
  grep -q "Last Reviewed:" "$file" || fail "missing Last Reviewed metadata: $file"
done

grep -q "indexes/architecture-index.md" docs/README.md || fail "docs/README.md missing architecture index link"
grep -q "indexes/standards-index.md" docs/README.md || fail "docs/README.md missing standards index link"
grep -q "indexes/product-specs-index.md" docs/README.md || fail "docs/README.md missing product specs index link"
grep -q "indexes/handoffs-index.md" docs/README.md || fail "docs/README.md missing handoffs index link"
grep -q "indexes/runbooks-index.md" docs/README.md || fail "docs/README.md missing runbooks index link"
grep -q "indexes/domain-models-index.md" docs/README.md || fail "docs/README.md missing domain-models index link"
grep -q "indexes/references-index.md" docs/README.md || fail "docs/README.md missing references index link"

grep -q "RFC 9457" docs/standards/api-error-responses.md || fail "api-error-responses.md missing RFC 9457"
grep -q "ProblemDetailFactory" docs/standards/api-error-responses.md || fail "api-error-responses.md missing ProblemDetailFactory reference"

[[ ! -d docs/domain_models ]] || fail "legacy docs/domain_models directory still exists"
[[ ! -d docs/tasks ]] || fail "legacy docs/tasks directory still exists"

echo "docs-lint: OK"
