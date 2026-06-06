#!/usr/bin/env bash
#
# Functional smoke test for the Spring AI MCP server over the Streamable HTTP
# transport. It performs a real MCP session against a running server:
#
#   1. initialize                      (opens a session)
#   2. notifications/initialized       (completes the handshake)
#   3. tools/list                      (asserts toolbox + task tools exist)
#   4. tools/call gen_uuid             (stateless tool)
#   5. tools/call create_task          (DynamoDB-backed tool)
#   6. tools/call list_tasks           (asserts the new task is returned)
#
# Usage:   ./scripts/smoke-test.sh [BASE_URL]
# Example: ./scripts/smoke-test.sh http://localhost:8080
#
# Exit code is non-zero if any step fails — suitable as a CI gate.
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
MCP_URL="${BASE_URL%/}/mcp"
PROTOCOL_VERSION="2025-06-18"

command -v jq >/dev/null 2>&1 || { echo "ERROR: jq is required"; exit 2; }

HEADERS_FILE="$(mktemp)"
trap 'rm -f "$HEADERS_FILE"' EXIT

SESSION_ID=""

# Extract the JSON-RPC payload from a response that may be either a plain JSON
# body or an SSE stream ("data: {...}" lines).
extract_json() {
  # If it looks like SSE, pull the first data: line; otherwise pass through.
  # Note the SDK frames events as "data:{...}" (no space after the colon).
  if grep -q '^data:' <<<"$1"; then
    grep '^data:' <<<"$1" | head -1 | sed -E 's/^data:[[:space:]]?//'
  else
    echo "$1"
  fi
}

# mcp_post <json-rpc-request>  -> echoes the JSON-RPC response payload
mcp_post() {
  local body="$1"
  local extra_headers=(-H "Content-Type: application/json"
                       -H "Accept: application/json, text/event-stream"
                       -H "MCP-Protocol-Version: ${PROTOCOL_VERSION}")
  if [[ -n "$SESSION_ID" ]]; then
    extra_headers+=(-H "Mcp-Session-Id: ${SESSION_ID}")
  fi
  local raw
  raw="$(curl -sS -D "$HEADERS_FILE" "${extra_headers[@]}" -d "$body" "$MCP_URL")"
  extract_json "$raw"
}

echo "==> MCP endpoint: $MCP_URL"

# 1. initialize -------------------------------------------------------------
echo "==> [1/6] initialize"
INIT_REQ=$(jq -nc --arg pv "$PROTOCOL_VERSION" '{
  jsonrpc:"2.0", id:1, method:"initialize",
  params:{ protocolVersion:$pv,
           capabilities:{},
           clientInfo:{ name:"smoke-test", version:"1.0.0" } }
}')
INIT_RESP="$(mcp_post "$INIT_REQ")"
SESSION_ID="$(grep -i '^mcp-session-id:' "$HEADERS_FILE" | head -1 | tr -d '\r' | awk '{print $2}')"
SERVER_NAME="$(jq -r '.result.serverInfo.name // empty' <<<"$INIT_RESP")"
[[ "$SERVER_NAME" == "spring-mcp-server" ]] || { echo "FAIL: unexpected serverInfo: $INIT_RESP"; exit 1; }
echo "    server: $SERVER_NAME   session: ${SESSION_ID:-<none>}"

# 2. notifications/initialized ---------------------------------------------
echo "==> [2/6] notifications/initialized"
mcp_post '{"jsonrpc":"2.0","method":"notifications/initialized"}' >/dev/null || true

# 3. tools/list -------------------------------------------------------------
echo "==> [3/6] tools/list"
TOOLS_RESP="$(mcp_post '{"jsonrpc":"2.0","id":2,"method":"tools/list"}')"
TOOL_NAMES="$(jq -r '.result.tools[].name' <<<"$TOOLS_RESP" | sort | tr '\n' ' ')"
echo "    tools: $TOOL_NAMES"
for expected in gen_uuid create_task list_tasks; do
  grep -qw "$expected" <<<"$TOOL_NAMES" || { echo "FAIL: missing tool '$expected'"; exit 1; }
done

# 4. tools/call gen_uuid ----------------------------------------------------
echo "==> [4/6] tools/call gen_uuid"
UUID_RESP="$(mcp_post '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"gen_uuid","arguments":{}}}')"
UUID_VAL="$(jq -r '.result.content[0].text // empty' <<<"$UUID_RESP")"
[[ "$UUID_VAL" =~ ^[0-9a-fA-F-]{36}$ ]] || { echo "FAIL: gen_uuid returned '$UUID_VAL'"; exit 1; }
echo "    uuid: $UUID_VAL"

# 5. tools/call create_task -------------------------------------------------
echo "==> [5/6] tools/call create_task"
CREATE_REQ=$(jq -nc '{
  jsonrpc:"2.0", id:4, method:"tools/call",
  params:{ name:"create_task", arguments:{ title:"Smoke test task" } }
}')
CREATE_RESP="$(mcp_post "$CREATE_REQ")"
IS_ERROR="$(jq -r '.result.isError // false' <<<"$CREATE_RESP")"
[[ "$IS_ERROR" != "true" ]] || { echo "FAIL: create_task errored: $CREATE_RESP"; exit 1; }
echo "    created: $(jq -rc '.result.content[0].text // .result.structuredContent // empty' <<<"$CREATE_RESP")"

# 6. tools/call list_tasks --------------------------------------------------
echo "==> [6/6] tools/call list_tasks"
LIST_RESP="$(mcp_post '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"list_tasks","arguments":{}}}')"
grep -q "Smoke test task" <<<"$LIST_RESP" || { echo "FAIL: created task not found in list: $LIST_RESP"; exit 1; }
echo "    list contains the created task ✔"

echo "==> SMOKE TEST PASSED"
