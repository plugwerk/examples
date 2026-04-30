#!/usr/bin/env bash
# Upload a PF4J plugin ZIP to a namespace. Requires JWT with MEMBER+ role —
# X-Api-Key cannot upload (it is read-only by design, see CLI README).
#
# Usage: upload-plugin.sh <jwt> <slug> <zip-path> [base-url]
#
# Logs go to stderr; only the release id goes to stdout.

set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <jwt> <slug> <zip-path> [base-url]" >&2
  exit 64
fi

JWT="$1"
SLUG="$2"
ZIP_PATH="$3"
BASE_URL="${4:-http://localhost:8080}"

if [[ ! -f "${ZIP_PATH}" ]]; then
  echo "[upload-plugin] file not found: ${ZIP_PATH}" >&2
  exit 1
fi

echo "[upload-plugin] POST /namespaces/${SLUG}/plugin-releases artifact=${ZIP_PATH}" >&2
response=$(curl -fsS -X POST "${BASE_URL}/api/v1/namespaces/${SLUG}/plugin-releases" \
  -H "Authorization: Bearer ${JWT}" \
  -F "artifact=@${ZIP_PATH}")

release_id=$(jq -r '.id // empty' <<<"${response}")
if [[ -z "${release_id}" ]]; then
  echo "[upload-plugin] upload response did not contain .id:" >&2
  echo "${response}" >&2
  exit 1
fi

echo "[upload-plugin] uploaded release ${release_id}" >&2
printf '%s\n' "${release_id}"
