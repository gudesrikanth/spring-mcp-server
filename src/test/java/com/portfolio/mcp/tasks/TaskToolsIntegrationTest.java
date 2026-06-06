package com.portfolio.mcp.tasks;

import com.portfolio.mcp.tasks.Task.TaskView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the Task MCP tools against a real (containerised) DynamoDB Local
 * instance, so the DynamoDB Enhanced Client wiring is covered end-to-end
 * without touching AWS. Requires Docker to be running.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class TaskToolsIntegrationTest {

    @Container
    static final GenericContainer<?> DYNAMO =
            new GenericContainer<>("amazon/dynamodb-local:2.5.2").withExposedPorts(8000);

    @DynamicPropertySource
    static void dynamoProperties(DynamicPropertyRegistry registry) {
        registry.add("app.dynamodb.endpoint",
                () -> "http://localhost:" + DYNAMO.getMappedPort(8000));
        registry.add("app.dynamodb.region", () -> "us-east-1");
    }

    @Autowired
    TaskTools taskTools;

    @Test
    void create_list_complete_delete_lifecycle() {
        TaskView created = taskTools.createTask("Write MCP docs", "2026-06-30");
        assertThat(created.id()).isNotBlank();
        assertThat(created.status()).isEqualTo("OPEN");
        assertThat(created.dueDate()).isEqualTo("2026-06-30");

        assertThat(taskTools.listTasks(null)).extracting(TaskView::id).contains(created.id());
        assertThat(taskTools.listTasks("OPEN")).extracting(TaskView::id).contains(created.id());
        assertThat(taskTools.listTasks("DONE")).extracting(TaskView::id).doesNotContain(created.id());

        TaskView completed = taskTools.completeTask(created.id());
        assertThat(completed.status()).isEqualTo("DONE");
        assertThat(taskTools.listTasks("DONE")).extracting(TaskView::id).contains(created.id());

        assertThat(taskTools.deleteTask(created.id())).isTrue();
        assertThatThrownBy(() -> taskTools.getTask(created.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTask_blankTitle_rejected() {
        assertThatThrownBy(() -> taskTools.createTask("  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }
}
