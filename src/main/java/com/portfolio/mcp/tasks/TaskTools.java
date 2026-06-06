package com.portfolio.mcp.tasks;

import com.portfolio.mcp.tasks.Task.TaskView;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * MCP tools for managing tasks, backed by DynamoDB.
 *
 * <p>These demonstrate stateful tools that mutate a real datastore — the
 * counterpart to the stateless {@code ToolboxTools}. Together with
 * {@link TaskResources} (read tasks as resources) and {@code TaskPrompts}
 * (LLM prompt templates) they show all three MCP server primitives over a
 * single domain.
 */
@Service
public class TaskTools {

    /** Allowed task states. */
    private static final List<String> STATUSES = List.of("OPEN", "DONE");

    private final TaskRepository repository;

    public TaskTools(TaskRepository repository) {
        this.repository = repository;
    }

    @McpTool(name = "create_task", description = "Create a new task. Returns the created task "
            + "including its generated id.")
    public TaskView createTask(
            @McpToolParam(description = "Short title of the task") String title,
            @McpToolParam(description = "Optional due date as ISO-8601, e.g. 2026-06-30", required = false)
            String dueDate) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Task task = new Task();
        task.setId(UUID.randomUUID().toString());
        task.setTitle(title.trim());
        task.setStatus("OPEN");
        task.setDueDate(dueDate == null || dueDate.isBlank() ? null : dueDate.trim());
        task.setCreatedAt(Instant.now().toString());
        return TaskView.from(repository.save(task));
    }

    @McpTool(name = "list_tasks", description = "List tasks, optionally filtered by status "
            + "(OPEN or DONE). Omit the status to list all tasks.")
    public List<TaskView> listTasks(
            @McpToolParam(description = "Filter: OPEN or DONE. Leave empty for all.", required = false)
            String status) {
        List<Task> tasks = (status == null || status.isBlank())
                ? repository.findAll()
                : repository.findByStatus(status.trim());
        return tasks.stream().map(TaskView::from).toList();
    }

    @McpTool(name = "get_task", description = "Fetch a single task by its id.")
    public TaskView getTask(
            @McpToolParam(description = "The task id (UUID)") String id) {
        return repository.findById(id)
                .map(TaskView::from)
                .orElseThrow(() -> new IllegalArgumentException("No task with id " + id));
    }

    @McpTool(name = "complete_task", description = "Mark a task as DONE. Returns the updated task.")
    public TaskView completeTask(
            @McpToolParam(description = "The task id (UUID)") String id) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No task with id " + id));
        task.setStatus("DONE");
        return TaskView.from(repository.save(task));
    }

    @McpTool(name = "delete_task", description = "Delete a task by id. Returns true if a task "
            + "was deleted.")
    public boolean deleteTask(
            @McpToolParam(description = "The task id (UUID)") String id) {
        return repository.deleteById(id).isPresent();
    }
}
