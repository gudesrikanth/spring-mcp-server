# CI/CD pipelines

Two GitHub Actions workflows, both authenticating to AWS with an **IAM access
key** (created by the bootstrap stack). They read these repo Actions
secrets/variables (from the bootstrap outputs):

| Name | Type | Used by | Value |
| --- | --- | --- | --- |
| `AWS_ACCESS_KEY_ID` | Secret | both | `aws_access_key_id` output |
| `AWS_SECRET_ACCESS_KEY` | Secret | both | `aws_secret_access_key` output |
| `AWS_REGION` | Secret | both | e.g. `us-east-1` |
| `TF_STATE_BUCKET` | Variable | infra | `state_bucket` output |
| `TF_LOCK_TABLE` | Variable | infra | `lock_table` output |

## 1. Infra — [`infra.yml`](../.github/workflows/infra.yml)

Provisions/updates AWS with Terraform.

| Trigger | Behaviour |
| --- | --- |
| PR touching `infra/terraform/**` | fmt → init → validate → **plan** (posted as a PR comment) |
| Push to `main` | fmt → init → validate → plan → **apply** |
| Manual (`workflow_dispatch`) | choose `plan`, `apply`, or `destroy` |

Steps: checkout → setup Terraform → **AWS credentials** → `fmt -check` →
`init` (S3 backend) → `validate` → `plan` → (`apply`/`destroy`) → print outputs.

## 2. App CI/CD — [`app-deploy.yml`](../.github/workflows/app-deploy.yml)

The application pipeline, one explicit step at a time.

**Job `build-test`** (runs on every push & PR):

1. Checkout
2. Set up **JDK 25** (Temurin) + Maven cache
3. **`./mvnw verify`** — unit tests + Testcontainers integration tests,
   including the real **MCP handshake** test
4. Upload test reports artifact

**Job `deploy`** (only on `main` / manual, after tests pass):

3. **Build Docker image** — tagged `:<git-sha>` and `:latest`
4. **Trivy scan** — fails the build on `HIGH`/`CRITICAL` vulnerabilities
5. **Push to ECR** — both tags
6. **Resolve deploy target** — find the running EC2 instance + its public IP
7. **Deploy via SSM** — a Run-Command on the instance does: ECR login →
   `docker pull` → stop old container → `docker run` new one on port 80
8. **Wait for health** — polls `/actuator/health`
9. **Functional test** — runs [`scripts/smoke-test.sh`](../scripts/smoke-test.sh)
   against the live endpoint (real `initialize` → `tools/list` → `tools/call`)

A job summary prints the image tag and the live MCP endpoint.

## Why this shape?

- **Tests gate everything** — `deploy` `needs: build-test`.
- **Scan before push** — no vulnerable image reaches the registry or the server.
- **Deploy by pull, not push** — the instance pulls from ECR over SSM; nothing
  SSHes in.
- **Verify in production** — the same smoke test you run locally also gates the
  deploy, so a green pipeline means a genuinely working server.

## First-time setup checklist

1. Run the [bootstrap](../infra/terraform/bootstrap/README.md).
2. Add the Actions secrets + variables above.
3. Push to `main` → Infra workflow runs → then App CI/CD deploys.
4. Open the endpoint from the job summary and connect a client
   ([HOW-CLIENTS-CONNECT.md](HOW-CLIENTS-CONNECT.md)).
