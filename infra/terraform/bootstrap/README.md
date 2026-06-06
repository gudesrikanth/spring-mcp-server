# Bootstrap (run once)

This stack creates the prerequisites that the main stack and CI rely on:

- An **S3 bucket** + **DynamoDB lock table** for remote Terraform state.
- A **GitHub OIDC provider** and an **IAM role** GitHub Actions assumes — so no
  long-lived AWS keys are ever stored in GitHub.

It is the only part you run from your laptop, and it uses **local state** (kept
on your machine; never committed). You typically run it exactly once.

## Prerequisites

- AWS CLI configured with admin-ish credentials (`aws configure` / SSO).
- Terraform >= 1.6 (`brew install hashicorp/tap/terraform`).

## Run it

```bash
cd infra/terraform/bootstrap

terraform init

terraform apply -var="github_repo=YOUR_GH_USERNAME/spring-mcp-server"
```

When it finishes, note the outputs:

```bash
terraform output
```

You'll get four values. Add three of them to your GitHub repository
(**Settings → Secrets and variables → Actions → Variables**):

| Output                    | GitHub Actions variable | Used by                         |
| ------------------------- | ----------------------- | ------------------------------- |
| `github_actions_role_arn` | `AWS_ROLE_ARN`          | both workflows (OIDC login)     |
| `region`                  | `AWS_REGION`            | both workflows                  |
| `state_bucket`            | `TF_STATE_BUCKET`       | `infra.yml` backend config      |
| `lock_table`              | `TF_LOCK_TABLE`         | `infra.yml` backend config      |

That's it — from now on everything happens through the GitHub Actions
workflows. See [`../README.md`](../README.md) for the main stack and
[`../../../docs/CICD.md`](../../../docs/CICD.md) for the pipelines.

## Tearing down

Destroy the main stack first (`infra.yml` has a `destroy` action, or run
`terraform destroy` in `../`), then:

```bash
terraform destroy -var="github_repo=YOUR_GH_USERNAME/spring-mcp-server"
```
