# Local development

## Prerequisites

- **JDK 25** (the project targets Java 25)
- **Docker** (for `docker compose`, and for the Testcontainers integration tests)
- The bundled **Maven wrapper** (`./mvnw`) — no separate Maven install needed

## Everyday commands

```bash
# Run the whole stack (server + local DynamoDB)
docker compose up --build

# Functional smoke test against it
./scripts/smoke-test.sh http://localhost:8080

# Build + run all tests
./mvnw verify

# Run the app directly against a local DynamoDB you started yourself
APP_DYNAMODB_ENDPOINT=http://localhost:8000 ./mvnw spring-boot:run
```

## Project facts

- MCP endpoint: `POST /mcp` (Streamable HTTP)
- Health: `GET /actuator/health`
- DynamoDB table: `mcp-tasks` (auto-created locally; Terraform-managed in AWS)
- Local DynamoDB credentials are dummy static values set in `DynamoDbConfig`
  when `app.dynamodb.endpoint` is present.

## Gotchas (seen on macOS during development)

These are environment quirks, not project bugs — noting them saves time.

### 1. Maven must run on JDK 25

If `JAVA_HOME` points at an older JDK you'll see `release version 25 not
supported`. Point Maven at 25 for the session:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./mvnw verify
```

### 2. Testcontainers + a very new Docker Engine

Docker Engine 29 raised its minimum API version. If the bundled docker-java
client requests an older version you'll see *"Could not find a valid Docker
environment … Status 400"* during the integration tests. Force a supported API
version for the test JVM:

```bash
export DOCKER_HOST=unix://$HOME/.docker/run/docker.sock   # Docker Desktop socket
./mvnw test -DargLine="-Dapi.version=1.44"
```

(CI runners use a compatible Docker, so this override is only needed on some
local setups.)

### 3. Port 8080 already in use

If another app/container holds 8080, start the server on another port:

```bash
SERVER_PORT=8085 ./mvnw spring-boot:run
./scripts/smoke-test.sh http://localhost:8085
```

## Adding a new tool

Add a method to a `@Component` and annotate it — that's the whole registration:

```java
@McpTool(name = "reverse", description = "Reverse a string.")
public String reverse(@McpToolParam(description = "text") String text) {
    return new StringBuilder(text).reverse().toString();
}
```

Restart; it shows up in `tools/list` automatically. Add a unit test alongside
the existing ones in `src/test`.
