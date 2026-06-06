"""Minimal MCP client for the Spring AI MCP server (Streamable HTTP).

Usage:
    python client.py [BASE_MCP_URL]

Examples:
    python client.py http://localhost:8080/mcp
    python client.py http://<elastic-ip>/mcp

It connects, lists the server's tools / resources / prompts, then exercises a
couple of tools (gen_uuid, create_task, list_tasks) and prints the results.
"""

import asyncio
import sys

from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client


async def main(url: str) -> None:
    print(f"Connecting to {url}\n")
    async with streamablehttp_client(url) as (read, write, _get_session_id):
        async with ClientSession(read, write) as session:
            # 1. Handshake.
            init = await session.initialize()
            print(f"Connected to: {init.serverInfo.name} v{init.serverInfo.version}\n")

            # 2. Discover capabilities.
            tools = await session.list_tools()
            print("Tools:")
            for tool in tools.tools:
                print(f"  - {tool.name}: {tool.description}")

            resources = await session.list_resources()
            print("\nResources:")
            for res in resources.resources:
                print(f"  - {res.uri}")

            templates = await session.list_resource_templates()
            for tmpl in templates.resourceTemplates:
                print(f"  - {tmpl.uriTemplate} (template)")

            prompts = await session.list_prompts()
            print("\nPrompts:")
            for prompt in prompts.prompts:
                print(f"  - {prompt.name}: {prompt.description}")

            # 3. Call a stateless tool.
            print("\n--- tools/call gen_uuid ---")
            uuid_result = await session.call_tool("gen_uuid", {})
            print(_text(uuid_result))

            # 4. Call DynamoDB-backed tools.
            print("\n--- tools/call create_task ---")
            created = await session.call_tool(
                "create_task", {"title": "Task created from the Python client"}
            )
            print(_text(created))

            print("\n--- tools/call list_tasks ---")
            listed = await session.call_tool("list_tasks", {})
            print(_text(listed))

            # 5. Read a resource.
            print("\n--- resources/read tasks://all ---")
            all_tasks = await session.read_resource("tasks://all")
            for content in all_tasks.contents:
                print(getattr(content, "text", content))


def _text(result) -> str:
    """Join the text content blocks of a tool result."""
    parts = []
    for block in result.content:
        parts.append(getattr(block, "text", str(block)))
    return "\n".join(parts) if parts else "(no text content)"


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/mcp"
    asyncio.run(main(target))
