# Example clients

Ready-to-use ways to connect to this MCP server. Full prose walkthrough:
[../docs/HOW-CLIENTS-CONNECT.md](../docs/HOW-CLIENTS-CONNECT.md).

| File | Client |
| --- | --- |
| [`python-client/`](python-client/) | Runnable Python client (official `mcp` SDK) |
| [`claude-desktop-config.json`](claude-desktop-config.json) | Claude Desktop config (via `mcp-remote`) |

Other options that need no files here:

- **MCP Inspector:** `npx @modelcontextprotocol/inspector` → connect to
  `http://localhost:8080/mcp` (Streamable HTTP).
- **Claude Code:** `claude mcp add --transport http spring-mcp http://localhost:8080/mcp`
- **Java:** see
  [`McpServerIntegrationTest`](../src/test/java/com/portfolio/mcp/McpServerIntegrationTest.java),
  which is a working Java client.
- **curl:** see [`../scripts/smoke-test.sh`](../scripts/smoke-test.sh).
