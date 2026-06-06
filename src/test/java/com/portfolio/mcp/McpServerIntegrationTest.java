package com.portfolio.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full MCP server over HTTP and drives it with a real MCP client
 * (the {@code modelcontextprotocol} Java SDK) over the Streamable HTTP
 * transport — the same protocol Claude Desktop, the MCP Inspector and other
 * clients use. This is the closest automated equivalent to a client connecting
 * to the deployed server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class McpServerIntegrationTest {

    @Container
    static final GenericContainer<?> DYNAMO =
            new GenericContainer<>("amazon/dynamodb-local:2.5.2").withExposedPorts(8000);

    @DynamicPropertySource
    static void dynamoProperties(DynamicPropertyRegistry registry) {
        registry.add("app.dynamodb.endpoint",
                () -> "http://localhost:" + DYNAMO.getMappedPort(8000));
        registry.add("app.dynamodb.region", () -> "us-east-1");
    }

    @LocalServerPort
    int port;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        var transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();
        client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(20))
                .build();
        InitializeResult init = client.initialize();
        assertThat(init.serverInfo().name()).isEqualTo("spring-mcp-server");
    }

    @AfterEach
    void disconnect() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    @Test
    void advertisesToolboxAndTaskTools() {
        List<String> toolNames = client.listTools().tools().stream().map(Tool::name).toList();
        assertThat(toolNames)
                .contains("gen_uuid", "base64_encode", "hash", "json_pretty", "regex_test")
                .contains("create_task", "list_tasks", "get_task", "complete_task", "delete_task");
    }

    @Test
    void callsStatelessToolboxTool() {
        CallToolResult result = client.callTool(
                new CallToolRequest("gen_uuid", Map.of()));
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.content()).isNotEmpty();
    }

    @Test
    void createsAndListsTasksThroughTools() {
        CallToolResult created = client.callTool(
                new CallToolRequest("create_task", Map.of("title", "Demo task via MCP")));
        assertThat(created.isError()).isNotEqualTo(Boolean.TRUE);

        CallToolResult listed = client.callTool(
                new CallToolRequest("list_tasks", Map.of()));
        assertThat(listed.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(listed.content()).isNotEmpty();
    }

    @Test
    void exposesResourcesAndTemplates() {
        List<String> resourceUris = client.listResources().resources().stream()
                .map(r -> r.uri()).toList();
        assertThat(resourceUris).contains("tasks://all");

        List<String> templates = client.listResourceTemplates().resourceTemplates().stream()
                .map(t -> t.uriTemplate()).toList();
        assertThat(templates).anyMatch(uri -> uri.contains("task://"));

        ReadResourceResult all = client.readResource(new ReadResourceRequest("tasks://all"));
        assertThat(all.contents()).isNotEmpty();
    }

    @Test
    void exposesPrompts() {
        List<String> promptNames = client.listPrompts().prompts().stream()
                .map(p -> p.name()).toList();
        assertThat(promptNames).contains("plan_my_day", "triage_tasks");

        GetPromptResult prompt = client.getPrompt(
                new GetPromptRequest("plan_my_day", Map.of()));
        assertThat(prompt.messages()).isNotEmpty();
    }
}
