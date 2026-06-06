# Bootstrap (run once)

This stack creates the prerequisites that the main stack and CI rely on:

- An **S3 bucket** + **DynamoDB lock table** for remote Terraform state.
- A dedicated **IAM user + access key** that GitHub Actions uses to authenticate
  to AWS.

It is the only part you run from your laptop, and it uses **local state** (kept
on your machine; never committed). You typically run it exactly once.

> Uses long-lived access keys for simplicity. They are static secrets — keep them
> only in GitHub Actions secrets, and delete the IAM user (`terraform destroy`)
> when you're done with the project.

## Prerequisites

- AWS CLI configured with admin-ish credentials (`aws configure` / SSO).
- Terraform >= 1.6 **or** OpenTofu (`tofu`) — commands below use `tofu`; replace
  with `terraform` if you prefer (identical syntax).

## Run it

```bash
cd infra/terraform/bootstrap

tofu init
tofu apply
```

When it finishes, read the outputs:

```bash
tofu output                          # state_bucket, lock_table, region, aws_access_key_id
tofu output -raw aws_secret_access_key   # the secret (printed on its own)
```

Add them to your GitHub repository
(**Settings → Secrets and variables → Actions**):

| Output                  | GitHub Actions entry                | Type       | Used by                    |
| ----------------------- | ----------------------------------- | ---------- | -------------------------- |
| `aws_access_key_id`     | `AWS_ACCESS_KEY_ID`                 | **Secret** | both workflows             |
| `aws_secret_access_key` | `AWS_SECRET_ACCESS_KEY`             | **Secret** | both workflows             |
| `region`                | `AWS_REGION`                        | **Secret** | both workflows             |
| `state_bucket`          | `TF_STATE_BUCKET`                   | Variable   | `infra.yml` backend config |
| `lock_table`            | `TF_LOCK_TABLE`                     | Variable   | `infra.yml` backend config |

Or set them from the terminal with `gh`:

```bash
REPO=gudesrikanth/spring-mcp-server
gh secret   set AWS_ACCESS_KEY_ID     -R "$REPO" -b "$(tofu output -raw aws_access_key_id)"
gh secret   set AWS_SECRET_ACCESS_KEY -R "$REPO" -b "$(tofu output -raw aws_secret_access_key)"
gh secret   set AWS_REGION            -R "$REPO" -b "$(tofu output -raw region)"
gh variable set TF_STATE_BUCKET       -R "$REPO" -b "$(tofu output -raw state_bucket)"
gh variable set TF_LOCK_TABLE         -R "$REPO" -b "$(tofu output -raw lock_table)"
```

That's it — from now on everything happens through the GitHub Actions workflows.
See [`../README.md`](../README.md) and [`../../../docs/CICD.md`](../../../docs/CICD.md).

## Tearing down

Destroy the main stack first (`infra.yml` `destroy` action, or `terraform
destroy` in `../`), then:

```bash
tofu destroy
```
