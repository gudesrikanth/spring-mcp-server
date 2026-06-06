# Spring AI MCP Server — a hands-on, deployable example

[![App CI/CD](https://github.com/gudesrikanth/spring-mcp-server/actions/workflows/app-deploy.yml/badge.svg)](https://github.com/gudesrikanth/spring-mcp-server/actions/workflows/app-deploy.yml)
[![Infra](https://github.com/gudesrikanth/spring-mcp-server/actions/workflows/infra.yml/badge.svg)](https://github.com/gudesrikanth/spring-mcp-server/actions/workflows/infra.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A complete, production-shaped **Model Context Protocol (MCP) server** built with
**Spring AI** — created to *learn MCP by doing*: what a server exposes, how it
speaks the protocol, and **how and from where clients connect to it**. It comes
with everything a portfolio project should have: tests, Docker, AWS Free-Tier
infrastructure as code, and CI/CD.

> **New to MCP?** Start with [docs/MCP-EXPLAINED.md](docs/MCP-EXPLAINED.md).
> **Just want to call it?** Jump to [docs/HOW-CLIENTS-CONNECT.md](docs/HOW-CLIENTS-CONNECT.md).

---

## What it does

The server exposes all three MCP server primitives over one small domain:

| Primitive     | Examples                                                                 |
| ------------- | ------------------------------------------------------------------------ |
| **Tools**     | `gen_uuid`, `base64_encode/decode`, `hash`, `json_pretty`, `regex_test`, `to_epoch`/`from_epoch` (stateless toolbox) and `create_task`, `list_tasks`, `get_task`, `complete_task`, `delete_task` (DynamoDB-backed) |
| **Resources** | `tasks://all` and the templated `task://{id}`                             |
| **Prompts**   | `plan_my_day`, `triage_tasks`                                            |

The **toolbox** tools are pure functions (great for understanding `tools/call`),
while the **task** tools persist to **DynamoDB** — showing a real, stateful MCP
server.

## Tech stack

- **Java 25**, **Spring Boot 4**, **Spring AI 2.0** MCP server starter
  (`@McpTool` / `@McpResource` / `@McpPrompt` annotations)
- **Streamable HTTP** transport — the MCP endpoint is `POST /mcp`
- **DynamoDB** (AWS SDK v2 enhanced client) for task storage
- **Docker** (multi-stage) + **docker-compose** for a one-command local stack
- **Terraform** for AWS Free-Tier infra (ECR, EC2 `t3.micro`, DynamoDB, IAM, EIP)
- **GitHub Actions** with **OIDC** (no static AWS keys): Terraform pipeline +
  build → test → scan → push → deploy → functional-test pipeline

## Architecture

```
        MCP clients (call from anywhere)
  Inspector · Claude Desktop · Claude Code · Python/Java SDK · curl
                       │  Streamable HTTP (JSON-RPC 2.0)
                       ▼
        http://<elastic-ip>/mcp
   ┌──────────────────────────────────────┐
   │ EC2 t3.micro (Docker)                 │
   │   spring-mcp-server  :8080 → host :80 │
   │   @McpTool / @McpResource / @McpPrompt│
   │   Actuator /actuator/health           │
   └───────────────┬──────────────────────┘
                   │ AWS SDK v2
                   ▼
            DynamoDB  "mcp-tasks"
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detail.

## Quickstart (local, no AWS)

Requirements: Docker, and for development JDK 25 + the bundled Maven wrapper.

```bash
# 1. Start the server + a local DynamoDB
docker compose up --build

# 2. In another terminal, run the functional smoke test
./scripts/smoke-test.sh http://localhost:8080
```

The MCP endpoint is now at **http://localhost:8080/mcp**. Point any MCP client at
it (see below). To explore interactively:

```bash
npx @modelcontextprotocol/inspector
# then connect to http://localhost:8080/mcp (transport: Streamable HTTP)
```

### Build & test from source

```bash
./mvnw verify        # unit + Testcontainers integration tests (needs Docker)
./mvnw spring-boot:run   # run directly (set APP_DYNAMODB_ENDPOINT for local DynamoDB)
```

## How clients connect

This is the heart of the project. Full, copy-pasteable instructions for **MCP
Inspector**, **Claude Desktop**, **Claude Code**, a **Python** client, a
**Java** client, and raw **curl** — for both local and the deployed server — are
in **[docs/HOW-CLIENTS-CONNECT.md](docs/HOW-CLIENTS-CONNECT.md)**. Runnable
examples live in [`clients/`](clients/).

## Deploy to AWS (Free Tier)

1. **Bootstrap once** (state backend + GitHub OIDC role):
   [`infra/terraform/bootstrap/README.md`](infra/terraform/bootstrap/README.md).
2. Add the resulting values as GitHub Actions **variables**
   (`AWS_ROLE_ARN`, `AWS_REGION`, `TF_STATE_BUCKET`, `TF_LOCK_TABLE`).
3. Push to `main`: the **Infra** workflow provisions AWS, then **App CI/CD**
   builds, scans, pushes, deploys, and smoke-tests the live server.

Details: [docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md) · [docs/CICD.md](docs/CICD.md).

## Documentation

| Doc | What's inside |
| --- | --- |
| [MCP-EXPLAINED.md](docs/MCP-EXPLAINED.md) | What MCP is; the JSON-RPC message flow; tools vs resources vs prompts |
| [HOW-CLIENTS-CONNECT.md](docs/HOW-CLIENTS-CONNECT.md) | Every way to call this server, with config snippets |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Code layout and request lifecycle |
| [INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md) | The AWS resources and why each exists |
| [CICD.md](docs/CICD.md) | The two pipelines, step by step |
| [LOCAL-DEVELOPMENT.md](docs/LOCAL-DEVELOPMENT.md) | Dev loop, gotchas (incl. Docker/Java tips) |
| [COST-AND-CLEANUP.md](docs/COST-AND-CLEANUP.md) | Free-tier limits and full teardown |

## License

MIT — see [LICENSE](LICENSE).
