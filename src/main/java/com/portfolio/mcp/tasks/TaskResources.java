package com.portfolio.mcp.tasks;

import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exposes tasks as MCP <em>resources</em>.
 *
 * <p>Where tools are "verbs" the model can invoke, resources are "nouns" the
 * client can read by URI. A client lists resources, then reads one via
 * {@code resources/read}. We publish two:
 * <ul>
 *   <li>{@code tasks://all} – a JSON array of every task;</li>
 *   <li>{@code task://{id}} – a single task by id (URI template).</li>
 * </ul>
 *
 * <p>The {@code id} method parameter is bound from the {@code {id}} variable in
 * the URI template by name, which is why the project compiles with
 * {@code -parameters} (configured by the Spring Boot parent).
 */
@Component
public class TaskResources {

    private final TaskRepository repository;

    public TaskResources(TaskRepository repository) {
        this.repository = repository;
    }

    @McpResource(
            uri = "tasks://all",
            name = "all_tasks",
            description = "All tasks as a JSON array",
            mimeType = "application/json")
    public ReadResourceResult allTasks() {
        String json = repository.findAll().stream()
                .map(TaskResources::toJson)
                .collect(Collectors.joining(",", "[", "]"));
        return new ReadResourceResult(
                List.of(new TextResourceContents("tasks://all", "application/json", json)));
    }

    @McpResource(
            uri = "task://{id}",
            name = "task_by_id",
            description = "A single task by its id",
            mimeType = "application/json")
    public ReadResourceResult taskById(String id) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No task with id " + id));
        return new ReadResourceResult(
                List.of(new TextResourceContents("task://" + id, "application/json", toJson(task))));
    }

    private static String toJson(Task t) {
        return """
                {"id":"%s","title":"%s","status":"%s","dueDate":%s,"createdAt":"%s"}"""
                .formatted(
                        t.getId(),
                        escape(t.getTitle()),
                        t.getStatus(),
                        t.getDueDate() == null ? "null" : "\"" + escape(t.getDueDate()) + "\"",
                        t.getCreatedAt());
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
