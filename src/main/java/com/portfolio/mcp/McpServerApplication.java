package com.portfolio.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring AI MCP server.
 *
 * <p>The {@code spring-ai-starter-mcp-server-webmvc} starter auto-configures a
 * Model Context Protocol server using the Streamable HTTP transport. Every bean
 * method annotated with {@code @McpTool}, {@code @McpResource} or
 * {@code @McpPrompt} is discovered and advertised to connected clients.
 *
 * <p>By default the MCP endpoint is exposed at {@code /mcp}.
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
