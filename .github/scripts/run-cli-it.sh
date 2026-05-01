#!/usr/bin/env bash
# Integration test for plugwerk-java-cli-example.
#
# Runs the full happy path against a Plugwerk server that has been brought up
# by the workflow's earlier steps (docker compose up + provision-namespace.sh).
#
# Required env (exported by the workflow before this runs):
#   PLUGWERK_BOOTSTRAP_JWT  one-shot superadmin JWT for upload + approve
#   PUBLIC_NS_KEY           X-Api-Key for the public-ns namespace
#   PRIVATE_NS_KEY          X-Api-Key for the private-ns namespace
#   CLI_RUNTIME_DIR         dir holding the fat JAR + plugins/ subdir with the SDK ZIP
#   CLI_PLUGINS_DIR         dir holding the example plugin ZIPs to upload
#
# Visibility-matrix mapping (issue #12 acceptance criterion):
#   io.plugwerk.example.cli.hello   -> public-ns   (anonymous catalog read works)
#   io.plugwerk.example.cli.sysinfo -> private-ns  (X-Api-Key required)

set -euo pipefail

: "${PLUGWERK_BOOTSTRAP_JWT:?must be set}"
: "${PUBLIC_NS_KEY:?must be set}"
: "${PRIVATE_NS_KEY:?must be set}"
: "${CLI_RUNTIME_DIR:?must be set}"
: "${CLI_PLUGINS_DIR:?must be set}"

BASE_URL="${PLUGWERK_SERVER_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------------------------------------------------------------------------
# Locate artefacts. Plugin versions are extracted from the filename so the
# script keeps working when VERSION bumps in the repo.
# ---------------------------------------------------------------------------

FAT_JAR=$(find "${CLI_RUNTIME_DIR}" -maxdepth 1 -name '*-fat.jar' | head -n1)
HELLO_ZIP=$(find "${CLI_PLUGINS_DIR}" -maxdepth 1 -name 'io.plugwerk.example.cli.hello-*.zip' | head -n1)
SYSINFO_ZIP=$(find "${CLI_PLUGINS_DIR}" -maxdepth 1 -name 'io.plugwerk.example.cli.sysinfo-*.zip' | head -n1)

[[ -f "${FAT_JAR}"    ]] || { echo "[run-cli-it] missing fat JAR in ${CLI_RUNTIME_DIR}" >&2; exit 1; }
[[ -f "${HELLO_ZIP}"  ]] || { echo "[run-cli-it] missing hello plugin ZIP in ${CLI_PLUGINS_DIR}" >&2; exit 1; }
[[ -f "${SYSINFO_ZIP}" ]] || { echo "[run-cli-it] missing sysinfo plugin ZIP in ${CLI_PLUGINS_DIR}" >&2; exit 1; }

# Extract plugin versions from filenames (e.g. io.plugwerk.example.cli.hello-1.0.0-SNAPSHOT.zip -> 1.0.0-SNAPSHOT)
HELLO_VERSION=$(basename "${HELLO_ZIP}"   | sed -E 's/^io\.plugwerk\.example\.cli\.hello-(.+)\.zip$/\1/')
SYSINFO_VERSION=$(basename "${SYSINFO_ZIP}" | sed -E 's/^io\.plugwerk\.example\.cli\.sysinfo-(.+)\.zip$/\1/')

echo "[run-cli-it] fat JAR:        ${FAT_JAR}"
echo "[run-cli-it] hello plugin:   ${HELLO_ZIP} (version ${HELLO_VERSION})"
echo "[run-cli-it] sysinfo plugin: ${SYSINFO_ZIP} (version ${SYSINFO_VERSION})"

# ---------------------------------------------------------------------------
# 1. Upload plugins server-side (JWT, MEMBER+).
# ---------------------------------------------------------------------------

echo
echo "=== 1. Uploading plugins to the server ==="
HELLO_RELEASE_ID=$(bash   "${SCRIPT_DIR}/upload-plugin.sh" "${PLUGWERK_BOOTSTRAP_JWT}" public-ns  "${HELLO_ZIP}")
SYSINFO_RELEASE_ID=$(bash "${SCRIPT_DIR}/upload-plugin.sh" "${PLUGWERK_BOOTSTRAP_JWT}" private-ns "${SYSINFO_ZIP}")

# Approve calls are no-ops when autoApproveReleases=true (the helper tolerates 409/422).
bash "${SCRIPT_DIR}/approve-release.sh" "${PLUGWERK_BOOTSTRAP_JWT}" public-ns  "${HELLO_RELEASE_ID}"
bash "${SCRIPT_DIR}/approve-release.sh" "${PLUGWERK_BOOTSTRAP_JWT}" private-ns "${SYSINFO_RELEASE_ID}"

# ---------------------------------------------------------------------------
# 2. CLI working directory: copy the SDK plugin into ./plugins next to the
#    fat JAR. PF4J extracts on first run.
# ---------------------------------------------------------------------------

CLI_WORK="$(pwd)/cli-work"
rm -rf "${CLI_WORK}"
mkdir -p "${CLI_WORK}/plugins"
cp "${CLI_RUNTIME_DIR}/plugins/"plugwerk-client-plugin-*-pf4j.zip "${CLI_WORK}/plugins/"

# PLUGWERK_PLUGINS_DIR is invariant across all invocations. PLUGWERK_NAMESPACE
# and PLUGWERK_API_KEY are exported per phase below; the run_cli helper just
# inherits whatever is currently in the environment.
export PLUGWERK_PLUGINS_DIR="${CLI_WORK}/plugins"

run_cli() {
  local label="$1"; shift
  local log="cli-stdout.log"
  echo
  echo "--- CLI [${label}]: $* ---"
  (
    cd "${CLI_WORK}"
    java -jar "${FAT_JAR}" "$@"
  ) 2>&1 | tee -a "${log}"
  return "${PIPESTATUS[0]}"
}

# ---------------------------------------------------------------------------
# 3. public-ns: list, install, invoke `hello`.
# ---------------------------------------------------------------------------

echo
echo "=== 3. public-ns flow (anonymous catalog read, key for install) ==="
# `list` runs anonymously to prove publicCatalog=true is honored end-to-end
# through the CLI — no X-Api-Key header is sent. `install` triggers a download
# which we intentionally keep key-authenticated, matching the documented
# day-to-day workflow.

export PLUGWERK_NAMESPACE=public-ns
unset PLUGWERK_API_KEY

run_cli "list public-ns (anon)" list >"public-list.out" 2>&1 || true
grep -q 'io.plugwerk.example.cli.hello' "public-list.out" || {
  echo "[run-cli-it] FAIL: hello plugin not in anonymous public-ns list" >&2
  cat "public-list.out" >&2
  exit 1
}

export PLUGWERK_API_KEY="${PUBLIC_NS_KEY}"
run_cli "install hello" install io.plugwerk.example.cli.hello "${HELLO_VERSION}"

# Subsequent invocations need no namespace/key — the locally extracted plugin
# is loaded by PF4J and the subcommand runs in-process.
unset PLUGWERK_NAMESPACE
unset PLUGWERK_API_KEY
run_cli "invoke hello" hello >"hello.out" 2>&1
grep -q 'Hello, World!' "hello.out" || {
  echo "[run-cli-it] FAIL: hello subcommand did not produce expected greeting" >&2
  cat "hello.out" >&2
  exit 1
}
echo "[run-cli-it] OK: public-ns -> hello -> 'Hello, World!'"

# ---------------------------------------------------------------------------
# 4. private-ns: list, install, invoke `sysinfo`. API key is required for every
#    server call here.
# ---------------------------------------------------------------------------

echo
echo "=== 4. private-ns flow (X-Api-Key required) ==="

export PLUGWERK_NAMESPACE=private-ns
export PLUGWERK_API_KEY="${PRIVATE_NS_KEY}"

run_cli "list private-ns" list >"private-list.out" 2>&1 || true
grep -q 'io.plugwerk.example.cli.sysinfo' "private-list.out" || {
  echo "[run-cli-it] FAIL: sysinfo plugin not in private-ns list" >&2
  cat "private-list.out" >&2
  exit 1
}

run_cli "install sysinfo" install io.plugwerk.example.cli.sysinfo "${SYSINFO_VERSION}"

unset PLUGWERK_NAMESPACE
unset PLUGWERK_API_KEY
run_cli "invoke sysinfo" sysinfo >"sysinfo.out" 2>&1
for needle in 'Java:' 'OS:' 'Heap:'; do
  grep -q "${needle}" "sysinfo.out" || {
    echo "[run-cli-it] FAIL: sysinfo output missing '${needle}'" >&2
    cat "sysinfo.out" >&2
    exit 1
  }
done
echo "[run-cli-it] OK: private-ns -> sysinfo -> Java/OS/Heap"

# ---------------------------------------------------------------------------
# 5. Auth contract on private-ns: anonymous catalog read must be 401, with key 200.
# ---------------------------------------------------------------------------

echo
echo "=== 5. Auth contract: public-ns vs private-ns ==="

# public-ns (publicCatalog=true): anonymous read allowed, key still works.
bash "${SCRIPT_DIR}/assert-http.sh" 200 "${BASE_URL}/api/v1/namespaces/public-ns/plugins"
bash "${SCRIPT_DIR}/assert-http.sh" 200 "${BASE_URL}/api/v1/namespaces/public-ns/plugins" \
  -H "X-Api-Key: ${PUBLIC_NS_KEY}"

# private-ns (publicCatalog=false): anonymous denied, key required.
bash "${SCRIPT_DIR}/assert-http.sh" 401 "${BASE_URL}/api/v1/namespaces/private-ns/plugins"
bash "${SCRIPT_DIR}/assert-http.sh" 200 "${BASE_URL}/api/v1/namespaces/private-ns/plugins" \
  -H "X-Api-Key: ${PRIVATE_NS_KEY}"

echo
echo "[run-cli-it] all assertions passed"
