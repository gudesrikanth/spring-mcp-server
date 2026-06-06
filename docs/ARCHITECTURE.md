# Architecture

## Code layout

```
src/main/java/com/portfolio/mcp/
├── McpServerApplication.java      # Spring Boot entry point
├── config/
│   └── DynamoDbConfig.java        # DynamoDb(Enhanced)Client beans; local endpoint + table bootstrap
├── toolbox/
│   └── ToolboxTools.java          # @McpTool stateless utilities
├── tasks/
│   ├── Task.java                  # @DynamoDbBean entity + immutable TaskView record
│   ├── TaskRepository.java        # CRUD over the enhanced client
│   ├── TaskTools.java             # @McpTool task operations
│   └── TaskResources.java         # @McpResource  tasks://all and task://{id}
└── prompts/
    └── TaskPrompts.java           # @McpPrompt plan_my_day, triage_tasks
```

## How a tool call flows

```
MCP client ──POST /mcp──► Spring AI MCP WebMVC router function
                          │ parse JSON-RPC, route "tools/call"
                          ▼
                  annotation dispatcher  ──► TaskTools.createTask(...)
                          │                      │
                          │                      ▼
                          │               TaskRepository ──► DynamoDB
                          ◄── result (auto-serialized to JSON) ──┘
   client ◄──SSE data:──── JSON-RPC result
```

The `spring-ai-starter-mcp-server-webmvc` starter auto-configures:

- the **router function** that serves `POST /mcp` (Streamable HTTP),
- an **annotation scanner** that finds every `@McpTool` / `@McpResource` /
  `@McpPrompt` bean and registers it,
- **JSON schema generation** from method parameters, so clients get typed tool
  inputs for free.

No manual registration code is needed — adding a new tool is just adding an
annotated method to a `@Component`.

## Configuration

Key settings in [`application.yml`](../src/main/resources/application.yml):

| Property | Meaning |
| --- | --- |
| `spring.ai.mcp.server.protocol=streamable` | Use Streamable HTTP transport |
| `spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp` | Endpoint path |
| `spring.ai.mcp.server.name` / `.version` / `.instructions` | Advertised in `initialize` |
| `app.dynamodb.endpoint` | Local DynamoDB override (empty in AWS) |
| `app.dynamodb.table-name` | Task table (`mcp-tasks`) |
| `management.endpoints.web.exposure.include=health,info` | Actuator for health checks |

## Environments

| Concern        | Local (`docker compose`)        | AWS (deployed)                          |
| -------------- | ------------------------------- | --------------------------------------- |
| DynamoDB       | `amazon/dynamodb-local` container | DynamoDB table `mcp-tasks`             |
| Credentials    | dummy static (set in config)    | EC2 instance role (default chain)       |
| Endpoint       | `localhost:8080/mcp`            | `http://<eip>/mcp` (host :80 → app :8080) |
| Table creation | auto-created on startup          | created by Terraform                    |
