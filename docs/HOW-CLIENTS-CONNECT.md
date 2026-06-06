# How (and from where) clients connect

This server speaks MCP over **Streamable HTTP** at a single endpoint:

- **Local:** `http://localhost:8080/mcp`
- **Deployed:** `http://<elastic-ip>/mcp` (from `terraform output mcp_endpoint`)

Anything that can speak MCP can connect — from your laptop, from another server,
from a hosted AI app — as long as it can reach that URL over the network. Below
are the common clients, each copy-pasteable. Replace `BASE_URL` with whichever
endpoint you're targeting.

> Quick mental check: a **client** opens a session (`initialize`), discovers
> capabilities (`tools/list`, `resources/list`, `prompts/list`), then uses them
> (`tools/call`, etc.). See [MCP-EXPLAINED.md](MCP-EXPLAINED.md).

---

## 1. MCP Inspector (best for exploring)

The official visual debugger. No install needed beyond Node:

```bash
npx @modelcontextprotocol/inspector
```

It opens a web UI. Choose **Transport: Streamable HTTP**, set the URL to
`http://localhost:8080/mcp` (or your deployed URL), click **Connect**, then
browse the **Tools**, **Resources**, and **Prompts** tabs and invoke them.

---

## 2. Claude Code

Add the server as an HTTP MCP server:

```bash
# Local
claude mcp add --transport http spring-mcp http://localhost:8080/mcp

# Deployed
claude mcp add --transport http spring-mcp http://<elastic-ip>/mcp
```

Then in a Claude Code session, the toolbox/task tools, the `task://` resources,
and the `plan_my_day` / `triage_tasks` prompts become available. Verify with:

```bash
claude mcp list
```

---

## 3. Claude Desktop

Claude Desktop loads MCP servers from its config file:

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

Claude Desktop primarily launches **stdio** servers, so for an HTTP server we
bridge with [`mcp-remote`](https://www.npmjs.com/package/mcp-remote):

```json
{
  "mcpServers": {
    "spring-mcp": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/mcp"]
    }
  }
}
```

(A ready copy is in [`../clients/claude-desktop-config.json`](../clients/claude-desktop-config.json).)
Restart Claude Desktop; the tools appear under the 🔌 / tools menu. For the
deployed server, swap in `http://<elastic-ip>/mcp`. Newer Claude Desktop builds
can also add a remote server directly via **Settings → Connectors → Add custom
connector**.

---

## 4. Python client (runnable)

A minimal client using the official `mcp` SDK is in
[`../clients/python-client/`](../clients/python-client/):

```bash
cd clients/python-client
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python client.py http://localhost:8080/mcp
```

It connects, lists tools/resources/prompts, calls `gen_uuid`, and creates +
lists a task — printing each result.

---

## 5. Java client (Spring AI / MCP SDK)

The integration test
[`McpServerIntegrationTest`](../src/test/java/com/portfolio/mcp/McpServerIntegrationTest.java)
*is* a working Java client. The essence:

```java
var transport = HttpClientStreamableHttpTransport
        .builder("http://localhost:8080")
        .endpoint("/mcp")
        .build();

try (McpSyncClient client = McpClient.sync(transport).build()) {
    client.initialize();
    client.listTools().tools().forEach(t -> System.out.println(t.name()));
    var result = client.callTool(new CallToolRequest("gen_uuid", Map.of()));
    System.out.println(result.content());
}
```

Dependencies: `io.modelcontextprotocol.sdk:mcp` (the MCP Java SDK), which Spring
AI builds on.

---

## 6. Raw curl (understand the wire protocol)

Streamable HTTP is just HTTP POSTs of JSON-RPC. You must `initialize` first and
reuse the returned `Mcp-Session-Id` header on later calls:

```bash
BASE_URL=http://localhost:8080

# initialize — capture the session id from the response headers
curl -i -sS "$BASE_URL/mcp" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize",
       "params":{"protocolVersion":"2025-06-18","capabilities":{},
                 "clientInfo":{"name":"curl","version":"1.0"}}}'

# then (using SID from the Mcp-Session-Id header):
curl -sS "$BASE_URL/mcp" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H "Mcp-Session-Id: $SID" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```

The fully worked sequence (with session handling and SSE parsing) is
[`scripts/smoke-test.sh`](../scripts/smoke-test.sh) — read it as the canonical
example.

---

## From where can clients call it?

| Location                         | Works? | Notes |
| -------------------------------- | ------ | ----- |
| Your laptop → `localhost:8080`   | ✅     | `docker compose up` |
| Your laptop → `http://<eip>/mcp` | ✅     | security group opens port 80 to `0.0.0.0/0` by default |
| Another server / CI → `<eip>`    | ✅     | it's just HTTP; great for automation |
| A hosted AI app → `<eip>`        | ✅     | use the public endpoint |

> **Security note:** the demo serves plain HTTP and is open to the internet, with
> no auth — fine for learning, not for production. To harden: restrict
> `allowed_cidr`, put it behind HTTPS (ALB/CloudFront + ACM, or a reverse proxy),
> and add MCP authorization. See [INFRASTRUCTURE.md](INFRASTRUCTURE.md).
