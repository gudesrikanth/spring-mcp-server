package com.portfolio.mcp.tasks;

import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;

/**
 * Thin persistence layer over the {@code mcp-tasks} DynamoDB table.
 *
 * <p>Kept deliberately small: the MCP tool layer ({@link TaskTools}) holds the
 * business rules, this class only does CRUD against DynamoDB.
 */
@Repository
public class TaskRepository {

    private final DynamoDbTable<Task> table;

    public TaskRepository(DynamoDbTable<Task> table) {
        this.table = table;
    }

    public Task save(Task task) {
        table.putItem(task);
        return task;
    }

    public Optional<Task> findById(String id) {
        return Optional.ofNullable(table.getItem(Key.builder().partitionValue(id).build()));
    }

    public List<Task> findAll() {
        return table.scan().items().stream().toList();
    }

    public List<Task> findByStatus(String status) {
        return table.scan().items().stream()
                .filter(t -> status.equalsIgnoreCase(t.getStatus()))
                .toList();
    }

    public Optional<Task> deleteById(String id) {
        Task deleted = table.deleteItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(deleted);
    }
}
