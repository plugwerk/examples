#!/usr/bin/env bash
# Approve a plugin release (DRAFT -> PUBLISHED). Requires JWT with ADMIN role.
#
# When the namespace was provisioned with autoApproveReleases=true, the server
# may already have approved the release on upload — in that case the approve
# call returns 4xx, which this script tolerates. Includes a small retry loop
# to absorb the upload->approval race window.
#
# Usage: approve-release.sh <jwt> <slug> <release-id> [base-url]

set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <jwt> <slug> <release-id> [base-url]" >&2
  exit 64
fi

JWT="$1"
SLUG="$2"
RELEASE_ID="$3"
BASE_URL="${4:-http://localhost:8080}"

attempt=1
max_attempts=3

while (( attempt <= max_attempts )); do
  echo "[approve-release] POST /namespaces/${SLUG}/reviews/${RELEASE_ID}/approve (attempt ${attempt}/${max_attempts})" >&2
  http_code=$(curl -sS -o /tmp/approve-response.json -w '%{http_code}' \
    -X POST "${BASE_URL}/api/v1/namespaces/${SLUG}/reviews/${RELEASE_ID}/approve" \
    -H "Authorization: Bearer ${JWT}")

  case "${http_code}" in
    200|201|204)
      echo "[approve-release] release ${RELEASE_ID} approved" >&2
      exit 0
      ;;
    409|422)
      # Already approved (autoApproveReleases) or in a state that doesn't allow approval — both fine.
      echo "[approve-release] release ${RELEASE_ID} already in non-DRAFT state (HTTP ${http_code}), continuing" >&2
      exit 0
      ;;
    404)
      # Race: server hasn't fully persisted the release yet. Retry.
      echo "[approve-release] release not yet visible (HTTP 404), retrying" >&2
      ;;
    *)
      echo "[approve-release] unexpected HTTP ${http_code}:" >&2
      cat /tmp/approve-response.json >&2 || true
      ;;
  esac

  attempt=$(( attempt + 1 ))
  sleep 1
done

echo "[approve-release] giving up after ${max_attempts} attempts" >&2
exit 1
