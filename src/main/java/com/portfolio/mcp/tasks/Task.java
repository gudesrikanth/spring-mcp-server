package com.portfolio.mcp.tasks;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * A task persisted in DynamoDB.
 *
 * <p>This is a mutable JavaBean because the DynamoDB Enhanced Client's
 * {@link DynamoDbBean} mapper requires a no-args constructor plus getters and
 * setters. The MCP tools convert it to/from the immutable {@code TaskView}
 * record before returning it to clients.
 */
@DynamoDbBean
public class Task {

    private String id;
    private String title;
    private String status;
    private String dueDate;
    private String createdAt;

    public Task() {
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /** Immutable projection returned to MCP clients (serialized to JSON). */
    public record TaskView(String id, String title, String status, String dueDate, String createdAt) {
        static TaskView from(Task t) {
            return new TaskView(t.getId(), t.getTitle(), t.getStatus(), t.getDueDate(), t.getCreatedAt());
        }
    }
}
