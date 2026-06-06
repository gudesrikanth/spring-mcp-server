# Python MCP client example

A small, runnable client using the official [`mcp`](https://pypi.org/project/mcp/)
Python SDK and the Streamable HTTP transport.

## Run

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

# Local server (docker compose up)
python client.py http://localhost:8080/mcp

# Deployed server
python client.py http://<elastic-ip>/mcp
```

## What it does

1. `initialize` (opens a session)
2. lists tools, resources, resource templates, and prompts
3. calls `gen_uuid`
4. calls `create_task` then `list_tasks`
5. reads the `tasks://all` resource

Use it as a starting point for your own MCP-powered application.
