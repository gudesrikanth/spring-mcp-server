package com.portfolio.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP <em>prompts</em> — reusable, parameterized instructions a client can
 * fetch and hand to an LLM. Prompts are how an MCP server ships "recipes" that
 * tie its tools together; the host application surfaces them (e.g. as slash
 * commands) and the model then calls {@code create_task} / {@code list_tasks}.
 */
@Component
public class TaskPrompts {

    @McpPrompt(name = "plan_my_day",
            description = "Generate a focused daily plan from the user's open tasks.")
    public GetPromptResult planMyDay(
            @McpArg(name = "focus", description = "Optional theme to prioritise, e.g. 'deep work'",
                    required = false) String focus) {
        String focusLine = (focus == null || focus.isBlank())
                ? ""
                : " Prioritise anything related to: " + focus + ".";
        String text = """
                You are my planning assistant. Use the `list_tasks` tool with status OPEN to \
                read my current tasks, then propose a realistic plan for today.%s
                Group the work into Morning / Afternoon / Evening, call out the single most \
                important task, and suggest which tasks to defer if there are too many.""".formatted(focusLine);
        return userPrompt("Plan my day from my open tasks.", text);
    }

    @McpPrompt(name = "triage_tasks",
            description = "Review all tasks and suggest which to do, defer, or delete.")
    public GetPromptResult triageTasks() {
        String text = """
                Act as a productivity coach. Call `list_tasks` (no status filter) to load every \
                task. For each one recommend an action — DO NOW, SCHEDULE, DELEGATE, or DELETE — \
                with a one-line justification. Where a task should be removed, remind me you can \
                call `delete_task` with its id.""";
        return userPrompt("Triage all my tasks.", text);
    }

    private static GetPromptResult userPrompt(String description, String instruction) {
        return new GetPromptResult(
                description,
                List.of(new PromptMessage(Role.USER, new TextContent(instruction))));
    }
}
