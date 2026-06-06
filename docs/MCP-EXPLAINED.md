# MCP, explained (with this server as the example)

## What is MCP?

The **Model Context Protocol (MCP)** is an open standard that lets AI
applications (the **host**, e.g. Claude Desktop) connect to external
capabilities through a uniform interface. Instead of every app inventing its own
plugin format, a tool provider runs an **MCP server**, and any **MCP client**
can discover and use it.

```
  Host application (Claude Desktop, Claude Code, your app)
        │ embeds one or more
        ▼
   MCP client  ──────────────►  MCP server  (this project)
        ◄──────────────         exposes tools / resources / prompts
                JSON-RPC 2.0
```

- **Host** — the AI app the user interacts with.
- **Client** — the connector inside the host that speaks MCP to one server.
- **Server** — exposes capabilities. *That's what we built.*

## The three server primitives

| Primitive    | Mental model        | Who decides to use it             | In this server |
| ------------ | ------------------- | --------------------------------- | -------------- |
| **Tool**     | a function / verb   | the **model** (it calls them)     | `gen_uuid`, `create_task`, … |
| **Resource** | a file / noun (URI) | the **app/user** (attach context) | `tasks://all`, `task://{id}` |
| **Prompt**   | a slash-command     | the **user** (invokes them)       | `plan_my_day`, `triage_tasks` |

In Spring AI these are just annotated methods — see
[`ToolboxTools.java`](../src/main/java/com/portfolio/mcp/toolbox/ToolboxTools.java),
[`TaskResources.java`](../src/main/java/com/portfolio/mcp/tasks/TaskResources.java),
and [`TaskPrompts.java`](../src/main/java/com/portfolio/mcp/prompts/TaskPrompts.java).

## Transport: Streamable HTTP

MCP defines two standard transports:

- **stdio** — the server is a subprocess; messages go over stdin/stdout. Great
  for local CLIs.
- **Streamable HTTP** — the server is reachable over HTTP at a single endpoint
  (here `POST /mcp`); responses may stream back as Server-Sent Events. This is
  what lets clients connect to a **remote** server — which is exactly why we use
  it.

This server uses Streamable HTTP (`spring.ai.mcp.server.protocol=streamable`),
so it can run on EC2 and be reached from anywhere.

## The message flow

MCP is **JSON-RPC 2.0**. A typical session:

```
client ──> initialize                         (version + capabilities handshake)
client <── result: serverInfo, capabilities
client ──> notifications/initialized          (handshake complete)
client ──> tools/list                         (discover tools)
client <── result: [ {name, description, inputSchema}, … ]
client ──> tools/call  {name, arguments}      (invoke a tool)
client <── result: { content: [ … ], structuredContent }
```

Resources use `resources/list` + `resources/read`; prompts use `prompts/list` +
`prompts/get`. You can watch all of this happen with the MCP Inspector or by
reading [`scripts/smoke-test.sh`](../scripts/smoke-test.sh), which performs the
whole flow with `curl`.

### A real `tools/call` against this server

Request:

```json
{ "jsonrpc": "2.0", "id": 3, "method": "tools/call",
  "params": { "name": "gen_uuid", "arguments": {} } }
```

Response (framed as an SSE `data:` line over Streamable HTTP):

```json
{ "jsonrpc": "2.0", "id": 3,
  "result": { "content": [ { "type": "text", "text": "2ee5df87-…" } ] } }
```

## Where to go next

- Connect a client: [HOW-CLIENTS-CONNECT.md](HOW-CLIENTS-CONNECT.md)
- See how the code is organised: [ARCHITECTURE.md](ARCHITECTURE.md)
- Spec & SDKs: <https://modelcontextprotocol.io>
