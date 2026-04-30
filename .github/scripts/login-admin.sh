#!/usr/bin/env bash
# Bootstrap login as the docker-compose admin (admin/admin) and print the JWT
# accessToken on stdout. The token is used only for one-shot setup operations
# (create namespaces, mint API keys, upload+approve plugin releases) — never
# for the actual application traffic, which uses a namespace-scoped X-Api-Key.
#
# Usage: login-admin.sh [base-url] [username] [password]
#   base-url defaults to http://localhost:8080
#   username defaults to admin
#   password defaults to admin (matches PLUGWERK_AUTH_ADMIN_PASSWORD in docker-compose.yml)
#
# Logs go to stderr; only the bare JWT goes to stdout.

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-admin}"
PASSWORD="${3:-admin}"

echo "[login-admin] POST ${BASE_URL}/api/v1/auth/login as ${USERNAME}" >&2

response=$(curl -fsS -X POST "${BASE_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")

token=$(jq -r '.accessToken // empty' <<<"${response}")
if [[ -z "${token}" ]]; then
  echo "[login-admin] login response did not contain accessToken:" >&2
  echo "${response}" >&2
  exit 1
fi

printf '%s\n' "${token}"
