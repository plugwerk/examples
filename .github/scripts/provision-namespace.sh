#!/usr/bin/env bash
# Idempotently provision a namespace and mint a fresh access key for it.
# The plain-text key is shown only once by the server, so we capture it here
# and print it on stdout for the caller to export into $GITHUB_ENV.
#
# Usage: provision-namespace.sh <jwt> <slug> <publicCatalog> <autoApproveReleases> [base-url]
#   jwt                  bootstrap JWT from login-admin.sh (must be superadmin)
#   slug                 namespace slug (e.g. public-ns)
#   publicCatalog        true|false
#   autoApproveReleases  true|false
#   base-url             defaults to http://localhost:8080
#
# Logs go to stderr; only the plain-text API key goes to stdout.

set -euo pipefail

if [[ $# -lt 4 ]]; then
  echo "Usage: $0 <jwt> <slug> <publicCatalog> <autoApproveReleases> [base-url]" >&2
  exit 64
fi

JWT="$1"
SLUG="$2"
PUBLIC_CATALOG="$3"
AUTO_APPROVE="$4"
BASE_URL="${5:-http://localhost:8080}"

# 1. Create the namespace. 409 means it already exists, which is fine for reruns.
echo "[provision-namespace] POST /namespaces slug=${SLUG} publicCatalog=${PUBLIC_CATALOG} autoApproveReleases=${AUTO_APPROVE}" >&2
http_code=$(curl -sS -o /tmp/ns-create.json -w '%{http_code}' \
  -X POST "${BASE_URL}/api/v1/namespaces" \
  -H "Authorization: Bearer ${JWT}" \
  -H 'Content-Type: application/json' \
  -d "{\"slug\":\"${SLUG}\",\"name\":\"${SLUG}\",\"publicCatalog\":${PUBLIC_CATALOG},\"autoApproveReleases\":${AUTO_APPROVE}}")

case "${http_code}" in
  201|200)
    echo "[provision-namespace] created ${SLUG}" >&2
    ;;
  409)
    echo "[provision-namespace] ${SLUG} already exists, continuing" >&2
    ;;
  *)
    echo "[provision-namespace] failed to create ${SLUG} (HTTP ${http_code}):" >&2
    cat /tmp/ns-create.json >&2 || true
    exit 1
    ;;
esac

# 2. Mint a fresh access key. The plain-text "key" field is shown only once.
echo "[provision-namespace] POST /namespaces/${SLUG}/access-keys" >&2
key_response=$(curl -fsS -X POST "${BASE_URL}/api/v1/namespaces/${SLUG}/access-keys" \
  -H "Authorization: Bearer ${JWT}" \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"ci-${SLUG}-$(date +%s)\",\"expiresAt\":null}")

api_key=$(jq -r '.key // empty' <<<"${key_response}")
if [[ -z "${api_key}" ]]; then
  echo "[provision-namespace] access-key response did not contain .key:" >&2
  echo "${key_response}" >&2
  exit 1
fi

printf '%s\n' "${api_key}"
